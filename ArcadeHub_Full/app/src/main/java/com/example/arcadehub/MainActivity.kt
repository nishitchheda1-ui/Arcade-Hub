package com.example.arcadehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.arcadehub.data.AuthRepository
import com.example.arcadehub.data.ScoreRepository
import com.example.arcadehub.ui.auth.AuthScreen
import com.example.arcadehub.ui.games.GameRouter
import com.example.arcadehub.ui.home.HomeScreen
import com.example.arcadehub.ui.leaderboard.LeaderboardScreen
import com.example.arcadehub.ui.splash.SplashScreen
import com.example.arcadehub.ui.theme.ArcadeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScoreRepository.init(applicationContext)
        AuthRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent { ArcadeTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val auth = AuthRepository()

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                val next = if (auth.isLoggedIn) "home" else "auth"
                nav.navigate(next) { popUpTo("splash") { inclusive = true } }
            }
        }
        composable("auth") {
            AuthScreen(
                onAuthenticated = { nav.navigate("home") { popUpTo("auth") { inclusive = true } } },
                onGuestPlay = {
                    auth.startGuestSession("Guest Player")
                    nav.navigate("home") { popUpTo("auth") { inclusive = true } }
                }
            )
        }
        composable("home") {
            HomeScreen(
                username = auth.activeDisplayName,
                onLogout = { auth.logout(); nav.navigate("auth") { popUpTo("home") { inclusive = true } } },
                onLeaderboard = { nav.navigate("leaderboard") },
                onGameClick = { nav.navigate("game/${it.id}") }
            )
        }
        composable("leaderboard") { LeaderboardScreen { nav.popBackStack() } }
        composable("game/{id}") { entry ->
            GameRouter(entry.arguments?.getString("id") ?: "") { nav.popBackStack() }
        }
    }
}
