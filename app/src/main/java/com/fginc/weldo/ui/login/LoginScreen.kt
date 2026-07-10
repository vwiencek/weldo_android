package com.fginc.weldo.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.auth.AppleSignIn

/**
 * The signed-out screen (mirrors the iOS LoginView): email/password with a create-account
 * toggle, Sign in with Apple (web OAuth), and an Advanced section for the base URL + a dev-token
 * shortcut that authenticates against the backend's `local` profile as `test-<name>`.
 */
@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    val vm: LoginViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registerMode by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var baseUrlField by remember { mutableStateOf(state.baseUrl) }
    var devName by remember { mutableStateOf("") }

    // Deliver an Apple id_token captured by the redirect activity.
    val pendingApple by AppleSignIn.pendingIdToken.collectAsState()
    LaunchedEffect(pendingApple) {
        pendingApple?.let {
            vm.onAppleToken(it, null)
            AppleSignIn.pendingIdToken.value = null
        }
    }

    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    val passwordValid = password.length >= 8
    val canSubmit = email.isNotBlank() && passwordValid && !state.loading

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Weldo", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Capture anything. Organize everything.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = password.isNotEmpty() && !passwordValid,
            supportingText = if (password.isNotEmpty() && !passwordValid) {
                { Text("At least 8 characters") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (registerMode) vm.passwordRegister(email, password) else vm.passwordLogin(email, password)
        }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (registerMode) "Create account" else "Sign in")
            }
        }
        TextButton(onClick = { registerMode = !registerMode; vm.clearError() }) {
            Text(if (registerMode) "Have an account? Sign in" else "New here? Create an account")
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { if (!AppleSignIn.launch(context)) vm.clearError() },
            enabled = AppleSignIn.isConfigured && !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(" Sign in with Apple") }
        if (!AppleSignIn.isConfigured) {
            Text(
                "Configure APPLE_WEB_CLIENT_ID to enable",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "Hide advanced" else "Advanced / Server")
        }
        if (advanced) {
            OutlinedTextField(
                value = baseUrlField,
                onValueChange = { baseUrlField = it },
                label = { Text("Backend base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { vm.setBaseUrl(baseUrlField) }, modifier = Modifier.align(Alignment.End)) {
                Text("Save server")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = devName,
                onValueChange = { devName = it },
                label = { Text("Dev token name (test-<name>)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { vm.useDevToken(devName) }, modifier = Modifier.align(Alignment.End)) {
                Text("Use dev token")
            }
        }
    }
}
