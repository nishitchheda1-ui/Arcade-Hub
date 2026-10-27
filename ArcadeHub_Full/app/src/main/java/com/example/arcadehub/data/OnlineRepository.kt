package com.example.arcadehub.data

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

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Room(
    val code: String,
    val game: String,
    val hostName: String,
    val guestName: String?,
    val board: List<Int>,
    val turn: Int,
    val status: String,   // "waiting" | "placing" | "playing" | "done" | "abandoned"
    val winner: Int,
    val rematch1: Boolean = false,
    val rematch2: Boolean = false,
    val round: Int = 1,
    // ── Battleship-specific ──
    val ships1: List<Int> = emptyList(),   // flat 100-int grid: 1=ship cell, 0=water
    val ships2: List<Int> = emptyList(),
    val shots1: List<Int> = emptyList(),   // shots player-1 fired at player-2: 0=unfired,1=miss,2=hit
    val shots2: List<Int> = emptyList(),   // shots player-2 fired at player-1
    val bReady1: Boolean = false,
    val bReady2: Boolean = false
)

/** Online rooms for Tic Tac Toe ("ttt"), Connect Four ("c4"), and Battleship ("battleship").
 *  Host = player 1, Guest = player 2. */
object OnlineRepository {
    private val db get() = Db.fs
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun create(game: String): String = withTimeout(15_000) { createRoom(game) }

    private suspend fun createRoom(game: String): String {
        val u = auth.currentUser!!
        repeat(8) {
            val code = (1000..9999).random().toString()
            val ref = db.collection("rooms").document(code)
            val ok = db.runTransaction { t ->
                val s = t.get(ref)
                if (s.exists() && s.getString("status") != "done" && s.getString("status") != "abandoned") false
                else {
                    val boardSize = when (game) { "ttt" -> 9; "c4" -> 42; else -> 0 }
                    val baseData = mutableMapOf<String, Any?>(
                        "game" to game,
                        "hostUid" to u.uid,
                        "hostName" to (u.displayName ?: "Player"),
                        "guestUid" to null,
                        "guestName" to null,
                        "turn" to 1,
                        "winner" to 0,
                        "rematch1" to false,
                        "rematch2" to false,
                        "round" to 1
                    )
                    if (game == "battleship") {
                        baseData["status"] = "waiting"
                        baseData["board"] = emptyList<Int>()
                        baseData["ships1"] = emptyList<Int>()
                        baseData["ships2"] = emptyList<Int>()
                        baseData["shots1"] = List(100) { 0 }
                        baseData["shots2"] = List(100) { 0 }
                        baseData["bReady1"] = false
                        baseData["bReady2"] = false
                    } else {
                        baseData["status"] = "waiting"
                        baseData["board"] = List(boardSize) { 0 }
                    }
                    t.set(ref, baseData)
                    true
                }
            }.await()
            if (ok) return code
        }
        throw IllegalStateException("Could not create a room, try again")
    }

    /** Returns null on success, otherwise an error message. */
    suspend fun join(code: String): String? = withTimeout(15_000) { joinRoom(code) }

    private suspend fun joinRoom(code: String): String? {
        val u = auth.currentUser!!
        val ref = db.collection("rooms").document(code)
        return db.runTransaction { t ->
            val s = t.get(ref)
            when {
                !s.exists() -> "Room not found"
                s.getString("hostUid") == u.uid -> "You created this room"
                s.getString("status") != "waiting" -> "Room already started"
                else -> {
                    val game = s.getString("game") ?: "ttt"
                    val updates = mutableMapOf<String, Any?>(
                        "guestUid" to u.uid,
                        "guestName" to (u.displayName ?: "Player"),
                        "status" to if (game == "battleship") "placing" else "playing"
                    )
                    t.update(ref, updates)
                    null
                }
            }
        }.await()
    }

    /** TTT / C4 move: idx is the cell/column tapped. */
    @Suppress("UNCHECKED_CAST")
    suspend fun move(code: String, me: Int, idx: Int) {
        val ref = db.collection("rooms").document(code)
        db.runTransaction { t ->
            val s = t.get(ref)
            if (s.getString("status") != "playing" || (s.getLong("turn") ?: 0L).toInt() != me) return@runTransaction false
            val board = (s.get("board") as List<Number>).map { it.toInt() }.toIntArray()
            val ttt = s.getString("game") == "ttt"
            if (ttt) { if (board[idx] != 0) return@runTransaction false; board[idx] = me }
            else if (c4Drop(board, idx, me) < 0) return@runTransaction false
            val res = if (ttt) tttResult(board) else c4Result(board)
            t.update(ref, mapOf(
                "board" to board.toList(), "turn" to 3 - me, "winner" to res,
                "status" to if (res != 0) "done" else "playing",
                "rematch1" to false, "rematch2" to false
            ))
            true
        }.await()
    }

    /** Battleship placement: player [me] submits their ship grid (flat 100-int list). */
    @Suppress("UNCHECKED_CAST")
    suspend fun battleshipPlaceShips(code: String, me: Int, shipGrid: List<Int>) {
        val ref = db.collection("rooms").document(code)
        db.runTransaction { t ->
            val s = t.get(ref)
            if (s.getString("status") != "placing") return@runTransaction false
            val shipsKey = if (me == 1) "ships1" else "ships2"
            val readyKey = if (me == 1) "bReady1" else "bReady2"
            val otherReady = if (me == 1)
                s.getBoolean("bReady2") ?: false
            else
                s.getBoolean("bReady1") ?: false

            val updates = mutableMapOf<String, Any?>(
                shipsKey to shipGrid,
                readyKey to true
            )
            // If both ready, start playing
            if (otherReady) {
                updates["status"] = "playing"
                updates["turn"] = 1
            }
            t.update(ref, updates)
            true
        }.await()
    }

