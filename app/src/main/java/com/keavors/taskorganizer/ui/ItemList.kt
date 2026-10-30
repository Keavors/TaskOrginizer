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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.ItemSource
import com.keavors.taskorganizer.data.Tab
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.ui.theme.ImportantColor
import com.keavors.taskorganizer.ui.theme.tabColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate

@Composable
fun ItemList(vm: MainViewModel, data: AppData, tab: Tab, contentPadding: PaddingValues) {
    val items = data.itemsOf(tab.id)
    val isTasks = tab.kind == TabKind.TASKS
    val open = if (isTasks) items.filter { !it.done } else items
    val done = if (isTasks) items.filter { it.done }.sortedByDescending { it.completedAt ?: 0 } else emptyList()
    var showDone by rememberSaveable(tab.id) { mutableStateOf(true) }
    val openIds = open.map { it.id }.toSet()

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val a = from.key as? String
        val b = to.key as? String
        if (a != null && b != null && a in openIds && b in openIds) vm.moveItem(tab.id, a, b)
    }

    // After a search: scroll to the entry and let it flash.
    val highlight = vm.highlightItemId
    LaunchedEffect(highlight, tab.id) {
        if (highlight == null) return@LaunchedEffect
        val keys = buildList {
            add("header")
            if (items.isEmpty()) add("empty")
            addAll(open.map { it.id })
            if (done.isNotEmpty()) add("done-header")
            if (showDone || done.none { it.id == highlight }) addAll(done.map { it.id }) else {
                showDone = true
                addAll(done.map { it.id })
            }
        }
        val index = keys.indexOf(highlight)
        if (index >= 0) listState.animateScrollToItem(index)
        delay(1800)
        vm.highlightItemId = null
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") { TabHeader(tab, open.size, done.size) }
        if (items.isEmpty()) item(key = "empty") { EmptyState(tab) }
        items(open, key = { it.id }) { item ->
            ReorderableItem(reorderState, key = item.id) { dragging ->
                ItemRow(vm, data, tab, item, dragging, highlight == item.id, Modifier.draggableHandle())
            }
        }
        if (done.isNotEmpty()) {
            item(key = "done-header") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDone = !showDone }) {
                        Icon(if (showDone) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Выполнено · ${done.size}")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.clearCompleted(tab.id) }) { Text("Очистить") }
                }
            }
            if (showDone) {
                items(done, key = { it.id }) { item ->
                    ItemRow(vm, data, tab, item, dragging = false, highlighted = highlight == item.id, dragHandle = null)
                }
            }
        }
    }
}

