package com.example.arcadehub.logic

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

enum class Difficulty(val label: String, val points: Int) { EASY("Easy", 5), MEDIUM("Medium", 10), HARD("Hard", 20) }

// ---------- Tic Tac Toe ----------
private val TTT_LINES = listOf(
    intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), intArrayOf(0, 3, 6),
    intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
)

/** 0 = running, 1/2 = that player won, 3 = draw */
fun tttResult(b: IntArray): Int {
    for (l in TTT_LINES) { val a = b[l[0]]; if (a != 0 && a == b[l[1]] && a == b[l[2]]) return a }
    return if (b.all { it != 0 }) 3 else 0
}

fun tttBotMove(b: IntArray, d: Difficulty, me: Int = 2): Int {
    val empty = b.indices.filter { b[it] == 0 }.shuffled()
    if (empty.isEmpty()) return 0
    val opp = 3 - me
    fun finishing(p: Int) = empty.firstOrNull { i -> b[i] = p; val w = tttResult(b) == p; b[i] = 0; w }
    return when (d) {
        Difficulty.EASY -> empty.random()
        Difficulty.MEDIUM -> finishing(me) ?: finishing(opp) ?: (if (4 in empty) 4 else empty.random())
        Difficulty.HARD -> {
            var best = -100; var bm = empty.first()
            for (i in empty) { b[i] = me; val s = tttMinimax(b, false, me, -100, 100, 1); b[i] = 0; if (s > best) { best = s; bm = i } }
            bm
        }
    }
}

private fun tttMinimax(b: IntArray, maxing: Boolean, me: Int, a0: Int, b0: Int, depth: Int): Int {
    val r = tttResult(b)
    if (r == me) return 10 - depth
    if (r == 3) return 0
    if (r != 0) return depth - 10
    var alpha = a0; var beta = b0
    val cur = if (maxing) me else 3 - me
    var best = if (maxing) -100 else 100
    for (i in b.indices) {
        if (b[i] != 0) continue
        b[i] = cur
        val s = tttMinimax(b, !maxing, me, alpha, beta, depth + 1)
        b[i] = 0
        if (maxing) { best = max(best, s); alpha = max(alpha, best) } else { best = min(best, s); beta = min(beta, best) }
        if (beta <= alpha) break
    }
    return best
}

// ---------- Connect Four (row 0 = top) ----------
const val C4_R = 6
const val C4_C = 7
private val C4_DIRS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
private val C4_ORDER = listOf(3, 2, 4, 1, 5, 0, 6)

fun c4Drop(b: IntArray, col: Int, p: Int): Int {
    for (r in C4_R - 1 downTo 0) if (b[r * C4_C + col] == 0) { b[r * C4_C + col] = p; return r }
    return -1
}

fun c4Win(b: IntArray, p: Int): Boolean {
    for (r in 0 until C4_R) for (c in 0 until C4_C) for ((dr, dc) in C4_DIRS) {
        var k = 0
        while (k < 4) {
            val rr = r + dr * k; val cc = c + dc * k
            if (rr !in 0 until C4_R || cc !in 0 until C4_C || b[rr * C4_C + cc] != p) break
            k++
        }
        if (k == 4) return true
    }
    return false
}

fun c4Result(b: IntArray): Int = when {
    c4Win(b, 1) -> 1
    c4Win(b, 2) -> 2
    b.none { it == 0 } -> 3
    else -> 0
}

private fun c4Eval(b: IntArray, me: Int): Int {
    var s = 0
    for (r in 0 until C4_R) { val v = b[r * C4_C + 3]; if (v == me) s += 3 else if (v != 0) s -= 3 }
    for (r in 0 until C4_R) for (c in 0 until C4_C) for ((dr, dc) in C4_DIRS) {
        val er = r + dr * 3; val ec = c + dc * 3
        if (er !in 0 until C4_R || ec !in 0 until C4_C) continue
        var m = 0; var o = 0
        for (k in 0..3) { val v = b[(r + dr * k) * C4_C + c + dc * k]; if (v == me) m++ else if (v != 0) o++ }
        if (o == 0) s += when (m) { 3 -> 5; 2 -> 2; else -> 0 }
        if (m == 0) s -= when (o) { 3 -> 5; 2 -> 2; else -> 0 }
    }
    return s
}

private fun c4Search(b: IntArray, depth: Int, a0: Int, b0: Int, maxing: Boolean, me: Int): Int {
    if (c4Win(b, me)) return 100000 + depth
    if (c4Win(b, 3 - me)) return -100000 - depth
    val cols = C4_ORDER.filter { b[it] == 0 }
    if (cols.isEmpty() || depth == 0) return c4Eval(b, me)
    var alpha = a0; var beta = b0
    var best = if (maxing) Int.MIN_VALUE else Int.MAX_VALUE
    for (c in cols) {
        val r = c4Drop(b, c, if (maxing) me else 3 - me)
        val s = c4Search(b, depth - 1, alpha, beta, !maxing, me)
        b[r * C4_C + c] = 0
        if (maxing) { best = max(best, s); alpha = max(alpha, best) } else { best = min(best, s); beta = min(beta, best) }
        if (beta <= alpha) break
    }
    return best
}

fun c4BotMove(b: IntArray, d: Difficulty, me: Int = 2): Int {
    val valid = (0 until C4_C).filter { b[it] == 0 }
    if (valid.isEmpty()) return 0
    fun winning(p: Int) = valid.firstOrNull { c -> val r = c4Drop(b, c, p); val w = c4Win(b, p); b[r * C4_C + c] = 0; w }
    return when (d) {
        Difficulty.EASY -> valid.random()
        Difficulty.MEDIUM -> winning(me) ?: winning(3 - me) ?: valid.minByOrNull { abs(it - 3) + Random.nextInt(3) } ?: valid.first()
        Difficulty.HARD -> {
            var best = Int.MIN_VALUE; var bc = valid.first()
            for (c in C4_ORDER.filter { it in valid }) {
                val r = c4Drop(b, c, me)
                val s = c4Search(b, 5, Int.MIN_VALUE, Int.MAX_VALUE, false, me)
                b[r * C4_C + c] = 0
                if (s > best) { best = s; bc = c }
            }
            bc
        }
    }
}
