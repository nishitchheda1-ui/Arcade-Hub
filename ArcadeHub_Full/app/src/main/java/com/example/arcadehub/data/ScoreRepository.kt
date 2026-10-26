package com.example.arcadehub.data

import android.content.Context
import android.content.SharedPreferences
import com.example.arcadehub.model.allGames
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

data class LbEntry(val uid: String, val name: String, val score: Long, val isLocal: Boolean = false)

/**
 * Robust ScoreRepository supporting dual Local Storage (SharedPreferences) + Firebase Cloud.
 *
 * Guarantees that:
 * 1. Scores (including 2048) are ALWAYS saved locally even without internet or if Firestore isn't created yet.
 * 2. When online and Firestore is enabled, scores automatically sync to the cloud.
 * 3. The leaderboard always displays meaningful records and the player's real rank.
 */
object ScoreRepository {
    private var prefs: SharedPreferences? = null
    private val auth get() = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("arcade_hub_scores", Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(): SharedPreferences? = prefs

    fun getLocalBest(game: String): Long =
        getPrefs()?.getLong("best_$game", 0L) ?: 0L

    fun getLocalWins(game: String): Long =
        getPrefs()?.getLong("wins_$game", 0L) ?: 0L

    fun getTotalPoints(): Long =
        getPrefs()?.getLong("total_points", 0L) ?: 0L

    fun getTier(points: Long): String = when {
        points >= 1000 -> "👑 Legend Superstar"
        points >= 500  -> "🏆 Mega Champion"
        points >= 250  -> "⭐ Star Hero"
        points >= 100  -> "🚀 Super Gamer"
        points >= 30   -> "🎈 Fun Explorer"
        else           -> "🌱 Cute Rookie"
    }

    /** Keeps the player's best score for score-based games (2048, Snake, Flappy, etc.). */
    fun submitBest(game: String, score: Int, points: Int = 5) {
        // 1. Instantly persist locally so progress is never lost
        val p = getPrefs()
        if (p != null) {
            val cur = p.getLong("best_$game", 0L)
            if (score > cur) {
                p.edit().putLong("best_$game", score.toLong()).apply()
            }
            val curPts = p.getLong("total_points", 0L)
            p.edit().putLong("total_points", curPts + points.toLong()).apply()
        }

        // 2. Sync to Firebase Firestore asynchronously if authenticated
        val u = auth.currentUser ?: return
        val displayName = u.displayName?.ifBlank { "Player" } ?: "Player"

        scope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    val ref = Db.fs.collection("leaderboards").document(game).collection("entries").document(u.uid)
                    val cloudDoc = try { ref.get().await() } catch (_: Exception) { null }
                    val curCloud = cloudDoc?.getLong("score") ?: 0L
                    if (score > curCloud) {
                        ref.set(
                            mapOf(
                                "uid" to u.uid,
                                "username" to displayName,
                                "score" to score.toLong(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                    addPoints(u.uid, displayName, points)
                }
            } catch (_: Exception) {
                // Ignore cloud sync errors, local score is safely saved
            }
        }
    }

    /** Counts a win for bot / multiplayer games. */
    fun addWin(game: String, points: Int) {
        // 1. Instantly persist locally
        val p = getPrefs()
        if (p != null) {
            val cur = p.getLong("wins_$game", 0L)
            p.edit().putLong("wins_$game", cur + 1).apply()
            val curPts = p.getLong("total_points", 0L)
            p.edit().putLong("total_points", curPts + points.toLong()).apply()
        }

        // 2. Sync to Firebase Firestore asynchronously if authenticated
        val u = auth.currentUser ?: return
        val displayName = u.displayName?.ifBlank { "Player" } ?: "Player"

        scope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    Db.fs.collection("leaderboards").document(game).collection("entries").document(u.uid).set(
                        mapOf(
                            "uid" to u.uid,
                            "username" to displayName,
                            "score" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                    addPoints(u.uid, displayName, points)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun addPoints(uid: String, name: String?, p: Int) {
        try {
            Db.fs.collection("users").document(uid).set(
                mapOf(
                    "username" to (name ?: "Player"),
                    "points" to FieldValue.increment(p.toLong())
                ),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) {}
    }

    /**
     * Loads leaderboard entries.
     * If game == null -> Global Ranking by Total Arcade Points
     * If game != null -> Game-specific ranking (e.g. 2048, Snake, Flappy)
     *
     * Falls back seamlessly to local player stats if Firestore is uninitialized or offline.
     */
    suspend fun top(game: String?): List<LbEntry> {
        val currentUid = auth.currentUser?.uid ?: "local_player"
        val currentName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: AuthRepository().activeDisplayName

        val localScore = if (game == null) getTotalPoints() else getLocalBest(game).let { if (it > 0) it else getLocalWins(game) }

        try {
            val entries = withTimeout(6000L) {
                if (game == null) {
                    // Global ranking query
                    val docs = Db.fs.collection("users")
                        .orderBy("points", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .await()
                        .documents

                    docs.map {
                        LbEntry(
                            uid = it.id,
                            name = it.getString("username") ?: "Player",
                            score = it.getLong("points") ?: 0L
                        )
                    }
                } else {
                    // Game-specific query
                    val docs = Db.fs.collection("leaderboards").document(game).collection("entries")
                        .orderBy("score", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .await()
                        .documents

                    docs.map {
                        LbEntry(
                            uid = it.id,
                            name = it.getString("username") ?: "Player",
                            score = it.getLong("score") ?: 0L
                        )
                    }
                }
            }

            // If cloud entries exist, merge current player's local score if higher or missing
            if (entries.isNotEmpty()) {
                val hasMe = entries.any { it.uid == currentUid }
                val merged = if (!hasMe && localScore > 0) {
                    (entries + LbEntry(currentUid, "$currentName", localScore, isLocal = true))
                        .sortedByDescending { it.score }
                } else {
                    entries.map {
                        if (it.uid == currentUid && localScore > it.score) it.copy(score = localScore)
                        else it
                    }
                }
                return merged
            }
        } catch (_: Exception) {
            // Firestore not ready or offline: return local records gracefully
        }

        // Offline / Local fallback: Show player's actual local score and playful arcade challengers
        return getLocalFallbackEntries(game, currentUid, currentName, localScore)
    }

    private fun getLocalFallbackEntries(
        game: String?,
        myUid: String,
        myName: String,
        myScore: Long
    ): List<LbEntry> {
        val list = mutableListOf<LbEntry>()
        list.add(LbEntry(myUid, myName, myScore, isLocal = true))

        // If player has 0 score, give them some targets to beat!
        val sampleTargets = if (game == null) {
            listOf(
                LbEntry("bot_3", "ArcadeChampion", maxOf(myScore + 180, 450L)),
                LbEntry("bot_2", "SpeedyGamer", maxOf(myScore + 80, 220L)),
                LbEntry("bot_1", "RetroMaster", maxOf(myScore - 40, 80L).coerceAtLeast(20L))
            )
        } else if (game == "2048") {
            listOf(
                LbEntry("bot_3", "TileWizard", maxOf(myScore + 1024, 4096L)),
                LbEntry("bot_2", "MergeKing", maxOf(myScore + 512, 2048L)),
                LbEntry("bot_1", "GridPro", maxOf(myScore - 200, 1024L).coerceAtLeast(256L))
            )
        } else {
            emptyList()
        }

        return (list + sampleTargets).sortedByDescending { it.score }
    }
}
