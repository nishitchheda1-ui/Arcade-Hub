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

// ======================= SNAKE =======================
@Composable
fun SnakeGame(onExit: () -> Unit) {
    val n = 18
    var snake by remember { mutableStateOf(listOf(9 to 9, 8 to 9, 7 to 9)) }
    var dir by remember { mutableStateOf(1 to 0) }
    var next by remember { mutableStateOf(1 to 0) }
    var food by remember { mutableStateOf(13 to 9) }
    var score by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var run by remember { mutableIntStateOf(0) }

    fun turn(d: Pair<Int, Int>) { if (d.first != -dir.first || d.second != -dir.second) next = d }
    fun reset() { snake = listOf(9 to 9, 8 to 9, 7 to 9); dir = 1 to 0; next = 1 to 0; food = 13 to 9; score = 0; over = false; run++ }

    LaunchedEffect(run) {
        while (!over) {
            delay((150L - score * 3L).coerceAtLeast(70L))
            dir = next
            val head = snake.first()
            val nh = (head.first + dir.first) to (head.second + dir.second)
            if (nh.first !in 0 until n || nh.second !in 0 until n || nh in snake.dropLast(1)) {
                over = true; ScoreRepository.submitBest("snake", score); break
            }
            val ate = nh == food
            snake = listOf(nh) + (if (ate) snake else snake.dropLast(1))
            if (ate) { score++; food = generateSequence { Random.nextInt(n) to Random.nextInt(n) }.first { it !in snake } }
        }
    }

    val currentSnakeScore by rememberUpdatedState(score)
    val currentSnakeOver by rememberUpdatedState(over)
    DisposableEffect(Unit) {
        onDispose {
            if (!currentSnakeOver && currentSnakeScore > 0) {
                ScoreRepository.submitBest("snake", currentSnakeScore)
            }
        }
    }

    GameScaffold("Snake", onExit) {
        Text("Score: $score", color = Leaf, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Swipe to steer", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).pointerInput(Unit) {
                var ax = 0f; var ay = 0f
                detectDragGestures(onDragStart = { ax = 0f; ay = 0f }, onDrag = { c, d ->
                    c.consume(); ax += d.x; ay += d.y
                    if (abs(ax) > 40f || abs(ay) > 40f) {
                        turn(if (abs(ax) > abs(ay)) (if (ax > 0) 1 else -1) to 0 else 0 to (if (ay > 0) 1 else -1))
                        ax = 0f; ay = 0f
                    }
                })
            }
        ) {
            val c = size.width / n
            for (gx in 0 until n) for (gy in 0 until n)
                drawRect(if ((gx + gy) % 2 == 0) Color(0xFFAAD751) else Color(0xFFA2D149), Offset(gx * c, gy * c), Size(c, c))
            drawCircle(Ink, c * 0.42f, Offset(food.first * c + c / 2, food.second * c + c / 2))
            drawCircle(Coral, c * 0.34f, Offset(food.first * c + c / 2, food.second * c + c / 2))
            snake.forEachIndexed { i, p ->
                drawRoundRect(Ink, Offset(p.first * c + 0.5f, p.second * c + 0.5f), Size(c - 1f, c - 1f), CornerRadius(c * 0.35f))
                drawRoundRect(if (i == 0) Color(0xFF2F5FD0) else Color(0xFF4F86F7), Offset(p.first * c + 2.5f, p.second * c + 2.5f), Size(c - 5f, c - 5f), CornerRadius(c * 0.3f))
            }
            drawFrame()
        }
        if (over) EndPanel("Game Over", "Score: $score", { reset() }, onExit)
    }
}

// ======================= FLAPPY BIRD =======================
private data class Pipe(val x: Float, val gap: Float, val passed: Boolean = false)

