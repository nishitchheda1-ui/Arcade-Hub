package com.example.arcadehub.ui.components

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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/** Playful, magical kid-friendly playground background with vertical gradient and floating twinkle stars & bubbles. */
@Composable
fun ArcadeBackground(content: @Composable BoxScope.() -> Unit) {
    val bgGradient = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF381F78),
                Color(0xFF26155E),
                Color(0xFF180C42)
            )
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                val step = 56.dp.toPx()
                var y = 0f
                var row = 0
                while (y < size.height + step) {
                    var x = if (row % 2 == 0) step * 0.25f else step * 0.75f
                    var col = 0
                    while (x < size.width + step) {
                        val kind = (row * 7 + col * 11) % 5
                        when (kind) {
                            0 -> {
                                // Cute 4-point twinkle sparkle star
                                val starR = 5.5.dp.toPx()
                                drawLine(
                                    Color(0xFFFFDF00).copy(alpha = 0.28f),
                                    Offset(x - starR, y),
                                    Offset(x + starR, y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    Color(0xFFFFDF00).copy(alpha = 0.28f),
                                    Offset(x, y - starR),
                                    Offset(x, y + starR),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(Color.White.copy(alpha = 0.35f), 1.8.dp.toPx(), Offset(x, y))
                            }
                            1 -> {
                                // Translucent soap bubble with cute gloss crescent
                                val bubbleR = 7.dp.toPx()
                                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.14f), bubbleR, Offset(x, y))
                                drawCircle(Color.White.copy(alpha = 0.22f), bubbleR * 0.3f, Offset(x - bubbleR * 0.35f, y - bubbleR * 0.35f))
                            }
                            2 -> {
                                // Candy donut ring
                                val ringR = 4.5.dp.toPx()
                                drawCircle(Color(0xFFFF69B4).copy(alpha = 0.20f), ringR, Offset(x, y), style = Stroke(2.dp.toPx()))
                            }
                            3 -> {
                                // Confetti cross sprinkle
                                val cr = 4.dp.toPx()
                                drawLine(
                                    Color(0xFF4ADE80).copy(alpha = 0.20f),
                                    Offset(x - cr, y - cr),
                                    Offset(x + cr, y + cr),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    Color(0xFF4ADE80).copy(alpha = 0.20f),
                                    Offset(x - cr, y + cr),
                                    Offset(x + cr, y - cr),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            else -> {
                                // Soft playful mini dot
                                drawCircle(Color.White.copy(alpha = 0.12f), 3.dp.toPx(), Offset(x, y))
                            }
                        }
                        x += step
                        col++
                    }
                    y += step * 0.7f
                    row++
                }
            },
        content = content
    )
}

/** Dark rounded frame drawn on top of a game canvas. */
fun DrawScope.drawFrame() {
    drawRoundRect(Ink, Offset(6f, 6f), Size(size.width - 12f, size.height - 12f), CornerRadius(32f), style = Stroke(12f))
}

/** Chunky 3D toy button with satisfying squishy press depth and cartoon gloss highlight. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Sun,
    enabled: Boolean = true,
    loading: Boolean = false,
    fontSize: TextUnit = 17.sp
) {
    val shape = RoundedCornerShape(20.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 0.dp else 6.dp, label = "lift")
    val scale by animateFloatAsState(if (pressed) 0.98f else 1.0f, label = "buttonScale")

    Box(
        modifier
            .height(60.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        // 3D bottom shadow base
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .background(Ink, shape)
        )
        // Main button face
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 6.dp - lift, bottom = lift)
                .clip(shape)
                .background(color)
                .border(3.2.dp, Ink, shape)
                .clickable(
                    interactionSource = src,
                    indication = null,
                    enabled = enabled && !loading,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle top glossy toy highlight
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
            )

            if (loading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Ink, strokeWidth = 3.dp)
            } else {
                Text(
                    text = text,
                    color = Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize,
                    maxLines = 1,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SquareButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Sun,
    desc: String? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 0.dp else 4.dp, label = "sqLift")
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "sqScale")

    Box(modifier.size(52.dp).scale(scale)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(Ink, shape)
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp - lift, bottom = lift)
                .clip(shape)
                .background(color)
                .border(3.dp, Ink, shape)
                .clickable(
                    interactionSource = src,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top gloss
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Icon(icon, desc, tint = Ink, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    color: Color = Sun,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pillScale")

    Box(
        Modifier
            .scale(scale)
            .clip(shape)
            .background(if (selected) color else CardBgElevated)
            .border(2.5.dp, if (selected) Ink else Color.White.copy(alpha = 0.35f), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Ink else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun CreamCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .background(Ink, shape)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clip(shape)
                .background(Cream)
                .border(3.2.dp, Ink, shape)
                .padding(18.dp),
            content = content
        )
    }
}

/** Title with a thick dark outline, like a cartoon logo. */
@Composable
fun OutlinedTitle(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Sun
) {
    Box(modifier) {
        // Deep 3D drop shadow
        Text(
            text = text,
            letterSpacing = 1.2.sp,
            style = TextStyle(
                color = Ink,
                fontSize = size,
                fontWeight = FontWeight.Black,
                drawStyle = Stroke(width = size.value * 0.45f, join = StrokeJoin.Round)
            ),
            modifier = Modifier.offset(x = 2.dp, y = 3.dp)
        )
        // Clean outer ink contour
        Text(
            text = text,
            letterSpacing = 1.2.sp,
            style = TextStyle(
                color = Ink,
                fontSize = size,
                fontWeight = FontWeight.Black,
                drawStyle = Stroke(width = size.value * 0.35f, join = StrokeJoin.Round)
            )
        )
        // Vibrant cartoon face fill
        Text(
            text = text,
            color = color,
            fontSize = size,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun GameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var visible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(icon, null, tint = Ink) },
        trailingIcon = if (isPassword) {
            { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Ink) } }
        } else null,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Cream, unfocusedContainerColor = Cream,
            focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Ink,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = Ink, unfocusedLabelColor = Ink.copy(alpha = 0.65f),
            focusedLeadingIconColor = Ink, unfocusedLeadingIconColor = Ink,
            focusedTrailingIconColor = Ink, unfocusedTrailingIconColor = Ink
        ),
        modifier = modifier.fillMaxWidth().border(3.dp, Ink, shape)
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Sun,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    GameButton(text = text, onClick = onClick, modifier = modifier, color = color, enabled = enabled, loading = loading)
}

@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    GameTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        icon = icon,
        modifier = modifier,
        isPassword = isPassword,
        keyboardType = keyboardType
    )
}
