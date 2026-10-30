package com.keavors.taskorganizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.Tab
import com.keavors.taskorganizer.ui.theme.tabColor

private fun norm(s: String) = s.lowercase().replace('ё', 'е')

private sealed interface Hit {
    val tab: Tab

    data class TabHit(override val tab: Tab) : Hit
    data class ItemHit(override val tab: Tab, val item: Item) : Hit
}

private fun search(data: AppData, query: String): List<Hit> {
    val needle = norm(query.trim())
    if (needle.isEmpty()) return emptyList()
    val tabs = data.tabs.filter { norm(it.title).contains(needle) }.map { Hit.TabHit(it) }
    val items = data.tabs.flatMap { tab -> data.itemsOf(tab.id).filter { norm(it.text).contains(needle) }.map { Hit.ItemHit(tab, it) } }
    return (tabs + items).take(200)
}

/** Highlights every match; "е"/"ё" and case are ignored (both keep the string length). */
@Composable
private fun highlighted(text: String, query: String): AnnotatedString {
    val needle = norm(query.trim())
    val hay = norm(text)
    val style = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.SemiBold)
    return buildAnnotatedString {
        var from = 0
        if (needle.isNotEmpty()) {
            var at = hay.indexOf(needle)
            while (at >= 0) {
                append(text.substring(from, at))
                pushStyle(style)
                append(text.substring(at, at + needle.length))
                pop()
                from = at + needle.length
                at = hay.indexOf(needle, from)
            }
        }
        append(text.substring(from))
    }
}

@Composable
fun SearchScreen(vm: MainViewModel, data: AppData) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val hits = remember(data, query) { search(data, query) }
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.screen = Screen.MAIN }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh, modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.weight(1f).padding(horizontal = 18.dp, vertical = 14.dp).focusRequester(focus),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) Text("Поиск по всем вкладкам", color = colors.onSurfaceVariant)
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Очистить") }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
            if (query.isNotBlank() && hits.isEmpty()) {
                item { Text("Ничего не найдено", color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            }
            items(hits, key = { if (it is Hit.ItemHit) it.item.id else "tab-" + it.tab.id }) { hit ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            vm.selectTab(hit.tab.id)
                            vm.highlightItemId = (hit as? Hit.ItemHit)?.item?.id
                            vm.screen = Screen.MAIN
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (hit) {
                        is Hit.TabHit -> {
                            Icon(Icons.Outlined.Folder, null, tint = colors.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            Text(highlighted(hit.tab.title, query), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("вкладка", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        }
                        is Hit.ItemHit -> {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(hit.tab.color?.let { tabColor(it) } ?: colors.outlineVariant)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    highlighted(hit.item.text, query),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (hit.item.done) TextDecoration.LineThrough else null,
                                    maxLines = 3
                                )
                                Text(hit.tab.title, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
