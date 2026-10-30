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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.Tab
import com.keavors.taskorganizer.data.TabColor
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.TabsLook
import com.keavors.taskorganizer.ui.theme.TopBarLook
import com.keavors.taskorganizer.ui.theme.tabColor

private val COLOR_NAMES = mapOf(
    TabColor.BLUE to "Синий", TabColor.VIOLET to "Фиолетовый", TabColor.PINK to "Розовый",
    TabColor.RED to "Красный", TabColor.ORANGE to "Оранжевый", TabColor.AMBER to "Жёлтый",
    TabColor.GREEN to "Зелёный", TabColor.TEAL to "Бирюзовый", TabColor.GRAY to "Серый"
)

fun openCount(tab: Tab, data: AppData): Int {
    val list = data.itemsOf(tab.id)
    return if (tab.kind == TabKind.TASKS) list.count { !it.done } else list.size
}

/** App title, search, menu and the row of tabs. Transparent: the design's background shows through. */
@Composable
fun TopBar(vm: MainViewModel, data: AppData) {
    val d = LocalDesign.current
    val c = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val terminal = d.topBar == TopBarLook.TERMINAL
    Column(Modifier.statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = if (d.topBar == TopBarLook.COMPACT) 0.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTitle(Modifier.weight(1f))
            if (terminal) {
                TerminalAction("[поиск]") { vm.screen = Screen.SEARCH }
            } else {
                AppIconButton(d.icons.search, "Поиск", tint = c.onSurface) { vm.screen = Screen.SEARCH }
            }
            Box {
                if (terminal) TerminalAction("[≡]") { menu = true }
                else AppIconButton(d.icons.more, "Ещё", tint = c.onSurface) { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (data.trash.isEmpty()) "Корзина" else "Корзина (${data.trash.size})") },
                        leadingIcon = { Icon(d.icons.trash, null) },
                        onClick = { menu = false; vm.screen = Screen.TRASH }
                    )
                    DropdownMenuItem(
                        text = { Text("Настройки") },
                        leadingIcon = { Icon(d.icons.settings, null) },
                        onClick = { menu = false; vm.screen = Screen.SETTINGS }
                    )
                }
            }
        }
        if (d.topBar == TopBarLook.STRICT) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 2.dp, color = c.onSurface)
        }
        TabsRow(vm, data)
    }
}

@Composable
private fun TerminalAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabsRow(vm: MainViewModel, data: AppData) {
    val listState = rememberLazyListState()
    var newTab by remember { mutableStateOf(false) }
    val activeIndex = data.tabs.indexOfFirst { it.id == data.activeTab.id }

    LaunchedEffect(activeIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == activeIndex && it.offset >= 0 && it.offset + it.size <= listState.layoutInfo.viewportEndOffset }) {
            listState.animateScrollToItem(activeIndex.coerceAtLeast(0))
        }
    }

    val look = LocalDesign.current.tabs
    val gap = when (look) {
        TabsLook.UNDERLINE, TabsLook.BRACKETS -> 4.dp
        TabsLook.BOOKMARKS -> 2.dp
        else -> 8.dp
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = if (look == TabsLook.BOOKMARKS) 0.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = if (look == TabsLook.BOOKMARKS) Alignment.Bottom else Alignment.CenterVertically
    ) {
        itemsIndexed(data.tabs, key = { _, tab -> tab.id }) { index, tab ->
            TabChip(vm, data, tab, index, Modifier.animateItem())
        }
        item(key = "add") { AddTabButton { newTab = true } }
    }

    if (newTab) {
        NewTabDialog(onDismiss = { newTab = false }) { title, kind ->
            newTab = false
            vm.addTab(title, kind)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabChip(vm: MainViewModel, data: AppData, tab: Tab, index: Int, modifier: Modifier) {
    val selected = tab.id == data.activeTab.id
    val count = openCount(tab, data)
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val icons = LocalDesign.current.icons

    Box(modifier) {
        TabLabel(
            title = tab.title,
            selected = selected,
            dot = tab.color?.let { tabColor(it) },
            count = count,
            onClick = { vm.selectTab(tab.id) },
            onLongClick = {
                vm.selectTab(tab.id)
                menu = true
            }
        )

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Переименовать") }, leadingIcon = { Icon(icons.edit, null) }, onClick = {
                menu = false
                rename = true
            })
            HorizontalDivider()
            KindItem("Задачи с галочками", icons.tasks, tab.kind == TabKind.TASKS) {
                menu = false
                vm.setTabKind(tab.id, TabKind.TASKS)
            }
            KindItem("Заметки", icons.book, tab.kind == TabKind.NOTES) {
                menu = false
                vm.setTabKind(tab.id, TabKind.NOTES)
            }
            HorizontalDivider()
            ColorRow(tab.color) {
                menu = false
                vm.setTabColor(tab.id, it)
            }
            HorizontalDivider()
            if (index > 0) {
                DropdownMenuItem(
                    text = { Text("Сдвинуть влево") },
                    leadingIcon = { Icon(icons.left, null) },
                    onClick = { menu = false; vm.moveTab(index, index - 1) }
                )
            }
            if (index < data.tabs.lastIndex) {
                DropdownMenuItem(
                    text = { Text("Сдвинуть вправо") },
                    leadingIcon = { Icon(icons.right, null) },
                    onClick = { menu = false; vm.moveTab(index, index + 1) }
                )
            }
            DropdownMenuItem(
                text = { Text(if (data.tabs.size > 1) "Удалить вкладку" else "Единственную вкладку удалить нельзя") },
                leadingIcon = { Icon(icons.delete, null) },
                enabled = data.tabs.size > 1,
                onClick = {
                    menu = false
                    if (data.itemsOf(tab.id).isEmpty()) vm.deleteTab(tab.id) else confirmDelete = true
                }
            )
        }
    }

    if (rename) {
        TextInputDialog(title = "Переименовать вкладку", initial = tab.title, label = "Название", onDismiss = { rename = false }) {
            rename = false
            vm.renameTab(tab.id, it)
        }
    }
    if (confirmDelete) {
        val n = data.itemsOf(tab.id).size
        ConfirmDialog(
            title = "Удалить «${tab.title}»?",
            text = "Вместе с вкладкой в корзину попадут ${entriesCount(n)}. Восстановить можно в течение 30 дней.",
            confirmLabel = "Удалить",
            onDismiss = { confirmDelete = false }
        ) { vm.deleteTab(tab.id) }
    }
}

@Composable
private fun KindItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = { if (checked) Icon(LocalDesign.current.icons.check, null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick
    )
}

@Composable
private fun ColorRow(current: TabColor?, onPick: (TabColor?) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("Цвет", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(8.dp))
        val rows = listOf(listOf<TabColor?>(null) + TabColor.entries.take(4), TabColor.entries.drop(4))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { color ->
                    val selected = color == current
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color?.let { tabColor(it) } ?: MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { onPick(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == null) Text("—", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text(
            current?.let { COLOR_NAMES[it] } ?: "Без цвета",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
