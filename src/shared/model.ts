// Domain model shared by the main process and the UI.
// Keep this file free of runtime dependencies: it is bundled into both sides.

export type Id = string

/** How a tab shows its entries: tasks get checkboxes, notes are plain entries. */
export type TabKind = 'tasks' | 'notes'

export const TAB_COLORS = ['blue', 'violet', 'pink', 'red', 'orange', 'amber', 'green', 'teal', 'gray'] as const
export type TabColor = (typeof TAB_COLORS)[number]

export interface Tab {
  id: Id
  title: string
  kind: TabKind
  color: TabColor | null
  createdAt: number
  updatedAt: number
}

export type ItemSource = 'keyboard' | 'voice'

export interface Item {
  id: Id
  text: string
  done: boolean
  important: boolean
  /** Local calendar date, `YYYY-MM-DD`. */
  dueDate: string | null
  createdAt: number
  updatedAt: number
  completedAt: number | null
  source: ItemSource
}

export interface TrashedItem {
  kind: 'item'
  id: Id
  /** Entries removed by one action share a batch id, so one "undo" restores them all. */
  batchId: Id
  deletedAt: number
  tabId: Id
  tabTitle: string
  index: number
  item: Item
}

export interface TrashedTab {
  kind: 'tab'
  id: Id
  batchId: Id
  deletedAt: number
  index: number
  tab: Tab
  items: Item[]
}

export type TrashEntry = TrashedItem | TrashedTab

/**
 * A word the user cares about (usually an English name) with the ways the
 * recognizer tends to mishear it. `text` goes into the recognition prompt,
 * every variant is replaced with `text` after recognition.
 */
export interface DictionaryTerm {
  id: Id
  text: string
  variants: string[]
  createdAt: number
}

export const DATA_SCHEMA_VERSION = 1

export interface AppData {
  schemaVersion: typeof DATA_SCHEMA_VERSION
  tabs: Tab[]
  /** Ordered entries of every tab, keyed by tab id. */
  items: Record<Id, Item[]>
  activeTabId: Id | null
  trash: TrashEntry[]
  dictionary: DictionaryTerm[]
}

export type ThemeMode = 'system' | 'light' | 'dark'
export type SpeechLanguage = 'ru' | 'auto' | 'en'
export type SpeechDevice = 'auto' | 'gpu' | 'cpu'
export type NewItemPosition = 'top' | 'bottom'

export const SETTINGS_SCHEMA_VERSION = 1

export interface Settings {
  schemaVersion: typeof SETTINGS_SCHEMA_VERSION
  theme: ThemeMode
  newItemPosition: NewItemPosition
  speech: {
    modelId: string
    language: SpeechLanguage
    device: SpeechDevice
    micDeviceId: string | null
    /** Minutes to keep the model in memory after the last use; 0 = unload at once, -1 = never unload. */
    keepLoadedMinutes: number
    /** Add the entry right after dictation instead of leaving the text in the input for review. */
    autoSubmit: boolean
  }
}

export const TRASH_RETENTION_DAYS = 30

export function newId(): Id {
  return crypto.randomUUID()
}

export function createTab(title: string, kind: TabKind = 'tasks', now = Date.now()): Tab {
  return { id: newId(), title, kind, color: null, createdAt: now, updatedAt: now }
}

export function createItem(text: string, source: ItemSource = 'keyboard', now = Date.now()): Item {
  return {
    id: newId(),
    text,
    done: false,
    important: false,
    dueDate: null,
    createdAt: now,
    updatedAt: now,
    completedAt: null,
    source
  }
}

export function createDefaultData(now = Date.now()): AppData {
  const tab = createTab('Задачи', 'tasks', now)
  return {
    schemaVersion: DATA_SCHEMA_VERSION,
    tabs: [tab],
    items: { [tab.id]: [] },
    activeTabId: tab.id,
    trash: [],
    dictionary: []
  }
}

export function createDefaultSettings(): Settings {
  return {
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    theme: 'system',
    newItemPosition: 'top',
    speech: {
      modelId: 'large-v3',
      language: 'ru',
      device: 'auto',
      micDeviceId: null,
      keepLoadedMinutes: 15,
      autoSubmit: false
    }
  }
}
