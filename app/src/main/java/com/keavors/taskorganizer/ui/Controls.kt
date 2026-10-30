package com.keavors.taskorganizer.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.ui.theme.ButtonLook
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.SectionLook
import com.keavors.taskorganizer.ui.theme.SwitchLook
import com.keavors.taskorganizer.ui.theme.BackgroundLook

enum class ButtonKind { PRIMARY, NORMAL, DANGER }

/** Text in the label style of the current design (capitals where the design wants them). */
@Composable
fun labelText(text: String): String = if (LocalDesign.current.uppercaseLabels) text.uppercase() else text

/** A glow under text in the neon design; unchanged elsewhere. */
@Composable
fun TextStyle.glowing(color: Color = MaterialTheme.colorScheme.primary): TextStyle =
    if (LocalDesign.current.glow) copy(shadow = Shadow(color.copy(alpha = 0.9f), blurRadius = 22f)) else this

/** An icon of the current icon family; glows in the neon design. */
@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = LocalDesign.current.iconSize
) {
    if (LocalDesign.current.glow && Build.VERSION.SDK_INT >= 31 && size >= 18.dp) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Box(
                Modifier.requiredSize(size + 14.dp).graphicsLayer { renderEffect = BlurEffect(9f, 9f, TileMode.Decal) },
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, Modifier.size(size), tint = tint) }
            Icon(icon, contentDescription, Modifier.size(size), tint = tint)
        }
    } else {
        Icon(icon, contentDescription, modifier.size(size), tint = tint)
    }
}

/** A tappable icon: plain, in a tonal circle (soft), square (strict) or glowing (neon). */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 48.dp,
    onClick: () -> Unit
) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val shape: Shape = when (d.button) {
        ButtonLook.SQUARE, ButtonLook.BRACKETS -> RectangleShape
        ButtonLook.GLOW -> CutCornerShape(10.dp)
        else -> CircleShape
    }
    val fill = if (d.button == ButtonLook.TONAL) Modifier.padding(4.dp).clip(shape).background(c.primaryContainer.copy(alpha = 0.7f)) else Modifier.clip(shape)
    Box(
        modifier.size(size).then(fill).clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, contentDescription, tint = if (d.button == ButtonLook.TONAL) c.onPrimaryContainer else tint)
    }
}

/** A button drawn by the current design. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.NORMAL,
    enabled: Boolean = true
) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val accent = if (kind == ButtonKind.DANGER) c.error else c.primary
    val shape = d.buttonShape
    val primary = kind == ButtonKind.PRIMARY
    val label = labelText(text)
    val style = MaterialTheme.typography.labelLarge.let { if (primary) it.copy(fontWeight = FontWeight.Bold) else it }
    val base = modifier.defaultMinSize(minHeight = 40.dp).alpha(if (enabled) 1f else 0.38f)
    val click = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)

    @Composable
    fun Face(frame: Modifier, color: Color, shown: String = label, textStyle: TextStyle = style) {
        Box(base.then(frame).then(click).padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(shown, style = textStyle, color = color, maxLines = 1)
        }
    }

    when (d.button) {
        ButtonLook.PILL ->
            if (primary) Face(Modifier.clip(shape).background(c.primary), c.onPrimary) else Face(Modifier.clip(shape), accent)
        ButtonLook.TEXT ->
            Face(Modifier.clip(shape), if (kind == ButtonKind.NORMAL) c.onSurface else accent)
        ButtonLook.TONAL -> when (kind) {
            ButtonKind.PRIMARY -> Face(Modifier.shadow(3.dp, shape).clip(shape).background(c.primary), c.onPrimary)
            ButtonKind.NORMAL -> Face(Modifier.clip(shape).background(c.primaryContainer), c.onPrimaryContainer)
            ButtonKind.DANGER -> Face(Modifier.clip(shape).background(c.errorContainer), c.onErrorContainer)
        }
        ButtonLook.SQUARE -> {
            val line = if (kind == ButtonKind.DANGER) c.error else c.onSurface
            if (primary) Face(Modifier.clip(shape).background(c.inverseSurface), c.inverseOnSurface)
            else Face(Modifier.clip(shape).border(1.5.dp, line, shape), line)
        }
        ButtonLook.BRACKETS ->
            Face(Modifier, if (kind == ButtonKind.NORMAL) c.onSurface else accent, shown = "[ $label ]")
        ButtonLook.INK -> {
            val ink = if (kind == ButtonKind.NORMAL) c.onSurface else accent
            Face(
                Modifier.drawBehind {
                    val y = size.height - 8.dp.toPx()
                    drawLine(ink.copy(alpha = 0.6f), Offset(10.dp.toPx(), y), Offset(size.width - 10.dp.toPx(), y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                },
                ink
            )
        }
        ButtonLook.GLOW ->
            if (primary) Face(Modifier.shadow(12.dp, shape, ambientColor = accent, spotColor = accent).clip(shape).background(accent), c.onPrimary)
            else Face(Modifier.clip(shape).border(1.dp, accent.copy(alpha = 0.8f), shape), accent, textStyle = style.glowing(accent))
    }
}

/** On/off switch drawn by the current design. */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val toggle = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    when (d.switch) {
        SwitchLook.MATERIAL -> Switch(checked = checked, onCheckedChange = onCheckedChange)
        SwitchLook.TEXT -> Text(
            if (checked) "[ВКЛ ]" else "[ВЫКЛ]",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (checked) c.primary else c.onSurfaceVariant,
            modifier = toggle.padding(8.dp)
        )
        SwitchLook.INK -> Box(toggle.padding(8.dp)) { InkBox(checked, 22.dp) }
        else -> TrackSwitch(checked, d.switch, toggle)
    }
}