    /** Battleship shot: player [me] fires at cell [cell] (0-99) on opponent's grid. */
    @Suppress("UNCHECKED_CAST")
    suspend fun battleshipShoot(code: String, me: Int, cell: Int) {
        val ref = db.collection("rooms").document(code)
        db.runTransaction { t ->
            val s = t.get(ref)
            if (s.getString("status") != "playing" || (s.getLong("turn") ?: 0L).toInt() != me) return@runTransaction false

            val oppShipsKey = if (me == 1) "ships2" else "ships1"
            val myShotsKey = if (me == 1) "shots1" else "shots2"

            val oppShips = (s.get(oppShipsKey) as? List<Number>)?.map { it.toInt() } ?: List(100) { 0 }
            val myShots = (s.get(myShotsKey) as? List<Number>)?.map { it.toInt() }?.toMutableList() ?: MutableList(100) { 0 }

            if (myShots[cell] != 0) return@runTransaction false // already fired here

            val isHit = oppShips[cell] == 1
            myShots[cell] = if (isHit) 2 else 1

            // Check if all opponent ship cells are hit
            val allSunk = oppShips.indices.all { i -> oppShips[i] == 0 || myShots[i] == 2 }

            val updates = mutableMapOf<String, Any?>(
                myShotsKey to myShots.toList()
            )
            if (allSunk) {
                updates["status"] = "done"
                updates["winner"] = me
                updates["turn"] = me
            } else if (!isHit) {
                // Miss → swap turn
                updates["turn"] = 3 - me
            }
            // Hit → same player continues (turn unchanged)

            t.update(ref, updates)
            true
        }.await()
    }

    suspend fun playAgain(code: String, me: Int) = withTimeout(15_000) {
        val ref = db.collection("rooms").document(code)
        db.runTransaction { t ->
            val s = t.get(ref)
            if (!s.exists()) return@runTransaction false
            val status = s.getString("status") ?: ""
            if (status != "done") return@runTransaction false

            val r1 = s.getBoolean("rematch1") ?: false
            val r2 = s.getBoolean("rematch2") ?: false
            val opponentReady = if (me == 1) r2 else r1

            if (opponentReady) {
                val game = s.getString("game") ?: "ttt"
                val curRound = (s.getLong("round") ?: 1L).toInt()
                val nextRound = curRound + 1
                val nextTurn = if (nextRound % 2 == 1) 1 else 2
                val resetData = mutableMapOf<String, Any?>(
                    "turn" to nextTurn,
                    "status" to if (game == "battleship") "placing" else "playing",
                    "winner" to 0,
                    "rematch1" to false,
                    "rematch2" to false,
                    "round" to nextRound
                )
                if (game == "battleship") {
                    resetData["ships1"] = emptyList<Int>()
                    resetData["ships2"] = emptyList<Int>()
                    resetData["shots1"] = List(100) { 0 }
                    resetData["shots2"] = List(100) { 0 }
                    resetData["bReady1"] = false
                    resetData["bReady2"] = false
                    resetData["board"] = emptyList<Int>()
                } else {
                    val boardSize = if (game == "ttt") 9 else 42
                    resetData["board"] = List(boardSize) { 0 }
                }
                t.update(ref, resetData)
            } else {
                t.update(ref, mapOf(if (me == 1) "rematch1" to true else "rematch2" to true))
            }
            true
        }.await()
    }

    suspend fun cancelPlayAgain(code: String, me: Int) {
        try {
            val ref = db.collection("rooms").document(code)
            db.runTransaction { t ->
                val s = t.get(ref)
                if (s.exists() && s.getString("status") == "done") {
                    t.update(ref, mapOf(if (me == 1) "rematch1" to false else "rematch2" to false))
                }
            }.await()
        } catch (_: Exception) {}
    }

    suspend fun leaveRoom(code: String, me: Int) {
        try {
            val ref = db.collection("rooms").document(code)
            db.runTransaction { t ->
                val s = t.get(ref)
                if (s.exists()) {
                    val status = s.getString("status") ?: ""
                    if (status !in listOf("done", "abandoned")) {
                        t.update(ref, mapOf("status" to "abandoned", "abandonedBy" to me))
                    }
                }
            }.await()
        } catch (_: Exception) {}
    }

    fun listen(code: String): Flow<Room?> = callbackFlow {
        val reg = db.collection("rooms").document(code).addSnapshotListener { s, _ ->
            trySend(s?.takeIf { it.exists() }?.toRoom())
        }
        awaitClose { reg.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toRoom() = Room(
        code = id,
        game = getString("game") ?: "ttt",
        hostName = getString("hostName") ?: "Host",
        guestName = getString("guestName"),
        board = (get("board") as? List<Number>)?.map { it.toInt() } ?: emptyList(),
        turn = (getLong("turn") ?: 1L).toInt(),
        status = getString("status") ?: "waiting",
        winner = (getLong("winner") ?: 0L).toInt(),
        rematch1 = getBoolean("rematch1") ?: false,
        rematch2 = getBoolean("rematch2") ?: false,
        round = (getLong("round") ?: 1L).toInt(),
        ships1 = (get("ships1") as? List<Number>)?.map { it.toInt() } ?: emptyList(),
        ships2 = (get("ships2") as? List<Number>)?.map { it.toInt() } ?: emptyList(),
        shots1 = (get("shots1") as? List<Number>)?.map { it.toInt() } ?: List(100) { 0 },
        shots2 = (get("shots2") as? List<Number>)?.map { it.toInt() } ?: List(100) { 0 },
        bReady1 = getBoolean("bReady1") ?: false,
        bReady2 = getBoolean("bReady2") ?: false
    )
}
