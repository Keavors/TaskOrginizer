package com.keavors.taskorganizer.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.DesignStyle
import com.keavors.taskorganizer.data.Palette

/** How an entry is drawn. */
enum class ItemLook { CARD, ELEVATED, OUTLINED, GLOW, FLAT, RULED, CODE }

/** The "done" mark: circle, square, [x], hand-drawn box, cut-corner box. */
enum class CheckLook { CIRCLE, SQUARE, BRACKETS, INK, CUT }

enum class TabsLook { PILLS, TONAL, UNDERLINE, BOXES, BRACKETS, GLOW, BOOKMARKS }

/** The input field at the bottom. */
enum class FieldLook { PILL, LINE, BOX, PROMPT, GLOW }

/** Buttons: filled pill, plain text, tonal, square box, [ text ], ink underline, glowing cut corners. */
enum class ButtonLook { PILL, TEXT, TONAL, SQUARE, BRACKETS, INK, GLOW }

enum class SwitchLook { MATERIAL, SLIM, CHUNKY, SQUARE, TEXT, INK, GLOW }

/** The microphone button. */
enum class MicLook { FILLED, OUTLINED, GRADIENT, SQUARE, REC, STAMP, GLOW }

/** How importance is shown on an entry. */
enum class FlagLook { FLAG, DOT, BANG, PIN, BOLT }

enum class BackgroundLook { PLAIN, SOFT_GRADIENT, PAPER, SCANLINES, NEON }

/** Groups of settings. */
enum class SectionLook { CARD, FLAT, SOFT, BOXED, CODE, PAPER, GLOW }

/** The bar with the app name, search and menu. */
enum class TopBarLook { STANDARD, MINIMAL, SOFT, STRICT, TERMINAL, PAPER, COMPACT, NEON }

enum class HeaderSize { TITLE, HEADLINE, LARGE }

@Immutable
data class HeaderLook(
    val size: HeaderSize = HeaderSize.HEADLINE,
    val weight: FontWeight = FontWeight.SemiBold,
    val uppercase: Boolean = false,
    val italic: Boolean = false,
    val prefix: String = "",
    val accent: Boolean = false
)

/** Everything a design style changes besides the Material colors, fonts and corner radii. */
@Immutable
data class AppDesign(
    val style: DesignStyle,
    val icons: IconSet,
    val iconSize: Dp,
    val item: ItemLook,
    val itemShape: Shape,
    val itemGap: Dp,
    val itemPadding: Dp,
    val check: CheckLook,
    val checkSize: Dp,
    val checkShape: Shape,
    val checkBorder: Dp,
    val tabs: TabsLook,
    val tabPaddingH: Dp,
    val tabPaddingV: Dp,
    val uppercaseLabels: Boolean,
    val field: FieldLook,
    val fieldShape: Shape,
    val fieldHeight: Dp,
    val button: ButtonLook,
    val buttonShape: CornerBasedShape,
    val chipShape: Shape,
    val switch: SwitchLook,
    val mic: MicLook,
    val micShape: Shape,
    val micSize: Dp,
    val flag: FlagLook,
    val background: BackgroundLook,
    val section: SectionLook,
    val topBar: TopBarLook,
    val glow: Boolean,
    val header: HeaderLook
)

val LocalDesign = staticCompositionLocalOf { designOf(DesignStyle.STANDARD) }

private val Pill = RoundedCornerShape(50)
private val Square = RoundedCornerShape(0.dp)

