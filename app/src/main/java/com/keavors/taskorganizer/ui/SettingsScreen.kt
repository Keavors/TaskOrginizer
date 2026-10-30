package com.keavors.taskorganizer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.BuildConfigInfo
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.NewItemPosition
import com.keavors.taskorganizer.data.ThemeMode
import com.keavors.taskorganizer.speech.DownloadState
import com.keavors.taskorganizer.speech.EngineState
import com.keavors.taskorganizer.speech.ModelFiles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Screen frame with a back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(title: String, subtitle: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, data: AppData, snackbar: SnackbarHostState) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val speech = settings.speech
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmImport = uri
    }

    ScreenScaffold("Настройки", onBack = { vm.screen = Screen.MAIN }, snackbar = snackbar) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SettingsSection("Голосовой ввод") {
                    ModelCard(vm)
                    HorizontalDivider()
                    ChoiceRow(
                        "Язык речи",
                        if (speech.language == "ru") "Русский: английские названия пишутся латиницей" else null,
                        listOf("ru" to "Русский", "auto" to "Авто", "en" to "English"),
                        speech.language
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(language = v)) } }
                    SwitchRow(
                        "Быстрый режим",
                        "Короткие фразы распознаются в разы быстрее. Если появятся ошибки — выключите.",
                        speech.fastMode
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(fastMode = v)) } }
                    SwitchRow(
                        "Сразу добавлять надиктованное",
                        "Без проверки текста в поле ввода",
                        speech.autoSubmit
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(autoSubmit = v)) } }
                    ChoiceRow(
                        "Держать модель в памяти",
                        "После того как приложение свёрнуто. Пока модель в памяти, диктовка начинается мгновенно.",
                        listOf(1 to "1 мин", 10 to "10 мин", 60 to "1 час", -1 to "Всегда"),
                        speech.keepLoadedMinutes
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(keepLoadedMinutes = v)) } }
                    NavRow(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        "Словарь",
                        if (data.dictionary.isEmpty()) "Названия, которые модель должна писать правильно" else "Слов: ${data.dictionary.size}"
                    ) { vm.screen = Screen.DICTIONARY }
                }
            }
            item {
                SettingsSection("Оформление") {
                    ChoiceRow(
                        "Тема",
                        null,
                        listOf(ThemeMode.SYSTEM to "Как в системе", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная"),
                        settings.theme
                    ) { v -> vm.updateSettings { it.copy(theme = v) } }
                    SwitchRow("Цвета из обоев", "Material You: оттенки берутся из обоев телефона", settings.dynamicColor) { v ->
                        vm.updateSettings { it.copy(dynamicColor = v) }
                    }
                    ChoiceRow(
                        "Новые записи",
                        null,
                        listOf(NewItemPosition.TOP to "Сверху", NewItemPosition.BOTTOM to "Снизу"),
                        settings.newItemPosition
                    ) { v -> vm.updateSettings { it.copy(newItemPosition = v) } }
                }
            }
            item {
                SettingsSection("Данные") {
                    NavRow(Icons.Outlined.DeleteSweep, "Корзина", if (data.trash.isEmpty()) "Пусто" else "Удалённых: ${data.trash.size}") {
                        vm.screen = Screen.TRASH
                    }
                    NavRow(Icons.Outlined.FileUpload, "Сохранить копию в файл", "Все вкладки, записи и словарь") {
                        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("Органайзер задач $day.json")
                    }
                    NavRow(Icons.Outlined.FileDownload, "Загрузить из файла", "Заменит текущие данные") {
                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    }
                }
            }
            item {
                SettingsSection("О приложении") {
                    Column(Modifier.padding(16.dp)) {
                        Text("Органайзер задач ${BuildConfigInfo.versionName(vm.getApplication())}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Распознавание речи: ${ModelFiles.MODEL_TITLE} через whisper.cpp, полностью на телефоне. " +
                                "VAD: Silero. Каждый день делается резервная копия данных (хранятся последние 14).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        vm.services.engine.systemInfo?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("Процессор: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    confirmImport?.let { uri ->
        ConfirmDialog(
            title = "Загрузить данные из файла?",
            text = "Все текущие вкладки и записи заменятся данными из файла. Текущие данные перед этим сохранятся в резервную копию.",
            confirmLabel = "Загрузить",
            onDismiss = { confirmImport = null }
        ) { vm.importFrom(uri) }
    }
}

@Composable
private fun ModelCard(vm: MainViewModel) {
    val engine by vm.services.engine.state.collectAsStateWithLifecycle()
    val download by vm.services.downloader.state.collectAsStateWithLifecycle()
    val last by vm.services.engine.lastRun.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp)) {
        Text(ModelFiles.MODEL_TITLE, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "874 МБ · работает без интернета · пишет английские названия латиницей",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        when {
            engine == EngineState.NotInstalled -> when (val d = download) {
                is DownloadState.Downloading -> {
                    Text("Скачано ${formatMb(d.received)} из ${formatMb(d.total)}" + if (d.bytesPerSecond > 0) " · ${formatMb(d.bytesPerSecond)}/с" else "")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { d.received / d.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { vm.cancelDownload() }) { Text("Отмена") }
                }
                is DownloadState.Verifying -> {
                    Text("Проверяю файл…")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { d.checked / d.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    if (d is DownloadState.Failed) {
                        Text(d.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(onClick = { vm.downloadModel() }) {
                        Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Скачать модель")
                    }
                }
            }
            else -> {
                val status = when (engine) {
                    EngineState.Loading -> "Загружается в память…"
                    EngineState.Ready, EngineState.Busy -> "Готова, загружена в память"
                    EngineState.Unloaded -> "Установлена · загрузится при диктовке"
                    is EngineState.Failed -> (engine as EngineState.Failed).message
                    else -> ""
                }
                Text(status, color = if (engine is EngineState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                last?.let {
                    Text(
                        "Последняя фраза: ${formatSeconds(it.audioSeconds)} речи распознаны за ${formatSeconds(it.processingMs / 1000.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Удалить модель") }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить модель?",
            text = "Освободится 874 МБ. Голосовой ввод перестанет работать, пока вы не скачаете модель заново.",
            confirmLabel = "Удалить",
            onDismiss = { confirmDelete = false }
        ) { vm.deleteModel() }
    }
}
