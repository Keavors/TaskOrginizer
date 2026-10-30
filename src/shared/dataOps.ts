// All changes to AppData go through these functions. They mutate the object
// they receive, so the UI store runs them inside an immer draft and tests run
// them on plain objects.
import {
  createItem,
  createTab,
  newId,
  TRASH_RETENTION_DAYS,
  type AppData,
  type DictionaryTerm,
  type Id,
  type Item,
  type ItemSource,
  type NewItemPosition,
  type Tab,
  type TabKind,
  type TrashedItem,
  type TrashedTab
} from './model'

const DAY_MS = 24 * 60 * 60 * 1000

export function getTab(data: AppData, tabId: Id): Tab | undefined {
  return data.tabs.find((t) => t.id === tabId)
}

export function tabItems(data: AppData, tabId: Id): Item[] {
  return (data.items[tabId] ??= [])
}

function clampIndex(index: number, length: number): number {
  return Math.max(0, Math.min(index, length))
}

function move<T>(list: T[], from: number, to: number): void {
  if (from < 0 || from >= list.length || to < 0 || to >= list.length || from === to) return
  const [x] = list.splice(from, 1)
  list.splice(to, 0, x)
}

// ---------------------------------------------------------------- tabs

export function addTab(data: AppData, title: string, kind: TabKind = 'tasks', now = Date.now()): Tab {
  const tab = createTab(title.trim() || 'Новая вкладка', kind, now)
  data.tabs.push(tab)
  data.items[tab.id] = []
  data.activeTabId = tab.id
  return tab
}

export function updateTab(
  data: AppData,
  tabId: Id,
  patch: Partial<Pick<Tab, 'title' | 'kind' | 'color'>>,
  now = Date.now()
): void {
  const tab = getTab(data, tabId)
  if (!tab) return
  if (patch.title !== undefined) {
    const title = patch.title.trim()
    if (title) tab.title = title
  }
  if (patch.kind !== undefined) tab.kind = patch.kind
  if (patch.color !== undefined) tab.color = patch.color
  tab.updatedAt = now
}

export function moveTab(data: AppData, fromId: Id, toId: Id): void {
  move(
    data.tabs,
    data.tabs.findIndex((t) => t.id === fromId),
    data.tabs.findIndex((t) => t.id === toId)
  )
}

export function setActiveTab(data: AppData, tabId: Id): void {
  if (getTab(data, tabId)) data.activeTabId = tabId
}

/** Moves a tab with all its entries to the trash. The last remaining tab cannot be deleted. */
export function deleteTab(data: AppData, tabId: Id, now = Date.now()): TrashedTab | undefined {
  const index = data.tabs.findIndex((t) => t.id === tabId)
  if (index < 0 || data.tabs.length <= 1) return undefined
  const [tab] = data.tabs.splice(index, 1)
  const entry: TrashedTab = {
    kind: 'tab',
    id: newId(),
    batchId: newId(),
    deletedAt: now,
    index,
    tab,
    items: data.items[tabId] ?? []
  }
  delete data.items[tabId]
  data.trash.unshift(entry)
  if (data.activeTabId === tabId) {
    data.activeTabId = data.tabs[Math.min(index, data.tabs.length - 1)].id
  }
  return entry
}

// ---------------------------------------------------------------- items

export function addItems(
  data: AppData,
  tabId: Id,
  texts: string[],
  opts: { source?: ItemSource; position?: NewItemPosition; now?: number } = {}
): Item[] {
  if (!getTab(data, tabId)) return []
  const now = opts.now ?? Date.now()
  const created = texts.map((t) => t.trim()).filter(Boolean).map((t) => createItem(t, opts.source ?? 'keyboard', now))
  const list = tabItems(data, tabId)
  if ((opts.position ?? 'top') === 'top') list.unshift(...created)
  else list.push(...created)
  return created
}

export function updateItem(
  data: AppData,
  tabId: Id,
  itemId: Id,
  patch: Partial<Pick<Item, 'text' | 'important' | 'dueDate'>>,
  now = Date.now()
): void {
  const item = tabItems(data, tabId).find((i) => i.id === itemId)
  if (!item) return
  if (patch.text !== undefined) item.text = patch.text
  if (patch.important !== undefined) item.important = patch.important
  if (patch.dueDate !== undefined) item.dueDate = patch.dueDate
  item.updatedAt = now
}

export function setItemDone(data: AppData, tabId: Id, itemId: Id, done: boolean, now = Date.now()): void {
  const item = tabItems(data, tabId).find((i) => i.id === itemId)
  if (!item || item.done === done) return
  item.done = done
  item.completedAt = done ? now : null
  item.updatedAt = now
}

/** Reorders entries of one tab: puts `activeId` where `overId` is. */
export function moveItem(data: AppData, tabId: Id, activeId: Id, overId: Id): void {
  const list = tabItems(data, tabId)
  move(
    list,
    list.findIndex((i) => i.id === activeId),
    list.findIndex((i) => i.id === overId)
  )
}

export function moveItemToTab(data: AppData, fromTabId: Id, itemId: Id, toTabId: Id, now = Date.now()): void {
  if (fromTabId === toTabId || !getTab(data, toTabId)) return
  const from = tabItems(data, fromTabId)
  const index = from.findIndex((i) => i.id === itemId)
  if (index < 0) return
  const [item] = from.splice(index, 1)
  item.updatedAt = now
  tabItems(data, toTabId).unshift(item)
}

