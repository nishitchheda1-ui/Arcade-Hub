package com.example.arcadehub.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
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
private val SHIP_NAMES = listOf("Carrier (5)", "Battleship (4)", "Cruiser (3)", "Submarine (3)", "Destroyer (2)")
private val SHIP_EMOJIS = listOf("🛳️", "⛴️", "🚢", "🚤", "⛵")

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

/** Returns cell list for a ship of [len] starting at [startCell] with given orientation, null if out of bounds. */
private fun shipCells(len: Int, startCell: Int, horiz: Boolean): List<Int>? {
    val r = startCell / 10; val c = startCell % 10
    return if (horiz) {
        if (c + len > 10) null else List(len) { startCell + it }
    } else {
        if (r + len > 10) null else List(len) { startCell + it * 10 }
    }
}

private fun allSunk(ships: List<List<Int>>, shots: List<Int>) = ships.all { s -> s.all { shots[it] == 2 } }

private fun neighbors(i: Int): List<Int> {
    val r = i / 10; val c = i % 10
    return listOfNotNull(if (r > 0) i - 10 else null, if (r < 9) i + 10 else null, if (c > 0) i - 1 else null, if (c < 9) i + 1 else null)
}

/** Bot shooting logic. Easy=random, Medium=hunt+chase, Hard=probability density. */
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
    var passPlay by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    val d = diff
    when {
        passPlay -> LocalBsPassPlay { passPlay = false }
        d == null -> BattleshipModePicker(onExit, onBot = { diff = it }, onPassPlay = { passPlay = true })
        else -> key(round) { BattleshipPlay(d, onExit) { round++ } }
    }
}

