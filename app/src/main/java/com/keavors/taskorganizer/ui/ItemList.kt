package com.keavors.taskorganizer.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.DataOps
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.ItemSort
import com.keavors.taskorganizer.data.ItemSource
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.Tab
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.ui.theme.HeaderSize
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.priorityColor
import com.keavors.taskorganizer.ui.theme.tabColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun ItemList(vm: MainViewModel, data: AppData, tab: Tab, contentPadding: PaddingValues) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val design = LocalDesign.current
    val items = data.itemsOf(tab.id)
    val isTasks = tab.kind == TabKind.TASKS
    val open = DataOps.displayOrder(if (isTasks) items.filter { !it.done } else items, settings.sort)
    val done = if (isTasks) items.filter { it.done }.sortedByDescending { it.completedAt ?: 0 } else emptyList()
    var showDone by rememberSaveable(tab.id) { mutableStateOf(true) }
    // Read by the drag callback, which outlives this composition.
    val openById by rememberUpdatedState(open.associateBy { it.id })
    val byPriority by rememberUpdatedState(settings.sort == ItemSort.PRIORITY)
    val tabId by rememberUpdatedState(tab.id)

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val a = openById[from.key as? String]
        val b = openById[to.key as? String]
        // Sorted by importance, an entry can only move within its level.
        if (a != null && b != null && (!byPriority || a.priority == b.priority)) vm.moveItem(tabId, a.id, b.id)
    }

    // After a search or a reminder: scroll to the entry and let it flash.
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
        verticalArrangement = Arrangement.spacedBy(design.itemGap),
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
    val h = LocalDesign.current.header
    val colors = MaterialTheme.colorScheme
    val base = when (h.size) {
        HeaderSize.TITLE -> MaterialTheme.typography.titleLarge
        HeaderSize.HEADLINE -> MaterialTheme.typography.headlineSmall
        HeaderSize.LARGE -> MaterialTheme.typography.headlineMedium
    }
    Column(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            tab.color?.let {
                Box(Modifier.size(12.dp).clip(CircleShape).background(tabColor(it)))
                Spacer(Modifier.width(10.dp))
            }
            val title = h.prefix + tab.title
            Text(
                if (h.uppercase) title.uppercase() else title,
                style = base.copy(
                    fontWeight = h.weight,
                    fontStyle = if (h.italic) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = if (h.uppercase) 1.sp else base.letterSpacing
                ),
                color = if (h.accent) colors.primary else colors.onSurface
            )
        }
        val subtitle = if (tab.kind == TabKind.TASKS) {
            listOfNotNull(activeCount(open), if (done > 0) doneCount(done) else null).joinToString(" · ")
        } else notesCount(open)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
    var timePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val design = LocalDesign.current
    val swipe = rememberSwipeToDismissBoxState()

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
        ItemFrame(item.priority, highlighted, dragging) {
            // The whole entry reacts: tap = edit, hold = menu.
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = !editing,
                        onClickLabel = "Изменить",
                        onLongClickLabel = "Меню",
                        onClick = { editing = true },
                        onLongClick = { menu = true }
                    )
                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTask) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClickLabel = if (item.done) "Вернуть" else "Выполнено") { vm.setDone(tab.id, item.id, !item.done) },
                        contentAlignment = Alignment.Center
                    ) { CheckMark(item.done) }
                } else {
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f).padding(vertical = design.itemPadding)) {
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
                            textDecoration = if (item.done) TextDecoration.LineThrough else null
                        )
                    }
                    if (!editing && (item.dueDate != null || item.source == ItemSource.VOICE)) {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            item.dueDate?.let { DueChip(it, item.dueTime, item.done) { dueMenu = true } }
                            if (item.source == ItemSource.VOICE) {
                                if (item.dueDate != null) Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.GraphicEq, "Надиктовано", Modifier.size(14.dp), tint = colors.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Box {
                    PriorityFlag(item.priority) { vm.setPriority(tab.id, item.id, item.priority.next()) }
                    DueMenu(
                        expanded = dueMenu,
                        date = item.dueDate,
                        time = item.dueTime,
                        onDismiss = { dueMenu = false },
                        onPickDate = { vm.setDue(tab.id, item.id, it, item.dueTime) },
                        onCustomDate = { datePicker = true },
                        onPickTime = { timePicker = true },
                        onClearTime = { vm.setDue(tab.id, item.id, item.dueDate, null) },
                        onClear = { vm.setDue(tab.id, item.id, null, null) }
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
        DueDateDialog(initial = item.dueDate, onDismiss = { datePicker = false }) { vm.setDue(tab.id, item.id, it, item.dueTime) }
    }
    if (timePicker) {
        val initial = item.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
        TimePickerDialog(
            initial = initial,
            onDismiss = { timePicker = false },
            onClear = if (item.dueTime != null) ({ vm.setDue(tab.id, item.id, item.dueDate, null) }) else null
        ) { time ->
            // Without a date: today, or tomorrow if that time has already passed today.
            val date = item.dueDate ?: LocalDate.now().let { today ->
                if (today.atTime(time).isBefore(LocalDateTime.now())) today.plusDays(1) else today
            }.toString()
            vm.setDue(tab.id, item.id, date, time.toString())
        }
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
        Modifier.fillMaxSize().clip(LocalDesign.current.itemShape).background(color).padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) Icon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) colors.onErrorContainer else colors.onPrimaryContainer)
    }
}

