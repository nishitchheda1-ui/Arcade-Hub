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
        try {
            val res = ScoreRepository.top(sel)
            rows = res
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            rows = emptyList()
        }
    }

    GameScaffold("🏆 Leaderboard", onBack) {

        // ── Game Selector Pills ──
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            item { Pill("🌍 All Games", sel == null, color = Sun) { sel = null } }
            val g2048 = allGames.firstOrNull { it.id == "2048" }
            if (g2048 != null) {
                item { Pill("${g2048.emoji} 2048", sel == "2048", color = Tangerine) { sel = "2048" } }
            }
            items(allGames.filter { it.id != "2048" }, key = { it.id }) { g ->
                Pill("${g.emoji} ${g.title}", sel == g.id, g.color) { sel = g.id }
            }
        }

        // ── Header Card ──
        val myScore = if (sel == null)
            ScoreRepository.getTotalPoints()
        else
            ScoreRepository.getLocalBest(sel ?: "").let { if (it > 0) it else ScoreRepository.getLocalWins(sel ?: "") }

        val list = rows
        val myIdx = list?.indexOfFirst { it.uid == me || it.name.contains("(you)", ignoreCase = true) } ?: -1

        CreamCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Title
                Column(Modifier.weight(1f)) {
                    Text(
                        if (game == null) "🏆 GLOBAL HALL OF FAME" else "${game.emoji} ${game.title.uppercase()}",
                        color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp
                    )
                    Text(
                        if (game == null) "Total XP across all 15 games"
                        else if (game.id == "2048") "Ranked by highest score"
                        else if (game.category == Category.SINGLE) "Ranked by high score"
                        else "Ranked by total wins",
                        color = Ink.copy(alpha = 0.6f), fontSize = 12.sp
                    )
                }
                // Tier badge
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Sun)
                        .border(2.dp, Ink, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(ScoreRepository.getTier(myScore), color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Ink.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // My stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Your Rank", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (myIdx >= 0) "#${myIdx + 1}" else "—",
                        color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Your Best", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$myScore $unit", color = Coral, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }

        // ── Leaderboard List ──
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Sun)
            }

            list.isEmpty() -> Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No scores yet!\nBe the first to play!",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    )
                }
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(list) { i, e ->
                    val isMine = e.uid == me || e.isLocal || e.name.contains("(you)", ignoreCase = true)
                    val shape = RoundedCornerShape(14.dp)
                    val rankEmoji = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> null }

                    Box(
                        Modifier.fillMaxWidth()
                            .clip(shape)
                            .background(if (isMine) Sun else Cream)
                            .border(2.5.dp, Ink, shape)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            // Rank
                            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                if (rankEmoji != null) {
                                    Text(rankEmoji, fontSize = 22.sp)
                                } else {
                                    Text("#${i + 1}", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                }
                            }

                            // Avatar
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (isMine) Coral.copy(alpha = 0.25f) else Sky.copy(alpha = 0.2f))
                                    .border(2.dp, Ink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isMine) "👤" else "🎮", fontSize = 16.sp)
                            }

                            Spacer(Modifier.width(10.dp))

                            // Name + tier
                            Column(Modifier.weight(1f)) {
                                Text(
                                    e.name + if (isMine && !e.name.contains("(you)", ignoreCase = true)) " (You)" else "",
                                    color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1
                                )
                                if (sel == null) {
                                    Text(
                                        ScoreRepository.getTier(e.score),
                                        color = Ink.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Score badge
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (isMine) Ink else Board)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "${e.score} $unit",
                                    color = if (isMine) Sun else Color.White,
                                    fontWeight = FontWeight.Black, fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
