package com.keavors.taskorganizer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.BuildConfigInfo
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.NewItemPosition
import com.keavors.taskorganizer.data.ThemeMode
import com.keavors.taskorganizer.speech.RecognitionServices
import com.keavors.taskorganizer.speech.speechLanguageTag
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
private fun NavRow(icon: ImageVector?, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
        }
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
                    ServiceRow(vm, speech.service)
                    HorizontalDivider()
                    ChoiceRow(
                        "Язык речи",
                        null,
                        listOf("ru-RU" to "Русский", "en-US" to "English"),
                        speechLanguageTag(speech.language)
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(language = v)) } }
                    SwitchRow(
                        "Только без интернета",
                        "Речь распознаётся на телефоне и никуда не отправляется",
                        speech.offlineOnly
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(offlineOnly = v)) } }
                    SwitchRow(
                        "Сразу добавлять надиктованное",
                        "Без проверки текста в поле ввода",
                        speech.autoSubmit
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(autoSubmit = v)) } }
                    NavRow(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        "Словарь",
                        if (data.dictionary.isEmpty()) "Как писать английские названия: «гитхаб» → GitHub" else "Слов: ${data.dictionary.size}"
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
                            "Распознавание речи — встроенная служба телефона, та же, что у голосового ввода. " +
                                "Каждый день делается резервная копия данных (хранятся последние 14).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

/** Which of the phone's speech recognizers dictation uses. */
@Composable
private fun ServiceRow(vm: MainViewModel, selected: String) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    // Read again whenever the picker opens or closes: services can be installed or removed meanwhile.
    val options = remember(picking) { RecognitionServices.options(context) }
    val systemDefault = remember(picking) { RecognitionServices.systemDefault(context) }
    val defaultLabel = systemDefault?.let { "Как в телефоне: ${it.label}" } ?: "Как в телефоне"
    val current = if (selected.isEmpty()) defaultLabel
    else options.firstOrNull { it.id == selected }?.label ?: "$defaultLabel (выбранная служба не найдена)"
    val pick: (String) -> Unit = { id ->
        picking = false
        vm.updateSettings { it.copy(speech = it.speech.copy(service = id)) }
    }

    NavRow(null, "Служба распознавания", current) { picking = true }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Служба распознавания") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Обычно это Google или Samsung. Если без интернета ничего не распознаётся, выберите другую.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ServiceOption(defaultLabel, "Служба по умолчанию в настройках Android", selected.isEmpty()) { pick("") }
                    options.forEach { o -> ServiceOption(o.label, o.detail, o.id == selected) { pick(o.id) } }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun ServiceOption(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
