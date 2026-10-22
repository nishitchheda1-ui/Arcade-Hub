package com.example.arcadehub.ui.games

import androidx.compose.animation.core.*
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

private val FLEET = listOf(5, 4, 3, 3, 2)

private fun randomFleet(): List<List<Int>> {
    while (true) {
        val used = HashSet<Int>(); val ships = ArrayList<List<Int>>(); var ok = true
        for (len in FLEET) {
            var placed = false
            repeat(200) {
                if (!placed) {
                    val horiz = Random.nextBoolean()
                    val r = Random.nextInt(if (horiz) 10 else 11 - len)
                    val c = Random.nextInt(if (horiz) 11 - len else 10)
                    val cells = List(len) { if (horiz) r * 10 + c + it else (r + it) * 10 + c }
                    if (cells.none { it in used }) { used.addAll(cells); ships.add(cells); placed = true }
                }
            }
            if (!placed) { ok = false; break }
        }
        if (ok) return ships
    }
}

private fun allSunk(ships: List<List<Int>>, shots: List<Int>) = ships.all { s -> s.all { shots[it] == 2 } }

private fun neighbors(i: Int): List<Int> {
    val r = i / 10; val c = i % 10
    return listOfNotNull(if (r > 0) i - 10 else null, if (r < 9) i + 10 else null, if (c > 0) i - 1 else null, if (c < 9) i + 1 else null)
}

/** Bot shooting logic. Easy = random, Medium = hunt on a checkerboard + chase hits, Hard = probability density. */
private fun botShot(shots: List<Int>, ships: List<List<Int>>, d: Difficulty): Int {
    val unfired = (0..99).filter { shots[it] == 0 }
    val sunkCells = ships.filter { s -> s.all { shots[it] == 2 } }.flatten().toSet()
    val activeHits = (0..99).filter { shots[it] == 2 && it !in sunkCells }
    if (d == Difficulty.EASY) return unfired.random()
    if (d == Difficulty.MEDIUM) {
        val cand = activeHits.flatMap { neighbors(it) }.filter { shots[it] == 0 }
        if (cand.isNotEmpty()) return cand.random()
        return unfired.filter { (it / 10 + it % 10) % 2 == 0 }.ifEmpty { unfired }.random()
    }
    val remaining = ships.filter { s -> !s.all { shots[it] == 2 } }.map { it.size }
    val dens = IntArray(100)
    val hitSet = activeHits.toSet()
    for (len in remaining) for (horiz in listOf(true, false)) {
        for (r in 0 until (if (horiz) 10 else 11 - len)) for (c in 0 until (if (horiz) 11 - len else 10)) {
            val cells = List(len) { if (horiz) r * 10 + c + it else (r + it) * 10 + c }
            if (cells.any { shots[it] == 1 || it in sunkCells }) continue
            val weight = 1 + cells.count { it in hitSet } * 30
            for (cell in cells) if (shots[cell] == 0) dens[cell] += weight
        }
    }
    val best = unfired.maxOf { dens[it] }
    return unfired.filter { dens[it] == best }.random()
}

@Composable
fun BattleshipGame(onExit: () -> Unit) {
    var diff by remember { mutableStateOf<Difficulty?>(null) }
    var round by remember { mutableIntStateOf(0) }
    val d = diff
    if (d == null) DifficultyPicker("Battleship", onExit) { diff = it }
    else key(round) { BattleshipPlay(d, onExit) { round++ } }
}

