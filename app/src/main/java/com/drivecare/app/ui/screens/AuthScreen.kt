package com.drivecare.app.ui.screens

import android.accounts.Account
import android.accounts.AccountManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.drivecare.app.ui.DriveCareViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: DriveCareViewModel,
    modifier: Modifier = Modifier,
    onAuthSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }
    var resetEmailInput by remember { mutableStateOf("") }

    val deviceAccounts = remember(context) {
        try {
            val am = AccountManager.get(context)
            am.getAccountsByType("com.google").toList()
        } catch (_: Exception) {
            emptyList<Account>()
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }

    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        var selectedEmail: String? = null
        var selectedName: String? = null
        var idToken: String? = null

        // 1. AccountManager KEY_ACCOUNT_NAME
        if (intentData != null) {
            selectedEmail = intentData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (selectedEmail.isNullOrBlank()) {
                val extras = intentData.extras
                selectedEmail = extras?.getString(AccountManager.KEY_ACCOUNT_NAME)
            }
        }

        // 2. GoogleSignIn Task result
        if (selectedEmail.isNullOrBlank()) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intentData)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null && !account.email.isNullOrBlank()) {
                    selectedEmail = account.email
                    selectedName = account.displayName
                    idToken = account.idToken
                }
            } catch (e: Exception) {
                Log.w("AuthScreen", "GoogleSignIn exception: ${e.message}")
            }
        }

        // 3. Direct task result fallback
        if (selectedEmail.isNullOrBlank()) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intentData)
                val acc = task.result
                if (acc != null && !acc.email.isNullOrBlank()) {
                    selectedEmail = acc.email
                    selectedName = acc.displayName
                    idToken = acc.idToken
                }
            } catch (_: Exception) {}
        }

        // 4. Last signed in account
        if (selectedEmail.isNullOrBlank()) {
            try {
                val lastAcc = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAcc != null && !lastAcc.email.isNullOrBlank()) {
                    selectedEmail = lastAcc.email
                    selectedName = lastAcc.displayName
                    idToken = lastAcc.idToken
                }
            } catch (_: Exception) {}
        }

        if (!selectedEmail.isNullOrBlank()) {
            val finalEmail = selectedEmail
            val finalName = selectedName?.ifBlank { finalEmail.substringBefore("@") }
                ?: finalEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }

            viewModel.signInWithGoogleAccount(idToken, finalEmail, finalName) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Signed in as $finalEmail", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    Toast.makeText(context, msg ?: "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            googleEmailInput = if (deviceAccounts.isNotEmpty()) deviceAccounts[0].name else (if (email.contains("@")) email else "user.drive@gmail.com")
            googleNameInput = fullName.ifBlank { googleEmailInput.substringBefore("@") }
            showGoogleDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App / Logo Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.size(80.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRegisterMode) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isRegisterMode) "Sign up for DriveCare Cloud Sync" else "Sign in to access your DriveCare cloud backup",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = null },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email / Gmail Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password (min 6 characters)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                if (!isRegisterMode) {
                    TextButton(
                        onClick = {
                            resetEmailInput = email
                            showForgotPasswordDialog = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?")
                    }
                }

                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPass = password.trim()
                        val cleanName = fullName.trim()

                        if (cleanEmail.isBlank() || cleanPass.isBlank() || (isRegisterMode && cleanName.isBlank())) {
                            errorMessage = "Please fill in all fields / Tamam khaney por karain"
                            return@Button
                        }
                        if (cleanPass.length < 6) {
                            errorMessage = "Password must be at least 6 characters / Password kam az kam 6 huroof ka hona chahiye"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        if (isRegisterMode) {
                            viewModel.signUpWithEmail(cleanEmail, cleanPass, cleanName) { success, msg ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                } else {
                                    errorMessage = msg ?: "Registration failed / Account nahi ban saka"
                                }
                            }
                        } else {
                            viewModel.signInWithEmail(cleanEmail, cleanPass) { success, msg ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                } else {
                                    errorMessage = msg ?: "Authentication failed / Sign in nahi ho saka"
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isRegisterMode) "Register / Create Account" else "Sign In")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = " OR ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        OutlinedButton(
            onClick = {
                try {
                    val chooserIntent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        arrayOf("com.google"),
                        true,
                        null,
                        null,
                        null,
                        null
                    )
                    googleLauncher.launch(chooserIntent)
                } catch (e: Exception) {
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } catch (_: Exception) {
                        val defaultEmail = if (deviceAccounts.isNotEmpty()) deviceAccounts[0].name else (if (email.contains("@")) email else "")
                        googleEmailInput = defaultEmail
                        googleNameInput = fullName.ifBlank { defaultEmail.substringBefore("@") }
                        showGoogleDialog = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In with Google Account")
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                errorMessage = null
            }
        ) {
            Text(
                if (isRegisterMode) "Already have an account? Sign In" else "Don't have an account? Create Account"
            )
        }
    }

    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GTranslate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Account Sign In")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (deviceAccounts.isNotEmpty()) {
                        Text(
                            text = "Select your Google Account / Account muntakhib karain:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        deviceAccounts.forEach { acc ->
                            Card(
                                onClick = {
                                    val gEmail = acc.name
                                    val gName = gEmail.substringBefore("@")
                                        .replace(".", " ")
                                        .replace("-", " ")
                                        .replace("_", " ")
                                        .split(" ")
                                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                    showGoogleDialog = false
                                    viewModel.signInWithGoogleAccount(gEmail, gName) { success, msg ->
                                        if (success) {
                                            Toast.makeText(context, "Signed in as $gEmail!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        } else {
                                            Toast.makeText(context, msg ?: "Sign in failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap to connect account / Sign in karain",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Or enter your Gmail address manually:",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Apna Gmail address yahan enter karain / Enter your Gmail address:")
                    }

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Gmail Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Name (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gEmail = googleEmailInput.trim()
                        if (gEmail.isBlank()) {
                            Toast.makeText(context, "Please enter your Gmail address / Sahi Gmail address daraj karain", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val gName = googleNameInput.trim().ifBlank { gEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } }
                        showGoogleDialog = false
                        viewModel.signInWithGoogleAccount(gEmail, gName) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "Signed in as $gEmail!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            } else {
                                Toast.makeText(context, msg ?: "Google sign in failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your email address to receive a password reset link.")
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isNotBlank()) {
                            viewModel.sendPasswordReset(resetEmailInput) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Password reset link sent to your email!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, msg ?: "Failed to send reset email", Toast.LENGTH_SHORT).show()
                                }
                                showForgotPasswordDialog = false
                            }
                        }
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
