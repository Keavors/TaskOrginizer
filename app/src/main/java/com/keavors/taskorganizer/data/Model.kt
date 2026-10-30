package com.keavors.taskorganizer.data

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

@Serializable
data class Item(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val important: Boolean = false,
    /** Local calendar date, `yyyy-MM-dd`. */
    val dueDate: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val source: ItemSource = ItemSource.KEYBOARD
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
 * recognizer mishears it. [text] goes into the recognition prompt; every
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

@Serializable
data class SpeechSettings(
    /** "ru" (Russian with English names), "auto" or "en". */
    val language: String = "ru",
    /** Add the entry right after dictation instead of leaving it in the input for review. */
    val autoSubmit: Boolean = false,
    /** Minutes to keep the model in memory after the app goes to the background; -1 = always. */
    val keepLoadedMinutes: Int = 10,
    /** Shorter encoder window for short phrases: several times faster. */
    val fastMode: Boolean = true,
    /** CPU threads; 0 = automatic. */
    val threads: Int = 0
)

@Serializable
data class Settings(
    val schemaVersion: Int = SETTINGS_SCHEMA_VERSION,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Colors from the wallpaper (Material You) instead of the app palette. */
    val dynamicColor: Boolean = false,
    val newItemPosition: NewItemPosition = NewItemPosition.TOP,
    val speech: SpeechSettings = SpeechSettings()
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
