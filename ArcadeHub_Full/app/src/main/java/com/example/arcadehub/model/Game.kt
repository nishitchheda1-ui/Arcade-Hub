package com.example.arcadehub.model

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random
import com.example.arcadehub.data.*
import com.example.arcadehub.logic.*
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*

enum class Category(val label: String) {
    SINGLE("Solo"), BOT("vs Bot"), MULTI("Multiplayer")
}

data class Game(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val category: Category,
    val color: Color,
    val available: Boolean = true
)

val allGames = listOf(
    Game("flappy", "Flappy Bird", "🐦", "Tap to fly", Category.SINGLE, Sky),
    Game("snake", "Snake", "🐍", "Eat and grow", Category.SINGLE, Leaf),
    Game("2048", "2048", "🔢", "Merge tiles", Category.SINGLE, Tangerine),
    Game("memory", "Memory Match", "🧠", "Find the pairs", Category.SINGLE, Coral),
    Game("minesweeper", "Minesweeper", "💣", "Avoid the mines", Category.SINGLE, Grape),
    Game("breakout", "Breakout", "🧱", "Smash the bricks", Category.SINGLE, Sun),
    Game("whack", "Whack-a-Mole", "🔨", "Fast reflexes", Category.SINGLE, Leaf),
    Game("tictactoe", "Tic Tac Toe", "❌", "Bot or friend", Category.BOT, Coral),
    Game("connect4", "Connect Four", "🔴", "Drop and connect", Category.BOT, Sun),
    Game("battleship", "Battleship", "🚢", "Sink the fleet", Category.BOT, Sky),
    Game("rps", "Rock Paper Scissors", "✊", "Outsmart the bot", Category.BOT, Grape),
    Game("minigolf", "Mini Golf Battle", "⛳", "2-4 players", Category.MULTI, Leaf),
    Game("dots", "Dots & Boxes", "🔲", "2-4 players", Category.MULTI, Coral),
    Game("ludo", "Ludo Race", "🎲", "2-4 players", Category.MULTI, Tangerine),
    Game("online", "Online Rooms", "🌐", "Play with a join code", Category.MULTI, Sky)
)
