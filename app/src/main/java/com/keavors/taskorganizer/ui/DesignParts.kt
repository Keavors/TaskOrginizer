package com.keavors.taskorganizer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.ui.theme.CheckLook
import com.keavors.taskorganizer.ui.theme.FieldLook
import com.keavors.taskorganizer.ui.theme.FlagLook
import com.keavors.taskorganizer.ui.theme.ItemLook
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.MicLook
import com.keavors.taskorganizer.ui.theme.TabsLook
import com.keavors.taskorganizer.ui.theme.TopBarLook
import com.keavors.taskorganizer.ui.theme.priorityColor

fun priorityName(priority: Priority): String = when (priority) {
    Priority.NORMAL -> "обычная"
    Priority.MEDIUM -> "средняя"
    Priority.HIGH -> "высокая"
}

/** Ruled lines of the notebook design. */
private val RuleColor = Color(0xFF8FB3D9)

/** Container of one entry in the current design. Yellow and red entries get a colored edge. */
@Composable
fun ItemFrame(priority: Priority, highlighted: Boolean, dragging: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val shape = d.itemShape
    val open = d.item == ItemLook.FLAT || d.item == ItemLook.RULED || d.item == ItemLook.CODE
    val base = if (open) Color.Transparent else c.surfaceContainerLowest
    val background by animateColorAsState(
        when {
            highlighted -> c.primaryContainer
            dragging -> c.surfaceContainerHigh
            else -> base
        },
        animationSpec = tween(400),
        label = "item"
    )
    val lift = if (dragging) Modifier.shadow(8.dp, shape) else Modifier
    val frame = when (d.item) {
        ItemLook.CARD -> lift.clip(shape).background(background).border(1.dp, c.outlineVariant.copy(alpha = if (dragging) 0f else 0.6f), shape)
        ItemLook.ELEVATED -> Modifier.shadow(if (dragging) 12.dp else 4.dp, shape).clip(shape).background(background)
        ItemLook.OUTLINED -> lift.clip(shape).background(background).border(1.5.dp, c.onSurface.copy(alpha = 0.85f), shape)
        ItemLook.GLOW -> Modifier.shadow(if (dragging) 20.dp else 10.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape).background(background).border(1.dp, c.primary.copy(alpha = 0.6f), shape)
        ItemLook.FLAT, ItemLook.RULED, ItemLook.CODE -> lift.background(background)
    }
    val edge = if (priority == Priority.NORMAL || d.item == ItemLook.RULED || d.item == ItemLook.CODE) null else priorityColor(priority)
    val edgeWidth = if (priority == Priority.HIGH) 6.dp else 4.dp
    Box(modifier.fillMaxWidth().then(frame).drawBehind { if (edge != null) drawRect(edge, size = Size(edgeWidth.toPx(), size.height)) }) {
        content()
        when (d.item) {
            ItemLook.FLAT -> HorizontalDivider(Modifier.align(Alignment.BottomCenter), thickness = 0.5.dp, color = c.outlineVariant)
            ItemLook.RULED -> HorizontalDivider(Modifier.align(Alignment.BottomCenter), thickness = 1.dp, color = RuleColor.copy(alpha = 0.6f))
            ItemLook.CODE -> {
                val dash = c.outline.copy(alpha = 0.5f)
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).drawBehind {
                        drawLine(dash, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    }
                )
            }
            else -> Unit
        }
    }
}

/** The red margin line of the notebook design, drawn behind the list. */
fun Modifier.notebookMargin(enabled: Boolean): Modifier = if (!enabled) this else drawBehind {
    val x = 62.dp.toPx()
    drawLine(Color(0xFFE05A5A).copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
}

/** The "done" mark of a task. */
@Composable
fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    when (d.check) {
        CheckLook.BRACKETS -> Text(
            if (checked) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (checked) c.primary else c.onSurfaceVariant,
            modifier = modifier
        )
        CheckLook.INK -> InkBox(checked, d.checkSize, modifier)
        else -> {
            val fill by animateColorAsState(if (checked) c.primary else Color.Transparent, label = "check")
            val shape = d.checkShape
            val glow = d.check == CheckLook.CUT && checked
            Box(
                modifier
                    .size(d.checkSize)
                    .then(if (glow) Modifier.shadow(8.dp, shape, ambientColor = c.primary, spotColor = c.primary) else Modifier)
                    .clip(shape)
                    .background(fill)
                    .border(d.checkBorder, if (checked) c.primary else if (d.check == CheckLook.CUT) c.primary.copy(alpha = 0.7f) else c.outline, shape),
                contentAlignment = Alignment.Center
            ) {
                if (checked) Icon(d.icons.check, null, tint = c.onPrimary, modifier = Modifier.size(d.checkSize * 0.72f))
            }
        }
    }
}

