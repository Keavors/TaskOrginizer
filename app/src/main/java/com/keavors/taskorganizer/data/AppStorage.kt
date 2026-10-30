package com.keavors.taskorganizer.data

import android.content.Context
import android.util.Log
import androidx.core.util.AtomicFile
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    // An unknown enum value (from a newer app version) becomes the default instead of failing the file.
    coerceInputValues = true
    classDiscriminator = "kind"
}

private val PrettyJson = Json(AppJson) { prettyPrint = true }

/**
 * Tabs, entries and settings on disk (app-private `files/data/`). Every write is
 * atomic (temp file + rename); a daily copy goes to `backups/` (last 14 kept).
 */
class AppStorage(context: Context) {
    enum class Recovery { NONE, BACKUP, NEW }

    class Loaded(val data: AppData, val recovery: Recovery)

    private val dir = File(context.filesDir, "data").apply { mkdirs() }
    private val backupsDir = File(dir, "backups")
    private val dataPath = File(dir, "data.json")
    private val dataFile = AtomicFile(dataPath)
    private val settingsFile = AtomicFile(File(dir, "settings.json"))
    private val lock = Any()

    fun loadData(): Loaded {
        if (!dataPath.exists()) {
            val fresh = defaultData()
            writeData(fresh)
            return Loaded(fresh, Recovery.NONE)
        }
        parse(dataFile)?.let {
            backupDaily()
            return Loaded(it, Recovery.NONE)
        }
        // The main file is damaged: keep it for manual recovery and use the newest good backup.
        Log.e(TAG, "data.json is unreadable, trying backups")
        runCatching { dataPath.copyTo(File(dir, "data.corrupt-${stamp()}.json"), overwrite = true) }
        backupsDir.listFiles()?.sortedByDescending { it.name }?.forEach { backup ->
            parse(AtomicFile(backup))?.let {
                writeData(it)
                return Loaded(it, Recovery.BACKUP)
            }
        }
        val fresh = defaultData()
        writeData(fresh)
        return Loaded(fresh, Recovery.NEW)
    }

    fun writeData(data: AppData) = write(dataFile, AppJson.encodeToString(AppData.serializer(), data))

    fun loadSettings(): Settings =
        runCatching { AppJson.decodeFromString(Settings.serializer(), settingsFile.readFully().decodeToString()) }
            .getOrElse { Settings() }

    fun writeSettings(settings: Settings) = write(settingsFile, AppJson.encodeToString(Settings.serializer(), settings))

    fun exportJson(data: AppData): String = PrettyJson.encodeToString(AppData.serializer(), data)

    /** Parses an exported file; throws if it is not ours. The current data is backed up first. */
    fun importJson(text: String): AppData {
        val data = prepare(AppJson.decodeFromString(AppData.serializer(), text))
        runCatching { dataPath.copyTo(File(backupsDir.apply { mkdirs() }, "before-import-${stamp()}.json"), overwrite = true) }
        return data
    }

    private fun prepare(data: AppData): AppData = DataOps.purgeOldTrash(DataOps.normalize(data))

    private fun parse(file: AtomicFile): AppData? =
        runCatching { prepare(AppJson.decodeFromString(AppData.serializer(), file.readFully().decodeToString())) }
            .onFailure { Log.w(TAG, "cannot parse ${file.baseFile.name}", it) }
            .getOrNull()

    private fun write(file: AtomicFile, text: String) = synchronized(lock) {
        val out = file.startWrite()
        try {
            out.write(text.toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private fun backupDaily() {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val target = File(backupsDir, "data-$day.json")
        if (target.exists()) return
        runCatching {
            backupsDir.mkdirs()
            dataPath.copyTo(target)
            backupsDir.listFiles { f -> f.name.startsWith("data-") }
                ?.sortedByDescending { it.name }
                ?.drop(KEEP_BACKUPS)
                ?.forEach { it.delete() }
        }
    }

    private fun stamp() = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())

    private companion object {
        const val TAG = "AppStorage"
        const val KEEP_BACKUPS = 14
    }
}
