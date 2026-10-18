package com.example.arcadehub.ui.games

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

@Composable
fun GameRouter(id: String, onExit: () -> Unit) {
    when (id) {
        "flappy" -> FlappyGame(onExit)
        "snake" -> SnakeGame(onExit)
        "2048" -> Game2048(onExit)
        "memory" -> MemoryGame(onExit)
        "minesweeper" -> MinesweeperGame(onExit)
        "breakout" -> BreakoutGame(onExit)
        "whack" -> WhackGame(onExit)
        "tictactoe" -> TicTacToeGame(onExit)
        "connect4" -> ConnectFourGame(onExit)
        "battleship" -> BattleshipGame(onExit)
        "rps" -> RpsGame(onExit)
        "minigolf" -> MiniGolfGame(onExit)
        "dots" -> DotsGame(onExit)
        "ludo" -> LudoGame(onExit)
        "online" -> OnlineGame(onExit)
        else -> onExit()
    }
}
