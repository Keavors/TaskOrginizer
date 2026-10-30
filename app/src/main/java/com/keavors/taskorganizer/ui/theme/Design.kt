package com.keavors.taskorganizer.ui.theme

import androidx.compose.foundation.shape.CircleShape
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
enum class ItemLook { CARD, ELEVATED, OUTLINED, GLOW, FLAT, RULED }

enum class CheckLook { CIRCLE, SQUARE, BRACKETS }

enum class TabsLook { PILLS, TONAL, UNDERLINE, BOXES, BRACKETS, GLOW }

/** The input field at the bottom. */
enum class FieldLook { PILL, LINE, BOX, PROMPT, GLOW }

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

/** Everything a design style changes besides colors, fonts and corner radii of Material components. */
@Immutable
data class AppDesign(
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
    val uppercaseTabs: Boolean,
    val field: FieldLook,
    val fieldShape: Shape,
    val fieldHeight: Dp,
    val micShape: Shape,
    val micSize: Dp,
    val glow: Boolean,
    val header: HeaderLook
)

val LocalDesign = staticCompositionLocalOf { designOf(DesignStyle.STANDARD) }

fun designOf(style: DesignStyle): AppDesign = when (style) {
    DesignStyle.STANDARD -> AppDesign(
        item = ItemLook.CARD, itemShape = RoundedCornerShape(16.dp), itemGap = 8.dp, itemPadding = 8.dp,
        check = CheckLook.CIRCLE, checkSize = 22.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.PILLS, tabPaddingH = 16.dp, tabPaddingV = 9.dp, uppercaseTabs = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(28.dp), fieldHeight = 56.dp,
        micShape = CircleShape, micSize = 56.dp, glow = false,
        header = HeaderLook(HeaderSize.HEADLINE, FontWeight.SemiBold)
    )
    DesignStyle.MINIMAL -> AppDesign(
        item = ItemLook.FLAT, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 12.dp,
        check = CheckLook.SQUARE, checkSize = 20.dp, checkShape = RoundedCornerShape(4.dp), checkBorder = 1.5.dp,
        tabs = TabsLook.UNDERLINE, tabPaddingH = 6.dp, tabPaddingV = 8.dp, uppercaseTabs = false,
        field = FieldLook.LINE, fieldShape = RectangleShape, fieldHeight = 52.dp,
        micShape = CircleShape, micSize = 52.dp, glow = false,
        header = HeaderLook(HeaderSize.LARGE, FontWeight.Light)
    )
    DesignStyle.SOFT -> AppDesign(
        item = ItemLook.ELEVATED, itemShape = RoundedCornerShape(24.dp), itemGap = 12.dp, itemPadding = 12.dp,
        check = CheckLook.CIRCLE, checkSize = 24.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.TONAL, tabPaddingH = 18.dp, tabPaddingV = 10.dp, uppercaseTabs = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(28.dp), fieldHeight = 58.dp,
        micShape = CircleShape, micSize = 58.dp, glow = false,
        header = HeaderLook(HeaderSize.HEADLINE, FontWeight.Bold)
    )
    DesignStyle.SHARP -> AppDesign(
        item = ItemLook.OUTLINED, itemShape = RoundedCornerShape(2.dp), itemGap = 6.dp, itemPadding = 8.dp,
        check = CheckLook.SQUARE, checkSize = 20.dp, checkShape = RectangleShape, checkBorder = 2.dp,
        tabs = TabsLook.BOXES, tabPaddingH = 14.dp, tabPaddingV = 8.dp, uppercaseTabs = true,
        field = FieldLook.BOX, fieldShape = RoundedCornerShape(2.dp), fieldHeight = 52.dp,
        micShape = RoundedCornerShape(2.dp), micSize = 52.dp, glow = false,
        header = HeaderLook(HeaderSize.TITLE, FontWeight.Bold, uppercase = true)
    )
    DesignStyle.TERMINAL -> AppDesign(
        item = ItemLook.FLAT, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 8.dp,
        check = CheckLook.BRACKETS, checkSize = 22.dp, checkShape = RectangleShape, checkBorder = 0.dp,
        tabs = TabsLook.BRACKETS, tabPaddingH = 2.dp, tabPaddingV = 8.dp, uppercaseTabs = false,
        field = FieldLook.PROMPT, fieldShape = RectangleShape, fieldHeight = 52.dp,
        micShape = RectangleShape, micSize = 52.dp, glow = false,
        header = HeaderLook(HeaderSize.TITLE, FontWeight.Bold, prefix = "# ")
    )
    DesignStyle.PAPER -> AppDesign(
        item = ItemLook.RULED, itemShape = RectangleShape, itemGap = 0.dp, itemPadding = 10.dp,
        check = CheckLook.SQUARE, checkSize = 20.dp, checkShape = RoundedCornerShape(3.dp), checkBorder = 1.5.dp,
        tabs = TabsLook.UNDERLINE, tabPaddingH = 6.dp, tabPaddingV = 8.dp, uppercaseTabs = false,
        field = FieldLook.LINE, fieldShape = RectangleShape, fieldHeight = 52.dp,
        micShape = CircleShape, micSize = 52.dp, glow = false,
        header = HeaderLook(HeaderSize.HEADLINE, FontWeight.Normal, italic = true)
    )
    DesignStyle.COMPACT -> AppDesign(
        item = ItemLook.CARD, itemShape = RoundedCornerShape(10.dp), itemGap = 4.dp, itemPadding = 4.dp,
        check = CheckLook.CIRCLE, checkSize = 18.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.PILLS, tabPaddingH = 12.dp, tabPaddingV = 6.dp, uppercaseTabs = false,
        field = FieldLook.PILL, fieldShape = RoundedCornerShape(24.dp), fieldHeight = 48.dp,
        micShape = CircleShape, micSize = 48.dp, glow = false,
        header = HeaderLook(HeaderSize.TITLE, FontWeight.SemiBold)
    )
    DesignStyle.NEON -> AppDesign(
        item = ItemLook.GLOW, itemShape = RoundedCornerShape(14.dp), itemGap = 10.dp, itemPadding = 10.dp,
        check = CheckLook.CIRCLE, checkSize = 22.dp, checkShape = CircleShape, checkBorder = 2.dp,
        tabs = TabsLook.GLOW, tabPaddingH = 16.dp, tabPaddingV = 9.dp, uppercaseTabs = false,
        field = FieldLook.GLOW, fieldShape = RoundedCornerShape(28.dp), fieldHeight = 56.dp,
        micShape = CircleShape, micSize = 56.dp, glow = true,
        header = HeaderLook(HeaderSize.HEADLINE, FontWeight.ExtraBold, accent = true)
    )
}

fun styleName(style: DesignStyle): String = when (style) {
    DesignStyle.STANDARD -> "Стандарт"
    DesignStyle.MINIMAL -> "Минимализм"
    DesignStyle.SOFT -> "Мягкий"
    DesignStyle.SHARP -> "Строгий"
    DesignStyle.TERMINAL -> "Терминал"
    DesignStyle.PAPER -> "Бумага"
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