fun designOf(style: DesignStyle): AppDesign = when (style) {
    // Material 3 as Google draws it: system font, filled icons, pills and cards.
    DesignStyle.STANDARD -> AppDesign(
        style = style, icons = iconSet(IconFamily.FILLED), iconSize = 24.dp,
        item = ItemLook.CARD, itemShape = RoundedCornerShape(16.dp), itemGap = 8.dp, itemPadding = 8.dp,
        check = CheckLook.CIRCLE, checkSize = 22.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.PILLS, tabPaddingH = 16.dp, tabPaddingV = 9.dp, uppercaseLabels = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(28.dp), fieldHeight = 56.dp,
        button = ButtonLook.PILL, buttonShape = Pill, chipShape = RoundedCornerShape(8.dp),
        switch = SwitchLook.MATERIAL, mic = MicLook.FILLED, micShape = CircleShape, micSize = 56.dp,
        flag = FlagLook.FLAG, background = BackgroundLook.PLAIN, section = SectionLook.CARD, topBar = TopBarLook.STANDARD,
        glow = false, header = HeaderLook(HeaderSize.HEADLINE, FontWeight.SemiBold)
    )
    // Thin font, outlined icons, no cards, hairlines and plain text buttons.
    DesignStyle.MINIMAL -> AppDesign(
        style = style, icons = iconSet(IconFamily.OUTLINED), iconSize = 22.dp,
        item = ItemLook.FLAT, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 12.dp,
        check = CheckLook.SQUARE, checkSize = 18.dp, checkShape = RoundedCornerShape(3.dp), checkBorder = 1.dp,
        tabs = TabsLook.UNDERLINE, tabPaddingH = 6.dp, tabPaddingV = 8.dp, uppercaseLabels = false,
        field = FieldLook.LINE, fieldShape = RectangleShape, fieldHeight = 52.dp,
        button = ButtonLook.TEXT, buttonShape = RoundedCornerShape(4.dp), chipShape = RoundedCornerShape(4.dp),
        switch = SwitchLook.SLIM, mic = MicLook.OUTLINED, micShape = CircleShape, micSize = 48.dp,
        flag = FlagLook.DOT, background = BackgroundLook.PLAIN, section = SectionLook.FLAT, topBar = TopBarLook.MINIMAL,
        glow = false, header = HeaderLook(HeaderSize.LARGE, FontWeight.Light)
    )
    // Rounded font, big soft cards with shadows, tonal pills, a gradient background.
    DesignStyle.SOFT -> AppDesign(
        style = style, icons = iconSet(IconFamily.ROUNDED), iconSize = 26.dp,
        item = ItemLook.ELEVATED, itemShape = RoundedCornerShape(28.dp), itemGap = 12.dp, itemPadding = 14.dp,
        check = CheckLook.CIRCLE, checkSize = 26.dp, checkShape = CircleShape, checkBorder = 2.5.dp,
        tabs = TabsLook.TONAL, tabPaddingH = 20.dp, tabPaddingV = 11.dp, uppercaseLabels = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(32.dp), fieldHeight = 60.dp,
        button = ButtonLook.TONAL, buttonShape = Pill, chipShape = RoundedCornerShape(50),
        switch = SwitchLook.CHUNKY, mic = MicLook.GRADIENT, micShape = CircleShape, micSize = 64.dp,
        flag = FlagLook.FLAG, background = BackgroundLook.SOFT_GRADIENT, section = SectionLook.SOFT, topBar = TopBarLook.SOFT,
        glow = false, header = HeaderLook(HeaderSize.HEADLINE, FontWeight.ExtraBold)
    )
    // Condensed capitals, sharp icons, square everything, black and white boxes.
    DesignStyle.SHARP -> AppDesign(
        style = style, icons = iconSet(IconFamily.SHARP), iconSize = 22.dp,
        item = ItemLook.OUTLINED, itemShape = Square, itemGap = 6.dp, itemPadding = 8.dp,
        check = CheckLook.SQUARE, checkSize = 20.dp, checkShape = RectangleShape, checkBorder = 2.dp,
        tabs = TabsLook.BOXES, tabPaddingH = 14.dp, tabPaddingV = 8.dp, uppercaseLabels = true,
        field = FieldLook.BOX, fieldShape = Square, fieldHeight = 52.dp,
        button = ButtonLook.SQUARE, buttonShape = Square, chipShape = Square,
        switch = SwitchLook.SQUARE, mic = MicLook.SQUARE, micShape = Square, micSize = 52.dp,
        flag = FlagLook.FLAG, background = BackgroundLook.PLAIN, section = SectionLook.BOXED, topBar = TopBarLook.STRICT,
        glow = false, header = HeaderLook(HeaderSize.HEADLINE, FontWeight.SemiBold, uppercase = true)
    )
    // Monospace text, brackets instead of controls, a command prompt.
    DesignStyle.TERMINAL -> AppDesign(
        style = style, icons = iconSet(IconFamily.SHARP), iconSize = 20.dp,
        item = ItemLook.CODE, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 6.dp,
        check = CheckLook.BRACKETS, checkSize = 22.dp, checkShape = RectangleShape, checkBorder = 0.dp,
        tabs = TabsLook.BRACKETS, tabPaddingH = 2.dp, tabPaddingV = 8.dp, uppercaseLabels = false,
        field = FieldLook.PROMPT, fieldShape = RectangleShape, fieldHeight = 48.dp,
        button = ButtonLook.BRACKETS, buttonShape = Square, chipShape = Square,
        switch = SwitchLook.TEXT, mic = MicLook.REC, micShape = RectangleShape, micSize = 48.dp,
        flag = FlagLook.BANG, background = BackgroundLook.SCANLINES, section = SectionLook.CODE, topBar = TopBarLook.TERMINAL,
        glow = false, header = HeaderLook(HeaderSize.TITLE, FontWeight.Bold, prefix = "# ")
    )
    // A notebook: handwriting, ruled lines, a red margin, bookmark tabs, ink.
    DesignStyle.PAPER -> AppDesign(
        style = style, icons = iconSet(IconFamily.TWOTONE), iconSize = 24.dp,
        item = ItemLook.RULED, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 8.dp,
        check = CheckLook.INK, checkSize = 22.dp, checkShape = RoundedCornerShape(2.dp), checkBorder = 1.5.dp,
        tabs = TabsLook.BOOKMARKS, tabPaddingH = 14.dp, tabPaddingV = 6.dp, uppercaseLabels = false,
        field = FieldLook.LINE, fieldShape = RectangleShape, fieldHeight = 52.dp,
        button = ButtonLook.INK, buttonShape = RoundedCornerShape(2.dp), chipShape = RoundedCornerShape(50),
        switch = SwitchLook.INK, mic = MicLook.STAMP, micShape = CircleShape, micSize = 54.dp,
        flag = FlagLook.PIN, background = BackgroundLook.PAPER, section = SectionLook.PAPER, topBar = TopBarLook.PAPER,
        glow = false, header = HeaderLook(HeaderSize.LARGE, FontWeight.Bold)
    )
    // Narrow font, small everything: many entries on one screen.
    DesignStyle.COMPACT -> AppDesign(
        style = style, icons = iconSet(IconFamily.FILLED), iconSize = 20.dp,
        item = ItemLook.CARD, itemShape = RoundedCornerShape(8.dp), itemGap = 4.dp, itemPadding = 2.dp,
        check = CheckLook.CIRCLE, checkSize = 18.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.PILLS, tabPaddingH = 12.dp, tabPaddingV = 6.dp, uppercaseLabels = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(10.dp), fieldHeight = 46.dp,
        button = ButtonLook.PILL, buttonShape = RoundedCornerShape(8.dp), chipShape = RoundedCornerShape(6.dp),
        switch = SwitchLook.MATERIAL, mic = MicLook.FILLED, micShape = RoundedCornerShape(12.dp), micSize = 46.dp,
        flag = FlagLook.DOT, background = BackgroundLook.PLAIN, section = SectionLook.CARD, topBar = TopBarLook.COMPACT,
        glow = false, header = HeaderLook(HeaderSize.TITLE, FontWeight.SemiBold)
    )
    // Black, glowing outlines, cut corners, a futuristic font.
    DesignStyle.NEON -> AppDesign(
        style = style, icons = iconSet(IconFamily.OUTLINED), iconSize = 24.dp,
        item = ItemLook.GLOW, itemShape = CutCornerShape(12.dp), itemGap = 10.dp, itemPadding = 10.dp,
        check = CheckLook.CUT, checkSize = 22.dp, checkShape = CutCornerShape(5.dp), checkBorder = 1.5.dp,
        tabs = TabsLook.GLOW, tabPaddingH = 16.dp, tabPaddingV = 9.dp, uppercaseLabels = true,
        field = FieldLook.GLOW, fieldShape = CutCornerShape(14.dp), fieldHeight = 56.dp,
        button = ButtonLook.GLOW, buttonShape = CutCornerShape(10.dp), chipShape = CutCornerShape(8.dp),
        switch = SwitchLook.GLOW, mic = MicLook.GLOW, micShape = CutCornerShape(14.dp), micSize = 56.dp,
        flag = FlagLook.BOLT, background = BackgroundLook.NEON, section = SectionLook.GLOW, topBar = TopBarLook.NEON,
        glow = true, header = HeaderLook(HeaderSize.HEADLINE, FontWeight.Black, uppercase = true, accent = true)
    )
}

fun styleName(style: DesignStyle): String = when (style) {
    DesignStyle.STANDARD -> "Стандарт"
    DesignStyle.MINIMAL -> "Минимализм"
    DesignStyle.SOFT -> "Мягкий"
    DesignStyle.SHARP -> "Строгий"
    DesignStyle.TERMINAL -> "Терминал"
    DesignStyle.PAPER -> "Блокнот"
    DesignStyle.COMPACT -> "Компактный"
    DesignStyle.NEON -> "Неон"
}

fun paletteName(palette: Palette): String = when (palette) {
    Palette.INDIGO -> "Индиго"
    Palette.BLUE -> "Синий"
    Palette.TEAL -> "Бирюзовый"
    Palette.GREEN -> "Зелёный"
    Palette.AMBER -> "Янтарный"
    Palette.RED -> "Красный"
    Palette.PINK -> "Розовый"
    Palette.VIOLET -> "Фиолетовый"
    Palette.GRAPHITE -> "Графит"
    Palette.WALLPAPER -> "Из обоев"
}
