package com.keavors.taskorganizer.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.keavors.taskorganizer.data.DesignStyle
import com.keavors.taskorganizer.data.Palette
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.TabColor
import com.keavors.taskorganizer.data.ThemeMode

/** Accent colors of a palette for one brightness. */
data class Accent(val primary: Color, val onPrimary: Color, val container: Color, val onContainer: Color)

private fun accent(light: Long, onLight: Long, lightContainer: Long, onLightContainer: Long) =
    Accent(Color(light), Color(onLight), Color(lightContainer), Color(onLightContainer))

fun paletteAccent(palette: Palette, dark: Boolean): Accent = when (palette) {
    Palette.INDIGO, Palette.WALLPAPER ->
        if (dark) accent(0xFFC2C1FF, 0xFF221C8A, 0xFF3B35B6, 0xFFE2E0FF) else accent(0xFF4F46E5, 0xFFFFFFFF, 0xFFE2E0FF, 0xFF16105C)
    Palette.BLUE ->
        if (dark) accent(0xFF93C5FD, 0xFF0B2A5B, 0xFF1E40AF, 0xFFDBEAFE) else accent(0xFF2563EB, 0xFFFFFFFF, 0xFFDBEAFE, 0xFF172554)
    Palette.TEAL ->
        if (dark) accent(0xFF5EEAD4, 0xFF00332E, 0xFF115E59, 0xFFCCFBF1) else accent(0xFF0D9488, 0xFFFFFFFF, 0xFFCCFBF1, 0xFF042F2E)
    Palette.GREEN ->
        if (dark) accent(0xFF86EFAC, 0xFF00391A, 0xFF166534, 0xFFDCFCE7) else accent(0xFF16A34A, 0xFFFFFFFF, 0xFFDCFCE7, 0xFF052E16)
    Palette.AMBER ->
        if (dark) accent(0xFFFCD34D, 0xFF3F2A00, 0xFF92400E, 0xFFFEF3C7) else accent(0xFFD97706, 0xFFFFFFFF, 0xFFFEF3C7, 0xFF451A03)
    Palette.RED ->
        if (dark) accent(0xFFFDA4AF, 0xFF5B0A1F, 0xFF9F1239, 0xFFFFE4E6) else accent(0xFFE11D48, 0xFFFFFFFF, 0xFFFFE4E6, 0xFF4C0519)
    Palette.PINK ->
        if (dark) accent(0xFFF9A8D4, 0xFF5A0B33, 0xFF9D174D, 0xFFFCE7F3) else accent(0xFFDB2777, 0xFFFFFFFF, 0xFFFCE7F3, 0xFF500724)
    Palette.VIOLET ->
        if (dark) accent(0xFFC4B5FD, 0xFF2E1065, 0xFF5B21B6, 0xFFEDE9FE) else accent(0xFF7C3AED, 0xFFFFFFFF, 0xFFEDE9FE, 0xFF2E1065)
    Palette.GRAPHITE ->
        if (dark) accent(0xFFE4E4E7, 0xFF18181B, 0xFF3F3F46, 0xFFF4F4F5) else accent(0xFF27272A, 0xFFFFFFFF, 0xFFE4E4E7, 0xFF18181B)
}

/** Background, surfaces, text and lines: what makes a style recognizable besides the accent. */
private data class Neutrals(
    val background: Color,
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val onSurface: Color,
    val onVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val variant: Color
)

private fun n(bg: Long, lowest: Long, low: Long, container: Long, high: Long, highest: Long, on: Long, onVariant: Long, outline: Long, outlineVariant: Long, variant: Long) =
    Neutrals(Color(bg), Color(lowest), Color(low), Color(container), Color(high), Color(highest), Color(on), Color(onVariant), Color(outline), Color(outlineVariant), Color(variant))

