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
    Game("flappy", "Flappy Bird", "🐦", "Flap your wings! 🪽", Category.SINGLE, Sky),
    Game("snake", "Snake", "🐍", "Munch juicy apples! 🍎", Category.SINGLE, Leaf),
    Game("2048", "2048", "🔢", "Match the numbers! 🔢", Category.SINGLE, Tangerine),
    Game("memory", "Memory Match", "🧠", "Find cute pairs! 🃏", Category.SINGLE, Coral),
    Game("minesweeper", "Minesweeper", "💣", "Find safe tiles! 🚩", Category.SINGLE, Grape),
    Game("breakout", "Breakout", "🧱", "Bounce & smash! 💥", Category.SINGLE, Sun),
    Game("whack", "Whack-a-Mole", "🔨", "Whack funny moles! 🐹", Category.SINGLE, Leaf),
    Game("tictactoe", "Tic Tac Toe", "❌", "3 in a row to win! ⭐", Category.BOT, Coral),
    Game("connect4", "Connect Four", "🔴", "Connect 4 discs! 🟡", Category.BOT, Sun),
    Game("battleship", "Battleship", "🚢", "Find hidden ships! 🌊", Category.BOT, Sky),
    Game("rps", "Rock Paper Scissors", "✊", "Rock, Paper, Win! ✂️", Category.BOT, Grape),
    Game("minigolf", "Mini Golf Battle", "⛳", "Putt & score! ⛳", Category.MULTI, Leaf),
    Game("dots", "Dots & Boxes", "🔲", "Claim candy boxes! 🎁", Category.MULTI, Coral),
    Game("ludo", "Ludo Race", "🎲", "Roll & race home! 🎲", Category.MULTI, Tangerine),
    Game("online", "Online Rooms", "🌐", "Play with friends! 🎈", Category.MULTI, Sky)
)
