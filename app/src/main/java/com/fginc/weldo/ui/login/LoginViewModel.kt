package com.fginc.weldo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
    val baseUrl: String = "",
)

class LoginViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository
    private val session = WeldoApp.graph.session

    private val _state = MutableStateFlow(LoginUiState(baseUrl = session.baseUrl.value))
    val state: StateFlow<LoginUiState> = _state

    fun passwordLogin(email: String, password: String) = auth { repo.passwordLogin(email, password) }
    fun passwordRegister(email: String, password: String) = auth { repo.passwordRegister(email, password) }

    fun onAppleToken(identityToken: String, appleUserId: String?) =
        auth { repo.appleLogin(identityToken, appleUserId) }

    fun useDevToken(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(error = "Enter a dev name (token becomes test-<name>).") }
            return
        }
        viewModelScope.launch {
            session.setToken("test-$trimmed")
            _state.update { it.copy(signedIn = true, error = null) }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            session.setBaseUrl(url)
            _state.update { it.copy(baseUrl = session.baseUrl.value) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun auth(block: suspend () -> Result<Unit>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                result.fold(
                    onSuccess = { _ -> it.copy(loading = false, signedIn = true) },
                    onFailure = { e -> it.copy(loading = false, error = e.message ?: "Something went wrong.") },
                )
            }
        }
    }
}