@Composable
private fun TrackSwitch(checked: Boolean, look: SwitchLook, toggle: Modifier) {
    val c = MaterialTheme.colorScheme
    class Spec(val w: Dp, val h: Dp, val thumb: Dp, val pad: Dp, val track: Shape, val knob: Shape)
    val s = when (look) {
        SwitchLook.SLIM -> Spec(40.dp, 14.dp, 22.dp, 0.dp, RoundedCornerShape(50), CircleShape)
        SwitchLook.CHUNKY -> Spec(58.dp, 34.dp, 28.dp, 3.dp, RoundedCornerShape(50), CircleShape)
        SwitchLook.SQUARE -> Spec(46.dp, 24.dp, 18.dp, 3.dp, RectangleShape, RectangleShape)
        else -> Spec(50.dp, 24.dp, 18.dp, 3.dp, CutCornerShape(6.dp), CutCornerShape(4.dp))
    }
    val x by animateDpAsState(if (checked) s.w - s.thumb - s.pad else s.pad, label = "switch")
    val trackColor by animateColorAsState(
        when (look) {
            SwitchLook.SLIM -> if (checked) c.primary.copy(alpha = 0.45f) else c.outlineVariant
            SwitchLook.CHUNKY -> if (checked) c.primary else c.surfaceContainerHighest
            SwitchLook.SQUARE -> if (checked) c.inverseSurface else Color.Transparent
            else -> if (checked) c.primary.copy(alpha = 0.22f) else Color.Transparent
        },
        label = "track"
    )
    val knobColor = when (look) {
        SwitchLook.SLIM -> if (checked) c.primary else c.surfaceContainerLowest
        SwitchLook.CHUNKY -> Color.White
        SwitchLook.SQUARE -> if (checked) c.inverseOnSurface else c.onSurface
        else -> if (checked) c.primary else c.outline
    }
    val border = when (look) {
        SwitchLook.SQUARE -> Modifier.border(1.5.dp, c.onSurface, s.track)
        SwitchLook.GLOW -> Modifier.border(1.dp, c.primary.copy(alpha = if (checked) 1f else 0.45f), s.track)
        else -> Modifier
    }
    val knobDecor = when {
        look == SwitchLook.SLIM || look == SwitchLook.CHUNKY -> Modifier.shadow(2.dp, s.knob)
        look == SwitchLook.GLOW && checked -> Modifier.shadow(8.dp, s.knob, ambientColor = c.primary, spotColor = c.primary)
        else -> Modifier
    }
    Box(
        toggle.padding(horizontal = 4.dp, vertical = 8.dp).width(s.w).height(maxOf(s.h, s.thumb)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(s.h).clip(s.track).background(trackColor).then(border))
        Box(
            Modifier.offset(x = x).size(s.thumb).then(knobDecor).clip(s.knob).background(knobColor)
                .then(if (look == SwitchLook.SLIM && !checked) Modifier.border(1.dp, c.outline, s.knob) else Modifier)
        )
    }
}