/** Importance mark: a flag, a dot, exclamation marks, a pin or a bolt. [onClick] null = a picture. */
@Composable
fun PriorityMark(priority: Priority, onClick: (() -> Unit)? = null) {
    val d = LocalDesign.current
    val color = priorityColor(priority)
    val description = "Важность: ${priorityName(priority)}"
    val tap = if (onClick != null) Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = description, onClick = onClick) else Modifier
    Box(Modifier.size(if (onClick != null) 48.dp else 28.dp).then(tap), contentAlignment = Alignment.Center) {
        when (d.flag) {
            FlagLook.FLAG -> AppIcon(if (priority == Priority.NORMAL) Icons.Outlined.Flag else d.icons.flag, description, tint = color)
            FlagLook.DOT -> Box(
                Modifier.size(if (priority == Priority.HIGH) 14.dp else 11.dp).clip(CircleShape)
                    .then(if (priority == Priority.NORMAL) Modifier.border(1.5.dp, color, CircleShape) else Modifier.background(color))
            )
            FlagLook.BANG -> Text(
                when (priority) {
                    Priority.NORMAL -> "·"
                    Priority.MEDIUM -> "!"
                    Priority.HIGH -> "!!!"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
            FlagLook.PIN -> AppIcon(d.icons.pin, description, Modifier.rotate(30f), tint = if (priority == Priority.NORMAL) color.copy(alpha = 0.7f) else color)
            FlagLook.BOLT -> AppIcon(d.icons.bolt, description, tint = color)
        }
    }
}

/** One tab in the row of tabs. Without [onClick] it is a picture (style preview). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabLabel(
    title: String,
    selected: Boolean,
    dot: Color?,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val text = labelText(title)
    val click = if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier
    val padding = Modifier.padding(horizontal = d.tabPaddingH, vertical = d.tabPaddingV)
    when (d.tabs) {
        TabsLook.PILLS, TabsLook.TONAL, TabsLook.GLOW -> {
            val glow = d.tabs == TabsLook.GLOW
            val shape = if (glow) MaterialTheme.shapes.small else CircleShape
            val bg = when {
                d.tabs == TabsLook.TONAL -> if (selected) c.primaryContainer else c.surfaceContainerLowest.copy(alpha = 0.6f)
                selected -> c.primary
                glow -> Color.Transparent
                else -> c.surfaceContainerHigh
            }
            val fg = when {
                d.tabs == TabsLook.TONAL -> if (selected) c.onPrimaryContainer else c.onSurfaceVariant
                selected -> c.onPrimary
                glow -> c.primary
                else -> c.onSurface
            }
            Row(
                modifier
                    .then(if (glow && selected) Modifier.shadow(12.dp, shape, ambientColor = c.primary, spotColor = c.primary) else Modifier)
                    .then(if (d.tabs == TabsLook.TONAL && selected) Modifier.shadow(3.dp, shape) else Modifier)
                    .clip(shape)
                    .background(bg)
                    .then(if (glow) Modifier.border(1.dp, c.primary.copy(alpha = if (selected) 1f else 0.5f), shape) else Modifier)
                    .then(click)
                    .then(padding),
                verticalAlignment = Alignment.CenterVertically
            ) { TabText(text, dot, count, fg, selected, ring = if (selected && d.tabs == TabsLook.PILLS) fg else null) }
        }
        TabsLook.UNDERLINE -> {
            val line = c.primary
            Row(
                modifier
                    .clip(MaterialTheme.shapes.small)
                    .then(click)
                    .drawBehind {
                        if (selected) drawRect(line, topLeft = Offset(0f, size.height - 2.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
                    }
                    .then(padding),
                verticalAlignment = Alignment.CenterVertically
            ) { TabText(text, dot, count, if (selected) c.onSurface else c.onSurfaceVariant, selected) }
        }
        TabsLook.BOXES -> Row(
            modifier
                .background(if (selected) c.inverseSurface else Color.Transparent)
                .border(1.5.dp, c.onSurface, MaterialTheme.shapes.small)
                .then(click)
                .then(padding),
            verticalAlignment = Alignment.CenterVertically
        ) { TabText(text, dot, count, if (selected) c.inverseOnSurface else c.onSurface, selected) }
        TabsLook.BRACKETS -> Row(modifier.then(click).then(padding), verticalAlignment = Alignment.CenterVertically) {
            TabText(if (selected) "[$text]" else " $text ", dot, count, if (selected) c.primary else c.onSurfaceVariant, selected)
        }
        TabsLook.BOOKMARKS -> {
            // A tab of a paper folder: rounded top corners, the selected one lighter and outlined.
            val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            Row(
                modifier
                    .clip(shape)
                    .background(if (selected) c.surfaceContainerLowest else c.surfaceContainerHigh)
                    .then(if (selected) Modifier.border(1.dp, c.outline.copy(alpha = 0.6f), shape) else Modifier)
                    .then(click)
                    .then(padding),
                verticalAlignment = Alignment.CenterVertically
            ) { TabText(text, dot, count, if (selected) c.onSurface else c.onSurfaceVariant, selected) }
        }
    }
}

@Composable
private fun TabText(text: String, dot: Color?, count: Int, color: Color, selected: Boolean, ring: Color? = null) {
    if (dot != null) {
        Box(
            Modifier.size(9.dp).clip(CircleShape).background(dot)
                .then(if (ring != null) Modifier.border(1.5.dp, ring, CircleShape) else Modifier)
        )
        Spacer(Modifier.width(8.dp))
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.let { if (LocalDesign.current.tabs == TabsLook.GLOW && !selected) it.glowing() else it },
        fontWeight = if (selected) FontWeight.Bold else null,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 180.dp)
    )
    if (count > 0) {
        Spacer(Modifier.width(6.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
    }
}

/** The "+" after the tabs. */
@Composable
fun AddTabButton(onClick: () -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val add = d.icons.add
    when (d.tabs) {
        TabsLook.BRACKETS -> Text(
            "[+]",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = c.primary,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Новая вкладка", onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp)
        )
        TabsLook.UNDERLINE -> AppIconButton(add, "Новая вкладка", onClick = onClick)
        else -> {
            val shape = when (d.tabs) {
                TabsLook.BOXES -> MaterialTheme.shapes.small
                TabsLook.GLOW -> MaterialTheme.shapes.small
                TabsLook.BOOKMARKS -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                else -> CircleShape
            }
            val frame = when (d.tabs) {
                TabsLook.BOXES -> Modifier.border(1.5.dp, c.onSurface, shape)
                TabsLook.GLOW -> Modifier.border(1.dp, c.primary.copy(alpha = 0.6f), shape)
                else -> Modifier.background(c.surfaceContainerHigh)
            }
            Box(Modifier.clip(shape).then(frame).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
                AppIcon(add, "Новая вкладка", size = 20.dp, tint = if (d.tabs == TabsLook.GLOW) c.primary else c.onSurface)
            }
        }
    }
}

/** Frame of the input field at the bottom. */
@Composable
fun FieldFrame(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val shape = d.fieldShape
    val line = c.outline
    val frame = when (d.field) {
        FieldLook.PILL -> if (d.item == ItemLook.ELEVATED) {
            Modifier.shadow(4.dp, shape).clip(shape).background(c.surfaceContainerLowest)
        } else Modifier.clip(shape).background(c.surfaceContainerHigh)
        FieldLook.BOX -> Modifier.clip(shape).background(c.surfaceContainerLowest).border(1.5.dp, c.onSurface, shape)
        FieldLook.GLOW -> Modifier.shadow(12.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape).background(c.surfaceContainerLowest).border(1.dp, c.primary.copy(alpha = 0.75f), shape)
        FieldLook.LINE, FieldLook.PROMPT -> Modifier.drawBehind {
            val y = size.height - 1.dp.toPx()
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
        }
    }
    Row(modifier.heightIn(min = d.fieldHeight).then(frame), verticalAlignment = Alignment.CenterVertically) {
        if (d.field == FieldLook.PROMPT) {
            Text(
                ">",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = c.primary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        content()
    }
}

/** Where the text starts inside [FieldFrame]. */
@Composable
fun fieldTextStart(): Dp = when (LocalDesign.current.field) {
    FieldLook.LINE -> 4.dp
    FieldLook.PROMPT -> 8.dp
    else -> 20.dp
}

/** The face of the microphone button: listening state and loudness come from the caller. */
@Composable
fun MicFace(listening: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val shape = d.micShape
    val accent = if (listening) c.error else c.primary
    val icon = if (listening) d.icons.stop else d.icons.mic
    val description = if (listening) "Остановить" else "Надиктовать: нажмите или удерживайте"
    when (d.mic) {
        MicLook.REC -> {
            val blink = rememberInfiniteTransition(label = "rec")
            val a by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "dot")
            Box(
                modifier.size(d.micSize).border(1.5.dp, accent, shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        busy -> "..."
                        listening -> "■"
                        else -> "REC"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent.copy(alpha = if (listening) a else 1f)
                )
            }
        }
        else -> {
            val frame = when (d.mic) {
                MicLook.FILLED -> Modifier.shadow(3.dp, shape).clip(shape).background(accent)
                MicLook.OUTLINED -> Modifier.clip(shape).then(
                    if (listening) Modifier.background(accent) else Modifier.border(1.5.dp, c.onSurface, shape)
                )
                MicLook.GRADIENT -> Modifier.shadow(10.dp, shape, ambientColor = accent, spotColor = accent).clip(shape)
                    .background(Brush.linearGradient(listOf(lerp(accent, Color.White, 0.25f), accent, lerp(accent, Color.Black, 0.2f))))
                MicLook.SQUARE -> Modifier.clip(shape).background(if (listening) accent else c.inverseSurface)
                MicLook.STAMP -> Modifier.clip(shape).then(if (listening) Modifier.background(accent.copy(alpha = 0.15f)) else Modifier)
                    .border(2.dp, accent, shape).padding(4.dp).border(1.dp, accent.copy(alpha = 0.6f), shape)
                MicLook.GLOW -> Modifier.shadow(16.dp, shape, ambientColor = accent, spotColor = accent).clip(shape)
                    .background(accent.copy(alpha = 0.18f)).border(1.5.dp, accent, shape)
                MicLook.REC -> Modifier
            }
            val tint = when (d.mic) {
                MicLook.OUTLINED -> if (listening) c.onError else c.onSurface
                MicLook.SQUARE -> if (listening) c.onError else c.inverseOnSurface
                MicLook.STAMP, MicLook.GLOW -> accent
                else -> if (listening) c.onError else c.onPrimary
            }
            Box(modifier.size(d.micSize).then(frame), contentAlignment = Alignment.Center) {
                if (busy) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp), color = tint, strokeWidth = 2.5.dp)
                } else {
                    AppIcon(icon, description, tint = tint, size = if (d.micSize >= 60.dp) 30.dp else 26.dp)
                }
            }
        }
    }
}

