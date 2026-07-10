package com.fginc.weldo.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.fginc.weldo.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Sign in with Apple on Android via Apple's **web** OAuth flow (the backend verifies the
 * identity token against `apple.web.client.id`). We open Apple's authorize page in a Chrome
 * Custom Tab; the Services ID's redirect must ultimately bounce back to `weldo://apple-auth`
 * carrying Apple's `id_token`, which [AppleRedirectActivity] captures into [pendingIdToken].
 *
 * This is scaffolding: it needs a real Services ID ([BuildConfig.APPLE_WEB_CLIENT_ID]) and a
 * hosted redirect ([BuildConfig.APPLE_REDIRECT_URI]) that forwards the `id_token` to the custom
 * scheme (Apple posts to an https URL, not a custom scheme). The backend's code-exchange is a
 * stub, so the token must arrive as an `id_token`, not an authorization `code`.
 */
object AppleSignIn {

    /** Set by [AppleRedirectActivity] when the redirect delivers an id_token; observed by LoginScreen. */
    val pendingIdToken = MutableStateFlow<String?>(null)

    val isConfigured: Boolean get() = BuildConfig.APPLE_WEB_CLIENT_ID.isNotBlank()

    /** Opens Apple's authorize page in a Custom Tab. No-op (returns false) if not configured. */
    fun launch(context: Context): Boolean {
        if (!isConfigured) return false
        val url = Uri.parse("https://appleid.apple.com/auth/authorize").buildUpon()
            .appendQueryParameter("response_type", "code id_token")
            .appendQueryParameter("response_mode", "fragment")
            .appendQueryParameter("client_id", BuildConfig.APPLE_WEB_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.APPLE_REDIRECT_URI)
            .appendQueryParameter("scope", "name email")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, url)
        return true
    }
}
