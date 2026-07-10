package com.fginc.weldo.auth

import android.app.Activity
import android.net.Uri
import android.os.Bundle

/**
 * Catches the `weldo://apple-auth` redirect (registered in the manifest), extracts Apple's
 * `id_token` (Apple may put it in the query or the fragment), hands it to [AppleSignIn], and
 * finishes immediately so control returns to the already-running [com.fginc.weldo.MainActivity].
 */
class AppleRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { uri ->
            extractIdToken(uri)?.let { AppleSignIn.pendingIdToken.value = it }
        }
        finish()
    }

    private fun extractIdToken(uri: Uri): String? {
        uri.getQueryParameter("id_token")?.let { return it }
        // Fragment: "id_token=...&code=...&state=..."
        val fragment = uri.fragment ?: return null
        return fragment.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == "id_token" }
            ?.get(1)
            ?.let { Uri.decode(it) }
    }
}
