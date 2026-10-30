// Validation of everything read from disk. Non-critical fields fall back to
// defaults instead of failing the whole file, so one odd value never costs
// the user their data.
import { z } from 'zod'
import {
  createDefaultSettings,
  DATA_SCHEMA_VERSION,
  SETTINGS_SCHEMA_VERSION,
  TAB_COLORS,
  type AppData,
  type Settings
} from './model'

const id = z.string().min(1)
const timestamp = z.number().finite().catch(0)

const tabSchema = z.object({
  id,
  title: z.string().catch('Без названия'),
  kind: z.enum(['tasks', 'notes']).catch('tasks'),
  color: z.enum(TAB_COLORS).nullable().catch(null),
  createdAt: timestamp,
  updatedAt: timestamp
})

const itemSchema = z.object({
  id,
  text: z.string().catch(''),
  done: z.boolean().catch(false),
  important: z.boolean().catch(false),
  dueDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/)
    .nullable()
    .catch(null),
  createdAt: timestamp,
  updatedAt: timestamp,
  completedAt: z.number().finite().nullable().catch(null),
  source: z.enum(['keyboard', 'voice']).catch('keyboard')
})

const trashedItemSchema = z.object({
  kind: z.literal('item'),
  id,
  batchId: id,
  deletedAt: timestamp,
  tabId: id,
  tabTitle: z.string().catch(''),
  index: z.number().int().catch(0),
  item: itemSchema
})

const trashedTabSchema = z.object({
  kind: z.literal('tab'),
  id,
  batchId: id,
  deletedAt: timestamp,
  index: z.number().int().catch(0),
  tab: tabSchema,
  items: z.array(itemSchema)
})

const termSchema = z.object({
  id,
  text: z.string().min(1),
  variants: z.array(z.string()).catch([]),
  createdAt: timestamp
})

export const appDataSchema = z.object({
  schemaVersion: z.literal(DATA_SCHEMA_VERSION),
  tabs: z.array(tabSchema).min(1),
  items: z.record(z.string(), z.array(itemSchema)),
  activeTabId: id.nullable().catch(null),
  trash: z.array(z.discriminatedUnion('kind', [trashedItemSchema, trashedTabSchema])).catch([]),
  dictionary: z.array(termSchema).catch([])
})

/** Upgrades older files to the current shape. Only version 1 exists so far. */
export function migrateData(raw: unknown): unknown {
  return raw
}

export function parseAppData(raw: unknown): AppData {
  return appDataSchema.parse(migrateData(raw)) as AppData
}

const defaults = createDefaultSettings()

export const settingsSchema = z.object({
  schemaVersion: z.literal(SETTINGS_SCHEMA_VERSION).catch(SETTINGS_SCHEMA_VERSION),
  theme: z.enum(['system', 'light', 'dark']).catch(defaults.theme),
  newItemPosition: z.enum(['top', 'bottom']).catch(defaults.newItemPosition),
  speech: z
    .object({
      modelId: z.string().min(1).catch(defaults.speech.modelId),
      language: z.enum(['ru', 'auto', 'en']).catch(defaults.speech.language),
      device: z.enum(['auto', 'gpu', 'cpu']).catch(defaults.speech.device),
      micDeviceId: z.string().nullable().catch(null),
      keepLoadedMinutes: z.number().int().min(-1).max(24 * 60).catch(defaults.speech.keepLoadedMinutes),
      autoSubmit: z.boolean().catch(defaults.speech.autoSubmit)
    })
    .catch(defaults.speech)
})

/** Never throws: anything unreadable becomes the default value. */
export function parseSettings(raw: unknown): Settings {
  const result = settingsSchema.safeParse(raw ?? {})
  return result.success ? (result.data as Settings) : createDefaultSettings()
}
