package com.drivecare.app.ui.screens

import android.accounts.Account
import android.accounts.AccountManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.drivecare.app.R
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

    val defaultWebClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "258091011057-rb04ltip87cb9ntivshb45hjtgbjsheb.apps.googleusercontent.com"
        } catch (_: Exception) {
            "258091011057-rb04ltip87cb9ntivshb45hjtgbjsheb.apps.googleusercontent.com"
        }
    }

    val gso = remember(defaultWebClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(defaultWebClientId)
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
        var selectedPhotoUrl: String? = null
        var idToken: String? = null
        var googleApiExceptionMsg: String? = null

        // 1. Primary: Parse GoogleSignIn Task result from Intent
        val task = GoogleSignIn.getSignedInAccountFromIntent(intentData)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && !account.email.isNullOrBlank()) {
                selectedEmail = account.email
                selectedName = account.displayName
                selectedPhotoUrl = account.photoUrl?.toString()
                idToken = account.idToken
                Log.d("GOOGLE_SIGN_IN", "Successfully retrieved Google account: ${account.email}, idToken present: ${!idToken.isNullOrBlank()}")
            }
        } catch (e: ApiException) {
            googleApiExceptionMsg = "ApiException status code ${e.statusCode}: ${e.message}"
            Log.e("GOOGLE_SIGN_IN", "GoogleSignIn.getSignedInAccountFromIntent failed with $googleApiExceptionMsg", e)
        } catch (e: Exception) {
            googleApiExceptionMsg = e.message ?: "Unknown GoogleSignIn error"
            Log.e("GOOGLE_SIGN_IN", "GoogleSignIn.getSignedInAccountFromIntent exception: $googleApiExceptionMsg", e)
        }

        // 2. Fallback: Last signed-in account if task result was null
        if (selectedEmail.isNullOrBlank()) {
            try {
                val lastAcc = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAcc != null && !lastAcc.email.isNullOrBlank()) {
                    selectedEmail = lastAcc.email
                    selectedName = lastAcc.displayName
                    selectedPhotoUrl = lastAcc.photoUrl?.toString()
                    idToken = lastAcc.idToken
                }
            } catch (e: Exception) {
                Log.w("GOOGLE_SIGN_IN", "getLastSignedInAccount fallback error: ${e.message}")
            }
        }

        // 3. Fallback: AccountManager KEY_ACCOUNT_NAME
        if (selectedEmail.isNullOrBlank() && intentData != null) {
            selectedEmail = intentData.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                ?: intentData.extras?.getString(AccountManager.KEY_ACCOUNT_NAME)
        }

        val logOutput = """
            [GOOGLE SIGN IN]
            Selected Email: ${selectedEmail ?: "None"}
            ID Token: ${if (idToken.isNullOrBlank()) "NULL / MISSING" else "PRESENT (${idToken.take(15)}...)"}
            Firebase UID: Pending auth
            Project ID: 258091011057 (driveare-1734e)
            Exception: ${googleApiExceptionMsg ?: "None"}
        """.trimIndent()
        Log.i("GOOGLE_SIGN_IN", logOutput)

        if (!selectedEmail.isNullOrBlank()) {
            Log.i("GOOGLE_SIGN_IN", "[GOOGLE ACCOUNT SELECTED]\nEmail: $selectedEmail")
            if (!idToken.isNullOrBlank()) {
                Log.i("GOOGLE_SIGN_IN", "[ID TOKEN RECEIVED]\nToken: ${idToken.take(15)}...")
            }
            val finalEmail = selectedEmail
            val emailUsernameFallback = finalEmail.substringBefore("@")
                .replace(".", " ")
                .replace("-", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val finalName = selectedName?.ifBlank { null } ?: emailUsernameFallback

            viewModel.signInWithGoogleAccount(idToken, finalEmail, finalName, selectedPhotoUrl) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Signed in as $finalEmail", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    errorMessage = msg ?: "Google sign in failed"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            val failMsg = googleApiExceptionMsg ?: "Google Sign-In was canceled or failed to obtain credentials."
            errorMessage = failMsg
            Log.w("GOOGLE_SIGN_IN", "[GOOGLE SIGN IN CANCELLED OR FAILED] $failMsg")
            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
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
                Image(
                    painter = painterResource(id = R.drawable.ic_drivecare_emblem),
                    contentDescription = "DriveCare Emblem",
                    modifier = Modifier.size(54.dp)
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
                    Log.i("GOOGLE_SIGN_IN", "[GOOGLE SIGN IN START]\nLaunching Google Sign-In Intent with Web Client ID: $defaultWebClientId")
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            Log.e("GOOGLE_SIGN_IN", "Failed to launch google sign in intent", e)
                            Toast.makeText(context, "Cannot launch Google Sign-In intent: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    try {
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    } catch (ex: Exception) {
                        Log.e("GOOGLE_SIGN_IN", "Failed to launch google sign in intent fallback", ex)
                        Toast.makeText(context, "Cannot launch Google Sign-In intent: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
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