/** Moves entries to the trash. Returns the trash entries (they share one batch id). */
export function deleteItems(data: AppData, tabId: Id, itemIds: Id[], now = Date.now()): TrashedItem[] {
  const tab = getTab(data, tabId)
  const list = tabItems(data, tabId)
  const ids = new Set(itemIds)
  const batchId = newId()
  const removed: TrashedItem[] = []
  for (let index = 0; index < list.length; ) {
    const item = list[index]
    if (ids.has(item.id)) {
      list.splice(index, 1)
      removed.push({
        kind: 'item',
        id: newId(),
        batchId,
        deletedAt: now,
        tabId,
        tabTitle: tab?.title ?? '',
        index: index + removed.length,
        item
      })
    } else {
      index++
    }
  }
  data.trash.unshift(...removed)
  return removed
}

export function clearCompleted(data: AppData, tabId: Id, now = Date.now()): TrashedItem[] {
  const done = tabItems(data, tabId).filter((i) => i.done).map((i) => i.id)
  return done.length ? deleteItems(data, tabId, done, now) : []
}

// ---------------------------------------------------------------- trash

/** Restores trash entries; tabs first, so their entries find their place. Returns the restored count. */
export function restoreTrash(data: AppData, entryIds: Id[]): number {
  const ids = new Set(entryIds)
  const entries = data.trash.filter((e) => ids.has(e.id))
  if (!entries.length) return 0
  data.trash = data.trash.filter((e) => !ids.has(e.id))

  const tabs = entries.filter((e): e is TrashedTab => e.kind === 'tab').sort((a, b) => a.index - b.index)
  for (const e of tabs) {
    if (getTab(data, e.tab.id)) continue
    data.tabs.splice(clampIndex(e.index, data.tabs.length), 0, e.tab)
    data.items[e.tab.id] = [...e.items, ...(data.items[e.tab.id] ?? [])]
    data.activeTabId = e.tab.id
  }

  const items = entries.filter((e): e is TrashedItem => e.kind === 'item').sort((a, b) => a.index - b.index)
  for (const e of items) {
    const tabId = getTab(data, e.tabId) ? e.tabId : (data.activeTabId ?? data.tabs[0].id)
    const list = tabItems(data, tabId)
    if (list.some((i) => i.id === e.item.id)) continue
    list.splice(clampIndex(e.index, list.length), 0, e.item)
  }
  return entries.length
}

export function restoreBatch(data: AppData, batchId: Id): number {
  return restoreTrash(
    data,
    data.trash.filter((e) => e.batchId === batchId).map((e) => e.id)
  )
}

export function deleteTrashEntries(data: AppData, entryIds: Id[]): void {
  const ids = new Set(entryIds)
  data.trash = data.trash.filter((e) => !ids.has(e.id))
}

export function emptyTrash(data: AppData): void {
  data.trash = []
}

export function purgeOldTrash(data: AppData, now = Date.now(), days = TRASH_RETENTION_DAYS): void {
  const limit = now - days * DAY_MS
  data.trash = data.trash.filter((e) => e.deletedAt >= limit)
}

// ---------------------------------------------------------------- dictionary

function findTerm(data: AppData, text: string): DictionaryTerm | undefined {
  const key = text.trim().toLowerCase()
  return data.dictionary.find((t) => t.text.trim().toLowerCase() === key)
}

function cleanVariants(variants: string[], text: string): string[] {
  const seen = new Set<string>([text.trim().toLowerCase()])
  const out: string[] = []
  for (const raw of variants) {
    const v = raw.trim().replace(/\s+/g, ' ')
    const key = v.toLowerCase()
    if (v && !seen.has(key)) {
      seen.add(key)
      out.push(v)
    }
  }
  return out
}

/** Adds a term, or merges the variants into an existing term with the same spelling. */
export function upsertTerm(data: AppData, text: string, variants: string[] = [], now = Date.now()): DictionaryTerm | undefined {
  const clean = text.trim().replace(/\s+/g, ' ')
  if (!clean) return undefined
  const existing = findTerm(data, clean)
  if (existing) {
    existing.variants = cleanVariants([...existing.variants, ...variants], existing.text)
    return existing
  }
  const term: DictionaryTerm = { id: newId(), text: clean, variants: cleanVariants(variants, clean), createdAt: now }
  data.dictionary.unshift(term)
  return term
}

export function updateTerm(data: AppData, termId: Id, patch: { text?: string; variants?: string[] }): void {
  const term = data.dictionary.find((t) => t.id === termId)
  if (!term) return
  if (patch.text !== undefined && patch.text.trim()) term.text = patch.text.trim().replace(/\s+/g, ' ')
  if (patch.variants !== undefined) term.variants = cleanVariants(patch.variants, term.text)
  else term.variants = cleanVariants(term.variants, term.text)
}

export function deleteTerm(data: AppData, termId: Id): void {
  data.dictionary = data.dictionary.filter((t) => t.id !== termId)
}

// ---------------------------------------------------------------- integrity

/** Repairs references after loading or importing: every tab has a list, the active tab exists. */
export function normalizeData(data: AppData): AppData {
  if (data.tabs.length === 0) {
    const tab = createTab('Задачи')
    data.tabs.push(tab)
  }
  const tabIds = new Set(data.tabs.map((t) => t.id))
  for (const id of tabIds) data.items[id] ??= []
  // Entries of a tab that no longer exists are kept: they move to the first tab.
  for (const id of Object.keys(data.items)) {
    if (!tabIds.has(id)) {
      data.items[data.tabs[0].id].push(...data.items[id])
      delete data.items[id]
    }
  }
  if (!data.activeTabId || !tabIds.has(data.activeTabId)) data.activeTabId = data.tabs[0].id
  return data
}
