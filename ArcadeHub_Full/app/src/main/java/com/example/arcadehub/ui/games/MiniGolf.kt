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

private const val BALL_R = 8f
private const val MAX_STROKES = 8

private data class Hole(val tee: Offset, val cup: Offset, val walls: List<Rect>)

private val HOLES = listOf(
    Hole(Offset(200f, 520f), Offset(200f, 90f), listOf(Rect(120f, 280f, 280f, 304f))),
    Hole(Offset(80f, 520f), Offset(320f, 90f), listOf(Rect(12f, 300f, 260f, 324f), Rect(140f, 160f, 388f, 184f))),
    Hole(Offset(200f, 530f), Offset(200f, 80f), listOf(Rect(60f, 380f, 170f, 404f), Rect(230f, 380f, 340f, 404f), Rect(160f, 200f, 240f, 280f)))
)

private fun stepBall(p0: Offset, v0: Offset, dt: Float, walls: List<Rect>): Pair<Offset, Offset> {
    var p = p0 + v0 * dt
    var v = v0 * (1f - 1.8f * dt).coerceAtLeast(0f)
    if (p.x < 12f + BALL_R) { p = Offset(12f + BALL_R, p.y); v = Offset(abs(v.x) * 0.85f, v.y) }
    if (p.x > 388f - BALL_R) { p = Offset(388f - BALL_R, p.y); v = Offset(-abs(v.x) * 0.85f, v.y) }
    if (p.y < 12f + BALL_R) { p = Offset(p.x, 12f + BALL_R); v = Offset(v.x, abs(v.y) * 0.85f) }
    if (p.y > 588f - BALL_R) { p = Offset(p.x, 588f - BALL_R); v = Offset(v.x, -abs(v.y) * 0.85f) }
    for (w in walls) {
        val cx = p.x.coerceIn(w.left, w.right); val cy = p.y.coerceIn(w.top, w.bottom)
        val dx = p.x - cx; val dy = p.y - cy; val d2 = dx * dx + dy * dy
        if (d2 < BALL_R * BALL_R) {
            if (d2 > 0.0001f) {
                val d = sqrt(d2); val nx = dx / d; val ny = dy / d
                p = Offset(cx + nx * BALL_R, cy + ny * BALL_R)
                val dot = v.x * nx + v.y * ny
                if (dot < 0f) v = Offset(v.x - 2f * dot * nx, v.y - 2f * dot * ny) * 0.85f
            } else v = -v
        }
    }
    return p to v
}

/** Closest approach to the cup (at low speed) for a given launch velocity. < 16 means it drops in. */
private fun simulate(start: Offset, v0: Offset, h: Hole): Float {
    var p = start; var v = v0; var best = 9999f
    repeat(600) {
        val (np, nv) = stepBall(p, v, 1f / 60f, h.walls); p = np; v = nv
        val sp = v.getDistance()
        if (sp < 450f) best = min(best, (p - h.cup).getDistance())
        if (sp < 10f) return best
    }
    return best
}

/** Bots simulate N candidate shots with the real physics and pick the best. More candidates = better bot. */
private fun botShot(start: Offset, h: Hole, d: Difficulty): Offset {
    val n = when (d) { Difficulty.EASY -> 4; Difficulty.MEDIUM -> 25; Difficulty.HARD -> 150 }
    val toCup = h.cup - start
    val dist = toCup.getDistance()
    var bestV = toCup / dist * min(dist * 1.9f, 950f)
    var bestS = if (d == Difficulty.EASY) Float.MAX_VALUE else simulate(start, bestV, h)
    repeat(n) {
        val ang = Random.nextFloat() * 2f * PI.toFloat()
        val sp = 250f + Random.nextFloat() * 700f
        val v = Offset(cos(ang) * sp, sin(ang) * sp)
        val s = simulate(start, v, h)
        if (s < bestS) { bestS = s; bestV = v }
    }
    return bestV
}

