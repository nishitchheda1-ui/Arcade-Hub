package com.example.arcadehub.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadehub.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel : ViewModel() {
    val repo = AuthRepository()
    var state by mutableStateOf(AuthUiState())
        private set

    fun clearError() { state = state.copy(error = null) }

    fun playAsGuest(name: String = "Guest Player", onSuccess: () -> Unit) {
        repo.startGuestSession(name.ifBlank { "Guest Player" })
        state = AuthUiState()
        onSuccess()
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) { state = AuthUiState(error = "Fill in all fields"); return }
        run(onSuccess) { repo.login(email.trim(), password) }
    }

    fun signUp(username: String, email: String, password: String, onSuccess: () -> Unit) {
        when {
            username.trim().length < 3 -> state = AuthUiState(error = "Username must be at least 3 characters")
            email.isBlank() -> state = AuthUiState(error = "Enter your email")
            password.length < 6 -> state = AuthUiState(error = "Password must be at least 6 characters")
            else -> run(onSuccess) { repo.signUp(username.trim(), email.trim(), password) }
        }
    }

    private fun run(onSuccess: () -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            state = AuthUiState(loading = true)
            try {
                withTimeout(15_000) { block() }
                state = AuthUiState()
                onSuccess()
            } catch (e: Exception) {
                state = AuthUiState(error = friendly(e))
            }
        }
    }

    private fun friendly(e: Exception) = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password"
        is FirebaseAuthInvalidUserException -> "No account found with that email"
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters"
        is FirebaseNetworkException -> "No internet connection"
        is TimeoutCancellationException -> "Network timed out. You can also play immediately as Guest!"
        else -> e.localizedMessage ?: "Something went wrong"
    }
}
