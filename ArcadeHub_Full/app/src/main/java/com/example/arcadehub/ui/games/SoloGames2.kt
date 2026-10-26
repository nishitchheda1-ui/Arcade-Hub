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

// ======================= MEMORY MATCH =======================
private data class MCard(val e: String, val up: Boolean = false, val done: Boolean = false)

@Composable
fun MemoryGame(onExit: () -> Unit) {
    val emojis = listOf("🍎", "🚀", "🎸", "🐱", "⚡", "🍕", "👾", "🌈")
    fun deck() = (emojis + emojis).shuffled().map { MCard(it) }
    var cards by remember { mutableStateOf(deck()) }
    var first by remember { mutableStateOf<Int?>(null) }
    var lock by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    var secs by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }
    var run by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(run, won) { while (!won) { delay(1000); secs++ } }

    fun score() = max(50, 1000 - moves * 15 - secs * 3)
    fun reset() { cards = deck(); first = null; lock = false; moves = 0; secs = 0; won = false; run++ }

    fun tap(i: Int) {
        if (lock || cards[i].up || cards[i].done) return
        cards = cards.toMutableList().also { it[i] = it[i].copy(up = true) }
        val a = first
        if (a == null) { first = i; return }
        moves++
        if (cards[a].e == cards[i].e) {
            cards = cards.toMutableList().also { it[a] = it[a].copy(done = true); it[i] = it[i].copy(done = true) }
            first = null
            if (cards.all { it.done }) { won = true; ScoreRepository.submitBest("memory", score()) }
        } else {
            lock = true
            scope.launch {
                delay(700)
                cards = cards.toMutableList().also { it[a] = it[a].copy(up = false); it[i] = it[i].copy(up = false) }
                first = null; lock = false
            }
        }
    }

    GameScaffold("Memory Match", onExit) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Moves: $moves   Time: ${secs}s ⭐", color = Coral, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0..3) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0..3) {
                        val i = r * 4 + c
                        val card = cards[i]
                        val shape = RoundedCornerShape(16.dp)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            Box(Modifier.fillMaxSize().padding(top = 4.dp).background(Ink, shape))
                            Box(
                                Modifier.fillMaxSize().padding(bottom = 4.dp)
                                    .clip(shape)
                                    .background(if (card.up || card.done) Cream else Grape)
                                    .border(3.dp, if (card.done) Leaf else Ink, shape)
                                    .clickable { tap(i) },
                                contentAlignment = Alignment.Center
                            ) { Text(if (card.up || card.done) card.e else "⭐", fontSize = 34.sp, color = if (card.up || card.done) Ink else Sun) }
                        }
                    }
                }
            }
            if (won) EndPanel("Pairs Matched! Woohoo! 🃏🎉", "Score: ${score()}  ($moves moves, ${secs}s) ⭐", { reset() }, onExit)
        }
    }
}

// ======================= MINESWEEPER =======================
private data class Cell(val mine: Boolean = false, val open: Boolean = false, val n: Int = 0)

private fun numColor(n: Int) = when (n) {
    1 -> Color(0xFF1976D2); 2 -> Color(0xFF388E3C); 3 -> Color(0xFFD32F2F); 4 -> Color(0xFF7B1FA2); else -> Color(0xFFFF8F00)
}

