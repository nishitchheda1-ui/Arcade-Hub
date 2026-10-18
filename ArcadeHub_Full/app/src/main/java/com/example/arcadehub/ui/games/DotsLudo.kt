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

// ======================= DOTS & BOXES =======================
private fun boxesOf(id: Int): List<Int> =
    if (id < 20) { val r = id / 4; val c = id % 4; listOfNotNull(if (r > 0) (r - 1) * 4 + c else null, if (r < 4) r * 4 + c else null) }
    else { val i = id - 20; val r = i / 5; val c = i % 5; listOfNotNull(if (c > 0) r * 4 + c - 1 else null, if (c < 4) r * 4 + c else null) }

private fun boxSides(b: Int): List<Int> { val r = b / 4; val c = b % 4; return listOf(r * 4 + c, (r + 1) * 4 + c, 20 + r * 5 + c, 20 + r * 5 + c + 1) }

private fun edgeLine(id: Int, m: Float, sp: Float): Pair<Offset, Offset> =
    if (id < 20) { val r = id / 4; val c = id % 4; Offset(m + c * sp, m + r * sp) to Offset(m + (c + 1) * sp, m + r * sp) }
    else { val i = id - 20; val r = i / 5; val c = i % 5; Offset(m + c * sp, m + r * sp) to Offset(m + c * sp, m + (r + 1) * sp) }

@Composable
fun DotsGame(onExit: () -> Unit) {
    var ps by remember { mutableStateOf<List<PlayerCfg>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val p = ps
    if (p == null) SetupScreen("Dots & Boxes", onExit) { ps = it }
    else key(round) { DotsPlay(p, onExit) { round++ } }
}

@Composable
private fun DotsPlay(ps: List<PlayerCfg>, onExit: () -> Unit, onAgain: () -> Unit) {
    val edges = remember { mutableStateListOf<Int>().apply { repeat(40) { add(-1) } } }
    val boxes = remember { mutableStateListOf<Int>().apply { repeat(16) { add(-1) } } }
    var cur by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }

    fun take(id: Int) {
        if (over || edges[id] != -1) return
        edges[id] = cur
        var got = 0
        for (b in boxesOf(id)) if (boxes[b] == -1 && boxSides(b).all { edges[it] != -1 }) { boxes[b] = cur; got++ }
        if (boxes.none { it == -1 }) {
            over = true
            val counts = ps.indices.map { p -> boxes.count { it == p } }
            val mx = counts.max()
            if (counts[0] == mx && counts.count { it == mx } == 1) ScoreRepository.addWin("dots", ps.getOrNull(1)?.bot?.points ?: 5)
        } else if (got == 0) cur = (cur + 1) % ps.size
    }

    fun botEdge(d: Difficulty): Int {
        val free = (0..39).filter { edges[it] == -1 }
        fun sides(b: Int) = boxSides(b).count { edges[it] != -1 }
        if (d == Difficulty.EASY) return free.random()
        val completing = free.filter { e -> boxesOf(e).any { sides(it) == 3 } }
        if (completing.isNotEmpty()) return completing.random()
        if (d == Difficulty.MEDIUM) return free.random()
        val safe = free.filter { e -> boxesOf(e).all { sides(it) < 2 } }
        if (safe.isNotEmpty()) return safe.random()
        return free.minByOrNull { e -> boxesOf(e).count { sides(it) == 2 } }!!
    }

    val taken = edges.count { it != -1 }
    LaunchedEffect(cur, taken, over) {
        val b = ps[cur].bot
        if (b != null && !over) { delay(500); take(botEdge(b)) }
    }

    GameScaffold("Dots & Boxes", onExit) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ps.indices.forEach { i ->
                Text("${ps[i].name}: ${boxes.count { it == i }}", color = ps[i].color,
                    fontWeight = if (i == cur && !over) FontWeight.Black else FontWeight.Normal, fontSize = if (i == cur && !over) 18.sp else 14.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f).background(Cream, RoundedCornerShape(16.dp)).border(4.dp, Ink, RoundedCornerShape(16.dp)).pointerInput(Unit) {
                detectTapGestures { o ->
                    if (ps[cur].bot != null || over) return@detectTapGestures
                    val m = size.width * 0.1f; val sp = (size.width - 2 * m) / 4f
                    var best = -1; var bd = Float.MAX_VALUE
                    for (id in 0..39) {
                        val (a, b) = edgeLine(id, m, sp)
                        val d = ((a + b) / 2f - o).getDistance()
                        if (d < bd) { bd = d; best = id }
                    }
                    if (bd < sp * 0.4f) take(best)
                }
            }
        ) {
            val m = size.width * 0.1f; val sp = (size.width - 2 * m) / 4f
            for (b in 0..15) if (boxes[b] != -1)
                drawRect(ps[boxes[b]].color.copy(alpha = 0.55f), Offset(m + (b % 4) * sp, m + (b / 4) * sp), Size(sp, sp))
            for (id in 0..39) {
                val (a, b) = edgeLine(id, m, sp)
                if (edges[id] == -1) drawLine(Ink.copy(alpha = 0.12f), a, b, 4f)
                else { drawLine(Ink, a, b, 17f, cap = StrokeCap.Round); drawLine(ps[edges[id]].color, a, b, 9f, cap = StrokeCap.Round) }
            }
            for (r in 0..4) for (c in 0..4) drawCircle(Ink, 9f, Offset(m + c * sp, m + r * sp))
        }
        if (over) {
            val counts = ps.indices.map { p -> boxes.count { it == p } }
            val mx = counts.max()
            val winners = ps.indices.filter { counts[it] == mx }.joinToString(" & ") { ps[it].name }
            EndPanel(if (counts.count { it == mx } > 1) "Tie: $winners" else "$winners wins!", ps.indices.joinToString("  ") { "${ps[it].name} ${counts[it]}" }, onAgain, onExit)
        }
    }
}

