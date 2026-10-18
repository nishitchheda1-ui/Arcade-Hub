package com.example.arcadehub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, tactile "Offline Games" casual palette: rich dark blue slate base, clean cream boards, candy accents.
val Ink = Color(0xFF0F172A)            // deep slate outline / dark text
val Bg = Color(0xFF1E293B)             // modern dark slate background
val BgDeep = Color(0xFF0F172A)         // deep wells / recesses
val Board = Color(0xFF27354F)          // card / tile surface
val CardBg = Color(0xFF222F46)         // primary tactile card surface
val CardBgElevated = Color(0xFF2C3C59) // elevated chip / input surface
val Cream = Color(0xFFF8FAFC)          // tactile game board / light cards
val TextLight = Color(0xFFF1F5F9)      // primary readable light text
val TextMuted = Color(0xFF94A3B8)      // secondary muted text

// Playful, friendly game colors
val Sun = Color(0xFFF59E0B)            // warm golden yellow
val Coral = Color(0xFFF43F5E)          // vibrant coral / pink-red
val Leaf = Color(0xFF10B981)           // fresh mint / emerald green
val Sky = Color(0xFF38BDF8)            // azure / sky blue
val Grape = Color(0xFF8B5CF6)          // playful violet / purple
val Tangerine = Color(0xFFFB923C)      // warm tangerine orange
val Rose = Color(0xFFF472B6)           // playful rose pink
val Teal = Color(0xFF14B8A6)           // energetic teal

// Compatibility aliases for all existing game views
val NeonCyan = Sky
val NeonPink = Coral
val NeonLime = Leaf
val NeonOrange = Tangerine
val NeonPurple = Grape
val NeonBg = BgDeep
val NeonSurface = CardBg

private val Scheme = darkColorScheme(
    primary = Sky,
    secondary = Coral,
    tertiary = Leaf,
    background = BgDeep,
    surface = CardBg,
    onPrimary = Color.White,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun ArcadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