@Composable
private fun BattleshipPlay(d: Difficulty, onExit: () -> Unit, onAgain: () -> Unit) {
    var myShips by remember { mutableStateOf(randomFleet()) }
    val enemyShips = remember { randomFleet() }
    val shotsAtMe = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } }
    val myShots = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } }
    var phase by remember { mutableIntStateOf(0) } // 0 placing, 1 playing, 2 over
    var myTurn by remember { mutableStateOf(true) }
    var won by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("Shuffle your fleet, then start") }
    var botTick by remember { mutableIntStateOf(0) } // bumps so the bot can shoot again after a hit

    fun sunkAt(ships: List<List<Int>>, shots: List<Int>, i: Int) = ships.firstOrNull { i in it }?.all { shots[it] == 2 } == true

    fun fire(i: Int) {
        if (phase != 1 || !myTurn || myShots[i] != 0) return
        val hit = enemyShips.any { i in it }
        myShots[i] = if (hit) 2 else 1
        msg = if (!hit) "You missed" else if (sunkAt(enemyShips, myShots, i)) "You sunk a ship! Shoot again" else "Hit! Shoot again"
        if (allSunk(enemyShips, myShots)) { phase = 2; won = true; ScoreRepository.addWin("battleship", d.points) }
        else if (!hit) myTurn = false // a hit keeps the turn, only a miss passes it to the bot
    }

    LaunchedEffect(myTurn, phase, botTick) {
        if (phase == 1 && !myTurn) {
            delay(700)
            val i = withContext(Dispatchers.Default) { botShot(shotsAtMe.toList(), myShips, d) }
            val hit = myShips.any { i in it }
            shotsAtMe[i] = if (hit) 2 else 1
            msg = if (!hit) "Bot missed - your turn" else if (sunkAt(myShips, shotsAtMe, i)) "Bot sunk your ship!" else "Bot hit your ship!"
            if (allSunk(myShips, shotsAtMe)) { phase = 2; won = false }
            else if (hit) botTick++ // bot hit, so it shoots again
            else myTurn = true
        }
    }

    GameScaffold("Battleship (${d.label})", onExit) {
        Text(msg, color = Leaf, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (phase == 0) {
            Text("Your fleet", color = Color.White)
            BattleGrid(shotsAtMe, myShips, null, Modifier.fillMaxWidth(0.9f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton("SHUFFLE", { myShips = randomFleet() }, Modifier.weight(1f), color = Coral)
                GameButton("START", { phase = 1; msg = "Your move - tap the enemy waters" }, Modifier.weight(1f))
            }
        } else {
            Text(if (myTurn && phase == 1) "ENEMY WATERS - your shot" else "ENEMY WATERS", color = Coral, fontSize = 12.sp)
            BattleGrid(
                myShots,
                if (phase == 2) enemyShips else enemyShips.filter { s -> s.all { myShots[it] == 2 } }, // sunk ships get revealed
                { fire(it) }, Modifier.fillMaxWidth(0.95f)
            )
            Spacer(Modifier.height(8.dp))
            Text("YOUR FLEET", color = Sun, fontSize = 12.sp)
            BattleGrid(shotsAtMe, myShips, null, Modifier.fillMaxWidth(0.5f))
            if (phase == 2) EndPanel(if (won) "Victory!" else "Defeat", if (won) "You sunk the enemy fleet" else "Your fleet was sunk", onAgain, onExit)
        }
    }
}

@Composable
private fun BattleGrid(shots: List<Int>, ships: List<List<Int>>?, onTap: ((Int) -> Unit)?, modifier: Modifier) {
    val drawn = ships ?: emptyList()
    val shipCells = remember(ships) { drawn.flatten().toSet() }
    // 0..1 looping clock that drives the flame flicker
    val fire by rememberInfiniteTransition(label = "fire").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "fireT"
    )
    Canvas(
        modifier.aspectRatio(1f).pointerInput(onTap) {
            detectTapGestures { o ->
                onTap?.let {
                    val c = size.width / 10f
                    it((o.y / c).toInt().coerceIn(0, 9) * 10 + (o.x / c).toInt().coerceIn(0, 9))
                }
            }
        }
    ) {
        val c = size.width / 10f
        val burning = drawn.filter { s -> s.all { shots[it] == 2 } }
        val burningCells = burning.flatten().toSet()

        // 1) water tiles, misses and hits on ships we can't see yet
        for (i in 0..99) {
            val x = (i % 10) * c; val y = (i / 10) * c
            val color = when {
                shots[i] == 2 && i !in shipCells -> Coral
                shots[i] == 1 -> Color(0xFF9CCBFF)
                else -> Color(0xFF3B8EF0)
            }
            drawRoundRect(color, Offset(x + 1.5f, y + 1.5f), Size(c - 3f, c - 3f), CornerRadius(6f))
            if (shots[i] == 1) drawCircle(Color.White, c * 0.12f, Offset(x + c / 2, y + c / 2))
            if (shots[i] == 2 && i !in shipCells) drawCircle(Ink, c * 0.14f, Offset(x + c / 2, y + c / 2))
        }

        // 2) ship silhouettes
        for (s in drawn) {
            val sunk = s in burning
            drawShip(s, c, hull = if (sunk) Color(0xFF3A2F2F) else Color(0xFF8A94A6), deck = if (sunk) Color(0xFF241C1C) else Color(0xFFC3CBD8))
        }

        // 3) hits on a visible ship that is not sunk yet
        for (i in shipCells) if (shots[i] == 2 && i !in burningCells) {
            val x = (i % 10) * c; val y = (i / 10) * c
            drawCircle(Coral, c * 0.28f, Offset(x + c / 2, y + c / 2))
            drawCircle(Ink, c * 0.12f, Offset(x + c / 2, y + c / 2))
        }

        // 4) fire on sunk ships
        for (s in burning) drawFire(s, c, fire)

        drawRect(Ink, style = Stroke(6f))
    }
}

