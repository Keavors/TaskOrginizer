package com.keavors.taskorganizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.TabColor
import com.keavors.taskorganizer.data.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E0FF),
    onPrimaryContainer = Color(0xFF16105C),
    secondary = Color(0xFF5C5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E1F9),
    onSecondaryContainer = Color(0xFF191A2C),
    tertiary = Color(0xFF7B5263),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF7F7FB),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF5B5966),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F1F8),
    surfaceContainer = Color(0xFFECEBF3),
    surfaceContainerHigh = Color(0xFFE7E6EF),
    surfaceContainerHighest = Color(0xFFE1E0E9),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFD5D2DD),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC2C1FF),
    onPrimary = Color(0xFF221C8A),
    primaryContainer = Color(0xFF3B35B6),
    onPrimaryContainer = Color(0xFFE2E0FF),
    secondary = Color(0xFFC6C4DD),
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = Color(0xFF3B3C51),
    onSecondaryContainer = Color(0xFFE3E1F9),
    tertiary = Color(0xFFEBB8CB),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF0D0E12),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F26),
    surfaceContainerHigh = Color(0xFF292930),
    surfaceContainerHighest = Color(0xFF34343B),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF3A3942),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/** Star of important entries. */
val ImportantColor = Color(0xFFF5A524)

fun tabColor(color: TabColor): Color = when (color) {
    TabColor.BLUE -> Color(0xFF3B82F6)
    TabColor.VIOLET -> Color(0xFF8B5CF6)
    TabColor.PINK -> Color(0xFFEC4899)
    TabColor.RED -> Color(0xFFEF4444)
    TabColor.ORANGE -> Color(0xFFF97316)
    TabColor.AMBER -> Color(0xFFF59E0B)
    TabColor.GREEN -> Color(0xFF22C55E)
    TabColor.TEAL -> Color(0xFF14B8A6)
    TabColor.GRAY -> Color(0xFF94A3B8)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun isDarkTheme(settings: Settings): Boolean = when (settings.theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TaskOrganizerTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, shapes = AppShapes, content = content)
}
