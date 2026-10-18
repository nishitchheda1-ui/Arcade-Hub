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

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/** One place to get Firestore. The app talks to the database called "(default)". */
object Db {
    /** If you created a NAMED Firestore database in the console, put its ID here instead. */
    const val DATABASE_ID = "(default)"

    val fs: FirebaseFirestore
        get() = if (DATABASE_ID == "(default)") FirebaseFirestore.getInstance()
        else FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
}

/** Turns scary Firebase errors into something a player (or you) can act on. */
fun Throwable.friendly(): String {
    val m = (message ?: "").lowercase()
    return when {
        "does not exist" in m || ("not_found" in m && "database" in m) ->
            "Firestore database not found. Open Firebase console > Firestore Database and create a database with ID (default)."
        "permission" in m -> "Firestore rules are blocking this. Re-publish the rules from SETUP.md."
        this is TimeoutCancellationException || "unavailable" in m || "timeout" in m ->
            "Couldn't reach Firestore. Check your internet and that the database exists."
        else -> message ?: "Something went wrong"
    }
}
