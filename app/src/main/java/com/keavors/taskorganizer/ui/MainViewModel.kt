package com.keavors.taskorganizer.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings as SystemSettings
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keavors.taskorganizer.App
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.AppStorage
import com.keavors.taskorganizer.data.DataOps
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.ItemSource
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.TabColor
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.data.TrashEntry
import com.keavors.taskorganizer.text.CorrectionSuggestion
import com.keavors.taskorganizer.text.Corrections
import com.keavors.taskorganizer.text.Entries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { MAIN, SETTINGS, DICTIONARY, SEARCH, TRASH }

/** A snackbar message, optionally with an action such as "Отменить". */
class UiMessage(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val services = (app as App).services
    private val store = services.store

    val data = store.data
    val settings = store.settings

    var screen by mutableStateOf(Screen.MAIN)
    /** Entry to scroll to and flash after a search. */
    var highlightItemId by mutableStateOf<String?>(null)
    /** Corrections of dictated text the user may want to remember. */
    var learn by mutableStateOf<List<CorrectionSuggestion>>(emptyList())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            store.ensureLoaded()
            when (store.recovery) {
                AppStorage.Recovery.BACKUP -> message("Файл данных был повреждён — восстановлено из резервной копии.")
                AppStorage.Recovery.NEW -> message("Файл данных был повреждён, резервных копий нет — начат новый список.")
                AppStorage.Recovery.NONE -> Unit
            }
        }
        viewModelScope.launch { services.dictation.messages.collect { message(it) } }
    }

    private fun message(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, action))
    }

    private fun undoable(text: String, undo: () -> Unit) = message(text, "Отменить", undo)

    fun notify(text: String) = message(text)

    // ------------------------------------------------------------------ entries

    /** Tasks: one line = one entry. Notes: the whole text is one entry. Returns how many were added. */
    fun addEntries(tabId: String, text: String, dictated: String?): Int {
        val tab = DataOps.tab(store.current, tabId) ?: return 0
        val texts = if (tab.kind == TabKind.TASKS) Entries.split(text) else listOf(text.trim()).filter { it.isNotEmpty() }
        if (texts.isEmpty()) return 0
        val source = if (dictated != null) ItemSource.VOICE else ItemSource.KEYBOARD
        store.update { DataOps.addItems(it, tabId, texts, source, settings.value.newItemPosition).first }
        if (texts.size > 1) message("Добавлено записей: ${texts.size}")
        if (dictated != null) {
            val known = store.current.dictionary
            learn = Corrections.suggest(dictated, text).filterNot { s ->
                known.any { it.text.equals(s.to, ignoreCase = true) && it.variants.any { v -> v.equals(s.from, ignoreCase = true) } }
            }
        }
        return texts.size
    }

    fun setDone(tabId: String, itemId: String, done: Boolean) = store.update { DataOps.setDone(it, tabId, itemId, done) }

    fun setPriority(tabId: String, itemId: String, priority: Priority) =
        store.update { DataOps.setPriority(it, tabId, itemId, priority) }

    /** Sets the due date and time; asks for notifications once if the entry will remind. */
    fun setDue(tabId: String, itemId: String, date: String?, time: String?) {
        store.update { DataOps.setDue(it, tabId, itemId, date, time) }
        val r = settings.value.reminders
        val reminds = date != null && r.enabled && (time != null || r.allDayMinute >= 0)
        if (reminds && !askedNotifications && !NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()) {
            askedNotifications = true
            _notificationRequests.tryEmit(Unit)
        }
    }

    private var askedNotifications = false
    private val _notificationRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** The screen asks Android for the notification permission. */
    val notificationRequests: SharedFlow<Unit> = _notificationRequests.asSharedFlow()

    fun notificationsDenied() =
        message("Уведомления выключены — напоминания не придут", "Включить") { openNotificationSettings() }

    fun openNotificationSettings() {
        val app = getApplication<Application>()
        val intent = Intent(SystemSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(SystemSettings.EXTRA_APP_PACKAGE, app.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }
    }

    /** From a reminder notification: show the entry in its tab. */
    fun openItem(tabId: String?, itemId: String) {
        viewModelScope.launch {
            store.ensureLoaded()
            val d = store.data.value ?: return@launch
            val tab = d.tabs.firstOrNull { t -> d.itemsOf(t.id).any { it.id == itemId } }?.id ?: tabId ?: return@launch
            store.update { DataOps.setActiveTab(it, tab) }
            screen = Screen.MAIN
            highlightItemId = itemId
        }
    }

    /** Saving an empty text deletes the entry (with undo). */
    fun setText(tabId: String, item: Item, text: String) {
        val clean = text.trim()
        when {
            clean.isEmpty() -> deleteItems(tabId, listOf(item.id))
            clean != item.text -> store.update { DataOps.setText(it, tabId, item.id, clean) }
        }
    }

    fun deleteItems(tabId: String, ids: List<String>) {
        val removed = store.updateWith { DataOps.deleteItems(it, tabId, ids) }.orEmpty()
        if (removed.isEmpty()) return
        val batch = removed.first().batchId
        undoable(if (removed.size == 1) "Запись удалена" else "Удалено записей: ${removed.size}") {
            store.update { DataOps.restoreBatch(it, batch) }
        }
    }

    fun clearCompleted(tabId: String) {
        val removed = store.updateWith { DataOps.clearCompleted(it, tabId) }.orEmpty()
        if (removed.isEmpty()) return
        val batch = removed.first().batchId
        undoable("Выполненные убраны в корзину (${removed.size})") { store.update { DataOps.restoreBatch(it, batch) } }
    }

    fun moveItem(tabId: String, activeId: String, overId: String) = store.update { DataOps.moveItem(it, tabId, activeId, overId) }

    fun moveItemToTab(fromTabId: String, itemId: String, toTabId: String) {
        store.update { DataOps.moveItemToTab(it, fromTabId, itemId, toTabId) }
        val title = DataOps.tab(store.current, toTabId)?.title ?: return
        undoable("Перенесено в «$title»") { store.update { DataOps.moveItemToTab(it, toTabId, itemId, fromTabId) } }
    }

    // ------------------------------------------------------------------ tabs

    fun selectTab(tabId: String) = store.update { DataOps.setActiveTab(it, tabId) }

    fun addTab(title: String, kind: TabKind) = store.update { DataOps.addTab(it, title, kind).first }

    fun renameTab(tabId: String, title: String) = store.update { DataOps.renameTab(it, tabId, title) }

    fun setTabKind(tabId: String, kind: TabKind) = store.update { DataOps.setTabKind(it, tabId, kind) }

    fun setTabColor(tabId: String, color: TabColor?) = store.update { DataOps.setTabColor(it, tabId, color) }

    fun moveTab(from: Int, to: Int) = store.update { DataOps.moveTab(it, from, to) }

    fun deleteTab(tabId: String) {
        val entry = store.updateWith { DataOps.deleteTab(it, tabId) } ?: return
        undoable("Вкладка «${entry.tab.title}» удалена") { store.update { DataOps.restoreBatch(it, entry.batchId) } }
    }

    // ------------------------------------------------------------------ trash

    fun restoreFromTrash(entry: TrashEntry) {
        store.update { DataOps.restoreTrash(it, listOf(entry.id)).first }
        message("Восстановлено")
    }

    fun deleteFromTrash(entry: TrashEntry) = store.update { DataOps.deleteTrashEntries(it, listOf(entry.id)) }

    fun emptyTrash() = store.update { DataOps.emptyTrash(it) }

    // ------------------------------------------------------------------ dictionary

    fun rememberCorrections(list: List<CorrectionSuggestion>) {
        if (list.isEmpty()) return
        store.update { d -> list.fold(d) { acc, s -> DataOps.upsertTerm(acc, s.to, listOf(s.from)) } }
        learn = learn - list.toSet()
        message(if (list.size == 1) "Запомнил: «${list[0].from}» → «${list[0].to}»" else "Запомнено исправлений: ${list.size}")
    }

    fun addTerm(text: String, variants: List<String>) = store.update { DataOps.upsertTerm(it, text, variants) }

    fun updateTerm(termId: String, text: String? = null, variants: List<String>? = null) =
        store.update { DataOps.updateTerm(it, termId, text, variants) }

    fun deleteTerm(termId: String) = store.update { DataOps.deleteTerm(it, termId) }

    // ------------------------------------------------------------------ settings

    fun updateSettings(change: (Settings) -> Settings) = store.updateSettings(change)

    // ------------------------------------------------------------------ export / import

    fun exportTo(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = store.exportJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
        }.onSuccess { message("Копия данных сохранена") }.onFailure { message("Не удалось сохранить файл: ${it.message}") }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            withContext(Dispatchers.IO) { store.parseImport(text) }
        }.onSuccess { imported: AppData ->
            store.replaceAll(imported)
            message("Данные загружены из файла")
        }.onFailure { message("Это не файл Органайзера задач или он повреждён.") }
    }
}
