import { describe, expect, it } from 'vitest'
import { createDefaultData, createDefaultSettings, type AppData } from '@shared/model'
import * as ops from '@shared/dataOps'
import { parseAppData, parseSettings } from '@shared/schema'

function fresh(): AppData {
  return createDefaultData(1000)
}

const texts = (data: AppData, tabId: string): string[] => data.items[tabId].map((i) => i.text)

describe('tabs', () => {
  it('starts with one "Задачи" tab', () => {
    const d = fresh()
    expect(d.tabs.map((t) => t.title)).toEqual(['Задачи'])
    expect(d.activeTabId).toBe(d.tabs[0].id)
  })

  it('adds, renames, reorders and activates tabs', () => {
    const d = fresh()
    const a = ops.addTab(d, 'Идеи', 'notes')
    const b = ops.addTab(d, '  ', 'tasks')
    expect(b.title).toBe('Новая вкладка')
    expect(d.activeTabId).toBe(b.id)
    ops.updateTab(d, a.id, { title: 'Мысли', color: 'violet' })
    expect(ops.getTab(d, a.id)).toMatchObject({ title: 'Мысли', color: 'violet', kind: 'notes' })
    ops.moveTab(d, b.id, d.tabs[0].id)
    expect(d.tabs.map((t) => t.title)).toEqual(['Новая вкладка', 'Задачи', 'Мысли'])
  })

  it('never deletes the last tab', () => {
    const d = fresh()
    expect(ops.deleteTab(d, d.tabs[0].id)).toBeUndefined()
    expect(d.tabs).toHaveLength(1)
  })

  it('deletes a tab into the trash and restores it with its entries', () => {
    const d = fresh()
    const t = ops.addTab(d, 'Покупки')
    ops.addItems(d, t.id, ['хлеб', 'молоко'])
    const entry = ops.deleteTab(d, t.id)!
    expect(d.tabs.map((x) => x.title)).toEqual(['Задачи'])
    expect(d.items[t.id]).toBeUndefined()
    expect(d.activeTabId).toBe(d.tabs[0].id)
    expect(ops.restoreBatch(d, entry.batchId)).toBe(1)
    expect(d.tabs.map((x) => x.title)).toEqual(['Задачи', 'Покупки'])
    expect(texts(d, t.id)).toEqual(['хлеб', 'молоко'])
    expect(d.trash).toHaveLength(0)
  })
})

describe('items', () => {
  it('adds entries at the top or at the bottom', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    ops.addItems(d, tab, ['один'])
    ops.addItems(d, tab, ['два', 'три'], { position: 'top' })
    ops.addItems(d, tab, ['четыре', '  '], { position: 'bottom', source: 'voice' })
    expect(texts(d, tab)).toEqual(['два', 'три', 'один', 'четыре'])
    expect(d.items[tab][3].source).toBe('voice')
  })

  it('completes and reopens entries', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    const [item] = ops.addItems(d, tab, ['задача'])
    ops.setItemDone(d, tab, item.id, true, 5000)
    expect(d.items[tab][0]).toMatchObject({ done: true, completedAt: 5000 })
    ops.setItemDone(d, tab, item.id, false, 6000)
    expect(d.items[tab][0]).toMatchObject({ done: false, completedAt: null })
  })

  it('reorders and moves entries between tabs', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    const [a, , c] = ops.addItems(d, tab, ['a', 'b', 'c'], { position: 'bottom' })
    ops.moveItem(d, tab, c.id, a.id)
    expect(texts(d, tab)).toEqual(['c', 'a', 'b'])
    const other = ops.addTab(d, 'Другое')
    ops.moveItemToTab(d, tab, a.id, other.id)
    expect(texts(d, tab)).toEqual(['c', 'b'])
    expect(texts(d, other.id)).toEqual(['a'])
  })

  it('deletes entries and restores them at their old positions', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    const created = ops.addItems(d, tab, ['0', '1', '2', '3', '4'], { position: 'bottom' })
    const removed = ops.deleteItems(d, tab, [created[1].id, created[3].id, created[4].id])
    expect(removed.map((r) => r.index)).toEqual([1, 3, 4])
    expect(texts(d, tab)).toEqual(['0', '2'])
    ops.restoreBatch(d, removed[0].batchId)
    expect(texts(d, tab)).toEqual(['0', '1', '2', '3', '4'])
  })

  it('clears completed entries into the trash', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    const [x, y] = ops.addItems(d, tab, ['x', 'y', 'z'], { position: 'bottom' })
    ops.setItemDone(d, tab, x.id, true)
    ops.setItemDone(d, tab, y.id, true)
    expect(ops.clearCompleted(d, tab)).toHaveLength(2)
    expect(texts(d, tab)).toEqual(['z'])
    expect(d.trash).toHaveLength(2)
  })

  it('restores an entry into the active tab when its own tab is gone', () => {
    const d = fresh()
    const t = ops.addTab(d, 'Временная')
    const [item] = ops.addItems(d, t.id, ['потеряшка'])
    const [trashed] = ops.deleteItems(d, t.id, [item.id])
    ops.deleteTab(d, t.id)
    ops.restoreTrash(d, [trashed.id])
    expect(texts(d, d.activeTabId!)).toContain('потеряшка')
  })

  it('purges trash older than the retention period', () => {
    const d = fresh()
    const tab = d.tabs[0].id
    const [a, b] = ops.addItems(d, tab, ['old', 'new'])
    ops.deleteItems(d, tab, [a.id], 0)
    ops.deleteItems(d, tab, [b.id], 40 * 86400000)
    ops.purgeOldTrash(d, 41 * 86400000, 30)
    expect(d.trash.map((e) => (e.kind === 'item' ? e.item.text : ''))).toEqual(['new'])
  })
})

