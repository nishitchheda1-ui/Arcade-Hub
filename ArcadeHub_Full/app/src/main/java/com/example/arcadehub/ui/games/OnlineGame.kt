package com.example.arcadehub.ui.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadehub.data.*
import com.example.arcadehub.logic.*
import com.example.arcadehub.ui.components.*
import com.example.arcadehub.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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

    // Local Pass & Play fallback mode
    if (isLocalPassPlay) {
        if (game == "ttt") {
            var localBoard by remember { mutableStateOf(List(9) { 0 }) }
            var localTurn by remember { mutableIntStateOf(1) }
            var localWinner by remember { mutableIntStateOf(0) }

            fun play(i: Int) {
                if (localWinner != 0 || localBoard[i] != 0) return
                val nb = localBoard.toMutableList().also { it[i] = localTurn }
                localBoard = nb
                val res = tttResult(nb.toIntArray())
                if (res != 0) {
                    localWinner = res
                    if (res == 1) ScoreRepository.addWin("tictactoe", 5)
                } else {
                    localTurn = 3 - localTurn
                }
            }

            GameScaffold("Local 2-Player (Tic Tac Toe)", { isLocalPassPlay = false }) {
                Spacer(Modifier.height(12.dp))
                Text(
                    when (localWinner) {
                        0 -> "Player $localTurn's turn (${if (localTurn == 1) "X" else "O"})"
                        3 -> "Draw!"
                        else -> "Player $localWinner wins! 🎉"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                TttBoard(localBoard, { play(it) })
                if (localWinner != 0) {
                    Spacer(Modifier.height(16.dp))
                    GameButton("PLAY AGAIN", { localBoard = List(9) { 0 }; localTurn = 1; localWinner = 0 }, Modifier.fillMaxWidth(), color = Leaf)
                }
            }
            return
        } else {
            var localBoard by remember { mutableStateOf(List(42) { 0 }) }
            var localTurn by remember { mutableIntStateOf(1) }
            var localWinner by remember { mutableIntStateOf(0) }

            fun play(col: Int) {
                if (localWinner != 0) return
                val b = localBoard.toIntArray()
                if (c4Drop(b, col, localTurn) < 0) return
                localBoard = b.toList()
                val res = c4Result(b)
                if (res != 0) {
                    localWinner = res
                    if (res == 1) ScoreRepository.addWin("connect4", 5)
                } else {
                    localTurn = 3 - localTurn
                }
            }

            GameScaffold("Local 2-Player (Connect Four)", { isLocalPassPlay = false }) {
                Spacer(Modifier.height(12.dp))
                Text(
                    when (localWinner) {
                        0 -> "Player $localTurn's turn (${if (localTurn == 1) "Coral" else "Sun"})"
                        3 -> "Draw!"
                        else -> "Player $localWinner wins! 🎉"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                C4Board(localBoard, { play(it) })
                if (localWinner != 0) {
                    Spacer(Modifier.height(16.dp))
                    GameButton("PLAY AGAIN", { localBoard = List(42) { 0 }; localTurn = 1; localWinner = 0 }, Modifier.fillMaxWidth(), color = Leaf)
                }
            }
            return
        }
    }

    if (c == null) {
        GameScaffold("Online Rooms", onExit) {
            Spacer(Modifier.height(12.dp))
            Text("Play a friend with a 4-digit room code", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("❌ Tic Tac Toe", game == "ttt", color = Coral) { game = "ttt" }
                Pill("🔴 Connect Four", game == "c4", color = Sun) { game = "c4" }
            }
            Spacer(Modifier.height(16.dp))

            GameButton(
                text = "CREATE ROOM CODE",
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        if (auth.currentUser == null) {
                            error = "Sign in with an account to host online rooms, or play Local 2-Player!"
                            busy = false
                            return@launch
                        }
                        try {
                            code = OnlineRepository.create(game)
                            me = 1
                        } catch (e: Exception) {
                            error = e.friendly()
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                loading = busy,
                color = Sun
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
                        busy = true
                        error = null
                        if (auth.currentUser == null) {
                            error = "Sign in with an account to join online rooms, or play Local 2-Player!"
                            busy = false
                            return@launch
                        }
                        try {
                            val err = OnlineRepository.join(input)
                            if (err == null) {
                                code = input
                                me = 2
                            } else {
                                error = err
                            }
                        } catch (e: Exception) {
                            error = e.friendly()
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = Coral,
                loading = busy,
                enabled = input.length == 4
            )

            // Database Does Not Exist / Setup Error Diagnostic Box
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                val isDbError = error!!.contains("Firestore", ignoreCase = true) ||
                        error!!.contains("database", ignoreCase = true) ||
                        error!!.contains("does not exist", ignoreCase = true)

                CreamCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Coral, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isDbError) "Firebase Firestore Setup Needed" else "Room Notice",
                            color = Ink,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isDbError) {
                            "The Cloud Firestore database has not been activated yet in Firebase Console for project 'arcade-hub-5f70a'.\n\n" +
                            "To enable real-time room codes:\n" +
                            "1. Open console.firebase.google.com\n" +
                            "2. Select project: arcade-hub-5f70a\n" +
                            "3. Click 'Firestore Database' (under Build)\n" +
                            "4. Click 'Create database' and choose 'Start in test mode'\n" +
                            "5. Click Done! Rooms will immediately work."
                        } else error!!,
                        color = Ink.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isDbError) {
                            GameButton(
                                text = "📋 COPY LINK",
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Firebase", "https://console.firebase.google.com/project/arcade-hub-5f70a/firestore"))
                                    Toast.makeText(context, "Firebase Firestore URL copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                color = Sky,
                                fontSize = 12.sp
                            )
                        }
                        GameButton(
                            text = "🎮 PLAY LOCAL 2P",
                            onClick = { isLocalPassPlay = true },
                            modifier = Modifier.weight(1f),
                            color = Leaf,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Instant Local Pass & Play Button
                Spacer(Modifier.height(20.dp))
                CreamCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PLAY TOGETHER ON THIS PHONE", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Text("No internet or database setup required. Take turns on this screen!", color = Ink.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    GameButton("PASS & PLAY LOCAL MATCH", { isLocalPassPlay = true }, Modifier.fillMaxWidth(), color = Leaf, fontSize = 14.sp)
                }
            }
        }
    } else {
        val flow: Flow<Room?> = remember(c) { OnlineRepository.listen(c) }
        val room by flow.collectAsState(initial = null)
        val r = room
        var awardedRoomCode by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(r?.status, r?.winner, c) {
            if (r != null && r.status == "done" && r.winner == me && awardedRoomCode != c) {
                awardedRoomCode = c
                ScoreRepository.addWin("online", 15)
            }
        }

        GameScaffold("Room $c", { code = null; input = "" }) {
            Spacer(Modifier.height(12.dp))
            when {
                r == null -> {
                    Text("Connecting to room...", color = Color.White)
                    CircularProgressIndicator(color = Sun, modifier = Modifier.padding(top = 16.dp))
                }
                r.status == "waiting" -> {
                    CreamCard(Modifier.fillMaxWidth()) {
                        Text("SHARE THIS CODE", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(c, color = Coral, fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
                        Text("Tell your friend to enter this 4-digit code under Join Room", color = Ink.copy(alpha = 0.75f), textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = Sun)
                    Text("Waiting for opponent to join...", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp))
                }
                else -> {
                    Text("${r.hostName} vs ${r.guestName ?: "?"}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    val mine = if (r.game == "ttt") (if (me == 1) "X" else "O") else (if (me == 1) "Pink" else "Green")
                    Text(
                        when {
                            r.status == "done" && r.winner == 3 -> "Draw!"
                            r.status == "done" -> if (r.winner == me) "You won! 🎉" else "You lost"
                            r.turn == me -> "Your turn (you are $mine)"
                            else -> "Opponent's turn (you are $mine)"
                        },
                        color = Sun,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    val onPick: (Int) -> Unit = { i ->
                        if (r.status == "playing" && r.turn == me) scope.launch { try { OnlineRepository.move(c, me, i) } catch (_: Exception) {} }
                    }
                    if (r.game == "ttt") TttBoard(r.board, onPick) else C4Board(r.board, onPick)
                    if (r.status == "done") {
                        Spacer(Modifier.height(16.dp))
                        GameButton("LEAVE ROOM", { code = null; input = "" }, Modifier.fillMaxWidth(), color = Coral)
                    }
                }
            }
        }
    }
}
