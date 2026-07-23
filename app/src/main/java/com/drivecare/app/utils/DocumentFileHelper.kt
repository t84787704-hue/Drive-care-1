package com.drivecare.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class SavedFileInfo(
    val fileUriString: String,
    val mimeType: String,
    val fileSize: Long,
    val fileName: String
)

object DocumentFileHelper {

    fun saveFileToInternalStorage(context: Context, sourceUri: Uri): SavedFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = getFileNameFromUri(context, sourceUri)
            var mimeType = contentResolver.getType(sourceUri) ?: ""

            if (mimeType.isBlank()) {
                val ext = MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString())
                if (ext.isNotBlank()) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase(Locale.US)) ?: ""
                }
            }

            if (mimeType.isBlank()) {
                if (fileName.lowercase(Locale.US).endsWith(".pdf")) {
                    mimeType = "application/pdf"
                } else if (fileName.lowercase(Locale.US).endsWith(".jpg") || fileName.lowercase(Locale.US).endsWith(".jpeg")) {
                    mimeType = "image/jpeg"
                } else if (fileName.lowercase(Locale.US).endsWith(".png")) {
                    mimeType = "image/png"
                } else {
                    mimeType = "application/octet-stream"
                }
            }

            val docsDir = File(context.filesDir, "documents").apply {
                if (!exists()) mkdirs()
            }

            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val destFile = File(docsDir, "doc_${System.currentTimeMillis()}_$cleanName")

            val inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            if (inputStream != null) {
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                val savedUri = Uri.fromFile(destFile).toString()
                SavedFileInfo(
                    fileUriString = savedUri,
                    mimeType = mimeType,
                    fileSize = destFile.length(),
                    fileName = fileName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "document_${System.currentTimeMillis()}"
    }

    fun deleteFileFromInternalStorage(fileUriString: String): Boolean {
        if (fileUriString.isBlank()) return false
        return try {
            val uri = Uri.parse(fileUriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return false)
                if (file.exists()) file.delete() else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
