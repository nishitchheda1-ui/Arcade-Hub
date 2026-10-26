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
        try {
            val res = ScoreRepository.top(sel)
            rows = res
            isLocalMode = res.any { it.isLocal }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            rows = emptyList()
        }
    }

    GameScaffold("Trophy Room 🏆", onBack) {
        // Quick Game Filter Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                Pill("🌍 Superstar Stars", sel == null, color = Sun) { sel = null }
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

        val competitorAvatars = listOf("🦊", "🐼", "🦁", "🐯", "🦄", "🐶", "🐱", "🚀", "👾", "🐻", "🐸", "🐰")

        // Explanatory Banner Card for Main Leaderboard or Game Leaderboard
        CreamCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (game == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆 SUPERSTAR TROPHY ROOM 🌟", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Sun).border(1.5.dp, Ink, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(ScoreRepository.getTier(myScore), color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ranks players by Total Superstar Stars collected across ALL 15 fun games! ✨",
                    color = Ink.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    "• Solo Games (Snake, 2048...): High scores give bonus stars ⭐\n• Bot Buddies: +5 (Friendly) · +10 (Clever) · +20 (Mega Boss)\n• Multiplayer & Online: +15 to +25 stars per win 🎉",
                    color = Ink.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${game.emoji} ${game.title.uppercase()} TROPHIES", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    if (game.id == "2048") {
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(Tangerine).border(1.5.dp, Ink, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
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
                    color = Ink.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Current player rank status
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (myIdx >= 0) "Your Superstar Rank: #${myIdx + 1} 🌟" else "Play games to earn shiny stars! 🎈",
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    "Your Best: $myScore $unit ⭐",
                    color = Coral,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Sun)
            }
        } else {
            if (list.isEmpty()) {
                Text("No scores yet. Play a game to record your score!", color = Color.White, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                itemsIndexed(list) { i, e ->
                    val isMine = e.uid == me || e.isLocal || e.name.contains("(you)", ignoreCase = true)
                    val shape = RoundedCornerShape(20.dp)
                    val avatar = if (isMine) "⭐" else competitorAvatars[kotlin.math.abs(e.name.hashCode()) % competitorAvatars.size]

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (isMine) Sun else Cream)
                            .border(3.2.dp, Ink, shape)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
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
                                fontSize = if (i < 3) 22.sp else 16.sp,
                                color = Ink,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(46.dp)
                            )

                            // Avatar Bubble
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMine) Coral.copy(alpha = 0.35f) else Sky.copy(alpha = 0.35f))
                                    .border(2.dp, Ink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatar, fontSize = 18.sp)
                            }

                            Spacer(Modifier.width(10.dp))

                            // Name & Tier
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = e.name + if (isMine && !e.name.contains("(you)", ignoreCase = true)) " (You)" else "",
                                    color = Ink,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                if (sel == null) {
                                    Text(
                                        ScoreRepository.getTier(e.score),
                                        color = Ink.copy(alpha = 0.65f),
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
