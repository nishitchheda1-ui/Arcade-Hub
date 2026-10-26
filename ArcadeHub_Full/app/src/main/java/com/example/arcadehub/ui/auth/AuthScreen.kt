package com.example.arcadehub.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arcadehub.R
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    vm: AuthViewModel = viewModel(),
    onGuestPlay: (() -> Unit)? = null
) {
    var isSignUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state = vm.state

    ArcadeBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(115.dp))
            Spacer(Modifier.height(4.dp))
            OutlinedTitle("ARCADE HUB", 34.sp)
            Text(
                "15 Super Fun Games • Play Solo or with Friends! 🌈🎈",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Instant Play as Guest Card (Zero wait time, zero signup lag!)
            CreamCard(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                Text(
                    "🚀 QUICK PLAY (NO LOGIN NEEDED!)",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    "Jump straight into any game! No password needed. Your best scores & stars are saved right here on your device! ⭐",
                    color = Ink.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
                )
                GameButton(
                    text = "🎮 START PLAYING NOW!",
                    onClick = {
                        if (onGuestPlay != null) onGuestPlay()
                        else vm.playAsGuest(username.ifBlank { "Guest Hero" }, onAuthenticated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = Leaf,
                    fontSize = 15.sp
                )
            }

            // Divider
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
                Text(
                    " ✨ OR SAVE STARS ONLINE ✨ ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.25f))
            }

            AnimatedVisibility(isSignUp) {
                Column {
                    GameTextField(username, { username = it; vm.clearError() }, "Player Nickname 👤", Icons.Default.Person)
                    Spacer(Modifier.height(12.dp))
                }
            }
            GameTextField(email, { email = it; vm.clearError() }, "Secret Email ✉️", Icons.Default.Email, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            GameTextField(password, { password = it; vm.clearError() }, "Secret Password 🔑", Icons.Default.Lock, isPassword = true)

            state.error?.let {
                val shape = RoundedCornerShape(16.dp)
                Text(
                    it,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(shape)
                        .background(Coral)
                        .border(3.dp, Ink, shape)
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            GameButton(
                text = if (isSignUp) "CREATE MY HERO ACCOUNT ⭐" else "LET'S PLAY! 🚀",
                onClick = {
                    if (isSignUp) vm.signUp(username, email, password, onAuthenticated)
                    else vm.login(email, password, onAuthenticated)
                },
                loading = state.loading,
                color = if (isSignUp) Tangerine else Sun,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                if (isSignUp) "Already have an account? Log in here! 🎈" else "New superstar? Create an account! 🌟",
                color = Sun,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable { isSignUp = !isSignUp; vm.clearError() }
                    .padding(8.dp)
            )
        }
    }
}
