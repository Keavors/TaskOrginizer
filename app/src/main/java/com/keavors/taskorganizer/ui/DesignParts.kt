package com.keavors.taskorganizer.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.ui.theme.CheckLook
import com.keavors.taskorganizer.ui.theme.FieldLook
import com.keavors.taskorganizer.ui.theme.ItemLook
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.TabsLook
import com.keavors.taskorganizer.ui.theme.priorityColor

fun priorityName(priority: Priority): String = when (priority) {
    Priority.NORMAL -> "обычная"
    Priority.MEDIUM -> "средняя"
    Priority.HIGH -> "высокая"
}

/** Container of one entry in the current design. Yellow and red entries get a colored edge. */
@Composable
fun ItemFrame(priority: Priority, highlighted: Boolean, dragging: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    val shape = d.itemShape
    val flat = d.item == ItemLook.FLAT || d.item == ItemLook.RULED
    val base = if (flat) c.background else c.surfaceContainerLowest
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
        ItemLook.ELEVATED -> Modifier.shadow(if (dragging) 10.dp else 3.dp, shape).clip(shape).background(background)
        ItemLook.OUTLINED -> lift.clip(shape).background(background).border(1.dp, c.outline.copy(alpha = 0.6f), shape)
        ItemLook.GLOW -> Modifier.shadow(if (dragging) 18.dp else 8.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape).background(background).border(1.dp, c.primary.copy(alpha = 0.55f), shape)
        ItemLook.FLAT, ItemLook.RULED -> lift.background(background)
    }
    val edge = if (priority == Priority.NORMAL) null else priorityColor(priority)
    Box(modifier.fillMaxWidth().then(frame).drawBehind { if (edge != null) drawRect(edge, size = Size(4.dp.toPx(), size.height)) }) {
        content()
        if (flat) {
            HorizontalDivider(
                Modifier.align(Alignment.BottomCenter),
                thickness = if (d.item == ItemLook.RULED) 1.dp else 0.5.dp,
                color = c.outlineVariant
            )
        }
    }
}

/** The "done" mark of a task: circle, square or [x]. */
@Composable
fun CheckMark(checked: Boolean, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    if (d.check == CheckLook.BRACKETS) {
        Text(
            if (checked) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = if (checked) c.primary else c.onSurfaceVariant,
            modifier = modifier
        )
        return
    }
    val fill by animateColorAsState(if (checked) c.primary else Color.Transparent, label = "check")
    val shape = d.checkShape
    Box(
        modifier
            .size(d.checkSize)
            .then(if (d.glow && checked) Modifier.shadow(6.dp, shape, ambientColor = c.primary, spotColor = c.primary) else Modifier)
            .clip(shape)
            .background(fill)
            .border(d.checkBorder, if (checked) c.primary else c.outline, shape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = c.onPrimary, modifier = Modifier.size(d.checkSize * 0.7f))
    }
}

/** Gray, yellow or red flag; a tap moves to the next level. */
@Composable
fun PriorityFlag(priority: Priority, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (priority == Priority.NORMAL) Icons.Outlined.Flag else Icons.Rounded.Flag,
            "Важность: ${priorityName(priority)}",
            tint = priorityColor(priority)
        )
    }
}