@Composable
fun MiniGolfGame(onExit: () -> Unit) {
    var ps by remember { mutableStateOf<List<PlayerCfg>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val p = ps
    if (p == null) SetupScreen("Mini Golf Battle", onExit) { ps = it }
    else key(round) { GolfPlay(p, onExit) { round++ } }
}

@Composable
private fun GolfPlay(ps: List<PlayerCfg>, onExit: () -> Unit, onAgain: () -> Unit) {
    var hole by remember { mutableIntStateOf(0) }
    var cur by remember { mutableIntStateOf(0) }
    val totals = remember { mutableStateListOf<Int>().apply { repeat(ps.size) { add(0) } } }
    var strokes by remember { mutableIntStateOf(0) }
    var pos by remember { mutableStateOf(HOLES[0].tee) }
    var vel by remember { mutableStateOf(Offset.Zero) }
    var rolling by remember { mutableStateOf(false) }
    var aim by remember { mutableStateOf<Offset?>(null) }
    var finished by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf("") }

    fun advance() {
        totals[cur] = totals[cur] + strokes; strokes = 0
        if (cur < ps.size - 1) cur++
        else if (hole < HOLES.size - 1) { hole++; cur = 0 }
        else {
            finished = true
            val best = totals.min()
            if (totals[0] == best && totals.count { it == best } == 1) ScoreRepository.addWin("minigolf", ps.getOrNull(1)?.bot?.points ?: 5)
            return
        }
        pos = HOLES[hole].tee; vel = Offset.Zero
    }
    fun hit(v: Offset) { vel = v; rolling = true; strokes++ }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = withFrameNanos { it }
            if (last == 0L) { last = t; continue }
            val dt = ((t - last) / 1e9f).coerceAtMost(0.03f); last = t
            if (!rolling) continue
            val hh = HOLES[hole]
            val (np, nv) = stepBall(pos, vel, dt, hh.walls)
            pos = np; vel = nv
            val sp = nv.getDistance()
            if ((np - hh.cup).getDistance() < 16f && sp < 450f) {
                rolling = false; banner = "⛳ In the hole!"; delay(700); banner = ""; advance()
            } else if (sp < 10f) {
                vel = Offset.Zero; rolling = false
                if (strokes >= MAX_STROKES) { banner = "Max strokes"; delay(600); banner = ""; advance() }
            }
        }
    }

    LaunchedEffect(cur, hole, strokes, rolling, finished) {
        val b = ps[cur].bot
        if (b != null && !rolling && !finished) {
            delay(700)
            val v = withContext(Dispatchers.Default) { botShot(pos, HOLES[hole], b) }
            hit(v)
        }
    }

    GameScaffold("Mini Golf Battle", onExit) {
        if (!finished) {
            Text("Hole ${hole + 1}/${HOLES.size} · ${ps[cur].name} · Strokes $strokes", color = ps[cur].color, fontWeight = FontWeight.Bold)
            Text(if (ps[cur].bot != null) "Bot is aiming..." else "Drag back and release to shoot", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.aspectRatio(400f / 600f).pointerInput(Unit) {
                    var s0 = Offset.Zero
                    detectDragGestures(
                        onDragStart = { s0 = it },
                        onDrag = { c, _ ->
                            c.consume()
                            if (!rolling && !finished && ps[cur].bot == null) aim = (s0 - c.position) / (size.width / 400f)
                        },
                        onDragEnd = {
                            val a = aim
                            if (a != null && !rolling && !finished && ps[cur].bot == null) {
                                val len = a.getDistance()
                                if (len > 8f) hit(a / len * min(len * 5f, 950f))
                            }
                            aim = null
                        },
                        onDragCancel = { aim = null }
                    )
                }
            ) {
                val s = size.width / 400f
                val hh = HOLES[min(hole, HOLES.lastIndex)]
                drawRoundRect(Ink, Offset(6f * s, 6f * s), Size(388f * s, 588f * s), CornerRadius(16f * s))
                drawRoundRect(Color(0xFF58B947), Offset(12f * s, 12f * s), Size(376f * s, 576f * s), CornerRadius(12f * s))
                hh.walls.forEach {
                    drawRect(Color(0xFF8B5A2B), Offset(it.left * s, it.top * s), Size(it.width * s, it.height * s))
                    drawRect(Ink, Offset(it.left * s, it.top * s), Size(it.width * s, it.height * s), style = Stroke(4f * s))
                }
                drawCircle(Color.Black, 14f * s, hh.cup * s)
                drawCircle(Color.White.copy(alpha = 0.25f), 6f * s, hh.tee * s)
                drawCircle(ps[cur].color, (BALL_R + 2f) * s, pos * s)
                drawCircle(Color.White, BALL_R * s, pos * s)
                aim?.let { a ->
                    val len = a.getDistance()
                    if (len > 1f) {
                        val e = pos + a * (min(len, 190f) / len)
                        drawLine(Color.White.copy(alpha = 0.8f), pos * s, e * s, 4f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    }
                }
            }
            if (banner.isNotEmpty()) Text(banner, color = Leaf, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Text(ps.indices.joinToString("   ") { "${ps[it].name}: ${totals[it]}" }, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        if (finished) {
            val best = totals.min()
            val winners = ps.indices.filter { totals[it] == best }.joinToString(" & ") { ps[it].name }
            EndPanel("$winners wins!", ps.indices.joinToString("  ") { "${ps[it].name} ${totals[it]}" } + " strokes", onAgain, onExit)
        }
    }
}
