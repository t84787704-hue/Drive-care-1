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
    fun openWhatsAppChat(
        context: Context,
        rawPhoneNumber: String,
        vehicleName: String = ""
    ) {
        val cleanPhone = rawPhoneNumber
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Owner phone number is not available", Toast.LENGTH_SHORT).show()
            return
        }

        val message = if (vehicleName.isNotBlank()) {
            "Hello, regarding vehicle $vehicleName on DriveCare app."
        } else {
            "Hello, regarding vehicle on DriveCare app."
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