private val StandardLight = n(0xFFF7F7FB, 0xFFFFFFFF, 0xFFF2F1F8, 0xFFECEBF3, 0xFFE7E6EF, 0xFFE1E0E9, 0xFF1B1B21, 0xFF5B5966, 0xFF787680, 0xFFD5D2DD, 0xFFE4E1EC)
private val StandardDark = n(0xFF121318, 0xFF0D0E12, 0xFF1B1B21, 0xFF1F1F26, 0xFF292930, 0xFF34343B, 0xFFE4E1E9, 0xFFC8C5D0, 0xFF928F9A, 0xFF3A3942, 0xFF47464F)

private fun neutrals(style: DesignStyle, dark: Boolean, a: Accent): Neutrals = when (style) {
    DesignStyle.STANDARD, DesignStyle.COMPACT -> if (dark) StandardDark else StandardLight
    DesignStyle.MINIMAL ->
        if (dark) n(0xFF0F0F0F, 0xFF0F0F0F, 0xFF161616, 0xFF1C1C1C, 0xFF232323, 0xFF2C2C2C, 0xFFEDEDED, 0xFFA0A0A0, 0xFF6E6E6E, 0xFF2A2A2A, 0xFF262626)
        else n(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFAFAFA, 0xFFF5F5F5, 0xFFF0F0F0, 0xFFE8E8E8, 0xFF111111, 0xFF6B6B6B, 0xFFA3A3A3, 0xFFE5E5E5, 0xFFEFEFEF)
    DesignStyle.SOFT -> if (dark) {
        val base = Color(0xFF141318)
        Neutrals(
            background = lerp(base, a.container, 0.18f),
            lowest = lerp(Color(0xFF1C1B21), a.container, 0.26f),
            low = lerp(Color(0xFF1E1D23), a.container, 0.22f),
            container = lerp(Color(0xFF232228), a.container, 0.26f),
            high = lerp(Color(0xFF2A2930), a.container, 0.3f),
            highest = lerp(Color(0xFF34333A), a.container, 0.3f),
            onSurface = Color(0xFFE8E6EE), onVariant = Color(0xFFC4C1CC), outline = Color(0xFF8F8C98),
            outlineVariant = lerp(Color(0xFF34333B), a.container, 0.2f), variant = lerp(Color(0xFF3A3942), a.container, 0.2f)
        )
    } else {
        Neutrals(
            background = lerp(Color(0xFFF8F7FB), a.container, 0.45f),
            lowest = Color.White,
            low = lerp(Color.White, a.container, 0.25f),
            container = lerp(Color.White, a.container, 0.4f),
            high = lerp(Color.White, a.container, 0.6f),
            highest = lerp(Color.White, a.container, 0.75f),
            onSurface = Color(0xFF1C1B20), onVariant = Color(0xFF5E5C66), outline = Color(0xFF8A8794),
            outlineVariant = lerp(Color(0xFFE2E1E8), a.container, 0.3f), variant = lerp(Color(0xFFE7E5EE), a.container, 0.3f)
        )
    }
    DesignStyle.SHARP ->
        if (dark) n(0xFF09090B, 0xFF18181B, 0xFF18181B, 0xFF1F1F23, 0xFF27272A, 0xFF3F3F46, 0xFFFAFAFA, 0xFFA1A1AA, 0xFF71717A, 0xFF3F3F46, 0xFF27272A)
        else n(0xFFF4F4F5, 0xFFFFFFFF, 0xFFFAFAFA, 0xFFF4F4F5, 0xFFE4E4E7, 0xFFD4D4D8, 0xFF09090B, 0xFF52525B, 0xFFA1A1AA, 0xFFD4D4D8, 0xFFE4E4E7)
    DesignStyle.TERMINAL ->
        if (dark) n(0xFF0A0F0A, 0xFF0A0F0A, 0xFF0F160F, 0xFF131B13, 0xFF182218, 0xFF1F2B1F, 0xFFD7E3D7, 0xFF8FAF8F, 0xFF3F6B3F, 0xFF1E3A1E, 0xFF142014)
        else n(0xFFF7F7F2, 0xFFFFFFFF, 0xFFF0F0EA, 0xFFEAEAE3, 0xFFE2E2DA, 0xFFD8D8CF, 0xFF1A1F1A, 0xFF4D574D, 0xFF8A948A, 0xFFCDD3CD, 0xFFE2E6E2)
    DesignStyle.PAPER ->
        if (dark) n(0xFF1F1B16, 0xFF1A1712, 0xFF25211B, 0xFF2A251F, 0xFF332D26, 0xFF3D362E, 0xFFEDE3D1, 0xFFBFB29C, 0xFF8A7E6A, 0xFF3F382F, 0xFF3A332B)
        else n(0xFFF6F0E1, 0xFFFBF7EC, 0xFFF3ECDB, 0xFFEEE6D2, 0xFFE8DFC8, 0xFFE0D5BC, 0xFF2E2A24, 0xFF6B6253, 0xFFA0947D, 0xFFDCCFB3, 0xFFE8DEC8)
    DesignStyle.NEON ->
        if (dark) n(0xFF000000, 0xFF050507, 0xFF0B0B10, 0xFF101016, 0xFF16161E, 0xFF1E1E28, 0xFFF2F2FF, 0xFFA6A6C0, 0xFF5A5A78, 0xFF26263A, 0xFF1A1A26)
        else n(0xFFFFFFFF, 0xFFFFFFFF, 0xFFF7F7FB, 0xFFF1F1F7, 0xFFEAEAF2, 0xFFE1E1EC, 0xFF0B0B12, 0xFF555566, 0xFF8B8BA0, 0xFFDCDCE8, 0xFFECECF4)
}

