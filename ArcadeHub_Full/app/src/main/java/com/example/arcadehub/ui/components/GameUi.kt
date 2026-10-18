package com.example.arcadehub.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadehub.logic.Difficulty
import com.example.arcadehub.ui.theme.*

data class PlayerCfg(val name: String, val color: Color, val bot: Difficulty? = null)

val PLAYER_COLORS = listOf(Sun, Coral, Leaf, Sky)

private fun Difficulty.avatar() = when (this) {
    Difficulty.EASY -> "🤖"
    Difficulty.MEDIUM -> "🧠"
    Difficulty.HARD -> "⚡"
}

private fun Difficulty.subtitle() = when (this) {
    Difficulty.EASY -> "Casual & relaxed • Makes frequent blunders"
    Difficulty.MEDIUM -> "Tactical & clever • Anticipates your moves"
    Difficulty.HARD -> "Grandmaster AI • Unbeatable minimax logic"
}

private fun Difficulty.cardColor() = when (this) {
    Difficulty.EASY -> Leaf
    Difficulty.MEDIUM -> Sun
    Difficulty.HARD -> Coral
}

@Composable
fun GameScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ArcadeBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, color = Cream, desc = "Back")
                Spacer(Modifier.width(14.dp))
                OutlinedTitle(title, 22.sp)
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = content
            )
        }
    }
}

@Composable
fun EndPanel(title: String, subtitle: String, onAgain: () -> Unit, onExit: () -> Unit) {
    CreamCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(subtitle, color = Ink.copy(alpha = 0.75f), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("PLAY AGAIN", onAgain, Modifier.weight(1f), color = Leaf, fontSize = 15.sp)
            GameButton("MENU", onExit, Modifier.weight(1f), color = Coral, fontSize = 15.sp)
        }
    }
}

/** Rich tactile Bot Option Card with distinct avatar, reward XP tag, and tactile depth */
@Composable
fun BotDifficultyCard(
    difficulty: Difficulty,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val src = remember { MutableInteractionSource() }
    val isPressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "pressScale")
    val color = difficulty.cardColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
    ) {
        // Shadow base
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 5.dp)
                .background(Ink, shape)
        )
        // Main tactile card
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
                .clip(shape)
                .background(Cream)
                .border(3.dp, Ink, shape)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cute Bot Avatar Bubble
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f))
                    .border(2.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(difficulty.avatar(), fontSize = 28.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${difficulty.label} Bot",
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    // XP Reward Pill
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "+${difficulty.points} XP",
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    difficulty.subtitle(),
                    color = Ink.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play arrow icon
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DifficultyPicker(title: String, onBack: () -> Unit, onPick: (Difficulty) -> Unit) {
    GameScaffold(title, onBack) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Select Bot Opponent",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp)
        )
        Text(
            "Defeating tougher bots earns higher Arcade XP!",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 14.dp)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Difficulty.entries.forEach { diff ->
                BotDifficultyCard(diff, onClick = { onPick(diff) })
            }
        }
    }
}

/** Mode picker for games with bots and pass-and-play. */
@Composable
fun SetupScreen(title: String, onBack: () -> Unit, maxPlayers: Int = 4, onStart: (List<PlayerCfg>) -> Unit) {
    GameScaffold(title, onBack) {
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Play vs Bot Header
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, null, tint = Sun, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PLAY VS BOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text("Test your skills against different bot personalities", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            // The 3 appealing bot cards
            Difficulty.entries.forEach { diff ->
                BotDifficultyCard(
                    difficulty = diff,
                    onClick = {
                        onStart(listOf(PlayerCfg("You", Sun), PlayerCfg("${diff.label} Bot", diff.cardColor(), diff)))
                    }
                )
            }

            // Section 2: Pass & Play Friends
            CreamCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, null, tint = Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PASS & PLAY WITH FRIENDS", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text("Take turns on this device with local friends", color = Ink.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (n in 2..maxPlayers) {
                        GameButton(
                            text = "$n Players",
                            onClick = {
                                onStart(List(n) { PlayerCfg("Player ${it + 1}", PLAYER_COLORS[it]) })
                            },
                            modifier = Modifier.weight(1f),
                            color = Sky,
                            fontSize = 14.sp
                        )
                    }
                }

                if (maxPlayers >= 4) {
                    Spacer(Modifier.height(8.dp))
                    GameButton(
                        text = "🎮 YOU + 3 BOTS (PARTY)",
                        onClick = {
                            onStart(listOf(PlayerCfg("You", Sun)) + (1..3).map { PlayerCfg("Bot $it", PLAYER_COLORS[it], Difficulty.MEDIUM) })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = Tangerine,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