@Composable
private fun DueChip(date: String, time: String?, done: Boolean, onClick: () -> Unit) {
    val (text, tone) = Due.label(date, time)
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        done -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        tone == DueTone.OVERDUE -> colors.errorContainer to colors.onErrorContainer
        tone == DueTone.TODAY -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.surfaceContainerHigh to colors.onSurfaceVariant
    }
    Row(
        Modifier.clip(MaterialTheme.shapes.extraSmall).background(bg).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (time != null) Icons.Outlined.Alarm else Icons.Outlined.CalendarMonth, null, Modifier.size(13.dp), tint = fg)
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
private fun DueMenu(
    expanded: Boolean,
    date: String?,
    time: String?,
    onDismiss: () -> Unit,
    onPickDate: (String) -> Unit,
    onCustomDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onClear: () -> Unit
) {
    val today = LocalDate.now()
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Сегодня") }, leadingIcon = { Icon(Icons.Outlined.Today, null) }, onClick = {
            onDismiss(); onPickDate(today.toString())
        })
        DropdownMenuItem(text = { Text("Завтра") }, leadingIcon = { Icon(Icons.Outlined.Event, null) }, onClick = {
            onDismiss(); onPickDate(today.plusDays(1).toString())
        })
        DropdownMenuItem(text = { Text("Через неделю") }, leadingIcon = { Icon(Icons.Outlined.Event, null) }, onClick = {
            onDismiss(); onPickDate(today.plusWeeks(1).toString())
        })
        DropdownMenuItem(text = { Text("Выбрать дату…") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }, onClick = {
            onDismiss(); onCustomDate()
        })
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (time == null) "Время…" else "Время: ${Due.time(time)}") },
            leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
            onClick = { onDismiss(); onPickTime() }
        )
        if (time != null) {
            DropdownMenuItem(text = { Text("Убрать время") }, leadingIcon = { Icon(Icons.Outlined.Alarm, null) }, onClick = {
                onDismiss(); onClearTime()
            })
        }
        if (date != null) {
            DropdownMenuItem(text = { Text("Убрать срок") }, leadingIcon = { Icon(Icons.Outlined.EventBusy, null) }, onClick = {
                onDismiss(); onClear()
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
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Priority.entries.forEach { p ->
                val selected = p == item.priority
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(priorityColor(p))
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable(onClickLabel = "Важность: ${priorityName(p)}") {
                            onDismiss()
                            vm.setPriority(tab.id, item.id, p)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider()
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
