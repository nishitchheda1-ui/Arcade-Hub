package com.example.arcadehub.ui.leaderboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadehub.data.*
import com.example.arcadehub.model.Category
import com.example.arcadehub.model.allGames
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    var sel by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<LbEntry>?>(null) }
    var isLocalMode by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val auth = FirebaseAuth.getInstance()
    val me = auth.currentUser?.uid ?: "local_player"
    val game = allGames.firstOrNull { it.id == sel }

    val unit = when {
        sel == null -> "pts"
        game?.id == "2048" -> "score"
        game?.category == Category.SINGLE -> "score"
        else -> "wins"
    }

    LaunchedEffect(sel, reload) {
        rows = null
        isLocalMode = false
        errorMsg = null
        try {
            val res = ScoreRepository.top(sel)
            rows = res
            isLocalMode = res.any { it.isLocal }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            rows = emptyList()
            errorMsg = e.message ?: "Could not load leaderboard"
        }
    }

    GameScaffold("Leaderboard", onBack) {
        // Quick Game Filter Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                Pill("🌍 Global (All Games)", sel == null, color = Sun) { sel = null }
            }
            // Put 2048 first right after Global for quick access!
            val g2048 = allGames.firstOrNull { it.id == "2048" }
            if (g2048 != null) {
                item {
                    Pill("${g2048.emoji} 2048", sel == "2048", color = Tangerine) { sel = "2048" }
                }
            }
            // Other games
            items(allGames.filter { it.id != "2048" }, key = { it.id }) { g ->
                Pill("${g.emoji} ${g.title}", sel == g.id, g.color) { sel = g.id }
            }
        }

        val list = rows
        val myIdx = list?.indexOfFirst { it.uid == me || it.name.contains("(you)", ignoreCase = true) } ?: -1
        val myScore = if (sel == null) ScoreRepository.getTotalPoints() else ScoreRepository.getLocalBest(sel ?: "").let { if (it > 0) it else ScoreRepository.getLocalWins(sel ?: "") }

        // Explanatory Banner Card for Main Leaderboard or Game Leaderboard
        CreamCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (game == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆 GLOBAL HALL OF FAME", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(Sun).border(1.5.dp, Ink, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(ScoreRepository.getTier(myScore), color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "What's shown here: Ranks players worldwide by Total Arcade Points (XP) earned across ALL 15 games.",
                    color = Ink.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    "• Solo Games (2048, Snake...): High scores earn bonus XP\n• Bot Matches: +5 XP (Easy) · +10 XP (Medium) · +20 XP (Hard)\n• Multiplayer & Online: +15 to +25 XP per victory",
                    color = Ink.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${game.emoji} ${game.title.uppercase()} LEADERBOARD", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    if (game.id == "2048") {
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Tangerine).border(1.5.dp, Ink, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Top Tiles", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (game.id == "2048") "Ranked by highest score & tiles merged in a single run."
                    else if (game.category == Category.SINGLE) "Ranked by highest score achieved in a single match."
                    else "Ranked by total match victories.",
                    color = Ink.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            // Current player rank status & personal best indicator
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (myIdx >= 0) "Your Rank: #${myIdx + 1}" else "Play to enter the leaderboard!",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ ", fontSize = 12.sp)
                    Text(
                        "Personal Best: $myScore $unit",
                        color = Coral,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Offline / Local-only indicator banner with Retry Action
        if (isLocalMode && list != null && list.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tangerine.copy(alpha = 0.15f))
                    .border(2.dp, Tangerine.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📱 Showing local scores · Connect for global rankings",
                        color = Tangerine,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Sun)
                            .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                            .clickable { reload++ }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("🔄 RETRY", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }

        if (list == null) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Sun)
                Spacer(Modifier.height(12.dp))
                Text("Loading leaderboard...", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        } else {
            if (list.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🕹️", fontSize = 44.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (errorMsg != null) "⚠️ $errorMsg" else "No scores recorded yet.\nPlay a game to record your score!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    GameButton(
                        "🔄 RETRY",
                        onClick = { reload++ },
                        modifier = Modifier.fillMaxWidth(0.5f),
                        color = Sun,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    itemsIndexed(list) { i, e ->
                        val isMine = e.uid == me || e.isLocal || e.name.contains("(you)", ignoreCase = true)
                        val isTop3 = i < 3
                        val shape = RoundedCornerShape(16.dp)

                        // Top-3 background and border distinction
                        val bgColor = when {
                            isMine -> Sun
                            i == 0 -> Color(0xFFFFF8DC) // Gold tint
                            i == 1 -> Color(0xFFF2F3F5) // Silver tint
                            i == 2 -> Color(0xFFFAF0E6) // Bronze tint
                            else -> Cream
                        }

                        val borderColor = when (i) {
                            0 -> Color(0xFFFFD700) // Gold border
                            1 -> Color(0xFFB0B7C0) // Silver border
                            2 -> Color(0xFFCD7F32) // Bronze border
                            else -> Ink
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(bgColor)
                                .border(if (isTop3) 4.dp else 3.dp, borderColor, shape)
                                .padding(horizontal = 14.dp, vertical = if (isTop3) 14.dp else 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Rank Medal or Number
                                Text(
                                    text = when (i) {
                                        0 -> "🥇"
                                        1 -> "🥈"
                                        2 -> "🥉"
                                        else -> "#${i + 1}"
                                    },
                                    fontSize = if (isTop3) 26.sp else 16.sp,
                                    color = Ink,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(46.dp)
                                )

                                // Avatar Bubble
                                Box(
                                    Modifier
                                        .size(if (isTop3) 38.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(if (isMine) Coral.copy(alpha = 0.3f) else Sky.copy(alpha = 0.3f))
                                        .border(2.dp, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isMine) "👤" else "🎮", fontSize = if (isTop3) 18.sp else 16.sp)
                                }

                                Spacer(Modifier.width(10.dp))

                                // Name, Tier & Local Tag
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = e.name + if (isMine && !e.name.contains("(you)", ignoreCase = true)) " (You)" else "",
                                            color = Ink,
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (isTop3) 16.sp else 15.sp,
                                            maxLines = 1
                                        )
                                        if (e.isLocal) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Tangerine.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text("📱 Local", color = Tangerine, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (sel == null) {
                                        Text(
                                            ScoreRepository.getTier(e.score),
                                            color = Ink.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Score Badge
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isMine) Ink else Board)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${e.score} $unit",
                                        color = if (isMine) Sun else Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
