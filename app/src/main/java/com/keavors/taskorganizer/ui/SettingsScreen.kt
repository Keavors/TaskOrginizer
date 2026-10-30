package com.keavors.taskorganizer.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.BuildConfigInfo
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.DesignStyle
import com.keavors.taskorganizer.data.ItemSort
import com.keavors.taskorganizer.data.NewItemPosition
import com.keavors.taskorganizer.data.Palette
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.ReminderSettings
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.ThemeMode
import com.keavors.taskorganizer.reminders.ReminderNotifier
import com.keavors.taskorganizer.reminders.ReminderText
import com.keavors.taskorganizer.speech.RecognitionServices
import com.keavors.taskorganizer.speech.speechLanguageTag
import com.keavors.taskorganizer.ui.theme.ButtonLook
import com.keavors.taskorganizer.ui.theme.LocalDesign
import com.keavors.taskorganizer.ui.theme.TaskOrganizerTheme
import com.keavors.taskorganizer.ui.theme.isDarkTheme
import com.keavors.taskorganizer.ui.theme.paletteAccent
import com.keavors.taskorganizer.ui.theme.paletteName
import com.keavors.taskorganizer.ui.theme.priorityColor
import com.keavors.taskorganizer.ui.theme.styleName
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

/** Screen frame with a back arrow and the design's background. Content stays above the navigation buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val d = LocalDesign.current
    ScreenBackground {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(labelText(title), style = MaterialTheme.typography.titleLarge.glowing()) },
                    navigationIcon = { AppIconButton(d.icons.back, "Назад", tint = MaterialTheme.colorScheme.onSurface, onClick = onBack) },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                )
            },
            snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
            containerColor = Color.Transparent,
            content = content
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        AppSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: ImageVector?, title: String, subtitle: String?, onClick: () -> Unit) {
    val d = LocalDesign.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AppIcon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (d.button == ButtonLook.BRACKETS) {
            Text(">", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(d.icons.chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A warning with a button: something Android has to allow. */
@Composable
private fun ActionRow(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        AppButton(action, onClick = onClick, kind = ButtonKind.PRIMARY)
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
        AppSegmented(options, selected, onSelect)
    }
}

