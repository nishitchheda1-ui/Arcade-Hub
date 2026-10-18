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

private val XBlue = Color(0xFF2F5FD0)

@Composable
fun TttBoard(board: List<Int>, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(0.92f).aspectRatio(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (r in 0..2) Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (c in 0..2) {
                val i = r * 3 + c; val v = board[i]
                val shape = RoundedCornerShape(16.dp)
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(shape).background(Cream).border(4.dp, Ink, shape).clickable { onTap(i) },
                    contentAlignment = Alignment.Center
                ) { if (v != 0) Text(if (v == 1) "X" else "O", color = if (v == 1) XBlue else Coral, fontSize = 56.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
fun C4Board(board: List<Int>, onCol: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().aspectRatio(7f / 6f).clip(shape).background(Color(0xFF2B59C3)).border(4.dp, Ink, shape).padding(8.dp)
    ) {
        for (c in 0 until 7) Column(Modifier.weight(1f).fillMaxHeight().clickable { onCol(c) }) {
            for (r in 0 until 6) {
                val v = board[r * 7 + c]
                Box(Modifier.weight(1f).fillMaxWidth().padding(3.dp)) {
                    Box(
                        Modifier.fillMaxSize().background(when (v) { 1 -> Coral; 2 -> Sun; else -> BgDeep }, CircleShape)
                            .border(2.dp, Ink, CircleShape)
                    )
                }
            }
        }
    }
}