/** One tab in the row of tabs, drawn in the current design. Without [onClick] it is a picture (style preview). */
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
    val text = if (d.uppercaseTabs) title.uppercase() else title
    val click = if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier
    val padding = Modifier.padding(horizontal = d.tabPaddingH, vertical = d.tabPaddingV)
    when (d.tabs) {
        TabsLook.PILLS, TabsLook.TONAL, TabsLook.GLOW -> {
            val glow = d.tabs == TabsLook.GLOW
            val bg = when {
                d.tabs == TabsLook.TONAL -> if (selected) c.primaryContainer else Color.Transparent
                selected -> c.primary
                glow -> c.surfaceContainerLowest
                else -> c.surfaceContainerHigh
            }
            val fg = when {
                d.tabs == TabsLook.TONAL -> if (selected) c.onPrimaryContainer else c.onSurfaceVariant
                selected -> c.onPrimary
                else -> c.onSurface
            }
            Row(
                modifier
                    .then(if (glow && selected) Modifier.shadow(10.dp, CircleShape, ambientColor = c.primary, spotColor = c.primary) else Modifier)
                    .clip(CircleShape)
                    .background(bg)
                    .then(if (glow) Modifier.border(1.dp, c.primary.copy(alpha = if (selected) 1f else 0.5f), CircleShape) else Modifier)
                    .then(click)
                    .then(padding),
                verticalAlignment = Alignment.CenterVertically
            ) { TabText(text, dot, count, fg, selected, ring = if (selected && d.tabs != TabsLook.TONAL) fg else null) }
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
            ) { TabText(text, dot, count, if (selected) c.primary else c.onSurfaceVariant, selected) }
        }
        TabsLook.BOXES -> {
            val shape = MaterialTheme.shapes.small
            Row(
                modifier
                    .clip(shape)
                    .background(if (selected) c.inverseSurface else Color.Transparent)
                    .border(1.dp, if (selected) c.inverseSurface else c.outline, shape)
                    .then(click)
                    .then(padding),
                verticalAlignment = Alignment.CenterVertically
            ) { TabText(text, dot, count, if (selected) c.inverseOnSurface else c.onSurface, selected) }
        }
        TabsLook.BRACKETS -> {
            Row(modifier.then(click).then(padding), verticalAlignment = Alignment.CenterVertically) {
                TabText(if (selected) "[$text]" else " $text ", dot, count, if (selected) c.primary else c.onSurfaceVariant, selected)
            }
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
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else null,
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
    when (d.tabs) {
        TabsLook.PILLS, TabsLook.TONAL, TabsLook.GLOW -> Box(
            Modifier.clip(CircleShape).background(c.surfaceContainerHigh).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)
        ) { Icon(Icons.Outlined.Add, "Новая вкладка", Modifier.size(20.dp)) }
        TabsLook.BOXES -> {
            val shape = MaterialTheme.shapes.small
            Box(
                Modifier.clip(shape).border(1.dp, c.outline, shape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp)
            ) { Icon(Icons.Outlined.Add, "Новая вкладка", Modifier.size(20.dp)) }
        }
        TabsLook.BRACKETS -> Text(
            "[+]",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = c.primary,
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp)
        )
        TabsLook.UNDERLINE -> Box(Modifier.clip(CircleShape).clickable(onClick = onClick).padding(8.dp)) {
            Icon(Icons.Outlined.Add, "Новая вкладка", Modifier.size(20.dp), tint = c.onSurfaceVariant)
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
        FieldLook.PILL -> Modifier.clip(shape).background(c.surfaceContainerHigh)
        FieldLook.BOX -> Modifier.clip(shape).background(c.surfaceContainerLowest).border(1.dp, c.outline, shape)
        FieldLook.GLOW -> Modifier.shadow(10.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape).background(c.surfaceContainerLowest).border(1.dp, c.primary.copy(alpha = 0.7f), shape)
        FieldLook.LINE, FieldLook.PROMPT -> Modifier.drawBehind {
            val y = size.height - 1.dp.toPx()
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
        }
    }
    Row(modifier.heightIn(min = d.fieldHeight).then(frame), verticalAlignment = Alignment.CenterVertically) {
        if (d.field == FieldLook.PROMPT) {
            Text(
                ">",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
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

/** A small picture of the list in the current theme, for the style picker. */
@Composable
fun StyleSample(modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * 0.6f, density.fontScale)) {
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TabLabel("Задачи", selected = true, dot = null, count = 0)
                TabLabel("Идеи", selected = false, dot = null, count = 0)
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(d.itemGap)) {
                SampleItem("Позвонить", Priority.HIGH, done = false)
                SampleItem("Купить хлеб", Priority.MEDIUM, done = false)
                SampleItem("Спорт", Priority.NORMAL, done = true)
            }
        }
    }
}

@Composable
private fun SampleItem(text: String, priority: Priority, done: Boolean) {
    val c = MaterialTheme.colorScheme
    ItemFrame(priority, highlighted = false, dragging = false) {
        Row(Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckMark(done)
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) c.onSurfaceVariant else c.onSurface,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (priority == Priority.NORMAL) Icons.Outlined.Flag else Icons.Rounded.Flag,
                null,
                tint = priorityColor(priority),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