/** A title and chips that wrap to the next line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsRow(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, data: AppData, snackbar: SnackbarHostState) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val speech = settings.speech
    val icons = LocalDesign.current.icons
    var confirmImport by remember { mutableStateOf<Uri?>(null) }

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
                SettingsSection("Оформление") {
                    StylePicker(settings) { v -> vm.updateSettings { it.copy(style = v) } }
                    PalettePicker(settings) { v -> vm.updateSettings { it.copy(palette = v) } }
                    ChoiceRow(
                        "Тема",
                        listOf(ThemeMode.SYSTEM to "Авто", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная"),
                        settings.theme
                    ) { v -> vm.updateSettings { it.copy(theme = v) } }
                    SwitchRow("Чёрный фон", null, settings.pureBlack) { v -> vm.updateSettings { it.copy(pureBlack = v) } }
                }
            }
            item {
                SettingsSection("Голосовой ввод") {
                    ServiceRow(vm, speech.service)
                    ChoiceRow(
                        "Язык речи",
                        listOf("ru-RU" to "Русский", "en-US" to "English"),
                        speechLanguageTag(speech.language)
                    ) { v -> vm.updateSettings { it.copy(speech = it.speech.copy(language = v)) } }
                    SwitchRow("Только без интернета", null, speech.offlineOnly) { v ->
                        vm.updateSettings { it.copy(speech = it.speech.copy(offlineOnly = v)) }
                    }
                    SwitchRow("Сразу добавлять надиктованное", null, speech.autoSubmit) { v ->
                        vm.updateSettings { it.copy(speech = it.speech.copy(autoSubmit = v)) }
                    }
                    NavRow(icons.book, "Словарь", if (data.dictionary.isEmpty()) null else "Слов: ${data.dictionary.size}") {
                        vm.screen = Screen.DICTIONARY
                    }
                }
            }
            item {
                SettingsSection("Список") {
                    ChoiceRow(
                        "Сортировка",
                        listOf(ItemSort.ADDED to "По дате", ItemSort.PRIORITY to "По важности"),
                        settings.sort
                    ) { v -> vm.updateSettings { it.copy(sort = v) } }
                    ChoiceRow(
                        "Новые записи",
                        listOf(NewItemPosition.TOP to "Сверху", NewItemPosition.BOTTOM to "Снизу"),
                        settings.newItemPosition
                    ) { v -> vm.updateSettings { it.copy(newItemPosition = v) } }
                }
            }
            item {
                SettingsSection("Напоминания") {
                    ReminderSettingsRows(settings.reminders) { change -> vm.updateSettings { it.copy(reminders = change(it.reminders)) } }
                }
            }
            item {
                SettingsSection("Данные") {
                    NavRow(icons.trash, "Корзина", if (data.trash.isEmpty()) null else "Удалённых: ${data.trash.size}") {
                        vm.screen = Screen.TRASH
                    }
                    NavRow(icons.upload, "Сохранить копию в файл", null) {
                        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("Органайзер задач $day.json")
                    }
                    NavRow(icons.download, "Загрузить из файла", "Заменит текущие данные") {
                        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    }
                }
            }
            item {
                SettingsSection("О приложении") {
                    Text(
                        "Органайзер задач ${BuildConfigInfo.versionName(vm.getApplication())}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
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

// ---------------------------------------------------------------------- voice

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
    else options.firstOrNull { it.id == selected }?.label ?: "$defaultLabel (выбранная не найдена)"
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
                    RadioOption(defaultLabel, null, selected.isEmpty()) { pick("") }
                    options.forEach { o -> RadioOption(o.label, o.detail, o.id == selected) { pick(o.id) } }
                }
            },
            confirmButton = { AppButton("Закрыть", onClick = { picking = false }) }
        )
    }
}

@Composable
private fun RadioOption(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppRadio(selected)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------- reminders

private val OFFSETS = listOf(0, 5, 10, 15, 30, 60, 120, 24 * 60, 2 * 24 * 60, 7 * 24 * 60)
private val REPEATS = listOf(0, 5, 10, 15, 30, 60)
private val REPEAT_COUNTS = listOf(1, 2, 3, 5, 10, 0)
private val SNOOZES = listOf(5, 10, 15, 30, 60, 180)
private val ALL_DAY_TIMES = listOf(-1, 8 * 60, 9 * 60, 12 * 60, 18 * 60)

private fun offsetLabel(minutes: Int): String = if (minutes == 0) "В срок" else "За " + ReminderText.duration(minutes.toLong())

private fun countLabel(n: Int): String = if (n == 0) "Пока не выполню" else "$n ${plural(n, "раз", "раза", "раз")}"

private fun clockLabel(minuteOfDay: Int): String = "%d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

@Composable
private fun ReminderSettingsRows(r: ReminderSettings, update: ((ReminderSettings) -> ReminderSettings) -> Unit) {
    var customOffset by remember { mutableStateOf(false) }
    var allDayPicker by remember { mutableStateOf(false) }

    SwitchRow("Напоминания", null, r.enabled) { v -> update { it.copy(enabled = v) } }
    if (!r.enabled) return
    ReminderAccessRows()
    ChipsRow("Когда") {
        (OFFSETS + r.offsets).distinct().sorted().forEach { m ->
            val on = m in r.offsets
            AppChip(offsetLabel(m), on) { update { it.copy(offsets = if (on) it.offsets - m else (it.offsets + m).distinct().sorted()) } }
        }
        AppChip("Своё…", false) { customOffset = true }
    }
    ChipsRow("Повторять после срока") {
        REPEATS.forEach { m ->
            AppChip(if (m == 0) "Нет" else "Каждые " + ReminderText.duration(m.toLong()), r.repeatMinutes == m) { update { it.copy(repeatMinutes = m) } }
        }
    }
    if (r.repeatMinutes > 0) {
        ChipsRow("Сколько раз") {
            REPEAT_COUNTS.forEach { n -> AppChip(countLabel(n), r.repeatCount == n) { update { it.copy(repeatCount = n) } } }
        }
    }
    ChipsRow("Отложить на") {
        SNOOZES.forEach { m -> AppChip(ReminderText.duration(m.toLong()), r.snoozeMinutes == m) { update { it.copy(snoozeMinutes = m) } } }
    }
    ChipsRow("Задачи без времени") {
        (ALL_DAY_TIMES + r.allDayMinute).distinct().forEach { m ->
            AppChip(if (m < 0) "Не напоминать" else "В " + clockLabel(m), r.allDayMinute == m) { update { it.copy(allDayMinute = m) } }
        }
        AppChip("Другое…", false) { allDayPicker = true }
    }
    ChipsRow("Для задач") {
        AppChip("Все", r.minPriority == Priority.NORMAL, priorityColor(Priority.NORMAL)) { update { it.copy(minPriority = Priority.NORMAL) } }
        AppChip("Жёлтые и красные", r.minPriority == Priority.MEDIUM, priorityColor(Priority.MEDIUM)) { update { it.copy(minPriority = Priority.MEDIUM) } }
        AppChip("Только красные", r.minPriority == Priority.HIGH, priorityColor(Priority.HIGH)) { update { it.copy(minPriority = Priority.HIGH) } }
    }
    SoundRow()
    SwitchRow("Пропущенные", "Показать после включения телефона", r.showMissed) { v -> update { it.copy(showMissed = v) } }
    SwitchRow("Режим будильника", "Точно в срок, со значком будильника", r.alarmClock) { v -> update { it.copy(alarmClock = v) } }

    if (customOffset) {
        MinutesDialog("Напомнить заранее", onDismiss = { customOffset = false }) { m ->
            update { it.copy(offsets = (it.offsets + m).distinct().sorted()) }
        }
    }
    if (allDayPicker) {
        val initial = if (r.allDayMinute >= 0) LocalTime.of(r.allDayMinute / 60, r.allDayMinute % 60) else LocalTime.of(9, 0)
        TimePickerDialog(initial = initial, onDismiss = { allDayPicker = false }) { t ->
            update { it.copy(allDayMinute = t.hour * 60 + t.minute) }
        }
    }
}

/** Sound and vibration of each level live in Android's channel settings. */
@Composable
private fun SoundRow() {
    val context = LocalContext.current
    ChipsRow("Звук и вибрация") {
        listOf(Priority.NORMAL to "Обычные", Priority.MEDIUM to "Жёлтые", Priority.HIGH to "Красные").forEach { (p, label) ->
            AppChip(label, false, priorityColor(p)) {
                val intent = Intent(SystemSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(SystemSettings.EXTRA_CHANNEL_ID, ReminderNotifier.channelId(p))
                runCatching { context.startActivity(intent) }
            }
        }
    }
}

/** What Android must allow for reminders to arrive on time; shown only when something is missing. */
@Composable
private fun ReminderAccessRows() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val notificationsOn = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactOn = remember(tick) {
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() != false
    }
    val unrestricted = remember(tick) {
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) != false
    }

    if (!notificationsOn) {
        ActionRow("Уведомления выключены", "Включить") {
            val canAsk = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (canAsk && !asked) {
                asked = true
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                open(context, Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(SystemSettings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }
    }
    if (!exactOn && Build.VERSION.SDK_INT >= 31) {
        ActionRow("Точное время запрещено", "Разрешить") {
            open(context, Intent(SystemSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)))
        }
    }
    if (!unrestricted) {
        ActionRow("Фоновая работа ограничена", "Снять") {
            open(context, Intent(SystemSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName)))
        }
    }
}

