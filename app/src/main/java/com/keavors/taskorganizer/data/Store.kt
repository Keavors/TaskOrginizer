package com.keavors.taskorganizer.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The app state in memory. Changes are applied on the main thread through
 * [update]; the file is written 300 ms after the last change and at once when
 * the app goes to the background ([flushBlocking]).
 */
class Store(private val storage: AppStorage, private val scope: CoroutineScope) {
    private val _data = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = _data.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    var recovery: AppStorage.Recovery = AppStorage.Recovery.NONE
        private set

    private val io = Dispatchers.IO.limitedParallelism(1)
    private var saveJob: Job? = null

    val current: AppData get() = _data.value ?: error("Data is not loaded")

    suspend fun load() {
        val (loaded, settings) = withContext(io) { storage.loadData() to storage.loadSettings() }
        recovery = loaded.recovery
        _settings.value = settings
        _data.value = loaded.data
    }

    fun update(change: (AppData) -> AppData) {
        val old = _data.value ?: return
        val new = change(old)
        if (new !== old) {
            _data.value = new
            scheduleSave()
        }
    }

    /** Like [update], for operations that also return a result (created item, trash entries, ...). */
    fun <R> updateWith(change: (AppData) -> Pair<AppData, R>): R? {
        val old = _data.value ?: return null
        val (new, result) = change(old)
        if (new !== old) {
            _data.value = new
            scheduleSave()
        }
        return result
    }

    fun replaceAll(data: AppData) {
        _data.value = data
        scheduleSave()
    }

    fun updateSettings(change: (Settings) -> Settings) {
        val next = change(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        scope.launch(io) { runCatching { storage.writeSettings(next) }.onFailure { Log.e(TAG, "settings save failed", it) } }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch(io) {
            delay(SAVE_DELAY_MS)
            _data.value?.let { runCatching { storage.writeData(it) }.onFailure { e -> Log.e(TAG, "save failed", e) } }
        }
    }

    /** Writes immediately; used when the app goes to the background. */
    fun flushBlocking() {
        saveJob?.cancel()
        _data.value?.let { runCatching { storage.writeData(it) }.onFailure { e -> Log.e(TAG, "save failed", e) } }
    }

    fun exportJson(): String = storage.exportJson(current)

    fun parseImport(text: String): AppData = storage.importJson(text)

    private companion object {
        const val TAG = "Store"
        const val SAVE_DELAY_MS = 300L
    }
}