@Composable
fun FlappyGame(onExit: () -> Unit) {
    val w = 400f; val h = 700f
    var birdY by remember { mutableFloatStateOf(300f) }
    var bv by remember { mutableFloatStateOf(0f) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    var score by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }

    fun reset() { birdY = 300f; bv = 0f; pipes = emptyList(); score = 0; started = false; over = false }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(0.033f); last = t
            if (!started || over) continue
            bv += 1300f * dt; birdY += bv * dt
            var ps = pipes.map { it.copy(x = it.x - 170f * dt) }.filter { it.x > -70f }
            if (ps.isEmpty() || ps.last().x < w - 230f) ps = ps + Pipe(w + 10f, Random.nextFloat() * (h - 390f) + 110f)
            ps = ps.map { if (!it.passed && it.x + 60f < 100f) { score++; it.copy(passed = true) } else it }
            pipes = ps
            val hit = birdY < 16f || birdY > h - 16f ||
                ps.any { 114f > it.x && 86f < it.x + 60f && (birdY - 14f < it.gap || birdY + 14f > it.gap + 170f) }
            if (hit) { over = true; ScoreRepository.submitBest("flappy", score) }
        }
    }

    val currentFlappyScore by rememberUpdatedState(score)
    val currentFlappyOver by rememberUpdatedState(over)
    DisposableEffect(Unit) {
        onDispose {
            if (!currentFlappyOver && currentFlappyScore > 0) {
                ScoreRepository.submitBest("flappy", currentFlappyScore)
            }
        }
    }

    GameScaffold("Flappy Bird", onExit) {
        Text("Score: $score", color = Sun, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.aspectRatio(w / h).clip(RoundedCornerShape(14.dp)).pointerInput(Unit) {
                    detectTapGestures { if (!over) { started = true; bv = -420f } }
                }
            ) {
                val s = size.width / w
                drawRect(Color(0xFF70C5CE), size = size)
                fun pipe(x: Float, y: Float, ph: Float) {
                    drawRect(Color(0xFF73BF2E), Offset(x * s, y * s), Size(60f * s, ph * s))
                    drawRect(Ink, Offset(x * s, y * s), Size(60f * s, ph * s), style = Stroke(4f * s))
                }
                pipes.forEach { pipe(it.x, -10f, it.gap + 10f); pipe(it.x, it.gap + 170f, h - it.gap - 160f) }
                drawRect(Color(0xFFDED895), Offset(0f, (h - 16f) * s), Size(size.width, 16f * s))
                drawCircle(Ink, 17f * s, Offset(100f * s, birdY * s))
                drawCircle(Sun, 14f * s, Offset(100f * s, birdY * s))
                drawCircle(Color.White, 5f * s, Offset(106f * s, (birdY - 4f) * s))
                drawCircle(Ink, 2.2f * s, Offset(107.5f * s, (birdY - 4f) * s))
                drawCircle(Coral, 4f * s, Offset(114f * s, (birdY + 3f) * s))
                drawFrame()
            }
            if (!started) Text("TAP TO FLY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        if (over) EndPanel("Crashed!", "Score: $score", { reset() }, onExit)
    }
}

// ======================= 2048 =======================
private fun slideRow(r: List<Int>): Pair<List<Int>, Int> {
    val t = r.filter { it != 0 }; val out = mutableListOf<Int>(); var s = 0; var i = 0
    while (i < t.size) {
        if (i + 1 < t.size && t[i] == t[i + 1]) { out.add(t[i] * 2); s += t[i] * 2; i += 2 } else { out.add(t[i]); i++ }
    }
    while (out.size < 4) out.add(0)
    return out to s
}

private fun transpose(b: List<List<Int>>) = List(4) { c -> List(4) { r -> b[r][c] } }