@Composable
fun MinesweeperGame(onExit: () -> Unit) {
    val size = 8; val mineCount = 10
    var cells by remember { mutableStateOf(List(size * size) { Cell() }) }
    var started by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var secs by remember { mutableIntStateOf(0) }
    var run by remember { mutableIntStateOf(0) }

    LaunchedEffect(run, started, over, won) { while (started && !over && !won) { delay(1000); secs++ } }

    fun nbrs(i: Int): List<Int> {
        val r = i / size; val c = i % size
        return (-1..1).flatMap { dr -> (-1..1).map { dc -> dr to dc } }.filter { it != (0 to 0) }
            .map { (r + it.first) to (c + it.second) }
            .filter { it.first in 0 until size && it.second in 0 until size }.map { it.first * size + it.second }
    }
    fun reset() { cells = List(size * size) { Cell() }; started = false; over = false; won = false; secs = 0; run++ }
    fun place(safe: Int) {
        val ban = nbrs(safe) + safe
        val mines = (0 until size * size).filter { it !in ban }.shuffled().take(mineCount).toSet()
        cells = List(size * size) { Cell(mine = it in mines, n = nbrs(it).count { j -> j in mines }) }
        started = true
    }
    fun reveal(i: Int) {
        val list = cells.toMutableList(); val stack = mutableListOf(i)
        val visited = BooleanArray(size * size)
        while (stack.isNotEmpty()) {
            val k = stack.removeAt(stack.lastIndex)
            if (visited[k]) continue
            visited[k] = true
            val c = list[k]
            if (c.open) continue
            list[k] = c.copy(open = true)
            if (c.n == 0 && !c.mine) {
                for (nb in nbrs(k)) if (!visited[nb] && !list[nb].open) stack.add(nb)
            }
        }
        cells = list
    }
    fun tap(i: Int) {
        if (over || won || cells[i].open) return
        if (!started) place(i)
        if (cells[i].mine) { over = true; cells = cells.map { if (it.mine) it.copy(open = true) else it }; return }
        reveal(i)
        if (cells.all { it.open || it.mine }) { won = true; ScoreRepository.submitBest("minesweeper", max(50, 500 - secs * 3), 10) }
    }

    val remainingSafe = (size * size - mineCount) - cells.count { it.open && !it.mine }

    GameScaffold("Minesweeper", onExit) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏱ ${secs}s    💣 $mineCount mines    ⛳ Remaining: $remainingSafe", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("Tap any square to dig. Clear all safe squares without hitting a mine!", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Ink).padding(4.dp).clip(RoundedCornerShape(12.dp))) {
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until size) Row(Modifier.weight(1f)) {
                        for (c in 0 until size) {
                            val i = r * size + c; val cell = cells[i]; val dark = (r + c) % 2 == 0
                            val bg = when {
                                cell.open && cell.mine -> Coral
                                cell.open -> if (dark) Color(0xFFE5C29F) else Color(0xFFD7B899)
                                else -> if (dark) Color(0xFFAAD751) else Color(0xFFA2D149)
                            }
                            Box(Modifier.weight(1f).fillMaxHeight().background(bg).clickable { tap(i) }, contentAlignment = Alignment.Center) {
                                val t = when { cell.open && cell.mine -> "💣"; cell.open && cell.n > 0 -> "${cell.n}"; else -> "" }
                                Text(t, color = numColor(cell.n), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
            if (won) EndPanel("Cleared! 🎉", "Time: ${secs}s", { reset() }, onExit)
            if (over) EndPanel("Boom 💥", "You hit a mine", { reset() }, onExit)
        }
    }
}

// ======================= BREAKOUT =======================
@Composable
fun BreakoutGame(onExit: () -> Unit) {
    val w = 400f; val h = 600f
    var px by remember { mutableFloatStateOf(160f) }
    var bx by remember { mutableFloatStateOf(200f) }
    var ballY by remember { mutableFloatStateOf(524f) }
    var vx by remember { mutableFloatStateOf(0f) }
    var vy by remember { mutableFloatStateOf(0f) }
    var bricks by remember { mutableStateOf(List(40) { true }) }
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var started by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    val rowColors = listOf(Coral, Tangerine, Leaf, Sun, Grape)

    fun reset() { px = 160f; bx = 200f; ballY = 524f; vx = 0f; vy = 0f; bricks = List(40) { true }; lives = 3; score = 0; level = 1; started = false; over = false }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(0.033f); last = t
            if (over) continue
            if (!started) { bx = px + 40f; ballY = 524f; continue }
            bx += vx * dt; ballY += vy * dt
            if (bx < 8f) { bx = 8f; vx = abs(vx) }
            if (bx > w - 8f) { bx = w - 8f; vx = -abs(vx) }
            if (ballY < 8f) { ballY = 8f; vy = abs(vy) }
            if (vy > 0 && ballY + 8f >= 540f && ballY + 8f <= 560f && bx > px - 4f && bx < px + 84f) {
                vx = (bx - (px + 40f)) / 40f * 320f; vy = -abs(vy)
            }
            for (idx in bricks.indices) {
                if (!bricks[idx]) continue
                val x0 = (idx % 8) * 50f + 4f; val y0 = 60f + (idx / 8) * 24f
                if (bx + 8f > x0 && bx - 8f < x0 + 42f && ballY + 8f > y0 && ballY - 8f < y0 + 20f) {
                    bricks = bricks.toMutableList().also { it[idx] = false }
                    score += 10
                    if (bx in x0..(x0 + 42f)) vy = -vy else vx = -vx
                    break
                }
            }
            if (bricks.none { it }) { level++; bricks = List(40) { true }; started = false }
            if (ballY > h) {
                lives--
                if (lives <= 0) { over = true; ScoreRepository.submitBest("breakout", score) } else started = false
            }
        }
    }

    DisposableEffect(Unit) { onDispose { if (!over && score > 0) ScoreRepository.submitBest("breakout", score) } }

    GameScaffold("Breakout", onExit) {
        Text("Score: $score    Lives: ${"❤".repeat(max(lives, 0))}    Lv $level", color = Sun, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.aspectRatio(w / h).clip(RoundedCornerShape(14.dp))
                    .pointerInput(level, over) {
                        detectDragGestures(
                            onDragStart = {
                                if (!started && !over) {
                                    started = true
                                    vx = (Random.nextFloat() - 0.5f) * 240f
                                    vy = -(340f + level * 30f)
                                }
                            },
                            onDrag = { c, d ->
                                c.consume()
                                px = (px + d.x / (size.width / w)).coerceIn(0f, w - 80f)
                            }
                        )
                    }
                    .pointerInput(level, over) {
                        detectTapGestures {
                            if (!started && !over) {
                                started = true
                                vx = (Random.nextFloat() - 0.5f) * 240f
                                vy = -(340f + level * 30f)
                            }
                        }
                    }
            ) {
                val s = size.width / w
                drawRect(Color(0xFF1B1F3B), size = size)
                bricks.forEachIndexed { i, on ->
                    if (on) drawRoundRect(rowColors[i / 8], Offset(((i % 8) * 50f + 4f) * s, (60f + (i / 8) * 24f) * s), Size(42f * s, 20f * s), CornerRadius(4f * s))
                }
                drawRoundRect(Color.White, Offset(px * s, 540f * s), Size(80f * s, 12f * s), CornerRadius(6f * s))
                drawCircle(Color.White, 8f * s, Offset(bx * s, ballY * s))
                drawFrame()
            }
            if (!started && !over) Text("TAP TO LAUNCH BALL! 🚀\nDrag paddle to bounce", color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
        }
        if (over) EndPanel("Oops! Out of Balls! 🧱", "Score: $score  •  Level: $level ⭐", { reset() }, onExit)
    }
}

