package com.drivecare.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object WhatsAppHelper {

    fun formatPhoneNumberForWhatsApp(rawPhoneNumber: String): String {
        var clean = rawPhoneNumber
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        if (clean.startsWith("0")) {
            clean = "92" + clean.substring(1)
        } else if (clean.length == 10 && clean.startsWith("3")) {
            clean = "92" + clean
        }
        return clean
    }

    fun openWhatsAppChat(
        context: Context,
        rawPhoneNumber: String,
        email: String = "",
        vehicleName: String = "",
        customText: String = "Hi"
    ) {
        val cleanPhone = formatPhoneNumberForWhatsApp(rawPhoneNumber)

        if (cleanPhone.isBlank()) {
            showNoWhatsAppDialog(context, email)
            return
        }

        val message = if (vehicleName.isNotBlank()) {
            "Hi, regarding vehicle $vehicleName on DriveCare app."
        } else {
            customText.ifBlank { "Hi" }
        }

        val encodedText = Uri.encode(message)
        val url = "https://wa.me/$cleanPhone?text=$encodedText"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showNoWhatsAppDialog(context: Context, email: String) {
        val emailDisplay = if (email.isNotBlank()) email else "Not provided"
        android.app.AlertDialog.Builder(context)
            .setTitle("WhatsApp Not Available")
            .setMessage("This user hasn't added WhatsApp number yet. Contact via Email: $emailDisplay")
            .setPositiveButton("OK", null)
            .show()
    }
}

@Composable
fun WhatsAppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Chat on WhatsApp"
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF25D366),
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "WhatsApp Icon",
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