/** d: 0 left, 1 right, 2 up, 3 down -> (board, gained, changed) */
private fun move2048(b: List<List<Int>>, d: Int): Triple<List<List<Int>>, Int, Boolean> {
    val lines = when (d) { 0 -> b; 1 -> b.map { it.reversed() }; 2 -> transpose(b); else -> transpose(b).map { it.reversed() } }
    var gain = 0
    val res = lines.map { val (r, s) = slideRow(it); gain += s; r }
    val back = when (d) { 0 -> res; 1 -> res.map { it.reversed() }; 2 -> transpose(res); else -> transpose(res.map { it.reversed() }) }
    return Triple(back, gain, back != b)
}

private fun spawn2048(b: List<List<Int>>): List<List<Int>> {
    val e = (0..15).filter { b[it / 4][it % 4] == 0 }
    if (e.isEmpty()) return b
    val i = e.random(); val v = if (Random.nextFloat() < 0.9f) 2 else 4
    return b.mapIndexed { r, row -> row.mapIndexed { c, x -> if (r * 4 + c == i) v else x } }
}

private fun tile2048(v: Int) = when (v) {
    0 -> Color(0xFFCDC1B4); 2 -> Color(0xFFEEE4DA); 4 -> Color(0xFFEDE0C8); 8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563); 32 -> Color(0xFFF67C5F); 64 -> Color(0xFFF65E3B); 128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61); 512 -> Color(0xFFEDC850); 1024 -> Color(0xFFEDC53F); 2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

@Composable
fun Game2048(onExit: () -> Unit) {
    fun fresh() = spawn2048(spawn2048(List(4) { List(4) { 0 } }))
    var board by remember { mutableStateOf(fresh()) }
    var score by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var localBest by remember { mutableLongStateOf(ScoreRepository.getLocalBest("2048")) }

    fun doMove(d: Int) {
        if (over) return
        val (nb, g, changed) = move2048(board, d)
        if (!changed) return
        val newScore = score + g
        board = spawn2048(nb)
        score = newScore
        if (newScore > localBest) {
            localBest = newScore.toLong()
        }
        if ((0..3).none { move2048(board, it).third }) {
            over = true
            ScoreRepository.submitBest("2048", newScore, points = 10)
        }
    }

    // Submit score on navigate-away only if game was NOT already over
    // (game-over already submitted above)
    val current2048Score by rememberUpdatedState(score)
    val current2048Over by rememberUpdatedState(over)
    DisposableEffect(Unit) {
        onDispose {
            if (!current2048Over && current2048Score > 0) {
                ScoreRepository.submitBest("2048", current2048Score, points = 5)
            }
        }
    }

    val maxTile = board.flatten().maxOrNull() ?: 0

    GameScaffold("2048", onExit) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("SCORE", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("$score", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text("BEST", color = Ink.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("$localBest", color = Sun, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            if (maxTile > 0) {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Sun).border(2.dp, Ink, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Tile: $maxTile", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        Text("Swipe in any direction to merge numbers", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFBBADA0), RoundedCornerShape(16.dp)).border(4.dp, Ink, RoundedCornerShape(16.dp)).padding(8.dp)
                .pointerInput(Unit) {
                    var ax = 0f; var ay = 0f
                    detectDragGestures(
                        onDragStart = { ax = 0f; ay = 0f },
                        onDrag = { c, d -> c.consume(); ax += d.x; ay += d.y },
                        onDragEnd = { if (abs(ax) > abs(ay)) doMove(if (ax > 0) 1 else 0) else doMove(if (ay > 0) 3 else 2) }
                    )
                }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                board.forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { v ->
                            val col = tile2048(v)
                            Box(Modifier.weight(1f).fillMaxHeight().background(col, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                if (v > 0) Text("$v", color = if (v <= 4) Color(0xFF776E65) else Color.White, fontWeight = FontWeight.Black, fontSize = if (v < 1000) 24.sp else 16.sp)
                            }
                        }
                    }
                }
            }
        }
        if (over) EndPanel("No more moves", "Score: $score  •  Best: $localBest", { board = fresh(); score = 0; over = false }, onExit)
    }
}
