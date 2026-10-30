package com.keavors.taskorganizer.data

/**
 * All changes to [AppData]. Pure functions: they return a new value and never
 * mutate their input, so the UI state stays consistent and tests are simple.
 */
object DataOps {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun now() = System.currentTimeMillis()

    fun tab(d: AppData, tabId: String): Tab? = d.tabs.firstOrNull { it.id == tabId }

    private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
        if (from !in indices || to !in indices || from == to) return this
        val list = toMutableList()
        list.add(to, list.removeAt(from))
        return list
    }

    private fun AppData.withItems(tabId: String, list: List<Item>): AppData = copy(items = items + (tabId to list))

    // ------------------------------------------------------------------ tabs

    fun addTab(d: AppData, title: String, kind: TabKind = TabKind.TASKS, now: Long = now()): Pair<AppData, Tab> {
        val tab = createTab(title.trim().ifEmpty { NEW_TAB_TITLE }, kind, now)
        return d.copy(tabs = d.tabs + tab, items = d.items + (tab.id to emptyList()), activeTabId = tab.id) to tab
    }

    fun renameTab(d: AppData, tabId: String, title: String, now: Long = now()): AppData {
        val clean = title.trim()
        if (clean.isEmpty()) return d
        return d.copy(tabs = d.tabs.map { if (it.id == tabId) it.copy(title = clean, updatedAt = now) else it })
    }

    fun setTabKind(d: AppData, tabId: String, kind: TabKind, now: Long = now()): AppData =
        d.copy(tabs = d.tabs.map { if (it.id == tabId) it.copy(kind = kind, updatedAt = now) else it })

    fun setTabColor(d: AppData, tabId: String, color: TabColor?, now: Long = now()): AppData =
        d.copy(tabs = d.tabs.map { if (it.id == tabId) it.copy(color = color, updatedAt = now) else it })

    fun moveTab(d: AppData, fromIndex: Int, toIndex: Int): AppData = d.copy(tabs = d.tabs.moved(fromIndex, toIndex))

    fun setActiveTab(d: AppData, tabId: String): AppData = if (tab(d, tabId) != null) d.copy(activeTabId = tabId) else d

    /** Moves a tab with its entries to the trash. The last remaining tab cannot be deleted. */
    fun deleteTab(d: AppData, tabId: String, now: Long = now()): Pair<AppData, TrashedTab?> {
        val index = d.tabs.indexOfFirst { it.id == tabId }
        if (index < 0 || d.tabs.size <= 1) return d to null
        val entry = TrashedTab(newId(), newId(), now, index, d.tabs[index], d.itemsOf(tabId))
        val tabs = d.tabs.filterIndexed { i, _ -> i != index }
        val active = if (d.activeTabId == tabId) tabs[minOf(index, tabs.size - 1)].id else d.activeTabId
        return d.copy(tabs = tabs, items = d.items - tabId, activeTabId = active, trash = listOf(entry) + d.trash) to entry
    }

    // ------------------------------------------------------------------ items

    fun addItems(
        d: AppData,
        tabId: String,
        texts: List<String>,
        source: ItemSource = ItemSource.KEYBOARD,
        position: NewItemPosition = NewItemPosition.TOP,
        now: Long = now()
    ): Pair<AppData, List<Item>> {
        if (tab(d, tabId) == null) return d to emptyList()
        val created = texts.map { it.trim() }.filter { it.isNotEmpty() }.map { createItem(it, source, now) }
        if (created.isEmpty()) return d to emptyList()
        val list = if (position == NewItemPosition.TOP) created + d.itemsOf(tabId) else d.itemsOf(tabId) + created
        return d.withItems(tabId, list) to created
    }

    fun updateItem(d: AppData, tabId: String, itemId: String, now: Long = now(), change: (Item) -> Item): AppData {
        val list = d.itemsOf(tabId)
        if (list.none { it.id == itemId }) return d
        return d.withItems(tabId, list.map { if (it.id == itemId) change(it).copy(updatedAt = now) else it })
    }

    fun setText(d: AppData, tabId: String, itemId: String, text: String, now: Long = now()): AppData =
        updateItem(d, tabId, itemId, now) { it.copy(text = text) }

    fun setPriority(d: AppData, tabId: String, itemId: String, priority: Priority, now: Long = now()): AppData =
        updateItem(d, tabId, itemId, now) { it.copy(priority = priority, important = false) }

    /** Sets the due date and time; without a date there is no time either. */
    fun setDue(d: AppData, tabId: String, itemId: String, date: String?, time: String?, now: Long = now()): AppData =
        updateItem(d, tabId, itemId, now) { it.copy(dueDate = date, dueTime = if (date == null) null else time) }

    /** Order on screen: as stored, or red, yellow, gray with the stored order inside each level. */
    fun displayOrder(items: List<Item>, sort: ItemSort): List<Item> =
        if (sort == ItemSort.PRIORITY) items.sortedByDescending { it.priority } else items

    fun setDone(d: AppData, tabId: String, itemId: String, done: Boolean, now: Long = now()): AppData =
        updateItem(d, tabId, itemId, now) { if (it.done == done) it else it.copy(done = done, completedAt = if (done) now else null) }

    /** Reorders entries of one tab: puts [activeId] where [overId] is. */
    fun moveItem(d: AppData, tabId: String, activeId: String, overId: String): AppData {
        val list = d.itemsOf(tabId)
        return d.withItems(tabId, list.moved(list.indexOfFirst { it.id == activeId }, list.indexOfFirst { it.id == overId }))
    }

    fun moveItemToTab(d: AppData, fromTabId: String, itemId: String, toTabId: String, now: Long = now()): AppData {
        if (fromTabId == toTabId || tab(d, toTabId) == null) return d
        val item = d.itemsOf(fromTabId).firstOrNull { it.id == itemId } ?: return d
        return d.withItems(fromTabId, d.itemsOf(fromTabId).filter { it.id != itemId })
            .withItems(toTabId, listOf(item.copy(updatedAt = now)) + d.itemsOf(toTabId))
    }

    /** Moves entries to the trash; returns the trash entries (one batch). */
    fun deleteItems(d: AppData, tabId: String, itemIds: Collection<String>, now: Long = now()): Pair<AppData, List<TrashedItem>> {
        val ids = itemIds.toSet()
        val list = d.itemsOf(tabId)
        val title = tab(d, tabId)?.title.orEmpty()
        val batch = newId()
        val removed = list.withIndex().filter { it.value.id in ids }.map { (index, item) ->
            TrashedItem(newId(), batch, now, index, tabId, title, item)
        }
        if (removed.isEmpty()) return d to emptyList()
        return d.withItems(tabId, list.filter { it.id !in ids }).copy(trash = removed + d.trash) to removed
    }

    fun clearCompleted(d: AppData, tabId: String, now: Long = now()): Pair<AppData, List<TrashedItem>> =
        deleteItems(d, tabId, d.itemsOf(tabId).filter { it.done }.map { it.id }, now)

    // ------------------------------------------------------------------ trash

    /** Restores trash entries (tabs first, so their entries find their place). Returns the restored count. */
    fun restoreTrash(d: AppData, entryIds: Collection<String>): Pair<AppData, Int> {
        val ids = entryIds.toSet()
        val entries = d.trash.filter { it.id in ids }
        if (entries.isEmpty()) return d to 0
        var data = d.copy(trash = d.trash.filter { it.id !in ids })

        for (e in entries.filterIsInstance<TrashedTab>().sortedBy { it.index }) {
            if (tab(data, e.tab.id) != null) continue
            val tabs = data.tabs.toMutableList().apply { add(e.index.coerceIn(0, size), e.tab) }
            data = data.copy(tabs = tabs, activeTabId = e.tab.id).withItems(e.tab.id, e.items + data.itemsOf(e.tab.id))
        }
        for (e in entries.filterIsInstance<TrashedItem>().sortedBy { it.index }) {
            val tabId = if (tab(data, e.tabId) != null) e.tabId else data.activeTab.id
            val list = data.itemsOf(tabId)
            if (list.any { it.id == e.item.id }) continue
            data = data.withItems(tabId, list.toMutableList().apply { add(e.index.coerceIn(0, size), e.item) })
        }
        return data to entries.size
    }

    fun restoreBatch(d: AppData, batchId: String): AppData =
        restoreTrash(d, d.trash.filter { it.batchId == batchId }.map { it.id }).first

    fun deleteTrashEntries(d: AppData, entryIds: Collection<String>): AppData {
        val ids = entryIds.toSet()
        return d.copy(trash = d.trash.filter { it.id !in ids })
    }

    fun emptyTrash(d: AppData): AppData = d.copy(trash = emptyList())

    fun purgeOldTrash(d: AppData, now: Long = now(), days: Int = TRASH_RETENTION_DAYS): AppData {
        val limit = now - days * DAY_MS
        return if (d.trash.all { it.deletedAt >= limit }) d else d.copy(trash = d.trash.filter { it.deletedAt >= limit })
    }

    // ------------------------------------------------------------------ dictionary

    private fun cleanVariants(variants: List<String>, text: String): List<String> {
        val seen = mutableSetOf(text.trim().lowercase())
        return variants.map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() && seen.add(it.lowercase()) }
    }

    /** Adds a term, or merges the variants into an existing term with the same spelling (case-insensitive). */
    fun upsertTerm(d: AppData, text: String, variants: List<String> = emptyList(), now: Long = now()): AppData {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return d
        val existing = d.dictionary.firstOrNull { it.text.trim().equals(clean, ignoreCase = true) }
        if (existing != null) {
            val merged = existing.copy(variants = cleanVariants(existing.variants + variants, existing.text))
            return d.copy(dictionary = d.dictionary.map { if (it.id == existing.id) merged else it })
        }
        val term = DictionaryTerm(newId(), clean, cleanVariants(variants, clean), now)
        return d.copy(dictionary = listOf(term) + d.dictionary)
    }

    fun updateTerm(d: AppData, termId: String, text: String? = null, variants: List<String>? = null): AppData =
        d.copy(dictionary = d.dictionary.map { term ->
            if (term.id != termId) return@map term
            val newText = text?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotEmpty() } ?: term.text
            term.copy(text = newText, variants = cleanVariants(variants ?: term.variants, newText))
        })

    fun deleteTerm(d: AppData, termId: String): AppData = d.copy(dictionary = d.dictionary.filter { it.id != termId })

    // ------------------------------------------------------------------ integrity

    /** The old "important" flag becomes the red level. */
    private fun Item.migrated(): Item =
        if (important) copy(important = false, priority = if (priority == Priority.NORMAL) Priority.HIGH else priority) else this

    private fun List<Item>.migrated(): List<Item> = if (any { it.important }) map { it.migrated() } else this

    /**
     * Repairs references after loading or importing: every tab has a list, the active tab exists.
     * Also converts data of older versions.
     */
    fun normalize(d: AppData): AppData {
        val tabs = d.tabs.ifEmpty { listOf(createTab(DEFAULT_TAB_TITLE)) }
        val ids = tabs.map { it.id }.toSet()
        val items = tabs.associate { it.id to d.items[it.id].orEmpty().migrated() }.toMutableMap()
        // Entries of a tab that no longer exists are kept: they move to the first tab.
        val orphans = d.items.filterKeys { it !in ids }.values.flatten().migrated()
        if (orphans.isNotEmpty()) items[tabs.first().id] = items[tabs.first().id].orEmpty() + orphans
        val active = d.activeTabId?.takeIf { it in ids } ?: tabs.first().id
        val trash = d.trash.map { entry ->
            when (entry) {
                is TrashedItem -> if (entry.item.important) entry.copy(item = entry.item.migrated()) else entry
                is TrashedTab -> if (entry.items.any { it.important }) entry.copy(items = entry.items.migrated()) else entry
            }
        }
        return d.copy(tabs = tabs, items = items, activeTabId = active, trash = trash)
    }
}