/** A hand-drawn box with a check, for the notebook design. */
@Composable
fun InkBox(checked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val mark = MaterialTheme.colorScheme.primary
    Box(
        modifier.size(size).drawWithCache {
            val w = this.size.width
            val h = this.size.height
            // Slightly uneven sides, like a pen.
            val box = Path().apply {
                moveTo(w * 0.06f, h * 0.12f)
                lineTo(w * 0.94f, h * 0.04f)
                lineTo(w * 0.9f, h * 0.95f)
                lineTo(w * 0.1f, h * 0.9f)
                close()
            }
            val tick = Path().apply {
                moveTo(w * 0.15f, h * 0.5f)
                lineTo(w * 0.42f, h * 0.82f)
                lineTo(w * 1.15f, -h * 0.2f)
            }
            onDrawBehind {
                drawPath(box, ink, style = Stroke(width = 1.5.dp.toPx()))
                if (checked) drawPath(tick, mark, style = Stroke(width = 2.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    )
}

/** A choice of a few options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.labelLarge
    when (d.button) {
        ButtonLook.BRACKETS -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            options.forEach { (value, label) ->
                val on = value == selected
                Text(
                    (if (on) "(*) " else "( ) ") + label,
                    style = style,
                    color = if (on) c.primary else c.onSurfaceVariant,
                    modifier = Modifier.clickable(role = Role.RadioButton) { onSelect(value) }.padding(vertical = 8.dp)
                )
            }
        }
        ButtonLook.INK, ButtonLook.TEXT -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val on = value == selected
                val line = c.primary
                val mark = if (d.button == ButtonLook.INK) {
                    if (on) Modifier.border(1.5.dp, line, RoundedCornerShape(50)) else Modifier
                } else {
                    Modifier.drawBehind {
                        if (on) drawLine(line, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 2.dp.toPx())
                    }
                }
                Text(
                    label,
                    style = style,
                    color = if (on) c.primary else c.onSurfaceVariant,
                    modifier = Modifier.clickable(role = Role.RadioButton) { onSelect(value) }.then(mark).padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        else -> SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size, d.buttonShape),
                    label = { Text(labelText(label), maxLines = 1) }
                )
            }
        }
    }
}

/** A chip that is on or off. */
@Composable
fun AppChip(label: String, selected: Boolean, dot: Color? = null, onClick: () -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    when (d.button) {
        ButtonLook.BRACKETS -> Text(
            (if (selected) "[x] " else "[ ] ") + label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.primary else c.onSurfaceVariant,
            modifier = Modifier.clickable(role = Role.Checkbox, onClick = onClick).padding(horizontal = 2.dp, vertical = 8.dp)
        )
        ButtonLook.INK -> Row(
            Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Checkbox, onClick = onClick)
                .then(if (selected) Modifier.border(1.5.dp, c.primary, RoundedCornerShape(50)) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dot != null) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.primary else c.onSurface)
        }
        else -> FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(labelText(label)) },
            shape = d.chipShape,
            leadingIcon = if (dot != null) {
                { Box(Modifier.size(10.dp).clip(CircleShape).background(dot)) }
            } else null,
            border = if (d.glow) BorderStroke(1.dp, c.primary.copy(alpha = if (selected) 1f else 0.4f))
            else FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
        )
    }
}

