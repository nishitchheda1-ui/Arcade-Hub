package com.example.arcadehub.ui.home

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadehub.data.ScoreRepository
import com.example.arcadehub.model.Category
import com.example.arcadehub.model.Game
import com.example.arcadehub.model.allGames
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*

@Composable
fun HomeScreen(
    username: String,
    onLogout: () -> Unit,
    onLeaderboard: () -> Unit,
    onGameClick: (Game) -> Unit
) {
    var filter by remember { mutableStateOf<Category?>(null) }
    val games = allGames.filter { filter == null || it.category == filter }
    val totalPoints = ScoreRepository.getTotalPoints()
    val tier = ScoreRepository.getTier(totalPoints)

    ArcadeBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    OutlinedTitle("ARCADE HUB", 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Hi, $username 👋",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(8.dp))
                        // Arcade Level Pill
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Sun)
                                .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                .clickable(onClick = onLeaderboard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$totalPoints pts", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                // Leaderboard Trophy Button
                SquareButton(Icons.Default.EmojiEvents, onLeaderboard, color = Sun, desc = "Leaderboard")
                Spacer(Modifier.width(8.dp))
                // Logout Button
                SquareButton(Icons.AutoMirrored.Filled.Logout, onLogout, color = Coral, desc = "Log out")
            }

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Pill("🌟 All (15)", filter == null, color = Sun) { filter = null }
                }
                item {
                    Pill("🕹️ Solo", filter == Category.SINGLE, color = Sky) { filter = Category.SINGLE }
                }
                item {
                    Pill("🤖 vs Bot", filter == Category.BOT, color = Leaf) { filter = Category.BOT }
                }
                item {
                    Pill("👥 Multiplayer", filter == Category.MULTI, color = Tangerine) { filter = Category.MULTI }
                }
            }

            // Game Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
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

@Composable
private fun GameCard(game: Game, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val bestScore = ScoreRepository.getLocalBest(game.id)
    val wins = ScoreRepository.getLocalWins(game.id)

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
    ) {
        // Drop shadow outline
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .background(Ink, shape)
        )
        // Main card body
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp)
                .clip(shape)
                .background(game.color)
                .border(3.dp, Ink, shape)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(game.emoji, fontSize = 42.sp)

                // High score badge or player tag
                val badgeText = when {
                    game.category == Category.SINGLE && bestScore > 0 -> "★ $bestScore"
                    game.category != Category.SINGLE && wins > 0 -> "🏆 $wins W"
                    game.category == Category.SINGLE -> "1P"
                    game.category == Category.BOT -> "vs Bot"
                    else -> "2-4P"
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink.copy(alpha = 0.15f))
                        .border(1.5.dp, Ink.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        badgeText,
                        color = Ink,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
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
                Text(
                    game.subtitle,
                    color = Ink.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
