package com.example.arcadehub.ui.splash

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

import androidx.compose.animation.core.*
import androidx.compose.ui.res.painterResource
import com.example.arcadehub.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val pop = remember { Animatable(0.3f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(700))
        delay(700)
        onFinished()
    }

    ArcadeBackground {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(200.dp).scale(pop.value))
            OutlinedTitle("ARCADE HUB", 42.sp, Modifier.alpha(fade.value))
            Text(
                "Where Fun Never Stops! 🎈🎮",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                modifier = Modifier.alpha(fade.value).padding(top = 10.dp)
            )
        }
    }
}
