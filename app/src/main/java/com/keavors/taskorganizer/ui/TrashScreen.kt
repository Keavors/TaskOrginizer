package com.keavors.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.TrashedItem
import com.keavors.taskorganizer.data.TrashedTab
import com.keavors.taskorganizer.ui.theme.LocalDesign

@Composable
fun TrashScreen(vm: MainViewModel, data: AppData, snackbar: SnackbarHostState) {
    var confirmEmpty by remember { mutableStateOf(false) }
    ScreenScaffold(
        "Корзина",
        onBack = { vm.screen = Screen.MAIN },
        snackbar = snackbar,
        spacing = 8.dp,
        actions = { if (data.trash.isNotEmpty()) AppButton("Очистить", onClick = { confirmEmpty = true }) }
    ) {
        item {
            Text(
                if (data.trash.isEmpty()) "Корзина пуста." else "Удалённое хранится 30 дней, потом исчезает навсегда.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(data.trash, key = { it.id }) { entry ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (entry is TrashedTab) LocalDesign.current.icons.folder else LocalDesign.current.icons.book,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        val (title, meta) = when (entry) {
                            is TrashedTab -> entry.tab.title to "вкладка, ${entriesCount(entry.items.size)}"
                            is TrashedItem -> entry.item.text to "из «${entry.tabTitle}»"
                        }
                        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 3)
                        Text(
                            "$meta · ${ago(entry.deletedAt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AppIconButton(LocalDesign.current.icons.restore, "Восстановить") { vm.restoreFromTrash(entry) }
                    AppIconButton(LocalDesign.current.icons.deleteForever, "Удалить навсегда") { vm.deleteFromTrash(entry) }
                }
            }
        }
    }
    if (confirmEmpty) {
        ConfirmDialog(
            title = "Очистить корзину?",
            text = "Всё содержимое корзины удалится навсегда.",
            confirmLabel = "Очистить",
            onDismiss = { confirmEmpty = false }
        ) { vm.emptyTrash() }
    }
}