// ======================= LUDO-STYLE RACE =======================
// Track squares 1..30, 0 = base, 31 = home. Each player has 2 tokens. Land on an opponent to send them back.
@Composable
fun LudoGame(onExit: () -> Unit) {
    var ps by remember { mutableStateOf<List<PlayerCfg>?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val p = ps
    if (p == null) SetupScreen("Ludo Race", onExit) { ps = it }
    else key(round) { LudoPlay(p, onExit) { round++ } }
}

@Composable
private fun LudoPlay(ps: List<PlayerCfg>, onExit: () -> Unit, onAgain: () -> Unit) {
    val n = ps.size
    var tokens by remember { mutableStateOf(List(n) { listOf(0, 0) }) }
    var turn by remember { mutableIntStateOf(0) }
    var dice by remember { mutableIntStateOf(0) }
    var winner by remember { mutableIntStateOf(-1) }
    var msg by remember { mutableStateOf("") }

    fun movable() = if (dice == 0) emptyList() else (0..1).filter { tokens[turn][it] < 31 }

    fun move(t: Int) {
        val roll = dice
        val np = min(31, tokens[turn][t] + roll)
        val nt = tokens.map { it.toMutableList() }
        nt[turn][t] = np
        var captured = false
        if (np in 1..30) for (o in 0 until n) if (o != turn) for (k in 0..1) if (nt[o][k] == np) { nt[o][k] = 0; captured = true }
        tokens = nt.map { it.toList() }
        msg = if (captured) "${ps[turn].name} captured a token!" else ""
        if (nt[turn].all { it == 31 }) {
            winner = turn
            if (turn == 0) ScoreRepository.addWin("ludo", ps.getOrNull(1)?.bot?.points ?: 5)
            return
        }
        dice = 0
        if (!(roll == 6 || captured)) turn = (turn + 1) % n
    }

    fun botChoice(): Int {
        val m = movable()
        val capture = m.firstOrNull { t ->
            val np = min(31, tokens[turn][t] + dice)
            np in 1..30 && (0 until n).any { o -> o != turn && tokens[o].any { it == np } }
        }
        return capture ?: m.maxByOrNull { tokens[turn][it] }!!
    }

    LaunchedEffect(turn, dice, winner) {
        if (winner >= 0) return@LaunchedEffect
        val isBot = ps[turn].bot != null
        if (dice == 0) {
            if (isBot) { delay(700); dice = Random.nextInt(1, 7) }
        } else {
            val m = movable()
            if (isBot) { delay(700); move(botChoice()) }
            else if (m.size == 1 || tokens[turn][0] == tokens[turn][1]) { delay(400); move(m[0]) }
        }
    }

    GameScaffold("Ludo Race", onExit) {
        Text(if (winner >= 0) "${ps[winner].name} wins!" else "${ps[turn].name}'s turn", color = ps[turn].color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(msg, color = Leaf, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().aspectRatio(6f / 5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (ri in 0..4) {
                val row = 4 - ri
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (dc in 0..5) {
                        val pos = row * 6 + (if (row % 2 == 0) dc else 5 - dc) + 1
                        Box(
                            Modifier.weight(1f).fillMaxHeight().background(if (pos == 30) Sun else Cream, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (pos == 30) "🏁" else "$pos", color = Ink.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (p in 0 until n) for (k in 0..1) if (tokens[p][k] == pos)
                                    Box(Modifier.size(16.dp).background(ps[p].color, CircleShape).border(2.dp, Ink, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ps.indices.forEach { p ->
            Text("${ps[p].name}: base ${tokens[p].count { it == 0 }} · home ${tokens[p].count { it == 31 }}/2", color = ps[p].color, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        if (winner < 0 && ps[turn].bot == null) {
            if (dice == 0) GameButton("🎲 ROLL DICE", { dice = Random.nextInt(1, 7) }, Modifier.fillMaxWidth(), color = ps[turn].color)
            else {
                Text("Rolled a $dice", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    movable().forEach { t ->
                        GameButton("MOVE TOKEN ${if (t == 0) "A" else "B"}  (${tokens[turn][t]})", { move(t) }, Modifier.weight(1f), color = ps[turn].color)
                    }
                }
            }
        } else if (winner < 0) Text(if (dice == 0) "Bot rolling..." else "Bot rolled $dice", color = Color.White)
        if (winner >= 0) EndPanel("${ps[winner].name} wins!", "Both tokens reached home", onAgain, onExit)
    }
}