private val Black = Neutrals(
    background = Color.Black, lowest = Color.Black, low = Color(0xFF0A0A0A), container = Color(0xFF111111),
    high = Color(0xFF1A1A1A), highest = Color(0xFF242424),
    onSurface = Color(0xFFEDEDED), onVariant = Color(0xFFA8A8A8), outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2A2A2A), variant = Color(0xFF1E1E1E)
)

/** Neon needs vivid accents even in the dark theme. */
private fun neonDark(light: Accent) = Accent(
    primary = lerp(light.primary, Color.White, 0.12f),
    onPrimary = Color.White,
    container = lerp(Color.Black, light.primary, 0.35f),
    onContainer = Color.White
)

private fun scheme(dark: Boolean, a: Accent, n: Neutrals): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = a.primary,
        onPrimary = a.onPrimary,
        primaryContainer = a.container,
        onPrimaryContainer = a.onContainer,
        inversePrimary = a.container,
        secondary = a.primary,
        onSecondary = a.onPrimary,
        secondaryContainer = lerp(n.high, a.container, 0.6f),
        onSecondaryContainer = n.onSurface,
        tertiary = a.primary,
        onTertiary = a.onPrimary,
        tertiaryContainer = a.container,
        onTertiaryContainer = a.onContainer,
        background = n.background,
        onBackground = n.onSurface,
        surface = n.background,
        onSurface = n.onSurface,
        surfaceVariant = n.variant,
        onSurfaceVariant = n.onVariant,
        surfaceTint = a.primary,
        inverseSurface = n.onSurface,
        inverseOnSurface = n.background,
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFD32F2F),
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = if (dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002),
        outline = n.outline,
        outlineVariant = n.outlineVariant,
        surfaceBright = n.highest,
        surfaceDim = n.background,
        surfaceContainerLowest = n.lowest,
        surfaceContainerLow = n.low,
        surfaceContainer = n.container,
        surfaceContainerHigh = n.high,
        surfaceContainerHighest = n.highest
    )
}

private fun colorScheme(context: Context, style: DesignStyle, palette: Palette, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val wallpaper = palette == Palette.WALLPAPER && Build.VERSION.SDK_INT >= 31
    val dynamic = if (wallpaper) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)) else null
    var a = dynamic?.let { Accent(it.primary, it.onPrimary, it.primaryContainer, it.onPrimaryContainer) } ?: paletteAccent(palette, dark)
    if (style == DesignStyle.NEON && dark) a = neonDark(dynamic?.let { Accent(it.primary, it.onPrimary, it.primaryContainer, it.onPrimaryContainer) } ?: paletteAccent(palette, false))
    val black = dark && pureBlack
    // The standard look keeps the whole Material You scheme, wallpaper tints included.
    if (dynamic != null && !black && (style == DesignStyle.STANDARD || style == DesignStyle.COMPACT)) return dynamic
    return scheme(dark, a, if (black) Black else neutrals(style, dark, a))
}

