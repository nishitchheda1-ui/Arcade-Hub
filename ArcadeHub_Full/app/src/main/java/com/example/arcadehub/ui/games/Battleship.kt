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
    if (unfired.isEmpty()) return 0
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
    val best = unfired.maxOfOrNull { dens[it] } ?: 0
    val bestCands = unfired.filter { dens[it] == best }
    return bestCands.randomOrNull() ?: unfired.first()
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

    fun sunkAt(ships: List<List<Int>>, shots: List<Int>, i: Int) = ships.firstOrNull { i in it }?.all { shots[it] == 2 } == true

    fun fire(i: Int) {
        if (phase != 1 || !myTurn || myShots[i] != 0) return
        val hit = enemyShips.any { i in it }
        myShots[i] = if (hit) 2 else 1
        msg = if (!hit) "You missed" else if (sunkAt(enemyShips, myShots, i)) "You sunk a ship!" else "Hit!"
        if (allSunk(enemyShips, myShots)) { phase = 2; won = true; ScoreRepository.addWin("battleship", d.points) } else myTurn = false
    }

    LaunchedEffect(myTurn, phase) {
        if (phase == 1 && !myTurn) {
            delay(700)
            val i = withContext(Dispatchers.Default) { botShot(shotsAtMe.toList(), myShips, d) }
            val hit = myShips.any { i in it }
            shotsAtMe[i] = if (hit) 2 else 1
            msg = if (!hit) "Bot missed" else if (sunkAt(myShips, shotsAtMe, i)) "Bot sunk your ship!" else "Bot hit your ship!"
            if (allSunk(myShips, shotsAtMe)) { phase = 2; won = false } else myTurn = true
        }
    }

    GameScaffold("Battleship (${d.label} Bot 🤖)", onExit) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(msg, color = Leaf, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (phase == 0) {
                Text("Your friendly fleet 🚢", color = Color.White, fontWeight = FontWeight.Bold)
                BattleGrid(shotsAtMe, myShips, null, Modifier.fillMaxWidth(0.9f))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GameButton("SHUFFLE 🔀", { myShips = randomFleet() }, Modifier.weight(1f), color = Coral)
                    GameButton("START! 🚢", { phase = 1; msg = "Your turn! Tap enemy waters 🌊" }, Modifier.weight(1f))
                }
            } else {
                Text(if (myTurn && phase == 1) "ENEMY WATERS - Your turn! 🎯" else "ENEMY WATERS", color = Coral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                BattleGrid(myShots, if (phase == 2) enemyShips else null, { fire(it) }, Modifier.fillMaxWidth(0.95f))
                Spacer(Modifier.height(8.dp))
                Text("YOUR FLEET", color = Sun, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                BattleGrid(shotsAtMe, myShips, null, Modifier.fillMaxWidth(0.5f))
                if (phase == 2) EndPanel(if (won) "Fleet Victory! 🚢🏆" else "Good Try, Captain! 🌊", if (won) "You found all the hidden ships! ⭐" else "Your fleet was found! Ready for rematch?", onAgain, onExit)
            }
        }
    }
}

@Composable
private fun BattleGrid(shots: List<Int>, ships: List<List<Int>>?, onTap: ((Int) -> Unit)?, modifier: Modifier) {
    val shipCells = remember(ships) { ships?.flatten()?.toSet() ?: emptySet() }
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
        for (i in 0..99) {
            val x = (i % 10) * c; val y = (i / 10) * c
            val color = when {
                shots[i] == 2 -> Coral
                shots[i] == 1 -> Color(0xFF9CCBFF)
                i in shipCells -> Color(0xFF8A94A6)
                else -> Color(0xFF3B8EF0)
            }
            drawRoundRect(color, Offset(x + 1.5f, y + 1.5f), Size(c - 3f, c - 3f), CornerRadius(6f))
            if (shots[i] == 1) drawCircle(Color.White, c * 0.12f, Offset(x + c / 2, y + c / 2))
            if (shots[i] == 2) drawCircle(Ink, c * 0.14f, Offset(x + c / 2, y + c / 2))
        }
        drawRect(Ink, style = Stroke(6f))
    }
}
