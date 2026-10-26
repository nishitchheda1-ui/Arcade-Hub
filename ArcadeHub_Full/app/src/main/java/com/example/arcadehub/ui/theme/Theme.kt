package com.example.arcadehub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Joyful, vibrant kid-friendly cartoon palette: deep comic-book indigo ink, warm marshmallow cream, juicy candy pops.
val Ink = Color(0xFF1B1446)            // deep comic book navy/indigo outline
val Bg = Color(0xFF261661)             // vibrant toy indigo/purple background
val BgDeep = Color(0xFF180D42)         // deep recesses / wells
val Board = Color(0xFF38257D)          // playful card/tile surface
val CardBg = Color(0xFF332070)         // primary tactile card surface
val CardBgElevated = Color(0xFF4A3496) // elevated chip / input surface
val Cream = Color(0xFFFFFDF5)          // fluffy marshmallow vanilla cream
val CreamDark = Color(0xFFF3EBCE)      // warm bevel for cream surfaces
val TextLight = Color(0xFFFFFFFF)      // readable crisp white text
val TextMuted = Color(0xFFC7BFE8)      // soft lavender secondary text

// Playful, cheerful candy colors loved by kids and younger audiences
val Sun = Color(0xFFFFC727)            // bright sunny honey yellow
val SunDark = Color(0xFFE5A400)        // 3D bottom bevel for yellow
val Coral = Color(0xFFFF4D6D)          // vibrant candy strawberry pink-red
val CoralDark = Color(0xFFD62249)      // 3D bottom bevel for coral
val Leaf = Color(0xFF22C55E)           // juicy mint / green apple
val LeafDark = Color(0xFF16A34A)       // 3D bottom bevel for leaf
val Sky = Color(0xFF2FD4F3)            // swimming pool cyan / bright sky blue
val SkyDark = Color(0xFF0EA5E9)        // 3D bottom bevel for sky
val Grape = Color(0xFFA259FF)          // magical toy purple / grape jelly
val GrapeDark = Color(0xFF7C2DE8)      // 3D bottom bevel for grape
val Tangerine = Color(0xFFFF8E3C)      // sweet juicy citrus orange
val TangerineDark = Color(0xFFE86910)  // 3D bottom bevel for tangerine
val Rose = Color(0xFFFF66C4)           // bubblegum pink
val RoseDark = Color(0xFFE042A2)       // 3D bottom bevel for rose
val Teal = Color(0xFF14D2B8)           // energetic tropical teal
val Banana = Color(0xFFFFE14D)         // pastel yellow
val Blueberry = Color(0xFF4D7CFF)      // bright royal blue

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
    tertiary = Sun,
    background = BgDeep,
    surface = CardBg,
    onPrimary = Ink,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun ArcadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
