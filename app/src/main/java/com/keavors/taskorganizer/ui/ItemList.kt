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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
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
import com.keavors.taskorganizer.ui.theme.ItemLook
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.TopBarLook
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

    // The list stays between the bars, so the bars can show the background of the design.
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(design.itemGap),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .notebookMargin(design.item == ItemLook.RULED)
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
                    Row(
                        Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button) { showDone = !showDone }.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(if (showDone) design.icons.expandLess else design.icons.expandMore, null, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(labelText("Выполнено · ${done.size}"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    AppButton("Очистить", onClick = { vm.clearCompleted(tab.id) })
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
    val d = LocalDesign.current
    val h = d.header
    val colors = MaterialTheme.colorScheme
    val base = when (h.size) {
        HeaderSize.TITLE -> MaterialTheme.typography.titleLarge
        HeaderSize.HEADLINE -> MaterialTheme.typography.headlineSmall
        HeaderSize.LARGE -> MaterialTheme.typography.headlineMedium
    }
    // The notebook design writes the title right of the margin.
    val start = if (d.item == ItemLook.RULED) 54.dp else 4.dp
    Column(Modifier.padding(start = start, top = 4.dp, bottom = 10.dp)) {
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
                    letterSpacing = if (h.uppercase) 1.5.sp else base.letterSpacing
                ).glowing(),
                color = if (h.accent) colors.primary else colors.onSurface
            )
        }
        val subtitle = if (tab.kind == TabKind.TASKS) {
            listOfNotNull(activeCount(open), if (done > 0) doneCount(done) else null).joinToString(" · ")
        } else notesCount(open)
        Text(
            if (d.topBar == TopBarLook.TERMINAL) "// $subtitle" else subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(tab: Tab) {
    val d = LocalDesign.current
    Column(
        Modifier.fillMaxWidth().padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                if (tab.kind == TabKind.TASKS) d.icons.tasks else d.icons.book,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 32.dp
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
    val focus = LocalFocusManager.current
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
            // Tap anywhere on the entry = its menu, hold = edit the text. Either ends typing elsewhere.
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = !editing,
                        onClickLabel = "Меню",
                        onLongClickLabel = "Изменить текст",
                        onClick = {
                            focus.clearFocus()
                            menu = true
                        },
                        onLongClick = {
                            focus.clearFocus()
                            editing = true
                        }
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
                    Spacer(Modifier.width(if (design.item == ItemLook.RULED) 50.dp else 14.dp))
                }
                if (design.item == ItemLook.RULED && isTask) Spacer(Modifier.width(2.dp))
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
                            item.dueDate?.let { DueBadge(it, item.dueTime, item.done) { dueMenu = true } }
                            if (item.source == ItemSource.VOICE) {
                                if (item.dueDate != null) Spacer(Modifier.width(8.dp))
                                Icon(design.icons.voice, "Надиктовано", Modifier.size(14.dp), tint = colors.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Box {
                    PriorityMark(item.priority) { vm.setPriority(tab.id, item.id, item.priority.next()) }
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
                        design.icons.drag,
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
    val icons = LocalDesign.current.icons
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Triple(colors.errorContainer, icons.delete, Alignment.CenterEnd)
        SwipeToDismissBoxValue.StartToEnd -> Triple(colors.primaryContainer, if (done) icons.noDate else icons.check, Alignment.CenterStart)
        SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.Center)
    }
    Box(
        Modifier.fillMaxSize().clip(LocalDesign.current.itemShape).background(color).padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (icon != null) AppIcon(icon, null, tint = if (direction == SwipeToDismissBoxValue.EndToStart) colors.onErrorContainer else colors.onPrimaryContainer)
    }
}

/** Due date (and time) under the text; tap = change it. */
@Composable
private fun DueBadge(date: String, time: String?, done: Boolean, onClick: () -> Unit) {
    val d = LocalDesign.current
    val (text, tone) = Due.label(date, time)
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        done -> colors.surfaceContainerHigh to colors.onSurfaceVariant
        tone == DueTone.OVERDUE -> colors.errorContainer to colors.onErrorContainer
        tone == DueTone.TODAY -> colors.primaryContainer to colors.onPrimaryContainer
        else -> colors.surfaceContainerHigh to colors.onSurfaceVariant
    }
    when (d.topBar) {
        TopBarLook.TERMINAL, TopBarLook.PAPER -> {
            // Plain text: "@ сегодня, 14:00" in the terminal, a handwritten note in the notebook.
            val ink = if (tone == DueTone.OVERDUE && !done) colors.error else if (tone == DueTone.TODAY && !done) colors.primary else colors.onSurfaceVariant
            Text(
                if (d.topBar == TopBarLook.TERMINAL) "@ $text" else "до: $text",
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(onClick = onClick).padding(vertical = 2.dp)
            )
        }
        else -> Row(
            Modifier.clip(d.chipShape).background(bg).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (time != null) d.icons.alarm else d.icons.calendar, null, Modifier.size(13.dp), tint = fg)
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
        }
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
private fun MenuItem(text: String, icon: ImageVector, color: Color = Color.Unspecified, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color) },
        leadingIcon = { Icon(icon, null, tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color) },
        onClick = onClick
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
    val icons = LocalDesign.current.icons
    val today = LocalDate.now()
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuItem("Сегодня", icons.today) { onDismiss(); onPickDate(today.toString()) }
        MenuItem("Завтра", icons.event) { onDismiss(); onPickDate(today.plusDays(1).toString()) }
        MenuItem("Через неделю", icons.event) { onDismiss(); onPickDate(today.plusWeeks(1).toString()) }
        MenuItem("Выбрать дату…", icons.calendar) { onDismiss(); onCustomDate() }
        HorizontalDivider()
        MenuItem(if (time == null) "Время…" else "Время: ${Due.time(time)}", icons.time) { onDismiss(); onPickTime() }
        if (time != null) MenuItem("Убрать время", icons.alarm) { onDismiss(); onClearTime() }
        if (date != null) MenuItem("Убрать срок", icons.noDate) { onDismiss(); onClear() }
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
    val icons = LocalDesign.current.icons
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
                    if (selected) Icon(icons.check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider()
        MenuItem("Изменить", icons.edit) { onDismiss(); onEdit() }
        MenuItem("Срок", icons.calendar) { onDismiss(); onDue() }
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
                MenuItem(target.title, icons.move) { onDismiss(); vm.moveItemToTab(tab.id, item.id, target.id) }
            }
        }
        HorizontalDivider()
        MenuItem("Удалить", icons.delete, MaterialTheme.colorScheme.error) { onDismiss(); vm.deleteItems(tab.id, listOf(item.id)) }
    }
}