/** Maps ship-local coordinates (u along the ship, v across it, both in cells) onto the grid. */
private fun shipPoint(cells: List<Int>, c: Float, u: Float, v: Float): Offset {
    val first = cells.minOf { it }
    val horiz = cells.size < 2 || cells[1] == cells[0] + 1
    val x0 = (first % 10) * c; val y0 = (first / 10) * c
    return if (horiz) Offset(x0 + u * c, y0 + v * c) else Offset(x0 + v * c, y0 + u * c)
}

private fun DrawScope.drawShip(cells: List<Int>, c: Float, hull: Color, deck: Color) {
    val sorted = cells.sorted()
    val len = sorted.size.toFloat()
    fun p(u: Float, v: Float) = shipPoint(sorted, c, u, v)
    val body = Path().apply {
        moveTo(p(0.08f, 0.2f).x, p(0.08f, 0.2f).y)
        lineTo(p(len - 0.55f, 0.16f).x, p(len - 0.55f, 0.16f).y)
        cubicTo(
            p(len - 0.2f, 0.2f).x, p(len - 0.2f, 0.2f).y,
            p(len - 0.05f, 0.4f).x, p(len - 0.05f, 0.4f).y,
            p(len - 0.03f, 0.5f).x, p(len - 0.03f, 0.5f).y
        )
        cubicTo(
            p(len - 0.05f, 0.6f).x, p(len - 0.05f, 0.6f).y,
            p(len - 0.2f, 0.8f).x, p(len - 0.2f, 0.8f).y,
            p(len - 0.55f, 0.84f).x, p(len - 0.55f, 0.84f).y
        )
        lineTo(p(0.08f, 0.8f).x, p(0.08f, 0.8f).y)
        close()
    }
    drawPath(body, hull)
    // deck stripe + cabin
    val a = p(0.3f, 0.38f); val b = p(len - 0.7f, 0.62f)
    drawRoundRect(deck, Offset(min(a.x, b.x), min(a.y, b.y)), Size(abs(b.x - a.x), abs(b.y - a.y)), CornerRadius(c * 0.08f))
    val m = len / 2f
    val ca = p(m - 0.3f, 0.3f); val cb = p(m + 0.3f, 0.7f)
    drawRoundRect(hull, Offset(min(ca.x, cb.x), min(ca.y, cb.y)), Size(abs(cb.x - ca.x), abs(cb.y - ca.y)), CornerRadius(c * 0.1f))
}

private fun DrawScope.drawFire(cells: List<Int>, c: Float, t: Float) {
    cells.forEachIndexed { k, cell ->
        val cx = (cell % 10) * c + c / 2f
        val base = (cell / 10) * c + c * 0.7f
        for (j in 0..1) {
            val ph = t + k * 0.29f + j * 0.5f
            val flick = 0.5f + 0.5f * sin(ph * 2f * PI.toFloat())
            val h = c * (0.45f + 0.35f * flick)
            val w = c * (0.2f - 0.05f * flick)
            val fx = cx + (j - 0.5f) * c * 0.3f + sin(ph * 4f * PI.toFloat()) * c * 0.04f
            fun flame(hh: Float, ww: Float) = Path().apply {
                moveTo(fx - ww, base)
                cubicTo(fx - ww * 1.4f, base - hh * 0.5f, fx - ww * 0.3f, base - hh * 0.7f, fx, base - hh)
                cubicTo(fx + ww * 0.3f, base - hh * 0.7f, fx + ww * 1.4f, base - hh * 0.5f, fx + ww, base)
                close()
            }
            drawPath(flame(h, w), Color(0xFFFF5A1F))
            drawPath(flame(h * 0.6f, w * 0.55f), Color(0xFFFFD23F))
        }
        // rising smoke puff
        val sp = (t + k * 0.23f) % 1f
        drawCircle(Color.Black.copy(alpha = 0.35f * (1f - sp)), c * (0.08f + 0.1f * sp), Offset(cx, base - c * (0.6f + 0.5f * sp)))
    }
}