/** Mode picker: play vs bot (3 difficulties) or pass & play with a friend on this device. */
@Composable
private fun BattleshipModePicker(onBack: () -> Unit, onBot: (Difficulty) -> Unit, onPassPlay: () -> Unit) {
    GameScaffold("⚓ Battleship", onBack) {
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("🤖 PLAY VS BOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Test your skills against different bot personalities", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Difficulty.entries.forEach { diff -> BotDifficultyCard(diff, onClick = { onBot(diff) }) }

            CreamCard(Modifier.fillMaxWidth()) {
                Text("👥 PASS & PLAY WITH FRIEND", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    "Take turns on this device. Each player hides their fleet behind a pass screen.",
                    color = Ink.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                GameButton("2 PLAYERS", onPassPlay, Modifier.fillMaxWidth(), color = Sky, fontSize = 15.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BattleshipPlay(d: Difficulty, onExit: () -> Unit, onAgain: () -> Unit) {
    // ── Placement State ──
    var placedShips by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var placingIdx by remember { mutableIntStateOf(0) }
    var placingHoriz by remember { mutableStateOf(true) }
    var previewCell by remember { mutableStateOf<Int?>(null) }

    // ── Game State ──
    val enemyShips = remember { randomFleet() }
    val shotsAtMe = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } }
    val myShots = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } }
    var phase by remember { mutableIntStateOf(0) } // 0=placing, 1=playing, 2=over
    var myTurn by remember { mutableStateOf(true) }
    var won by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("Tap the grid to place your Carrier (5 cells)! 🚢") }
    var botTick by remember { mutableIntStateOf(0) }

    val usedCells = placedShips.flatten().toSet()
    val currentLen = FLEET.getOrNull(placingIdx)
    val previewCells = if (currentLen != null && previewCell != null)
        shipCells(currentLen, previewCell!!, placingHoriz) else null
    val previewValid = previewCells != null && previewCells.none { it in usedCells }

    fun sunkAt(ships: List<List<Int>>, shots: List<Int>, i: Int) =
        ships.firstOrNull { i in it }?.all { shots[it] == 2 } == true

    fun fire(i: Int) {
        if (phase != 1 || !myTurn || myShots[i] != 0) return
        val hit = enemyShips.any { i in it }
        myShots[i] = if (hit) 2 else 1
        msg = if (!hit) "💦 Miss! The sea is safe… for now." else if (sunkAt(enemyShips, myShots, i)) "💥 BOOM! Ship sunk! Shoot again!" else "🔥 Direct Hit! Shoot again!"
        if (allSunk(enemyShips, myShots)) { phase = 2; won = true; ScoreRepository.addWin("battleship", d.points) }
        else if (!hit) myTurn = false
    }

    LaunchedEffect(myTurn, phase, botTick) {
        if (phase == 1 && !myTurn) {
            delay(800)
            val i = withContext(Dispatchers.Default) { botShot(shotsAtMe.toList(), placedShips, d) }
            val hit = placedShips.any { i in it }
            shotsAtMe[i] = if (hit) 2 else 1
            msg = if (!hit) "🌊 Bot missed! Your turn!" else if (sunkAt(placedShips, shotsAtMe, i)) "💀 Bot sunk your ship!" else "⚠️ Bot hit your ship!"
            if (allSunk(placedShips, shotsAtMe)) { phase = 2; won = false }
            else if (hit) botTick++ else myTurn = true
        }
    }

    GameScaffold("⚓ Battleship (${d.label})", onExit) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (phase) {
                // ══════════════════════════════════════════
                // 0 ── PLACEMENT PHASE
                // ══════════════════════════════════════════
                0 -> {
                    // Status card
                    CreamCard(Modifier.fillMaxWidth()) {
                        Text("🚢 PLACE YOUR FLEET", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(msg, color = Ink.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(10.dp))

                    // Ship progress tracker
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FLEET.forEachIndexed { idx, len ->
                            val isPlaced = idx < placedShips.size
                            val isCurrent = idx == placingIdx
                            Box(
                                Modifier.weight(1f).height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(when { isPlaced -> Leaf; isCurrent -> Sun; else -> Board })
                                    .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isPlaced) "✓" else "${SHIP_EMOJIS[idx]}",
                                    color = if (isPlaced || isCurrent) Ink else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    if (currentLen != null) {
                        Text(
                            "Now placing: ${SHIP_NAMES[placingIdx]}",
                            color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "✅ All ships placed! Hit START BATTLE!",
                            color = Leaf, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Placement grid
                    Text(
                        "Orientation: ${if (placingHoriz) "➡️ Horizontal" else "⬇️ Vertical"}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    PlacementGrid(
                        placedShips = placedShips,
                        previewCells = previewCells,
                        previewValid = previewValid,
                        modifier = Modifier.fillMaxWidth(0.95f),
                        onHover = { previewCell = it },
                        onTap = { cell ->
                            if (currentLen != null) {
                                val cells = shipCells(currentLen, cell, placingHoriz)
                                if (cells != null && cells.none { it in usedCells }) {
                                    placedShips = placedShips + listOf(cells)
                                    placingIdx++
                                    previewCell = null
                                    msg = if (placingIdx < FLEET.size)
                                        "Now place your ${SHIP_NAMES[placingIdx]} ${SHIP_EMOJIS[placingIdx]} 🎯"
                                    else "All ships placed! Hit START BATTLE! ⚔️"
                                } else {
                                    msg = "❌ Can't place here! Ships can't overlap or go off the edge."
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // Action buttons row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GameButton(
                            "🔄 ROTATE",
                            {
                                placingHoriz = !placingHoriz
                                // Force preview recalculation
                                previewCell = previewCell
                            },
                            Modifier.weight(1f),
                            color = Sky,
                            enabled = currentLen != null
                        )
                        GameButton(
                            "🎲 SHUFFLE ALL",
                            {
                                placedShips = randomFleet()
                                placingIdx = FLEET.size
                                previewCell = null
                                msg = "Fleet shuffled randomly! Hit START BATTLE! ⚔️"
                            },
                            Modifier.weight(1f),
                            color = Coral
                        )
                    }

                    if (placedShips.isNotEmpty() && placingIdx > 0) {
                        Spacer(Modifier.height(8.dp))
                        GameButton(
                            "↩️ UNDO LAST SHIP",
                            {
                                placedShips = placedShips.dropLast(1)
                                placingIdx--
                                previewCell = null
                                msg = "Re-place your ${SHIP_NAMES[placingIdx]} ${SHIP_EMOJIS[placingIdx]} 🎯"
                            },
                            Modifier.fillMaxWidth(),
                            color = Tangerine
                        )
                    }

                    if (placingIdx >= FLEET.size) {
                        Spacer(Modifier.height(10.dp))
                        GameButton(
                            "⚔️ START BATTLE!",
                            { phase = 1; msg = "🎯 YOUR TURN — Tap the enemy grid to fire!" },
                            Modifier.fillMaxWidth(),
                            color = Leaf
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ══════════════════════════════════════════
                // 1 & 2 ── PLAYING / OVER PHASE
                // ══════════════════════════════════════════
                else -> {
                    // Status banner
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (myTurn && phase == 1) Leaf.copy(alpha = 0.18f) else Coral.copy(alpha = 0.18f))
                            .border(2.dp, if (myTurn && phase == 1) Leaf else Coral, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            msg,
                            color = if (myTurn && phase == 1) Leaf else Coral,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (myTurn && phase == 1) "🎯 ENEMY WATERS — TAP TO FIRE!" else "🌊 ENEMY WATERS",
                        color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Black
                    )
                    BattleGrid(
                        myShots,
                        if (phase == 2) enemyShips else enemyShips.filter { s -> s.all { myShots[it] == 2 } },
                        { fire(it) }, Modifier.fillMaxWidth(0.95f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("🛡️ YOUR FLEET", color = Sun, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    BattleGrid(shotsAtMe, placedShips, null, Modifier.fillMaxWidth(0.5f))

                    if (phase == 2) {
                        Spacer(Modifier.height(8.dp))
                        EndPanel(
                            if (won) "🏆 VICTORY!" else "💀 DEFEAT",
                            if (won) "You sunk the entire enemy fleet!" else "Your fleet was completely destroyed!",
                            onAgain, onExit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Placement Grid – shows placed ships + live preview of current ship
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlacementGrid(
    placedShips: List<List<Int>>,
    previewCells: List<Int>?,
    previewValid: Boolean,
    modifier: Modifier,
    onHover: (Int) -> Unit,
    onTap: (Int) -> Unit
) {
    val shipSet = placedShips.flatten().toSet()
    Canvas(
        modifier.aspectRatio(1f).pointerInput(Unit) {
            detectTapGestures(
                onPress = { o ->
                    val cSize = size.width / 10f
                    val cell = (o.y / cSize).toInt().coerceIn(0, 9) * 10 + (o.x / cSize).toInt().coerceIn(0, 9)
                    onHover(cell)
                    tryAwaitRelease()
                },
                onTap = { o ->
                    val cSize = size.width / 10f
                    val cell = (o.y / cSize).toInt().coerceIn(0, 9) * 10 + (o.x / cSize).toInt().coerceIn(0, 9)
                    onTap(cell)
                }
            )
        }
    ) {
        val c = size.width / 10f

        // Water background tiles
        for (i in 0..99) {
            val x = (i % 10) * c; val y = (i / 10) * c
            val inPreview = previewCells?.contains(i) == true
            val bg = when {
                inPreview && previewValid  -> Color(0xFF22C55E).copy(alpha = 0.45f)
                inPreview && !previewValid -> Coral.copy(alpha = 0.45f)
                else                       -> Color(0xFF3B8EF0)
            }
            drawRoundRect(bg, Offset(x + 1.5f, y + 1.5f), Size(c - 3f, c - 3f), CornerRadius(6f))
        }

        // Grid lines
        for (i in 1..9) {
            drawLine(Color.White.copy(alpha = 0.12f), Offset(i * c, 0f), Offset(i * c, size.height), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, i * c), Offset(size.width, i * c), strokeWidth = 1f)
        }

        // Already-placed ships
        for (s in placedShips) drawShip(s, c, hull = Color(0xFF8A94A6), deck = Color(0xFFC3CBD8))

        // Ghost preview of the ship being placed
        if (previewCells != null) {
            val ghostHull = if (previewValid) Color(0xFF22C55E) else Coral
            val ghostDeck = ghostHull.copy(alpha = 0.6f)
            if (previewCells.size >= 2) {
                drawShip(previewCells, c, hull = ghostHull, deck = ghostDeck)
            } else if (previewCells.size == 1) {
                val i = previewCells[0]; val x = (i % 10) * c; val y = (i / 10) * c
                drawRoundRect(ghostHull.copy(alpha = 0.8f), Offset(x + 2f, y + 2f), Size(c - 4f, c - 4f), CornerRadius(6f))
            }
        }

        // Border
        drawRect(Ink, style = Stroke(6f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Battle Grid – used during play for both enemy waters and own fleet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BattleGrid(shots: List<Int>, ships: List<List<Int>>?, onTap: ((Int) -> Unit)?, modifier: Modifier) {
    val drawn = ships ?: emptyList()
    val shipCells = remember(ships) { drawn.flatten().toSet() }
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

        // Water, miss, hit tiles
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

        // Grid lines
        for (i in 1..9) {
            drawLine(Color.White.copy(alpha = 0.1f), Offset(i * c, 0f), Offset(i * c, size.height), strokeWidth = 1f)
            drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, i * c), Offset(size.width, i * c), strokeWidth = 1f)
        }

        // Ship silhouettes
        for (s in drawn) {
            val sunk = s in burning
            drawShip(s, c, hull = if (sunk) Color(0xFF3A2F2F) else Color(0xFF8A94A6), deck = if (sunk) Color(0xFF241C1C) else Color(0xFFC3CBD8))
        }

        // Hit markers on visible non-sunk ships
        for (i in shipCells) if (shots[i] == 2 && i !in burningCells) {
            val x = (i % 10) * c; val y = (i / 10) * c
            drawCircle(Coral, c * 0.28f, Offset(x + c / 2, y + c / 2))
            drawCircle(Ink, c * 0.12f, Offset(x + c / 2, y + c / 2))
        }

        // Animated fire on sunk ships
        for (s in burning) drawFire(s, c, fire)

        drawRect(Ink, style = Stroke(6f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawing helpers
// ─────────────────────────────────────────────────────────────────────────────

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
        cubicTo(p(len - 0.2f, 0.2f).x, p(len - 0.2f, 0.2f).y, p(len - 0.05f, 0.4f).x, p(len - 0.05f, 0.4f).y, p(len - 0.03f, 0.5f).x, p(len - 0.03f, 0.5f).y)
        cubicTo(p(len - 0.05f, 0.6f).x, p(len - 0.05f, 0.6f).y, p(len - 0.2f, 0.8f).x, p(len - 0.2f, 0.8f).y, p(len - 0.55f, 0.84f).x, p(len - 0.55f, 0.84f).y)
        lineTo(p(0.08f, 0.8f).x, p(0.08f, 0.8f).y)
        close()
    }
    drawPath(body, hull)
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
        val sp = (t + k * 0.23f) % 1f
        drawCircle(Color.Black.copy(alpha = 0.35f * (1f - sp)), c * (0.08f + 0.1f * sp), Offset(cx, base - c * (0.6f + 0.5f * sp)))
    }
}