/** The app name at the top, in the manner of the current design. */
@Composable
fun AppTitle(modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val t = MaterialTheme.typography
    when (d.topBar) {
        TopBarLook.STANDARD -> Text("Органайзер задач", style = t.titleLarge, fontWeight = FontWeight.SemiBold, modifier = modifier)
        TopBarLook.MINIMAL -> Text("органайзер", style = t.labelLarge, color = c.onSurfaceVariant, modifier = modifier)
        TopBarLook.SOFT -> Text("Органайзер", style = t.headlineSmall, color = c.primary, modifier = modifier)
        TopBarLook.STRICT -> Text("ОРГАНАЙЗЕР ЗАДАЧ", style = t.titleLarge.copy(letterSpacing = 2.sp), modifier = modifier)
        TopBarLook.TERMINAL -> {
            val blink = rememberInfiniteTransition(label = "cursor")
            val a by blink.animateFloat(1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "cursor")
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                Text("~/органайзер $ ", style = t.titleSmall, color = c.primary)
                Box(Modifier.size(width = 9.dp, height = 18.dp).background(c.primary.copy(alpha = a)))
            }
        }
        TopBarLook.PAPER -> {
            val ink = c.primary
            Text(
                "Мои дела",
                style = t.headlineMedium,
                modifier = modifier.drawBehind {
                    val y = size.height - 4.dp.toPx()
                    drawLine(ink.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width * 0.55f, y - 2.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
            )
        }
        TopBarLook.COMPACT -> Text("Органайзер", style = t.titleMedium, modifier = modifier)
        TopBarLook.NEON -> Text("ОРГАНАЙЗЕР", style = t.titleLarge.copy(letterSpacing = 4.sp, fontWeight = FontWeight.Black).glowing(), color = c.primary, modifier = modifier)
    }
}

/** A small picture of the main screen in the current theme, for the style picker. */
@Composable
fun StyleSample(modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * 0.55f, density.fontScale)) {
        ScreenBackground(modifier) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                AppTitle()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TabLabel("Задачи", selected = true, dot = null, count = 0)
                    TabLabel("Идеи", selected = false, dot = null, count = 0)
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f).notebookMargin(d.item == ItemLook.RULED), verticalArrangement = Arrangement.spacedBy(d.itemGap)) {
                    SampleItem("Позвонить", Priority.HIGH, done = false)
                    SampleItem("Купить хлеб", Priority.MEDIUM, done = false)
                    SampleItem("Спорт", Priority.NORMAL, done = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldFrame(Modifier.weight(1f)) {}
                    Spacer(Modifier.width(8.dp))
                    MicFace(listening = false, busy = false)
                }
            }
        }
    }
}

@Composable
private fun SampleItem(text: String, priority: Priority, done: Boolean) {
    val c = MaterialTheme.colorScheme
    ItemFrame(priority, highlighted = false, dragging = false) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) { CheckMark(done) }
            Spacer(Modifier.width(14.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) c.onSurfaceVariant else c.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PriorityMark(priority)
        }
    }
}
