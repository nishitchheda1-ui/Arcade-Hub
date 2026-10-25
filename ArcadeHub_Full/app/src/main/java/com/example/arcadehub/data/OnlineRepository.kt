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
    val code: String, val game: String, val hostName: String, val guestName: String?,
    val board: List<Int>, val turn: Int, val status: String, val winner: Int,
    val rematch1: Boolean = false, val rematch2: Boolean = false, val round: Int = 1
)

/** Online rooms for Tic Tac Toe ("ttt") and Connect Four ("c4"). Host = player 1, guest = player 2. */
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
                    t.set(ref, mapOf(
                        "game" to game, "hostUid" to u.uid, "hostName" to (u.displayName ?: "Player"),
                        "guestUid" to null, "guestName" to null,
                        "board" to List(if (game == "ttt") 9 else 42) { 0 },
                        "turn" to 1, "status" to "waiting", "winner" to 0,
                        "rematch1" to false, "rematch2" to false, "round" to 1
                    ))
                    true
                }
            }.await()
            if (ok) return code
        }
        throw IllegalStateException("Could not create a room, try again")
    }

    /** returns null on success, otherwise an error message */
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
                    t.update(ref, mapOf("guestUid" to u.uid, "guestName" to (u.displayName ?: "Player"), "status" to "playing"))
                    null
                }
            }
        }.await()
    }

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
                t.update(ref, mapOf(
                    "board" to List(if (game == "ttt") 9 else 42) { 0 },
                    "turn" to nextTurn,
                    "status" to "playing",
                    "winner" to 0,
                    "rematch1" to false,
                    "rematch2" to false,
                    "round" to nextRound
                ))
            } else {
                t.update(ref, mapOf(
                    if (me == 1) "rematch1" to true else "rematch2" to true
                ))
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
                    if (status == "playing" || status == "waiting" || status == "done") {
                        t.update(ref, mapOf(
                            "status" to "abandoned",
                            "abandonedBy" to me
                        ))
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
        code = id, game = getString("game") ?: "ttt", hostName = getString("hostName") ?: "Host",
        guestName = getString("guestName"),
        board = (get("board") as? List<Number>)?.map { it.toInt() } ?: emptyList(),
        turn = (getLong("turn") ?: 1L).toInt(), status = getString("status") ?: "waiting",
        winner = (getLong("winner") ?: 0L).toInt(),
        rematch1 = getBoolean("rematch1") ?: false,
        rematch2 = getBoolean("rematch2") ?: false,
        round = (getLong("round") ?: 1L).toInt()
    )
}
