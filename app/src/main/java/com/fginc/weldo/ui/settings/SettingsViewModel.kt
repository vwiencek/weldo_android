package com.fginc.weldo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.ProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val handle: String = "",
    val email: String = "",
    val memberSince: String? = null,
    val handleAvailable: Boolean? = null,
    val baseUrl: String = "",
    val statsPeriod: String = "month",
)

private const val STATS_PERIOD_KEY = "android.statsPeriod"
private const val DEFAULT_STATS_PERIOD = "month"

class SettingsViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository
    private val session = WeldoApp.graph.session

    private val _state = MutableStateFlow(
        SettingsUiState(baseUrl = session.baseUrl.value, statsPeriod = session.statsPeriod.value),
    )
    val state: StateFlow<SettingsUiState> = _state

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.profile().onSuccess { p ->
                _state.update {
                    it.copy(
                        loading = false,
                        firstName = p.firstName.orEmpty(),
                        lastName = p.lastName.orEmpty(),
                        birthDate = p.birthDate.orEmpty(),
                        handle = p.handle.orEmpty(),
                        email = p.email.orEmpty(),
                        memberSince = p.createdAt,
                        baseUrl = session.baseUrl.value,
                        statsPeriod = session.statsPeriod.value,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Couldn't load profile.") }
            }
            // Hydrate preference-backed settings from the server (source of truth across devices).
            repo.getPreferences().onSuccess { prefs ->
                prefs[STATS_PERIOD_KEY]?.let { period ->
                    session.setStatsPeriod(period)
                    _state.update { it.copy(statsPeriod = period) }
                }
            }
        }
    }

    fun setFirstName(v: String) = _state.update { it.copy(firstName = v, saved = false) }
    fun setLastName(v: String) = _state.update { it.copy(lastName = v, saved = false) }
    fun setBirthDate(v: String) = _state.update { it.copy(birthDate = v, saved = false) }

    fun setHandle(v: String) {
        _state.update { it.copy(handle = v, handleAvailable = null, saved = false) }
        val h = v.trim()
        if (h.isBlank()) return
        viewModelScope.launch {
            repo.handleAvailable(h).onSuccess { avail ->
                // Ignore stale responses if the user kept typing.
                if (_state.value.handle.trim() == h) _state.update { it.copy(handleAvailable = avail) }
            }
        }
    }

    fun save() {
        _state.update { it.copy(saving = true, error = null, saved = false) }
        viewModelScope.launch {
            val s = _state.value
            repo.updateProfile(
                ProfileUpdateRequest(
                    firstName = s.firstName.ifBlank { null },
                    lastName = s.lastName.ifBlank { null },
                    birthDate = s.birthDate.ifBlank { null },
                    handle = s.handle.ifBlank { null },
                ),
            ).onSuccess {
                _state.update { it.copy(saving = false, saved = true, error = null) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save.") }
            }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            session.setBaseUrl(url)
            _state.update { it.copy(baseUrl = session.baseUrl.value) }
        }
    }

    fun setStatsPeriod(period: String) {
        viewModelScope.launch {
            session.setStatsPeriod(period)
            _state.update { it.copy(statsPeriod = period) }
            repo.putPreference(STATS_PERIOD_KEY, period)  // best-effort mirror to server
        }
    }

    /** Clears the server-side stats-period preference and falls back to the local default. */
    fun resetStatsPeriod() {
        viewModelScope.launch {
            repo.deletePreference(STATS_PERIOD_KEY)
            session.setStatsPeriod(DEFAULT_STATS_PERIOD)
            _state.update { it.copy(statsPeriod = DEFAULT_STATS_PERIOD) }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.signOut()
            onDone()
        }
    }
}
