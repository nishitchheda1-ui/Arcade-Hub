package com.example.arcadehub.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepository {
    companion object {
        private var prefs: SharedPreferences? = null

        fun init(context: Context) {
            if (prefs == null) {
                prefs = context.applicationContext.getSharedPreferences("arcade_hub_auth", Context.MODE_PRIVATE)
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUser get() = auth.currentUser

    var isGuest: Boolean
        get() = prefs?.getBoolean("is_guest", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("is_guest", value)?.apply() }

    var guestName: String
        get() = prefs?.getString("guest_name", "Guest Player") ?: "Guest Player"
        set(value) { prefs?.edit()?.putString("guest_name", value)?.apply() }

    val isLoggedIn: Boolean
        get() = currentUser != null || isGuest

    val activeDisplayName: String
        get() = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: if (isGuest) guestName else "Player"

    fun startGuestSession(name: String = "Guest Player") {
        isGuest = true
        guestName = name
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        isGuest = false
    }

    suspend fun signUp(username: String, email: String, password: String) {
        // Fast sign up: create user and set display name immediately
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Failed to create user")
        user.updateProfile(userProfileChangeRequest { displayName = username }).await()
        isGuest = false

        // Background write to Firestore with timeout so signup is never delayed or blocked
        scope.launch {
            try {
                withTimeoutOrNull(2500L) {
                    Db.fs.collection("users").document(user.uid).set(
                        mapOf(
                            "username" to username,
                            "email" to email,
                            "points" to 0L,
                            "createdAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (_: Exception) {
                // If Firestore is unavailable or uninitialized, player still has their full Auth account!
            }
        }
    }

    fun logout() {
        try { auth.signOut() } catch (_: Exception) {}
        isGuest = false
    }
}