private fun open(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}

// ---------------------------------------------------------------------- appearance

/** Miniatures of every style in the current colors: font, icons, buttons, tabs and entries. */
@Composable
private fun StylePicker(settings: Settings, onPick: (DesignStyle) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Стиль", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(DesignStyle.entries) { style ->
                val selected = style == settings.style
                val frame = RoundedCornerShape(16.dp)
                Column(
                    Modifier.width(132.dp).clip(frame).clickable { onPick(style) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .width(132.dp)
                            .height(196.dp)
                            .clip(frame)
                            .border(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, frame)
                    ) {
                        TaskOrganizerTheme(settings.copy(style = style)) { StyleSample() }
                    }
                    Spacer(Modifier.height(6.dp))
                    TaskOrganizerTheme(settings.copy(style = style)) {
                        Text(
                            styleName(style),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else null,
                            color = if (selected) colors.primary else colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePicker(settings: Settings, onPick: (Palette) -> Unit) {
    val dark = isDarkTheme(settings)
    val palettes = Palette.entries.filter { it != Palette.WALLPAPER || Build.VERSION.SDK_INT >= 31 }
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Цвет", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(palettes) { palette ->
                val selected = palette == settings.palette
                val accent = paletteAccent(palette, dark)
                val fill = if (palette == Palette.WALLPAPER) {
                    Modifier.background(Brush.sweepGradient(listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFEF4444))))
                } else Modifier.background(accent.primary)
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(fill)
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable(onClickLabel = paletteName(palette)) { onPick(palette) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(LocalDesign.current.icons.check, paletteName(palette), tint = if (palette == Palette.WALLPAPER) Color.White else accent.onPrimary)
                }
            }
        }
    }
}
