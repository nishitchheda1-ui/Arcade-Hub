package com.example.arcadehub.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.arcadehub.data.ScoreRepository
import com.example.arcadehub.model.Category
import com.example.arcadehub.model.Game
import com.example.arcadehub.model.allGames
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*
import kotlin.math.abs

@Composable
fun HomeScreen(
    username: String,
    onLogout: () -> Unit,
    onLeaderboard: () -> Unit,
    onGameClick: (Game) -> Unit
) {
    var filter by remember { mutableStateOf<Category?>(null) }
    val games = allGames.filter { filter == null || it.category == filter }
    var totalPoints by remember { mutableStateOf(ScoreRepository.getTotalPoints()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                totalPoints = ScoreRepository.getTotalPoints()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val tier = ScoreRepository.getTier(totalPoints)

    val kidAvatars = listOf("🦁", "🦊", "🐼", "🦄", "🐯", "🐶", "🐱", "🚀", "👾", "🐻")
    val myAvatar = kidAvatars[abs(username.hashCode()) % kidAvatars.size]

    ArcadeBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kid Avatar Bubble
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Sun)
                        .border(2.5.dp, Ink, CircleShape)
                        .clickable(onClick = onLeaderboard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(myAvatar, fontSize = 24.sp)
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    OutlinedTitle("ARCADE HUB", 24.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Hi, $username! 🎈",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(8.dp))
                        // Shiny Star XP Pill
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Sun)
                                .border(1.5.dp, Ink, RoundedCornerShape(10.dp))
                                .clickable(onClick = onLeaderboard)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("⭐ $totalPoints Stars", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                // Leaderboard Trophy Button
                SquareButton(Icons.Default.EmojiEvents, onLeaderboard, color = Sun, desc = "Trophy Room")
                Spacer(Modifier.width(8.dp))
                // Logout Button
                SquareButton(Icons.AutoMirrored.Filled.Logout, onLogout, color = Coral, desc = "Log out")
            }

            // Cheerful Banner Pill
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "🌈 $tier · Beat records & collect stars! ✨",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Pill("🌟 All (15)", filter == null, color = Sun) { filter = null }
                }
                item {
                    Pill("🕹️ Solo Fun", filter == Category.SINGLE, color = Sky) { filter = Category.SINGLE }
                }
                item {
                    Pill("🤖 Bot Buddies", filter == Category.BOT, color = Leaf) { filter = Category.BOT }
                }
                item {
                    Pill("👥 Party Games", filter == Category.MULTI, color = Tangerine) { filter = Category.MULTI }
                }
            }

            // Game Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(games, key = { it.id }) { game ->
                    GameCard(game) { onGameClick(game) }
                }
            }
        }
    }
}

private fun gameCardGradient(base: Color): Brush {
    val dark = Color(
        red = (base.red * 0.85f).coerceIn(0f, 1f),
        green = (base.green * 0.85f).coerceIn(0f, 1f),
        blue = (base.blue * 0.85f).coerceIn(0f, 1f),
        alpha = 1f
    )
    return Brush.verticalGradient(listOf(base, dark))
}

@Composable
private fun GameCard(game: Game, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val bestScore = ScoreRepository.getLocalBest(game.id)
    val wins = ScoreRepository.getLocalWins(game.id)

    val src = remember { MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "cardScale")

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .scale(scale)
    ) {
        // Drop shadow base
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .background(Ink, shape)
        )
        // Main card body
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp)
                .clip(shape)
                .background(gameCardGradient(game.color))
                .border(3.2.dp, Ink, shape)
                .clickable(interactionSource = src, indication = null, onClick = onClick)
        ) {
            // Top gloss highlight
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Toy Emoji Bubble
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Cream)
                            .border(2.5.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(game.emoji, fontSize = 28.sp)
                    }

                    // High score badge or player tag
                    val badgeText = when {
                        game.category == Category.SINGLE && bestScore > 0 -> "⭐ $bestScore"
                        game.category != Category.SINGLE && wins > 0 -> "🏆 $wins W"
                        game.category == Category.SINGLE -> "✨ 1P"
                        game.category == Category.BOT -> "🤖 vs Bot"
                        else -> "🎉 2-4P"
                    }

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Cream)
                            .border(2.dp, Ink, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            badgeText,
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Column {
                    Text(
                        game.title,
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        game.subtitle,
                        color = Ink.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