describe('dictionary', () => {
  it('merges variants into an existing term regardless of case', () => {
    const d = fresh()
    ops.upsertTerm(d, 'Docker', ['докер'])
    ops.upsertTerm(d, 'docker', ['доккер', 'Докер', ' '])
    expect(d.dictionary).toHaveLength(1)
    expect(d.dictionary[0].variants).toEqual(['докер', 'доккер'])
  })

  it('updates and deletes terms', () => {
    const d = fresh()
    const t = ops.upsertTerm(d, 'GitHub', ['гитхаб'])!
    ops.updateTerm(d, t.id, { variants: ['гитхаб', 'гит хаб', 'GitHub'] })
    expect(d.dictionary[0].variants).toEqual(['гитхаб', 'гит хаб'])
    ops.deleteTerm(d, t.id)
    expect(d.dictionary).toHaveLength(0)
  })
})

describe('schema', () => {
  it('round-trips default data', () => {
    const d = fresh()
    ops.addItems(d, d.tabs[0].id, ['x'])
    expect(parseAppData(JSON.parse(JSON.stringify(d)))).toEqual(d)
  })

  it('repairs odd values instead of failing', () => {
    const d = JSON.parse(JSON.stringify(fresh()))
    d.tabs[0].color = 'neon'
    d.dictionary = 'oops'
    const parsed = parseAppData(d)
    expect(parsed.tabs[0].color).toBeNull()
    expect(parsed.dictionary).toEqual([])
  })

  it('rejects files without tabs', () => {
    expect(() => parseAppData({ schemaVersion: 1, tabs: [], items: {} })).toThrow()
  })

  it('normalizes broken references', () => {
    const d = fresh()
    d.items['ghost'] = [d.items[d.tabs[0].id][0] ?? { ...ops.addItems(d, d.tabs[0].id, ['g'])[0] }]
    d.activeTabId = 'missing'
    ops.normalizeData(d)
    expect(d.items['ghost']).toBeUndefined()
    expect(d.activeTabId).toBe(d.tabs[0].id)
  })

  it('fills missing settings with defaults', () => {
    expect(parseSettings(undefined)).toEqual(createDefaultSettings())
    const s = parseSettings({ theme: 'dark', speech: { language: 'en', keepLoadedMinutes: 'x' } })
    expect(s.theme).toBe('dark')
    expect(s.speech.language).toBe('en')
    expect(s.speech.keepLoadedMinutes).toBe(createDefaultSettings().speech.keepLoadedMinutes)
  })
})