@Composable
private fun TabHeader(tab: Tab, open: Int, done: Int) {
    Column(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            tab.color?.let {
                Box(Modifier.size(12.dp).clip(CircleShape).background(tabColor(it)))
                Spacer(Modifier.width(10.dp))
            }
            Text(tab.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        val subtitle = if (tab.kind == TabKind.TASKS) {
            listOfNotNull(activeCount(open), if (done > 0) doneCount(done) else null).joinToString(" · ")
        } else notesCount(open)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(tab: Tab) {
    Column(
        Modifier.fillMaxWidth().padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (tab.kind == TabKind.TASKS) Icons.Outlined.ListAlt else Icons.Outlined.StickyNote2,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (tab.kind == TabKind.TASKS) "Задач пока нет" else "Заметок пока нет",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Напишите внизу или нажмите на микрофон и надиктуйте.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    vm: MainViewModel,
    data: AppData,
    tab: Tab,
    item: Item,
    dragging: Boolean,
    highlighted: Boolean,
    dragHandle: Modifier?
) {
    val isTask = tab.kind == TabKind.TASKS
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var dueMenu by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val swipe = rememberSwipeToDismissBoxState()
    val background by animateColorAsState(
        when {
            highlighted -> colors.primaryContainer
            dragging -> colors.surfaceContainerHigh
            else -> colors.surfaceContainerLowest
        },
        animationSpec = tween(400),
        label = "row"
    )

    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = isTask && !editing,
        enableDismissFromEndToStart = !editing,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> vm.deleteItems(tab.id, listOf(item.id))
                SwipeToDismissBoxValue.StartToEnd -> {
                    vm.setDone(tab.id, item.id, !item.done)
                    scope.launch { swipe.reset() }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        backgroundContent = { SwipeBackground(swipe.dismissDirection, item.done) }
    ) {
        Surface(
            color = background,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (dragging) Modifier.shadow(8.dp, MaterialTheme.shapes.medium) else Modifier)
                .border(1.dp, colors.outlineVariant.copy(alpha = if (dragging) 0f else 0.6f), MaterialTheme.shapes.medium)
        ) {
            Row(Modifier.padding(start = 4.dp, end = 0.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isTask) {
                    CheckCircle(checked = item.done) { vm.setDone(tab.id, item.id, !item.done) }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    if (editing) {
                        ItemEditor(item, multiLine = !isTask, onDone = { text ->
                            editing = false
                            vm.setText(tab.id, item, text)
                        })
                    } else {
                        Text(
                            item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.done) colors.onSurfaceVariant else colors.onSurface,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { editing = true },
                                onLongClick = { menu = true }
                            )
                        )
                    }
                    if (!editing && (item.dueDate != null || item.source == ItemSource.VOICE)) {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            item.dueDate?.let { DueChip(it, item.done) { dueMenu = true } }
                            if (item.source == ItemSource.VOICE) {
                                if (item.dueDate != null) Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.GraphicEq, "Надиктовано", Modifier.size(14.dp), tint = colors.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { vm.setImportant(tab.id, item.id, !item.important) }) {
                        Icon(
                            if (item.important) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                            if (item.important) "Снять «важное»" else "Отметить как важное",
                            tint = if (item.important) ImportantColor else colors.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    DueMenu(
                        expanded = dueMenu,
                        current = item.dueDate,
                        onDismiss = { dueMenu = false },
                        onPick = { vm.setDueDate(tab.id, item.id, it) },
                        onCustom = { datePicker = true }
                    )
                    ItemMenu(
                        expanded = menu,
                        vm = vm,
                        data = data,
                        tab = tab,
                        item = item,
                        onDismiss = { menu = false },
                        onEdit = { editing = true },
                        onDue = { dueMenu = true }
                    )
                }
                if (dragHandle != null) {
                    Icon(
                        Icons.Rounded.DragIndicator,
                        "Перетащить",
                        tint = colors.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = dragHandle.padding(start = 0.dp, end = 10.dp).size(22.dp)
                    )
                } else {
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }

    if (datePicker) {
        DueDateDialog(initial = item.dueDate, onDismiss = { datePicker = false }) { vm.setDueDate(tab.id, item.id, it) }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, done: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Triple(colors.errorContainer, Icons.Outlined.Delete, Alignment.CenterEnd)
        SwipeToDismissBoxValue.StartToEnd -> Triple(colors.primaryContainer, if (done) Icons.Outlined.EventBusy else Icons.Rounded.Check, Alignment.CenterStart)
        SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.Center)
    }
    Box(
        Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) Icon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) colors.onErrorContainer else colors.onPrimaryContainer)
    }
}

@Composable
private fun CheckCircle(checked: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) colors.primary else Color.Transparent, label = "check")
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, if (checked) colors.primary else colors.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Rounded.Check, "Выполнено", tint = colors.onPrimary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun DueChip(iso: String, done: Boolean, onClick: () -> Unit) {
    val (text, tone) = Due.label(iso)
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        done -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        tone == DueTone.OVERDUE -> colors.errorContainer to colors.onErrorContainer
        tone == DueTone.TODAY -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.surfaceContainerHigh to colors.onSurfaceVariant
    }
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(13.dp), tint = fg)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
private fun ItemEditor(item: Item, multiLine: Boolean, onDone: (String) -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(item.text, TextRange(item.text.length))) }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            onDone(value.text)
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (multiLine) ImeAction.Default else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { finish() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .onFocusChanged {
                if (focused && !it.isFocused) finish()
                focused = it.isFocused
            }
    )
}

@Composable
private fun DueMenu(expanded: Boolean, current: String?, onDismiss: () -> Unit, onPick: (String?) -> Unit, onCustom: () -> Unit) {
    val today = LocalDate.now()
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Сегодня") }, leadingIcon = { Icon(Icons.Outlined.Today, null) }, onClick = {
            onDismiss(); onPick(today.toString())
        })
        DropdownMenuItem(text = { Text("Завтра") }, leadingIcon = { Icon(Icons.Outlined.Event, null) }, onClick = {
            onDismiss(); onPick(today.plusDays(1).toString())
        })
        DropdownMenuItem(text = { Text("Через неделю") }, leadingIcon = { Icon(Icons.Outlined.Event, null) }, onClick = {
            onDismiss(); onPick(today.plusWeeks(1).toString())
        })
        DropdownMenuItem(text = { Text("Выбрать дату…") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }, onClick = {
            onDismiss(); onCustom()
        })
        if (current != null) {
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Убрать срок") }, leadingIcon = { Icon(Icons.Outlined.EventBusy, null) }, onClick = {
                onDismiss(); onPick(null)
            })
        }
    }
}

@Composable
private fun ItemMenu(
    expanded: Boolean,
    vm: MainViewModel,
    data: AppData,
    tab: Tab,
    item: Item,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDue: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Изменить") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
            onDismiss(); onEdit()
        })
        DropdownMenuItem(text = { Text("Срок") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }, onClick = {
            onDismiss(); onDue()
        })
        val others = data.tabs.filter { it.id != tab.id }
        if (others.isNotEmpty()) {
            HorizontalDivider()
            Text(
                "Перенести во вкладку",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            others.forEach { target ->
                DropdownMenuItem(text = { Text(target.title) }, leadingIcon = { Icon(Icons.Outlined.SwapHoriz, null) }, onClick = {
                    onDismiss(); vm.moveItemToTab(tab.id, item.id, target.id)
                })
            }
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); vm.deleteItems(tab.id, listOf(item.id)) }
        )
    }
}