/** The mark of a single choice; the row around it handles the tap. */
@Composable
fun AppRadio(selected: Boolean) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    when (d.button) {
        ButtonLook.BRACKETS -> Text(
            if (selected) "(*)" else "( )",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) c.primary else c.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
        ButtonLook.SQUARE, ButtonLook.GLOW -> {
            val shape = if (d.button == ButtonLook.GLOW) CutCornerShape(5.dp) else RectangleShape
            val color = if (d.button == ButtonLook.GLOW) c.primary else c.onSurface
            Box(Modifier.padding(14.dp).size(20.dp).border(2.dp, color, shape), contentAlignment = Alignment.Center) {
                if (selected) Box(Modifier.size(10.dp).clip(shape).background(color))
            }
        }
        ButtonLook.INK -> Box(Modifier.padding(13.dp)) { InkBox(selected, 22.dp) }
        else -> RadioButton(selected = selected, onClick = null)
    }
}

/** The background of a screen: plain, a soft gradient, paper, terminal scan lines or neon glow. */
@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val paint = when (d.background) {
        BackgroundLook.PLAIN, BackgroundLook.PAPER -> Modifier.background(c.background)
        BackgroundLook.SOFT_GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                0f to lerp(c.background, c.primaryContainer, 0.75f),
                0.6f to c.background,
                1f to lerp(c.background, c.primaryContainer, 0.35f)
            )
        )
        BackgroundLook.SCANLINES -> Modifier.background(c.background).drawWithCache {
            val step = 3.dp.toPx()
            val lines = Path().apply {
                var y = 0f
                while (y < size.height) {
                    moveTo(0f, y)
                    lineTo(size.width, y)
                    y += step
                }
            }
            val color = c.onSurface.copy(alpha = 0.04f)
            onDrawBehind { drawPath(lines, color, style = Stroke(width = 1f)) }
        }
        BackgroundLook.NEON -> Modifier.background(c.background).drawBehind {
            val first = c.primary.copy(alpha = 0.30f)
            val second = lerp(c.primary, Color(0xFFFF2BD6), 0.65f).copy(alpha = 0.20f)
            val r1 = size.maxDimension * 0.75f
            drawCircle(Brush.radialGradient(listOf(first, Color.Transparent), center = Offset.Zero, radius = r1), r1, Offset.Zero)
            val corner = Offset(size.width, size.height)
            val r2 = size.maxDimension * 0.65f
            drawCircle(Brush.radialGradient(listOf(second, Color.Transparent), center = corner, radius = r2), r2, corner)
        }
    }
    Box(modifier.fillMaxSize().then(paint), content = content)
}

/** A group of settings: card, hairlines, soft card, box, code block, plain paper or glowing frame. */
@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val heading = when (d.section) {
        SectionLook.CODE -> "## $title"
        else -> labelText(title)
    }
    val headingStyle = if (d.section == SectionLook.PAPER) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(heading, style = headingStyle.glowing(), color = c.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        val frame = when (d.section) {
            SectionLook.CARD -> Modifier.clip(MaterialTheme.shapes.large).background(c.surfaceContainerLowest)
            SectionLook.FLAT -> Modifier.drawBehind { drawLine(c.outlineVariant, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            SectionLook.SOFT -> RoundedCornerShape(28.dp).let { Modifier.shadow(4.dp, it).clip(it).background(c.surfaceContainerLowest) }
            SectionLook.BOXED -> Modifier.border(1.5.dp, c.onSurface, RectangleShape)
            SectionLook.CODE -> Modifier.drawBehind { drawLine(c.primary.copy(alpha = 0.6f), Offset.Zero, Offset(0f, size.height), 2.dp.toPx()) }.padding(start = 6.dp)
            SectionLook.PAPER -> Modifier
            SectionLook.GLOW -> CutCornerShape(14.dp).let {
                Modifier.shadow(10.dp, it, ambientColor = c.primary, spotColor = c.primary).clip(it)
                    .background(c.surfaceContainerLowest).border(1.dp, c.primary.copy(alpha = 0.6f), it)
            }
        }
        Column(Modifier.fillMaxWidth().then(frame).padding(vertical = 4.dp)) { content() }
    }
}