private fun TextStyle.adapt(family: FontFamily, scale: Float, weight: FontWeight? = null): TextStyle = copy(
    fontFamily = family,
    fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
    fontWeight = weight ?: fontWeight
)

private fun typography(style: DesignStyle): Typography {
    val base = Typography()
    val family = when (style) {
        DesignStyle.TERMINAL -> FontFamily.Monospace
        DesignStyle.PAPER -> FontFamily.Serif
        DesignStyle.MINIMAL, DesignStyle.SHARP -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
    val scale = if (style == DesignStyle.COMPACT) 0.92f else 1f
    val heading = when (style) {
        DesignStyle.MINIMAL -> FontWeight.Light
        DesignStyle.SOFT, DesignStyle.SHARP -> FontWeight.Bold
        DesignStyle.NEON -> FontWeight.ExtraBold
        else -> null
    }
    return base.copy(
        displayLarge = base.displayLarge.adapt(family, scale, heading),
        displayMedium = base.displayMedium.adapt(family, scale, heading),
        displaySmall = base.displaySmall.adapt(family, scale, heading),
        headlineLarge = base.headlineLarge.adapt(family, scale, heading),
        headlineMedium = base.headlineMedium.adapt(family, scale, heading),
        headlineSmall = base.headlineSmall.adapt(family, scale, heading),
        titleLarge = base.titleLarge.adapt(family, scale),
        titleMedium = base.titleMedium.adapt(family, scale),
        titleSmall = base.titleSmall.adapt(family, scale),
        bodyLarge = base.bodyLarge.adapt(family, scale),
        bodyMedium = base.bodyMedium.adapt(family, scale),
        bodySmall = base.bodySmall.adapt(family, scale),
        labelLarge = base.labelLarge.adapt(family, scale),
        labelMedium = base.labelMedium.adapt(family, scale),
        labelSmall = base.labelSmall.adapt(family, scale)
    )
}

private fun corners(xs: Int, s: Int, m: Int, l: Int, xl: Int) = Shapes(
    extraSmall = RoundedCornerShape(xs.dp),
    small = RoundedCornerShape(s.dp),
    medium = RoundedCornerShape(m.dp),
    large = RoundedCornerShape(l.dp),
    extraLarge = RoundedCornerShape(xl.dp)
)

private fun shapes(style: DesignStyle): Shapes = when (style) {
    DesignStyle.STANDARD -> corners(8, 12, 16, 24, 32)
    DesignStyle.MINIMAL -> corners(4, 6, 8, 12, 16)
    DesignStyle.SOFT -> corners(12, 18, 24, 28, 36)
    DesignStyle.SHARP -> corners(0, 2, 2, 4, 4)
    DesignStyle.TERMINAL -> corners(0, 0, 0, 0, 0)
    DesignStyle.PAPER -> corners(2, 4, 6, 8, 12)
    DesignStyle.COMPACT -> corners(6, 8, 10, 16, 24)
    DesignStyle.NEON -> corners(8, 12, 14, 20, 28)
}

/** Gray, yellow, red. */
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.NORMAL -> Color(0xFF9CA3AF)
    Priority.MEDIUM -> Color(0xFFEAB308)
    Priority.HIGH -> Color(0xFFEF4444)
}

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

@Composable
fun isDarkTheme(settings: Settings): Boolean = when (settings.theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TaskOrganizerTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val context = LocalContext.current
    val colors = remember(settings.style, settings.palette, settings.pureBlack, dark) {
        colorScheme(context, settings.style, settings.palette, dark, settings.pureBlack)
    }
    val type = remember(settings.style) { typography(settings.style) }
    val shapes = remember(settings.style) { shapes(settings.style) }
    val design = remember(settings.style) { designOf(settings.style) }
    CompositionLocalProvider(LocalDesign provides design) {
        MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
    }
}
