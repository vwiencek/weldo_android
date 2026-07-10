package com.fginc.weldo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fginc.weldo.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "weldo_session")

/**
 * Persistent session + settings, mirroring the iOS app's UserDefaults keys.
 * Holds the backend session bearer (an HMAC JWT, ~30-day TTL), the optional Apple user id,
 * the configurable base URL, and the stats-period preference.
 *
 * In-memory StateFlows are the source of truth for synchronous reads (the OkHttp interceptor
 * and the API factory read them without suspending); every write also persists to DataStore.
 */
class WeldoSession(private val appContext: Context) {

    private val store = appContext.dataStore

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _baseUrl = MutableStateFlow(BuildConfig.DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl

    private val _appleUserId = MutableStateFlow<String?>(null)
    val appleUserId: StateFlow<String?> = _appleUserId

    private val _statsPeriod = MutableStateFlow("month")
    val statsPeriod: StateFlow<String> = _statsPeriod

    val isSignedIn: Boolean get() = !_token.value.isNullOrBlank()

    init {
        // One-time synchronous hydrate at startup; cheap and keeps later reads non-suspending.
        runBlocking {
            val prefs = try {
                store.data.first()
            } catch (_: Exception) {
                emptyPreferences()
            }
            _token.value = prefs[KEY_TOKEN]
            _appleUserId.value = prefs[KEY_APPLE_USER]
            _baseUrl.value = prefs[KEY_BASE_URL]?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_BASE_URL
            _statsPeriod.value = prefs[KEY_STATS_PERIOD] ?: "month"
        }
    }

    suspend fun setToken(value: String?) {
        _token.value = value
        store.edit { if (value.isNullOrBlank()) it.remove(KEY_TOKEN) else it[KEY_TOKEN] = value }
    }

    suspend fun setAppleUserId(value: String?) {
        _appleUserId.value = value
        store.edit { if (value.isNullOrBlank()) it.remove(KEY_APPLE_USER) else it[KEY_APPLE_USER] = value }
    }

    suspend fun setBaseUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        _baseUrl.value = normalized.ifBlank { BuildConfig.DEFAULT_BASE_URL }
        store.edit { it[KEY_BASE_URL] = _baseUrl.value }
    }

    suspend fun setStatsPeriod(value: String) {
        _statsPeriod.value = value
        store.edit { it[KEY_STATS_PERIOD] = value }
    }

    suspend fun signOut() {
        setToken(null)
        setAppleUserId(null)
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("app_token")
        val KEY_APPLE_USER = stringPreferencesKey("apple_user_id")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_STATS_PERIOD = stringPreferencesKey("stats_period")
    }
}
