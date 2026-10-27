package com.example.arcadehub.ui.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random
import com.example.arcadehub.data.*
import com.example.arcadehub.logic.*
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

// ─────────────────────────────────────────────────────────────────────────────
// Helpers shared across this file
// ─────────────────────────────────────────────────────────────────────────────

private val BS_FLEET = listOf(5, 4, 3, 3, 2)
private val BS_SHIP_NAMES = listOf("Carrier (5)", "Battleship (4)", "Cruiser (3)", "Submarine (3)", "Destroyer (2)")
private val BS_SHIP_EMOJIS = listOf("🛳️", "⛴️", "🚢", "🚤", "⛵")

private fun bsRandomFleet(): List<List<Int>> {
    while (true) {
        val used = HashSet<Int>(); val ships = ArrayList<List<Int>>(); var ok = true
        for (len in BS_FLEET) {
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

private fun bsShipCells(len: Int, startCell: Int, horiz: Boolean): List<Int>? {
    val r = startCell / 10; val c = startCell % 10
    return if (horiz) { if (c + len > 10) null else List(len) { startCell + it } }
    else { if (r + len > 10) null else List(len) { startCell + it * 10 } }
}

/** Converts a List<List<Int>> ships structure to a flat 100-int grid. */
private fun shipsToGrid(ships: List<List<Int>>): List<Int> {
    val grid = MutableList(100) { 0 }
    ships.forEach { s -> s.forEach { grid[it] = 1 } }
    return grid
}

/** Converts a flat 100-int grid back to a list of ship cell-lists (for display). */
private fun gridToShipCells(grid: List<Int>): List<List<Int>> {
    // We only need this for display — return each contiguous run of 1s as a ship
    val visited = BooleanArray(100)
    val ships = mutableListOf<List<Int>>()
    for (i in 0..99) {
        if (grid[i] == 1 && !visited[i]) {
            // Try horizontal run first, then vertical
            val hRun = mutableListOf(i)
            var c = i % 10 + 1
            while (c < 10 && grid[i - (i % 10) + c] == 1 && !visited[i - (i % 10) + c]) { hRun.add(i - (i % 10) + c); c++ }
            val vRun = mutableListOf(i)
            var r = i / 10 + 1
            while (r < 10 && grid[r * 10 + i % 10] == 1 && !visited[r * 10 + i % 10]) { vRun.add(r * 10 + i % 10); r++ }
            val run = if (hRun.size >= vRun.size) hRun else vRun
            run.forEach { visited[it] = true }
            ships.add(run)
        }
    }
    return ships
}

private fun bsAllSunk(oppShipGrid: List<Int>, myShots: List<Int>): Boolean =
    oppShipGrid.indices.all { i -> oppShipGrid[i] == 0 || myShots[i] == 2 }

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnlineGame(onExit: () -> Unit) {
    var game by remember { mutableStateOf("ttt") }
    var code by remember { mutableStateOf<String?>(null) }
    var me by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var isLocalPassPlay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val c = code

    // ── LOCAL PASS & PLAY ──────────────────────────────────────────────────
    if (isLocalPassPlay) {
        when (game) {
            "ttt" -> LocalTttPassPlay { isLocalPassPlay = false }
            "c4"  -> LocalC4PassPlay  { isLocalPassPlay = false }
            else  -> LocalBsPassPlay  { isLocalPassPlay = false }
        }
        return
    }

    // ── LOBBY ──────────────────────────────────────────────────────────────
    if (c == null) {
        GameScaffold("🌐 Online Rooms", onExit) {
            Spacer(Modifier.height(12.dp))
            Text("Play a friend with a 4-digit room code", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Game selector
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill("❌ Tic Tac Toe",  game == "ttt",        color = Coral)  { game = "ttt" }
                Pill("🔴 Connect Four", game == "c4",         color = Sun)    { game = "c4" }
                Pill("⚓ Battleship",   game == "battleship", color = Sky)    { game = "battleship" }
            }
            Spacer(Modifier.height(16.dp))

            GameButton(
                text = "CREATE ROOM CODE",
                onClick = {
                    scope.launch {
                        busy = true; error = null
                        if (auth.currentUser == null) {
                            error = "Sign in to host online rooms, or use Local 2-Player!"; busy = false; return@launch
                        }
                        try { code = OnlineRepository.create(game); me = 1 }
                        catch (e: Exception) { error = e.friendly() }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(), loading = busy, color = Sun
            )

            Spacer(Modifier.height(20.dp))
            Text("or join with a code", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            GameTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit).take(4) },
                label = "4-digit room code",
                icon = Icons.Default.Lock,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
            Spacer(Modifier.height(12.dp))
            GameButton(
                text = "JOIN ROOM",
                onClick = {
                    scope.launch {
                        busy = true; error = null
                        if (auth.currentUser == null) {
                            error = "Sign in to join online rooms, or use Local 2-Player!"; busy = false; return@launch
                        }
                        try {
                            val err = OnlineRepository.join(input)
                            if (err == null) { code = input; me = 2 } else { error = err }
                        } catch (e: Exception) { error = e.friendly() }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(), color = Coral, loading = busy, enabled = input.length == 4
            )

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                val isDbError = error!!.contains("Firestore", ignoreCase = true) ||
                        error!!.contains("database", ignoreCase = true) ||
                        error!!.contains("does not exist", ignoreCase = true)
                CreamCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Coral, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isDbError) "Firebase Setup Needed" else "Room Notice", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isDbError) {
                            "Firestore not activated for this project.\n\n" +
                            "To enable rooms:\n1. Go to console.firebase.google.com\n" +
                            "2. Select project: arcade-hub-5f70a\n3. Firestore Database → Create database → Test mode"
                        } else error!!,
                        color = Ink.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isDbError) {
                            GameButton(
                                "📋 COPY LINK",
                                {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Firebase", "https://console.firebase.google.com/project/arcade-hub-5f70a/firestore"))
                                    Toast.makeText(context, "Firebase URL copied!", Toast.LENGTH_SHORT).show()
                                },
                                Modifier.weight(1f), color = Sky, fontSize = 12.sp
                            )
                        }
                        GameButton("🎮 PLAY LOCAL 2P", { isLocalPassPlay = true }, Modifier.weight(1f), color = Leaf, fontSize = 12.sp)
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))
                CreamCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PLAY TOGETHER ON THIS PHONE", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Text("No internet required. Take turns on this screen!", color = Ink.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    GameButton("PASS & PLAY LOCAL MATCH", { isLocalPassPlay = true }, Modifier.fillMaxWidth(), color = Leaf, fontSize = 14.sp)
                }
            }
        }
        return
    }

    // ── ONLINE ROOM (connected) ────────────────────────────────────────────
    val flow: Flow<Room?> = remember(c) { OnlineRepository.listen(c) }
    val room by flow.collectAsState(initial = null)
    val r = room
    var awardedRounds by remember { mutableStateOf(setOf<Int>()) }
    val roundNum = r?.round ?: 1

    LaunchedEffect(r?.status, roundNum) {
        if (r != null && r.status == "done" && r.winner == me && !awardedRounds.contains(roundNum)) {
            awardedRounds = awardedRounds + roundNum
            ScoreRepository.addWin("online", 15)
        }
    }

    GameScaffold(
        title = "Room $c",
        onBack = {
            scope.launch { OnlineRepository.leaveRoom(c, me) }
            code = null; input = ""
        }
    ) {
        Spacer(Modifier.height(12.dp))
        when {
            r == null -> {
                Text("Connecting to room...", color = Color.White)
                CircularProgressIndicator(color = Sun, modifier = Modifier.padding(top = 16.dp))
            }

            r.status == "waiting" -> {
                CreamCard(Modifier.fillMaxWidth()) {
                    Text("SHARE THIS CODE", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(c, color = Coral, fontSize = 48.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
                    Text("Tell your friend to enter this code under Join Room", color = Ink.copy(alpha = 0.75f), textAlign = TextAlign.Center, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = Sun)
                Text("Waiting for opponent to join...", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp))
            }

            r.status == "abandoned" -> {
                CreamCard(Modifier.fillMaxWidth()) {
                    Text("ROOM DISCONNECTED", color = Coral, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("The other player has left the match.", color = Ink.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    GameButton("RETURN TO LOBBY", { code = null; input = "" }, Modifier.fillMaxWidth(), color = Sun)
                }
            }

            r.status == "placing" && r.game == "battleship" -> {
                // Battleship placement phase
                OnlineBattleshipPlacing(r = r, me = me, code = c, scope = scope)
            }

            r.game == "battleship" -> {
                // Battleship playing/done phase
                OnlineBattleshipPlaying(r = r, me = me, code = c, scope = scope,
                    onLeave = { scope.launch { OnlineRepository.leaveRoom(c, me) }; code = null; input = "" },
                    context = context
                )
            }

            else -> {
                // TTT / C4 online
                val opponentName = if (me == 1) (r.guestName ?: "Guest") else r.hostName
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${r.hostName} vs ${r.guestName ?: "?"}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    if (r.round > 1) Text("Round ${r.round}", color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                val mine = if (r.game == "ttt") (if (me == 1) "X" else "O") else (if (me == 1) "Pink" else "Green")
                Text(
                    when {
                        r.status == "done" && r.winner == 3 -> "Draw!"
                        r.status == "done" -> if (r.winner == me) "You won! 🎉" else "You lost"
                        r.turn == me -> "Your turn (you are $mine)"
                        else -> "Opponent's turn (you are $mine)"
                    },
                    color = Sun, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                val onPick: (Int) -> Unit = { i ->
                    if (r.status == "playing" && r.turn == me)
                        scope.launch { try { OnlineRepository.move(c, me, i) } catch (_: Exception) {} }
                }
                if (r.game == "ttt") TttBoard(r.board, onPick) else C4Board(r.board, onPick)

                if (r.status == "done") {
                    RematchPanel(r, me, opponentName, c, scope, context,
                        onLeave = { code = null; input = "" })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Battleship Placement Phase (online)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineBattleshipPlacing(r: Room, me: Int, code: String, scope: kotlinx.coroutines.CoroutineScope) {
    val myReady = if (me == 1) r.bReady1 else r.bReady2
    val oppReady = if (me == 1) r.bReady2 else r.bReady1

    if (myReady) {
        // Waiting for opponent
        CreamCard(Modifier.fillMaxWidth()) {
            Text("✅ YOUR FLEET IS READY!", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text("Waiting for opponent to finish placing their ships...", color = Ink.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = Sun)
        return
    }

    // Ship placement UI (same as single-player but submits to Firestore)
    var placedShips by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var placingIdx by remember { mutableIntStateOf(0) }
    var placingHoriz by remember { mutableStateOf(true) }
    var previewCell by remember { mutableStateOf<Int?>(null) }
    var msg by remember { mutableStateOf("Place your fleet before the battle starts! 🚢") }
    var submitting by remember { mutableStateOf(false) }

    val usedCells = placedShips.flatten().toSet()
    val currentLen = BS_FLEET.getOrNull(placingIdx)
    val previewCells = if (currentLen != null && previewCell != null)
        bsShipCells(currentLen, previewCell!!, placingHoriz) else null
    val previewValid = previewCells != null && previewCells.none { it in usedCells }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        CreamCard(Modifier.fillMaxWidth()) {
            Text("⚓ ${r.hostName} vs ${r.guestName ?: "..."}", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("Both players place ships. Battle starts when both are ready!", color = Ink.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (me == 1) r.bReady1.let { if (it) Leaf else Sun } else r.bReady2.let { if (it) Leaf else Sun }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("You: ${if (myReady) "✅ Ready" else "⏳ Placing"}", color = Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (oppReady) Leaf else Board).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Opponent: ${if (oppReady) "✅ Ready" else "⏳ Placing"}", color = if (oppReady) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(msg, color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        // Progress tracker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BS_FLEET.forEachIndexed { idx, _ ->
                Box(
                    Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(when { idx < placedShips.size -> Leaf; idx == placingIdx -> Sun; else -> Board })
                        .border(2.dp, Ink, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (idx < placedShips.size) "✓" else BS_SHIP_EMOJIS[idx], color = if (idx <= placingIdx) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (currentLen != null) Text("Placing: ${BS_SHIP_NAMES[placingIdx]}", color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        else Text("✅ All placed! Hit CONFIRM!", color = Leaf, fontWeight = FontWeight.Bold, fontSize = 13.sp)

        Spacer(Modifier.height(8.dp))
        OnlinePlacementGrid(
            placedShips = placedShips,
            previewCells = previewCells,
            previewValid = previewValid,
            modifier = Modifier.fillMaxWidth(0.95f),
            onHover = { previewCell = it },
            onTap = { cell ->
                if (currentLen != null) {
                    val cells = bsShipCells(currentLen, cell, placingHoriz)
                    if (cells != null && cells.none { it in usedCells }) {
                        placedShips = placedShips + listOf(cells)
                        placingIdx++
                        previewCell = null
                        msg = if (placingIdx < BS_FLEET.size) "Place ${BS_SHIP_NAMES[placingIdx]} ${BS_SHIP_EMOJIS[placingIdx]}" else "All placed! Confirm to start!"
                    } else { msg = "❌ Can't place here! No overlap / off-grid." }
                }
            }
        )
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameButton("🔄 ROTATE", { placingHoriz = !placingHoriz }, Modifier.weight(1f), color = Sky, enabled = currentLen != null)
            GameButton("🎲 SHUFFLE", {
                placedShips = bsRandomFleet(); placingIdx = BS_FLEET.size; previewCell = null
                msg = "Fleet shuffled! Hit CONFIRM!"
            }, Modifier.weight(1f), color = Coral)
        }
        if (placedShips.isNotEmpty() && placingIdx > 0) {
            Spacer(Modifier.height(8.dp))
            GameButton("↩️ UNDO", {
                placedShips = placedShips.dropLast(1); placingIdx--; previewCell = null
                msg = "Re-place ${BS_SHIP_NAMES[placingIdx]}"
            }, Modifier.fillMaxWidth(), color = Tangerine)
        }
        if (placingIdx >= BS_FLEET.size) {
            Spacer(Modifier.height(10.dp))
            GameButton("✅ CONFIRM & READY!", {
                scope.launch {
                    submitting = true
                    try {
                        val grid = shipsToGrid(placedShips)
                        OnlineRepository.battleshipPlaceShips(code, me, grid)
                    } catch (_: Exception) {}
                    submitting = false
                }
            }, Modifier.fillMaxWidth(), color = Leaf, loading = submitting)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Battleship Playing Phase (online)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineBattleshipPlaying(r: Room, me: Int, code: String, scope: kotlinx.coroutines.CoroutineScope, onLeave: () -> Unit, context: android.content.Context) {
    val myShots = if (me == 1) r.shots1 else r.shots2
    val oppShots = if (me == 1) r.shots2 else r.shots1
    val myShipGrid = if (me == 1) r.ships1 else r.ships2
    val oppShipGrid = if (me == 1) r.ships2 else r.ships1
    val myShipCells = remember(myShipGrid) { gridToShipCells(myShipGrid) }
    val oppShipCells = remember(oppShipGrid) { gridToShipCells(oppShipGrid) }

    val isMyTurn = r.turn == me
    val isDone = r.status == "done"
    val iWon = isDone && r.winner == me

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${r.hostName} vs ${r.guestName ?: "?"}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            if (r.round > 1) Text("Round ${r.round}", color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))

        // Status banner
        val statusMsg = when {
            isDone && iWon -> "🏆 You sunk the enemy fleet! VICTORY!"
            isDone -> "💀 Your fleet was destroyed. Defeat!"
            isMyTurn -> "🎯 YOUR TURN — Tap enemy waters to fire!"
            else -> "⏳ Opponent's turn. Waiting..."
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(when { isDone && iWon -> Leaf.copy(.15f); isDone -> Coral.copy(.15f); isMyTurn -> Sky.copy(.15f); else -> Board })
                .border(2.dp, when { isDone && iWon -> Leaf; isDone -> Coral; isMyTurn -> Sky; else -> Color.White.copy(.3f) }, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(statusMsg, color = when { isDone && iWon -> Leaf; isDone -> Coral; isMyTurn -> Sky; else -> Color.White }, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))

        // Enemy grid
        Text("🎯 ENEMY WATERS${if (isMyTurn && !isDone) " — TAP TO FIRE!" else ""}", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Black)
        OnlineBsGrid(
            shots = myShots,
            ships = if (isDone) oppShipCells else oppShipCells.filter { s -> s.all { myShots[it] == 2 } },
            onTap = if (isMyTurn && !isDone) { cell ->
                scope.launch { try { OnlineRepository.battleshipShoot(code, me, cell) } catch (_: Exception) {} }
            } else null,
            modifier = Modifier.fillMaxWidth(0.95f)
        )
        Spacer(Modifier.height(8.dp))

        // Own grid
        Text("🛡️ YOUR FLEET", color = Sun, fontSize = 12.sp, fontWeight = FontWeight.Black)
        OnlineBsGrid(shots = oppShots, ships = myShipCells, onTap = null, modifier = Modifier.fillMaxWidth(0.5f))

        if (isDone) {
            Spacer(Modifier.height(8.dp))
            val opponentName = if (me == 1) (r.guestName ?: "Guest") else r.hostName
            RematchPanel(r, me, opponentName, code, scope, context, onLeave)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Battleship grids for online (same drawing as single-player)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlinePlacementGrid(
    placedShips: List<List<Int>>,
    previewCells: List<Int>?,
    previewValid: Boolean,
    modifier: Modifier,
    onHover: (Int) -> Unit,
    onTap: (Int) -> Unit
) {
    val shipSet = placedShips.flatten().toSet()
    Canvas(modifier.aspectRatio(1f).pointerInput(Unit) {
        detectTapGestures(
            onPress = { o ->
                val cs = size.width / 10f
                onHover((o.y / cs).toInt().coerceIn(0, 9) * 10 + (o.x / cs).toInt().coerceIn(0, 9))
                tryAwaitRelease()
            },
            onTap = { o ->
                val cs = size.width / 10f
                onTap((o.y / cs).toInt().coerceIn(0, 9) * 10 + (o.x / cs).toInt().coerceIn(0, 9))
            }
        )
    }) {
        val c = size.width / 10f
        for (i in 0..99) {
            val x = (i % 10) * c; val y = (i / 10) * c
            val inP = previewCells?.contains(i) == true
            drawRoundRect(
                when { inP && previewValid -> Color(0xFF22C55E).copy(.45f); inP -> Coral.copy(.45f); else -> Color(0xFF3B8EF0) },
                Offset(x + 1.5f, y + 1.5f), Size(c - 3f, c - 3f), CornerRadius(6f)
            )
        }
        for (i in 1..9) {
            drawLine(Color.White.copy(.12f), Offset(i * c, 0f), Offset(i * c, size.height), 1f)
            drawLine(Color.White.copy(.12f), Offset(0f, i * c), Offset(size.width, i * c), 1f)
        }
        for (s in placedShips) onlineDrawShip(s, c, Color(0xFF8A94A6), Color(0xFFC3CBD8))
        if (previewCells != null) {
            val gh = if (previewValid) Color(0xFF22C55E) else Coral
            if (previewCells.size >= 2) onlineDrawShip(previewCells, c, gh, gh.copy(.6f))
        }
        drawRect(Ink, style = Stroke(6f))
    }
}

@Composable
private fun OnlineBsGrid(shots: List<Int>, ships: List<List<Int>>, onTap: ((Int) -> Unit)?, modifier: Modifier) {
    val shipCells = remember(ships) { ships.flatten().toSet() }
    val fire by rememberInfiniteTransition(label = "fire").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "ft")
    Canvas(modifier.aspectRatio(1f).pointerInput(onTap) {
        detectTapGestures { o ->
            onTap?.let {
                val c = size.width / 10f
                it((o.y / c).toInt().coerceIn(0, 9) * 10 + (o.x / c).toInt().coerceIn(0, 9))
            }
        }
    }) {
        val c = size.width / 10f
        val burning = ships.filter { s -> s.all { shots[it] == 2 } }
        val burningCells = burning.flatten().toSet()
        for (i in 0..99) {
            val x = (i % 10) * c; val y = (i / 10) * c
            drawRoundRect(
                when { shots[i] == 2 && i !in shipCells -> Coral; shots[i] == 1 -> Color(0xFF9CCBFF); else -> Color(0xFF3B8EF0) },
                Offset(x + 1.5f, y + 1.5f), Size(c - 3f, c - 3f), CornerRadius(6f)
            )
            if (shots[i] == 1) drawCircle(Color.White, c * .12f, Offset(x + c / 2, y + c / 2))
            if (shots[i] == 2 && i !in shipCells) drawCircle(Ink, c * .14f, Offset(x + c / 2, y + c / 2))
        }
        for (i in 1..9) {
            drawLine(Color.White.copy(.1f), Offset(i * c, 0f), Offset(i * c, size.height), 1f)
            drawLine(Color.White.copy(.1f), Offset(0f, i * c), Offset(size.width, i * c), 1f)
        }
        for (s in ships) {
            val sunk = s in burning
            onlineDrawShip(s, c, if (sunk) Color(0xFF3A2F2F) else Color(0xFF8A94A6), if (sunk) Color(0xFF241C1C) else Color(0xFFC3CBD8))
        }
        for (i in shipCells) if (shots[i] == 2 && i !in burningCells) {
            val x = (i % 10) * c; val y = (i / 10) * c
            drawCircle(Coral, c * .28f, Offset(x + c / 2, y + c / 2))
            drawCircle(Ink, c * .12f, Offset(x + c / 2, y + c / 2))
        }
        for (s in burning) onlineDrawFire(s, c, fire)
        drawRect(Ink, style = Stroke(6f))
    }
}

private fun bsShipPoint(cells: List<Int>, c: Float, u: Float, v: Float): Offset {
    val first = cells.minOf { it }
    val horiz = cells.size < 2 || cells[1] == cells[0] + 1
    val x0 = (first % 10) * c; val y0 = (first / 10) * c
    return if (horiz) Offset(x0 + u * c, y0 + v * c) else Offset(x0 + v * c, y0 + u * c)
}

private fun DrawScope.onlineDrawShip(cells: List<Int>, c: Float, hull: Color, deck: Color) {
    val sorted = cells.sorted(); val len = sorted.size.toFloat()
    fun p(u: Float, v: Float) = bsShipPoint(sorted, c, u, v)
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
    val m = len / 2f; val ca = p(m - 0.3f, 0.3f); val cb = p(m + 0.3f, 0.7f)
    drawRoundRect(hull, Offset(min(ca.x, cb.x), min(ca.y, cb.y)), Size(abs(cb.x - ca.x), abs(cb.y - ca.y)), CornerRadius(c * 0.1f))
}

private fun DrawScope.onlineDrawFire(cells: List<Int>, c: Float, t: Float) {
    cells.forEachIndexed { k, cell ->
        val cx = (cell % 10) * c + c / 2f; val base = (cell / 10) * c + c * 0.7f
        for (j in 0..1) {
            val ph = t + k * 0.29f + j * 0.5f
            val flick = 0.5f + 0.5f * sin(ph * 2f * Math.PI.toFloat())
            val h = c * (0.45f + 0.35f * flick); val w = c * (0.2f - 0.05f * flick)
            val fx = cx + (j - 0.5f) * c * 0.3f + sin(ph * 4f * Math.PI.toFloat()) * c * 0.04f
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
        drawCircle(Color.Black.copy(.35f * (1f - sp)), c * (.08f + .1f * sp), Offset(cx, base - c * (.6f + .5f * sp)))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Local Pass & Play games
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocalTttPassPlay(onExit: () -> Unit) {
    var board by remember { mutableStateOf(List(9) { 0 }) }
    var turn by remember { mutableIntStateOf(1) }
    var winner by remember { mutableIntStateOf(0) }
    fun play(i: Int) {
        if (winner != 0 || board[i] != 0) return
        val nb = board.toMutableList().also { it[i] = turn }
        board = nb
        val res = tttResult(nb.toIntArray())
        if (res != 0) { winner = res; if (res == 1) ScoreRepository.addWin("tictactoe", 5) }
        else turn = 3 - turn
    }
    GameScaffold("Local 2-Player — Tic Tac Toe", onExit) {
        Spacer(Modifier.height(12.dp))
        Text(when (winner) { 0 -> "Player $turn's turn (${if (turn == 1) "X" else "O"})"; 3 -> "Draw! 🤝"; else -> "Player $winner wins! 🎉" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        TttBoard(board, { play(it) })
        if (winner != 0) { Spacer(Modifier.height(16.dp)); GameButton("PLAY AGAIN", { board = List(9) { 0 }; turn = 1; winner = 0 }, Modifier.fillMaxWidth(), color = Leaf) }
    }
}

@Composable
private fun LocalC4PassPlay(onExit: () -> Unit) {
    var board by remember { mutableStateOf(List(42) { 0 }) }
    var turn by remember { mutableIntStateOf(1) }
    var winner by remember { mutableIntStateOf(0) }
    fun play(col: Int) {
        if (winner != 0) return
        val b = board.toIntArray()
        if (c4Drop(b, col, turn) < 0) return
        board = b.toList()
        val res = c4Result(b)
        if (res != 0) { winner = res; if (res == 1) ScoreRepository.addWin("connect4", 5) }
        else turn = 3 - turn
    }
    GameScaffold("Local 2-Player — Connect Four", onExit) {
        Spacer(Modifier.height(12.dp))
        Text(when (winner) { 0 -> "Player $turn's turn"; 3 -> "Draw! 🤝"; else -> "Player $winner wins! 🎉" }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        C4Board(board, { play(it) })
        if (winner != 0) { Spacer(Modifier.height(16.dp)); GameButton("PLAY AGAIN", { board = List(42) { 0 }; turn = 1; winner = 0 }, Modifier.fillMaxWidth(), color = Leaf) }
    }
}

/** Local Battleship pass & play with a pass-shield between turns. */
@Composable
fun LocalBsPassPlay(onExit: () -> Unit) {
    // Placement
    var p1Ships by remember { mutableStateOf<List<List<Int>>?>(null) }
    var p2Ships by remember { mutableStateOf<List<List<Int>>?>(null) }
    // During play
    val shots1 = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } } // P1's shots at P2
    val shots2 = remember { mutableStateListOf<Int>().apply { repeat(100) { add(0) } } } // P2's shots at P1
    var currentPlayer by remember { mutableIntStateOf(1) }
    var winner by remember { mutableIntStateOf(0) }
    // Phase: "p1placing" | "shield1" | "p2placing" | "shield2" | "playing" | "passShield" | "done"
    var phase by remember { mutableStateOf("p1placing") }

    fun allSunkLocal(ships: List<List<Int>>, shots: List<Int>) = ships.all { s -> s.all { shots[it] == 2 } }

    fun fireAt(cell: Int) {
        val targetShips = if (currentPlayer == 1) p2Ships!! else p1Ships!!
        val targetShots = if (currentPlayer == 1) shots1 else shots2
        if (targetShots[cell] != 0) return
        val hit = targetShips.any { cell in it }
        targetShots[cell] = if (hit) 2 else 1
        if (allSunkLocal(targetShips, targetShots)) {
            winner = currentPlayer; phase = "done"
        } else if (!hit) {
            // Miss: switch player via pass shield
            currentPlayer = 3 - currentPlayer
            phase = "passShield"
        }
        // Hit: same player continues (no phase change)
    }

    GameScaffold("Battleship ⚓ Pass & Play", onExit) {
      Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (phase) {
            "p1placing" -> LocalBsPlace(playerNum = 1,
                onDone = { ships -> p1Ships = ships; phase = "shield1" },
                onShuffle = { p1Ships = bsRandomFleet(); phase = "shield1" }
            )
            "shield1" -> PassShield(nextPlayer = 2, onReady = { phase = "p2placing" })
            "p2placing" -> LocalBsPlace(playerNum = 2,
                onDone = { ships -> p2Ships = ships; phase = "playing" },
                onShuffle = { p2Ships = bsRandomFleet(); phase = "playing" }
            )
            "passShield" -> PassShield(nextPlayer = currentPlayer, onReady = { phase = "playing" })
            "playing", "done" -> {
                val myShots = if (currentPlayer == 1) shots1 else shots2
                val oppShips = if (currentPlayer == 1) p2Ships!! else p1Ships!!
                val myFleet = if (currentPlayer == 1) p1Ships!! else p2Ships!!
                val myFleetShots = if (currentPlayer == 1) shots2 else shots1

                if (phase == "done") {
                    CreamCard(Modifier.fillMaxWidth()) {
                        Text("🏆 Player $winner wins!", color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(8.dp))
                        GameButton("PLAY AGAIN", {
                            shots1.forEachIndexed { i, _ -> shots1[i] = 0 }
                            shots2.forEachIndexed { i, _ -> shots2[i] = 0 }
                            p1Ships = null; p2Ships = null
                            currentPlayer = 1; winner = 0; phase = "p1placing"
                        }, Modifier.fillMaxWidth(), color = Leaf)
                        Spacer(Modifier.height(8.dp))
                        GameButton("EXIT", onExit, Modifier.fillMaxWidth(), color = Coral)
                    }
                } else {
                    Text("👤 Player $currentPlayer's turn — tap enemy waters!", color = Sun, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("🎯 ENEMY WATERS", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    OnlineBsGrid(shots = myShots, ships = oppShips.filter { s -> s.all { myShots[it] == 2 } }, onTap = { fireAt(it) }, modifier = Modifier.fillMaxWidth(.95f))
                    Spacer(Modifier.height(8.dp))
                    Text("🛡️ YOUR FLEET", color = Sun, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    OnlineBsGrid(shots = myFleetShots, ships = myFleet, onTap = null, modifier = Modifier.fillMaxWidth(.5f))
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
      }
    }
}

@Composable
private fun LocalBsPlace(playerNum: Int, onDone: (List<List<Int>>) -> Unit, onShuffle: () -> Unit) {
    var placedShips by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var placingIdx by remember { mutableIntStateOf(0) }
    var placingHoriz by remember { mutableStateOf(true) }
    var previewCell by remember { mutableStateOf<Int?>(null) }
    var msg by remember { mutableStateOf("Player $playerNum: Place your Carrier (5 cells)") }

    val usedCells = placedShips.flatten().toSet()
    val currentLen = BS_FLEET.getOrNull(placingIdx)
    val previewCells = if (currentLen != null && previewCell != null) bsShipCells(currentLen, previewCell!!, placingHoriz) else null
    val previewValid = previewCells != null && previewCells.none { it in usedCells }

    CreamCard(Modifier.fillMaxWidth()) {
        Text("🚢 PLAYER $playerNum — PLACE YOUR FLEET", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(msg, color = Ink.copy(.75f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BS_FLEET.forEachIndexed { idx, _ ->
            Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(8.dp)).background(when { idx < placedShips.size -> Leaf; idx == placingIdx -> Sun; else -> Board }).border(2.dp, Ink, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(if (idx < placedShips.size) "✓" else BS_SHIP_EMOJIS[idx], color = if (idx <= placingIdx) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    OnlinePlacementGrid(placedShips, previewCells, previewValid, Modifier.fillMaxWidth(.95f), { previewCell = it }) { cell ->
        if (currentLen != null) {
            val cells = bsShipCells(currentLen, cell, placingHoriz)
            if (cells != null && cells.none { it in usedCells }) {
                placedShips = placedShips + listOf(cells); placingIdx++; previewCell = null
                msg = if (placingIdx < BS_FLEET.size) "Player $playerNum: Place ${BS_SHIP_NAMES[placingIdx]}" else "All placed! Hit DONE."
            } else { msg = "❌ Invalid! No overlap, must fit on grid." }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GameButton("🔄 ROTATE", { placingHoriz = !placingHoriz }, Modifier.weight(1f), color = Sky, enabled = currentLen != null)
        GameButton("🎲 SHUFFLE", { onShuffle() }, Modifier.weight(1f), color = Coral)
    }
    if (placingIdx >= BS_FLEET.size) {
        Spacer(Modifier.height(8.dp))
        GameButton("✅ DONE — PASS TO OPPONENT", { onDone(placedShips) }, Modifier.fillMaxWidth(), color = Leaf)
    }
}

@Composable
private fun PassShield(nextPlayer: Int, onReady: () -> Unit) {
    CreamCard(Modifier.fillMaxWidth()) {
        Text("📱 PASS THE PHONE", color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Text("Hand the phone to Player $nextPlayer.\nDon't peek! 👀", color = Ink.copy(.7f), textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        GameButton("👤 I'M PLAYER $nextPlayer — READY!", onReady, Modifier.fillMaxWidth(), color = Sun)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared rematch panel (TTT, C4, Battleship online)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RematchPanel(r: Room, me: Int, opponentName: String, code: String, scope: kotlinx.coroutines.CoroutineScope, context: android.content.Context, onLeave: () -> Unit) {
    val myRematch = if (me == 1) r.rematch1 else r.rematch2
    val oppRematch = if (me == 1) r.rematch2 else r.rematch1

    Spacer(Modifier.height(12.dp))
    when {
        oppRematch && !myRematch -> {
            CreamCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("REMATCH OFFERED!", color = Leaf, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("$opponentName wants to play again!", color = Ink.copy(.85f), fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GameButton("⚡ ACCEPT", { scope.launch { try { OnlineRepository.playAgain(code, me) } catch (e: Exception) { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() } } }, Modifier.weight(1.3f), color = Leaf)
                GameButton("LEAVE", { scope.launch { OnlineRepository.leaveRoom(code, me) }; onLeave() }, Modifier.weight(.7f), color = Coral)
            }
        }
        myRematch && !oppRematch -> {
            CreamCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("REMATCH REQUESTED (1/2)", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("Waiting for $opponentName to accept...", color = Ink.copy(.75f), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GameButton("WAITING... (1/2)", {}, Modifier.weight(1.3f), color = Cream, enabled = false)
                GameButton("LEAVE", { scope.launch { OnlineRepository.cancelPlayAgain(code, me); OnlineRepository.leaveRoom(code, me) }; onLeave() }, Modifier.weight(.7f), color = Coral)
            }
        }
        else -> {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GameButton("🔄 PLAY AGAIN", { scope.launch { try { OnlineRepository.playAgain(code, me) } catch (e: Exception) { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() } } }, Modifier.weight(1.2f), color = Leaf)
                GameButton("LEAVE ROOM", { scope.launch { OnlineRepository.leaveRoom(code, me) }; onLeave() }, Modifier.weight(.8f), color = Coral)
            }
        }
    }
}
