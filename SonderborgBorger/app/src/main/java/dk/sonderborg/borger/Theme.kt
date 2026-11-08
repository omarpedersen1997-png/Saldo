package dk.sonderborg.borger

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Sea = Color(0xFF0B4F6C)
val SeaLight = Color(0xFF9FD8E8)
val Sand = Color(0xFFF4EFE6)
val Brick = Color(0xFFB5523B)

private val Light = lightColorScheme(
    primary = Sea,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8F2),
    onPrimaryContainer = Color(0xFF002331),
    secondary = Brick,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF3B0A00),
    background = Sand,
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE8E2D6),
    onSurfaceVariant = Color(0xFF4A473F),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0EADF),
    outline = Color(0xFF7B776D),
)

private val Dark = darkColorScheme(
    primary = SeaLight,
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF0B4F6C),
    onPrimaryContainer = Color(0xFFCDE8F2),
    secondary = Color(0xFFFFB4A0),
    onSecondary = Color(0xFF5F1600),
    secondaryContainer = Color(0xFF7E2B16),
    onSecondaryContainer = Color(0xFFFFDBD1),
    background = Color(0xFF111416),
    onBackground = Color(0xFFE2E2E0),
    surface = Color(0xFF15191B),
    onSurface = Color(0xFFE2E2E0),
    surfaceVariant = Color(0xFF2A3135),
    onSurfaceVariant = Color(0xFFC0C8CC),
    surfaceContainer = Color(0xFF1C2225),
    surfaceContainerHigh = Color(0xFF242B2F),
    outline = Color(0xFF8A9296),
)

@Composable
fun BorgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
