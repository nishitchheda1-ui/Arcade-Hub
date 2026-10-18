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

/** Solid blue background with a soft dot pattern. */
@Composable
fun ArcadeBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Bg).drawBehind {
            val step = 52.dp.toPx()
            var y = 0f; var row = 0
            while (y < size.height + step) {
                var x = if (row % 2 == 0) 0f else step / 2f
                while (x < size.width + step) { drawCircle(Color.White.copy(alpha = 0.06f), 5.dp.toPx(), Offset(x, y)); x += step }
                y += step / 2f; row++
            }
        },
        content = content
    )
}

/** Dark rounded frame drawn on top of a game canvas. */
fun DrawScope.drawFrame() {
    drawRoundRect(Ink, Offset(6f, 6f), Size(size.width - 12f, size.height - 12f), CornerRadius(28f), style = Stroke(12f))
}

/** Chunky button with a hard shadow that presses down. */
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
    val shape = RoundedCornerShape(16.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 0.dp else 5.dp, label = "lift")
    Box(modifier.height(58.dp).alpha(if (enabled) 1f else 0.5f)) {
        Box(Modifier.fillMaxSize().padding(top = 5.dp).background(Ink, shape))
        Box(
            Modifier.fillMaxSize().padding(top = 5.dp - lift, bottom = lift)
                .clip(shape).background(color).border(3.dp, Ink, shape)
                .clickable(interactionSource = src, indication = null, enabled = enabled && !loading, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = Ink, strokeWidth = 3.dp)
            else Text(text, color = Ink, fontWeight = FontWeight.Black, fontSize = fontSize, maxLines = 1)
        }
    }
}

@Composable
fun SquareButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Sun, desc: String? = null) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.size(50.dp)) {
        Box(Modifier.fillMaxSize().padding(top = 4.dp).background(Ink, shape))
        Box(
            Modifier.fillMaxSize().padding(bottom = 4.dp).clip(shape).background(color).border(3.dp, Ink, shape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, desc, tint = Ink) }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, color: Color = Sun, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape).background(if (selected) color else Board).border(3.dp, Ink, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp)
    ) { Text(text, color = if (selected) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1) }
}

@Composable
fun CreamCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier) {
        Box(Modifier.matchParentSize().padding(top = 6.dp).background(Ink, shape))
        Column(
            Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(shape).background(Cream).border(3.dp, Ink, shape).padding(16.dp),
            content = content
        )
    }
}

/** Title with a thick dark outline, like a cartoon logo. */
@Composable
fun OutlinedTitle(text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = Sun) {
    Box(modifier) {
        Text(
            text, letterSpacing = 1.sp,
            style = TextStyle(color = Ink, fontSize = size, fontWeight = FontWeight.Black, drawStyle = Stroke(width = size.value * 0.5f, join = StrokeJoin.Round))
        )
        Text(text, color = color, fontSize = size, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
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
    val shape = RoundedCornerShape(16.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = if (isPassword) {
            { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }
        } else null,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Cream, unfocusedContainerColor = Cream,
            focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Ink,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = Ink, unfocusedLabelColor = Ink.copy(alpha = 0.6f),
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
