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

// ======================= TIC TAC TOE =======================
@Composable
fun TicTacToeGame(onExit: () -> Unit) {
    var ps by remember { mutableStateOf<List<PlayerCfg>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val p = ps
    if (p == null) SetupScreen("Tic Tac Toe", onExit, maxPlayers = 2) { ps = it }
    else key(round) { TttPlay(p, onExit) { round++ } }
}

@Composable
private fun TttPlay(ps: List<PlayerCfg>, onExit: () -> Unit, onAgain: () -> Unit) {
    var board by remember { mutableStateOf(List(9) { 0 }) }
    var turn by remember { mutableIntStateOf(1) }
    var result by remember { mutableIntStateOf(0) }
    val bot = ps[1].bot

    fun play(i: Int) {
        if (result != 0 || board[i] != 0) return
        val nb = board.toMutableList().also { it[i] = turn }
        board = nb
        val r = tttResult(nb.toIntArray())
        if (r != 0) { result = r; if (r == 1) ScoreRepository.addWin("tictactoe", bot?.points ?: 3) } else turn = 3 - turn
    }

    LaunchedEffect(turn, result) {
        if (bot != null && turn == 2 && result == 0) {
            delay(450)
            val m = withContext(Dispatchers.Default) { tttBotMove(board.toIntArray(), bot) }
            play(m)
        }
    }

    GameScaffold("Tic Tac Toe", onExit) {
        Spacer(Modifier.height(12.dp))
        Text(
            when (result) {
                0 -> "${ps[turn - 1].name}'s turn (${if (turn == 1) "X 🔷" else "O 🔴"})"
                3 -> "It's a Draw! 🤝"
                else -> "🎉 ${ps[result - 1].name} wins! 🏆"
            },
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(16.dp))
        TttBoard(board, { if (bot == null || turn == 1) play(it) })
        if (result != 0) EndPanel(
            when { result == 3 -> "Great Match! It's a draw!"; result == 1 -> if (bot != null) "Woohoo! You Won! 🎉" else "Player 1 Wins! 🏆"; else -> if (bot != null) "Bot Won! Good try! 🎈" else "Player 2 Wins! 🏆" },
            if (bot != null) "Bot: ${bot.label}" else "Awesome game!", onAgain, onExit
        )
    }
}

// ======================= CONNECT FOUR =======================
@Composable
fun ConnectFourGame(onExit: () -> Unit) {
    var ps by remember { mutableStateOf<List<PlayerCfg>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val p = ps
    if (p == null) SetupScreen("Connect Four", onExit, maxPlayers = 2) { ps = it }
    else key(round) { C4Play(p, onExit) { round++ } }
}

@Composable
private fun C4Play(ps: List<PlayerCfg>, onExit: () -> Unit, onAgain: () -> Unit) {
    var board by remember { mutableStateOf(List(42) { 0 }) }
    var turn by remember { mutableIntStateOf(1) }
    var result by remember { mutableIntStateOf(0) }
    var thinking by remember { mutableStateOf(false) }
    val bot = ps[1].bot

    fun play(col: Int) {
        if (result != 0) return
        val b = board.toIntArray()
        if (c4Drop(b, col, turn) < 0) return
        board = b.toList()
        val r = c4Result(b)
        if (r != 0) { result = r; if (r == 1) ScoreRepository.addWin("connect4", bot?.points ?: 3) } else turn = 3 - turn
    }

    LaunchedEffect(turn, result) {
        if (bot != null && turn == 2 && result == 0) {
            thinking = true
            delay(350)
            val m = withContext(Dispatchers.Default) { c4BotMove(board.toIntArray(), bot) }
            thinking = false
            play(m)
        }
    }

    GameScaffold("Connect Four", onExit) {
        Spacer(Modifier.height(12.dp))
        Text(
            when (result) {
                0 -> if (thinking) "Bot buddy is thinking... 🤔" else "${ps[turn - 1].name}'s turn (${if (turn == 1) "Red 🔴" else "Yellow 🟡"})"
                3 -> "It's a Draw! 🤝"; else -> "🎉 ${ps[result - 1].name} wins! 🏆"
            }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(16.dp))
        C4Board(board, { if (!thinking && (bot == null || turn == 1)) play(it) })
        if (result != 0) EndPanel(
            when { result == 3 -> "Great Match! It's a draw!"; result == 1 -> if (bot != null) "Woohoo! You Won! 🎉" else "Player 1 Wins! 🏆"; else -> if (bot != null) "Bot Won! Good try! 🎈" else "Player 2 Wins! 🏆" },
            if (bot != null) "Bot: ${bot.label}" else "Awesome game!", onAgain, onExit
        )
    }
}

// ======================= ROCK PAPER SCISSORS =======================
@Composable
fun RpsGame(onExit: () -> Unit) {
    var diff by remember { mutableStateOf<Difficulty?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val d = diff
    if (d == null) DifficultyPicker("Rock Paper Scissors", onExit) { diff = it }
    else key(round) { RpsPlay(d, onExit) { round++ } }
}

@Composable
private fun RpsPlay(d: Difficulty, onExit: () -> Unit, onAgain: () -> Unit) {
    val icons = listOf("✊", "✋", "✌️")
    val labels = listOf("Rock 🪨", "Paper 📄", "Scissors ✂️")
    val history = remember { mutableListOf<Int>() }
    var you by remember { mutableIntStateOf(0) }
    var botScore by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun counter(x: Int) = (x + 1) % 3
    fun botPick(): Int {
        if (d == Difficulty.EASY || history.isEmpty()) return Random.nextInt(3)
        val freq = history.groupingBy { it }.eachCount()
        val common = freq.maxByOrNull { it.value }!!.key
        if (d == Difficulty.MEDIUM) return if (Random.nextFloat() < 0.5f) counter(common) else Random.nextInt(3)
        val lastMove = history.last()
        val next = history.dropLast(0).withIndex().filter { it.index < history.size - 1 && it.value == lastMove }
            .map { history[it.index + 1] }.groupingBy { it }.eachCount()
        val predicted = next.maxByOrNull { it.value }?.key ?: common
        return counter(predicted)
    }

    fun play(m: Int) {
        if (you >= 5 || botScore >= 5) return
        val b = botPick(); history.add(m); last = m to b
        if (m != b) { if ((m - b + 3) % 3 == 1) you++ else botScore++ }
        if (you == 5) ScoreRepository.addWin("rps", d.points)
    }

    GameScaffold("Rock Paper Scissors", onExit) {
        Text("First to 5 wins  ·  Bot Buddy: ${d.label} 🤖", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("You: $you ⭐", color = Sun, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Bot: $botScore", color = Coral, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(20.dp))
        last?.let { (m, b) ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icons[m], fontSize = 68.sp); Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(icons[b], fontSize = 68.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (m == b) "It's a tie round! 🤝" else if ((m - b + 3) % 3 == 1) "🎉 You won this round! Woohoo!" else "🤖 Bot got this round! Keep going!",
                color = Leaf,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(20.dp))
        if (you < 5 && botScore < 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                icons.forEachIndexed { i, ic ->
                    val shape = RoundedCornerShape(22.dp)
                    Box(Modifier.size(98.dp)) {
                        Box(Modifier.fillMaxSize().padding(top = 5.dp).background(Ink, shape))
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = 5.dp)
                                .clip(shape)
                                .background(Cream)
                                .border(3.2.dp, Ink, shape)
                                .clickable { play(i) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(ic, fontSize = 40.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(labels[i], color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else EndPanel(
            if (you == 5) "🎉 You Beat the Bot! Champion! 🏆" else "🎈 Good Try! Bot Won the Match!",
            "$you - $botScore",
            onAgain,
            onExit
        )
    }
}
