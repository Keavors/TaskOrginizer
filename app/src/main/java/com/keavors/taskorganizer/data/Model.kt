package com.keavors.taskorganizer.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TabKind {
    @SerialName("tasks") TASKS,
    @SerialName("notes") NOTES
}

@Serializable
enum class TabColor {
    @SerialName("blue") BLUE,
    @SerialName("violet") VIOLET,
    @SerialName("pink") PINK,
    @SerialName("red") RED,
    @SerialName("orange") ORANGE,
    @SerialName("amber") AMBER,
    @SerialName("green") GREEN,
    @SerialName("teal") TEAL,
    @SerialName("gray") GRAY
}

@Serializable
enum class ItemSource {
    @SerialName("keyboard") KEYBOARD,
    @SerialName("voice") VOICE
}

@Serializable
data class Tab(
    val id: String,
    val title: String,
    val kind: TabKind = TabKind.TASKS,
    val color: TabColor? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/** Importance of an entry: gray, yellow, red. Ordered from the least important. */
@Serializable
enum class Priority {
    @SerialName("normal") NORMAL,
    @SerialName("medium") MEDIUM,
    @SerialName("high") HIGH;

    fun next(): Priority = entries[(ordinal + 1) % entries.size]
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Item(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val priority: Priority = Priority.NORMAL,
    /** Local calendar date, `yyyy-MM-dd`. */
    val dueDate: String? = null,
    /** Local time `HH:mm` on [dueDate]; null = the whole day. */
    val dueTime: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val source: ItemSource = ItemSource.KEYBOARD,
    /** Versions up to 0.2 had only an "important" flag; [DataOps.normalize] turns it into a red [priority]. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val important: Boolean = false
)

/** Deleted entries and tabs are kept for [TRASH_RETENTION_DAYS] days. Entries of one action share a batch id. */
@Serializable
sealed interface TrashEntry {
    val id: String
    val batchId: String
    val deletedAt: Long
    val index: Int
}

@Serializable
@SerialName("item")
data class TrashedItem(
    override val id: String,
    override val batchId: String,
    override val deletedAt: Long,
    override val index: Int,
    val tabId: String,
    val tabTitle: String,
    val item: Item
) : TrashEntry

@Serializable
@SerialName("tab")
data class TrashedTab(
    override val id: String,
    override val batchId: String,
    override val deletedAt: Long,
    override val index: Int,
    val tab: Tab,
    val items: List<Item>
) : TrashEntry

/**
 * A word the user cares about (usually an English name) and the ways the
 * recognizer writes it. [text] is passed to the recognizer as a hint; every
 * variant is replaced with [text] after recognition.
 */
@Serializable
data class DictionaryTerm(
    val id: String,
    val text: String,
    val variants: List<String> = emptyList(),
    val createdAt: Long = 0
)

@Serializable
data class AppData(
    val schemaVersion: Int = DATA_SCHEMA_VERSION,
    val tabs: List<Tab>,
    /** Ordered entries of every tab, keyed by tab id. */
    val items: Map<String, List<Item>> = emptyMap(),
    val activeTabId: String? = null,
    val trash: List<TrashEntry> = emptyList(),
    val dictionary: List<DictionaryTerm> = emptyList()
) {
    val activeTab: Tab get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    fun itemsOf(tabId: String): List<Item> = items[tabId].orEmpty()
}

@Serializable
enum class ThemeMode {
    @SerialName("system") SYSTEM,
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK
}

@Serializable
enum class NewItemPosition {
    @SerialName("top") TOP,
    @SerialName("bottom") BOTTOM
}

/** Order of open entries: as added, or red first, then yellow, then gray. */
@Serializable
enum class ItemSort {
    @SerialName("added") ADDED,
    @SerialName("priority") PRIORITY
}

/** Complete look of the app: fonts, shapes, how entries, tabs and the input are drawn. */
@Serializable
enum class DesignStyle {
    @SerialName("standard") STANDARD,
    @SerialName("minimal") MINIMAL,
    @SerialName("soft") SOFT,
    @SerialName("sharp") SHARP,
    @SerialName("terminal") TERMINAL,
    @SerialName("paper") PAPER,
    @SerialName("compact") COMPACT,
    @SerialName("neon") NEON
}

/** Accent colors. WALLPAPER takes them from the wallpaper (Android 12+). */
@Serializable
enum class Palette {
    @SerialName("indigo") INDIGO,
    @SerialName("blue") BLUE,
    @SerialName("teal") TEAL,
    @SerialName("green") GREEN,
    @SerialName("amber") AMBER,
    @SerialName("red") RED,
    @SerialName("pink") PINK,
    @SerialName("violet") VIOLET,
    @SerialName("graphite") GRAPHITE,
    @SerialName("wallpaper") WALLPAPER
}

@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    /** Minutes before the due time; 0 = at the due time. */
    val offsets: List<Int> = listOf(0),
    /** After the due time, remind again every N minutes while the entry is not done; 0 = no repeats. */
    val repeatMinutes: Int = 0,
    /** How many repeats; 0 = until the entry is done. */
    val repeatCount: Int = 3,
    val snoozeMinutes: Int = 10,
    /** Entries with a date but no time remind at this minute of the day; -1 = they do not remind. */
    val allDayMinute: Int = -1,
    /** Only entries of this level and above remind. */
    val minPriority: Priority = Priority.NORMAL,
    /** Show reminders missed while the phone was off (up to a day old). */
    val showMissed: Boolean = true,
    /** Alarm-clock alarms: never late, but the status bar shows an alarm icon. */
    val alarmClock: Boolean = false
)

@Serializable
data class SpeechSettings(
    /** Language of speech for the recognizer: "ru-RU" or "en-US". */
    val language: String = "ru-RU",
    /** Recognition service: empty = the phone's default, "on-device" or a service component name. */
    val service: String = "",
    /** Ask the recognizer to work on the phone only, without internet. */
    val offlineOnly: Boolean = true,
    /** Add the entry right after dictation instead of leaving it in the input for review. */
    val autoSubmit: Boolean = false
)

@Serializable
data class Settings(
    val schemaVersion: Int = SETTINGS_SCHEMA_VERSION,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val style: DesignStyle = DesignStyle.STANDARD,
    val palette: Palette = Palette.INDIGO,
    /** Pure black backgrounds in the dark theme. */
    val pureBlack: Boolean = false,
    val newItemPosition: NewItemPosition = NewItemPosition.TOP,
    val sort: ItemSort = ItemSort.ADDED,
    val speech: SpeechSettings = SpeechSettings(),
    val reminders: ReminderSettings = ReminderSettings()
)

const val DATA_SCHEMA_VERSION = 1
const val SETTINGS_SCHEMA_VERSION = 1
const val TRASH_RETENTION_DAYS = 30
const val DEFAULT_TAB_TITLE = "Задачи"
const val NEW_TAB_TITLE = "Новая вкладка"

fun newId(): String = UUID.randomUUID().toString()

fun createTab(title: String, kind: TabKind = TabKind.TASKS, now: Long = System.currentTimeMillis()): Tab =
    Tab(id = newId(), title = title, kind = kind, createdAt = now, updatedAt = now)

fun createItem(text: String, source: ItemSource = ItemSource.KEYBOARD, now: Long = System.currentTimeMillis()): Item =
    Item(id = newId(), text = text, createdAt = now, updatedAt = now, source = source)

fun defaultData(now: Long = System.currentTimeMillis()): AppData {
    val tab = createTab(DEFAULT_TAB_TITLE, TabKind.TASKS, now)
    return AppData(tabs = listOf(tab), items = mapOf(tab.id to emptyList()), activeTabId = tab.id)
}