// ======================= WHACK-A-MOLE =======================
@Composable
fun WhackGame(onExit: () -> Unit) {
    var mole by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var time by remember { mutableIntStateOf(30) }
    var run by remember { mutableIntStateOf(0) }

    LaunchedEffect(run) {
        while (time > 0) { delay(1000); time-- }
        mole = -1; ScoreRepository.submitBest("whack", score)
    }
    LaunchedEffect(run) {
        while (time > 0) {
            mole = Random.nextInt(9)
            delay((750L - score * 8L).coerceAtLeast(350L))
            mole = -1; delay(120)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (score > 0) ScoreRepository.submitBest("whack", score)
        }
    }

    GameScaffold("Whack-a-Mole", onExit) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Moles Whacked: $score 🔨    Time: ${time}s", color = Sun, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF7BC96F), RoundedCornerShape(20.dp)).border(4.dp, Ink, RoundedCornerShape(20.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (r in 0..2) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (c in 0..2) {
                        val i = r * 3 + c
                        Box(
                            Modifier.weight(1f).fillMaxHeight().background(if (mole == i) Color(0xFF8B5A2B) else Color(0xFF5D3A1A), CircleShape)
                                .border(4.dp, Ink, CircleShape)
                                .clickable { if (time > 0 && mole == i) { score++; mole = -1 } },
                            contentAlignment = Alignment.Center
                        ) { if (mole == i) Text("🐹", fontSize = 44.sp) }
                    }
                }
            }
            if (time == 0) EndPanel("Time's Up! Super Whacking! 🐹", "You whacked $score cheeky moles! ⭐", { score = 0; time = 30; mole = -1; run++ }, onExit)
        }
    }
}
