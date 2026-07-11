package com.fginc.weldo.ui.capture

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64

/** Reads a picked document and returns (base64, mimeType, fileName) for /capture/file. */
object FileUtil {
    fun uriToBase64(context: Context, uri: Uri): Triple<String, String?, String?>? = try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val mime = context.contentResolver.getType(uri)
        Triple(base64, mime, displayName(context, uri))
    } catch (_: Exception) {
        null
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}
