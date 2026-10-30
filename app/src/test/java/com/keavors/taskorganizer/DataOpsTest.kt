package com.keavors.taskorganizer

import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.AppJson
import com.keavors.taskorganizer.data.DataOps
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.ItemSort
import com.keavors.taskorganizer.data.ItemSource
import com.keavors.taskorganizer.data.NewItemPosition
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.TabColor
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.data.TrashedItem
import com.keavors.taskorganizer.data.defaultData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataOpsTest {
    private fun texts(d: AppData, tabId: String) = d.itemsOf(tabId).map { it.text }

    @Test fun startsWithOneTasksTab() {
        val d = defaultData()
        assertEquals(listOf("Задачи"), d.tabs.map { it.title })
        assertEquals(d.tabs[0].id, d.activeTabId)
    }

    @Test fun addsRenamesAndReordersTabs() {
        var d = defaultData()
        val (d1, ideas) = DataOps.addTab(d, "Идеи", TabKind.NOTES)
        val (d2, blank) = DataOps.addTab(d1, "  ")
        d = DataOps.setTabColor(DataOps.renameTab(d2, ideas.id, "Мысли"), ideas.id, TabColor.VIOLET)
        assertEquals("Новая вкладка", blank.title)
        assertEquals(blank.id, d.activeTabId)
        assertEquals(TabColor.VIOLET, DataOps.tab(d, ideas.id)!!.color)
        d = DataOps.moveTab(d, 2, 0)
        assertEquals(listOf("Новая вкладка", "Задачи", "Мысли"), d.tabs.map { it.title })
    }

    @Test fun neverDeletesTheLastTab() {
        val d = defaultData()
        assertNull(DataOps.deleteTab(d, d.tabs[0].id).second)
    }

    @Test fun deletedTabComesBackWithItsEntries() {
        val (d1, tab) = DataOps.addTab(defaultData(), "Покупки")
        val d2 = DataOps.addItems(d1, tab.id, listOf("хлеб", "молоко"), position = NewItemPosition.BOTTOM).first
        val (d3, entry) = DataOps.deleteTab(d2, tab.id)
        assertEquals(listOf("Задачи"), d3.tabs.map { it.title })
        assertEquals(d3.tabs[0].id, d3.activeTabId)
        val d4 = DataOps.restoreBatch(d3, entry!!.batchId)
        assertEquals(listOf("Задачи", "Покупки"), d4.tabs.map { it.title })
        assertEquals(listOf("хлеб", "молоко"), texts(d4, tab.id))
        assertTrue(d4.trash.isEmpty())
    }

    @Test fun addsAtTopOrBottom() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        var d = DataOps.addItems(d0, tab, listOf("один")).first
        d = DataOps.addItems(d, tab, listOf("два", "три"), position = NewItemPosition.TOP).first
        d = DataOps.addItems(d, tab, listOf("четыре", " "), ItemSource.VOICE, NewItemPosition.BOTTOM).first
        assertEquals(listOf("два", "три", "один", "четыре"), texts(d, tab))
        assertEquals(ItemSource.VOICE, d.itemsOf(tab)[3].source)
    }

    @Test fun completesAndReopens() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, created) = DataOps.addItems(d0, tab, listOf("задача"))
        val d2 = DataOps.setDone(d1, tab, created[0].id, true, now = 5000)
        assertEquals(5000L, d2.itemsOf(tab)[0].completedAt)
        val d3 = DataOps.setDone(d2, tab, created[0].id, false, now = 6000)
        assertNull(d3.itemsOf(tab)[0].completedAt)
    }

    @Test fun reordersAndMovesBetweenTabs() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, created) = DataOps.addItems(d0, tab, listOf("a", "b", "c"), position = NewItemPosition.BOTTOM)
        val d2 = DataOps.moveItem(d1, tab, created[2].id, created[0].id)
        assertEquals(listOf("c", "a", "b"), texts(d2, tab))
        val (d3, other) = DataOps.addTab(d2, "Другое")
        val d4 = DataOps.moveItemToTab(d3, tab, created[0].id, other.id)
        assertEquals(listOf("c", "b"), texts(d4, tab))
        assertEquals(listOf("a"), texts(d4, other.id))
    }

    @Test fun restoresDeletedEntriesAtTheirPositions() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, created) = DataOps.addItems(d0, tab, listOf("0", "1", "2", "3", "4"), position = NewItemPosition.BOTTOM)
        val (d2, removed) = DataOps.deleteItems(d1, tab, listOf(created[1].id, created[3].id, created[4].id))
        assertEquals(listOf(1, 3, 4), removed.map { it.index })
        assertEquals(listOf("0", "2"), texts(d2, tab))
        assertEquals(listOf("0", "1", "2", "3", "4"), texts(DataOps.restoreBatch(d2, removed[0].batchId), tab))
    }

    @Test fun clearsCompletedIntoTrash() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, c) = DataOps.addItems(d0, tab, listOf("x", "y", "z"), position = NewItemPosition.BOTTOM)
        val d2 = DataOps.setDone(DataOps.setDone(d1, tab, c[0].id, true), tab, c[1].id, true)
        val (d3, removed) = DataOps.clearCompleted(d2, tab)
        assertEquals(2, removed.size)
        assertEquals(listOf("z"), texts(d3, tab))
    }

    @Test fun purgesOldTrash() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, c) = DataOps.addItems(d0, tab, listOf("old", "new"))
        val d2 = DataOps.deleteItems(d1, tab, listOf(c[0].id), now = 0).first
        val d3 = DataOps.deleteItems(d2, tab, listOf(c[1].id), now = 40L * 86_400_000).first
        val d4 = DataOps.purgeOldTrash(d3, now = 41L * 86_400_000, days = 30)
        assertEquals(listOf("new"), d4.trash.map { (it as TrashedItem).item.text })
    }

    @Test fun dictionaryMergesVariantsIgnoringCase() {
        var d = DataOps.upsertTerm(defaultData(), "Docker", listOf("докер"))
        d = DataOps.upsertTerm(d, "docker", listOf("доккер", "Докер", " "))
        assertEquals(1, d.dictionary.size)
        assertEquals(listOf("докер", "доккер"), d.dictionary[0].variants)
        d = DataOps.updateTerm(d, d.dictionary[0].id, variants = listOf("докер", "Docker"))
        assertEquals(listOf("докер"), d.dictionary[0].variants)
    }

    @Test fun jsonRoundTripIncludingTrash() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, c) = DataOps.addItems(d0, tab, listOf("a", "b"))
        val d2 = DataOps.deleteItems(d1, tab, listOf(c[0].id)).first
        val (d3, _) = DataOps.addTab(d2, "Вторая")
        val d4 = DataOps.deleteTab(d3, d3.activeTabId!!).first
        val json = AppJson.encodeToString(AppData.serializer(), d4)
        assertEquals(d4, AppJson.decodeFromString(AppData.serializer(), json))
        assertTrue(json.contains("\"kind\":\"tab\""))
    }

    @Test fun unknownValuesFallBackToDefaults() {
        // "threads" was a setting of the old Whisper version: unknown now, so it is skipped.
        val json = """{"theme":"neon","speech":{"language":"en-US","threads":2},"unknown":1}"""
        val s = AppJson.decodeFromString(Settings.serializer(), json)
        assertEquals(Settings().theme, s.theme)
        assertEquals("en-US", s.speech.language)
    }

    @Test fun oldImportantFlagBecomesRed() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val old = Item(id = "x", text = "Важное", important = true)
        val fixed = DataOps.normalize(d0.copy(items = mapOf(tab to listOf(old))))
        val item = fixed.itemsOf(tab).single()
        assertEquals(Priority.HIGH, item.priority)
        assertEquals(false, item.important)
        // The flag is not written any more.
        assertTrue("important" !in AppJson.encodeToString(Item.serializer(), item))
    }

    @Test fun priorityOrderKeepsTheOrderInsideALevel() {
        val items = listOf(
            Item(id = "1", text = "a"),
            Item(id = "2", text = "b", priority = Priority.HIGH),
            Item(id = "3", text = "c", priority = Priority.MEDIUM),
            Item(id = "4", text = "d", priority = Priority.HIGH)
        )
        assertEquals(listOf("2", "4", "3", "1"), DataOps.displayOrder(items, ItemSort.PRIORITY).map { it.id })
        assertEquals(listOf("1", "2", "3", "4"), DataOps.displayOrder(items, ItemSort.ADDED).map { it.id })
    }

    @Test fun dueTimeNeedsADate() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val (d1, created) = DataOps.addItems(d0, tab, listOf("a"))
        val id = created[0].id
        val d2 = DataOps.setDue(d1, tab, id, "2026-11-02", "14:00")
        assertEquals("14:00", d2.itemsOf(tab)[0].dueTime)
        val d3 = DataOps.setDue(d2, tab, id, null, "14:00")
        assertNull(d3.itemsOf(tab)[0].dueDate)
        assertNull(d3.itemsOf(tab)[0].dueTime)
    }

    @Test fun normalizeRepairsReferences() {
        val d0 = defaultData()
        val tab = d0.tabs[0].id
        val withOrphan = d0.copy(items = d0.items + ("ghost" to DataOps.addItems(d0, tab, listOf("g")).second), activeTabId = "missing")
        val fixed = DataOps.normalize(withOrphan)
        assertTrue("ghost" !in fixed.items)
        assertEquals(listOf("g"), texts(fixed, tab))
        assertEquals(tab, fixed.activeTabId)
    }
}
