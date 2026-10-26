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
    Difficulty.MEDIUM -> "🦊"
    Difficulty.HARD -> "⚡"
}

private fun Difficulty.subtitle() = when (this) {
    Difficulty.EASY -> "Friendly & cheerful • Loves to play and gives easy wins! 😊"
    Difficulty.MEDIUM -> "Clever & tricky • Thinks ahead with smart moves! 🎯"
    Difficulty.HARD -> "Mega Boss Bot • Super grandmaster arcade champion! 👑"
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
    val isWin = title.contains("win", ignoreCase = true) || title.contains("won", ignoreCase = true) || title.contains("victory", ignoreCase = true)
    val celebratoryEmoji = if (isWin) "🎉" else "⭐"

    CreamCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isWin) Sun.copy(alpha = 0.3f) else Sky.copy(alpha = 0.3f))
                .border(2.5.dp, if (isWin) Sun else Sky, CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(celebratoryEmoji, fontSize = 28.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (isWin) "WOOHOO! AWESOME JOB! 🎉" else title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            subtitle,
            color = Ink.copy(alpha = 0.75f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("PLAY AGAIN 🔄", onAgain, Modifier.weight(1f), color = Leaf, fontSize = 15.sp)
            GameButton("ALL GAMES 🏠", onExit, Modifier.weight(1f), color = Coral, fontSize = 15.sp)
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
    val shape = RoundedCornerShape(22.dp)
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
                .padding(top = 6.dp)
                .background(Ink, shape)
        )
        // Main tactile card
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(shape)
                .background(Cream)
                .border(3.2.dp, Ink, shape)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cute Bot Avatar Bubble
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f))
                    .border(2.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(difficulty.avatar(), fontSize = 30.sp)
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
                    // Stars Reward Pill
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "+${difficulty.points} ⭐ STARS",
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    difficulty.subtitle(),
                    color = Ink.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play arrow icon
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.5.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun DifficultyPicker(title: String, onBack: () -> Unit, onPick: (Difficulty) -> Unit) {
    GameScaffold(title, onBack) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose Your Bot Buddy! 🤖🎈",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp)
        )
        Text(
            "Defeat bot buddies to win shiny trophies and superstar stars! ⭐",
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 14.dp)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Icon(Icons.Default.SmartToy, null, tint = Sun, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PLAY VS BOT BUDDIES 🤖", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
                Text("Test your skills against friendly robot pals!", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    Icon(Icons.Default.Group, null, tint = Ink, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🎉 PLAY WITH FRIENDS ON 1 SCREEN! 🎈", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                Text("Pass the phone to friends and take turns! 🎪", color = Ink.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (n in 2..maxPlayers) {
                        GameButton(
                            text = "$n Players 👫",
                            onClick = {
                                onStart(List(n) { PlayerCfg("Player ${it + 1}", PLAYER_COLORS[it]) })
                            },
                            modifier = Modifier.weight(1f),
                            color = Sky,
                            fontSize = 13.sp
                        )
                    }
                }

                if (maxPlayers >= 4) {
                    Spacer(Modifier.height(10.dp))
                    GameButton(
                        text = "🎮 PARTY: YOU + 3 BOT BUDDIES!",
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
