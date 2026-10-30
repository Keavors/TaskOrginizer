// Contract between the main process and the UI. The preload script exposes
// `TaskOrganizerApi` as `window.api`; channel names live here so both sides
// stay in sync.
import type { AppData, Settings, SpeechLanguage } from './model'

export const IPC = {
  appInfo: 'app:info',
  appOpenFolder: 'app:open-folder',
  appTitleBarTheme: 'app:title-bar-theme',

  dataLoad: 'data:load',
  dataSave: 'data:save',
  dataSaveSync: 'data:save-sync',
  dataExport: 'data:export',
  dataImport: 'data:import',

  settingsLoad: 'settings:load',
  settingsSave: 'settings:save',

  speechStatus: 'speech:status',
  speechPrepare: 'speech:prepare',
  speechTranscribe: 'speech:transcribe',
  speechStatusChanged: 'speech:status-changed',

  setupState: 'setup:state',
  setupDownloadModel: 'setup:download-model',
  setupRemoveModel: 'setup:remove-model',
  setupInstallGpu: 'setup:install-gpu',
  setupRemoveGpu: 'setup:remove-gpu',
  setupCancel: 'setup:cancel',
  setupChanged: 'setup:changed'
} as const

export interface AppInfo {
  version: string
  whisperVersion: string
  dataDir: string
  modelsDir: string
}

export interface LoadDataResult {
  data: AppData
  /** Set when the main file was unreadable and a backup or a fresh file was used. */
  recoveredFrom: 'backup' | 'new' | null
}

export type SpeechState = 'not-installed' | 'stopped' | 'loading' | 'ready' | 'busy' | 'error'
export type ComputeDevice = 'gpu' | 'cpu'

export interface SpeechStatus {
  state: SpeechState
  modelId: string
  device: ComputeDevice | null
  /** Human-readable details for errors and warnings (in Russian). */
  message: string | null
}

export interface TranscribeRequest {
  /** Mono PCM, 16 kHz, values in [-1, 1]. */
  pcm: Float32Array
  language: SpeechLanguage
  prompt: string
}

export interface TranscribeResult {
  text: string
  audioSeconds: number
  processingMs: number
  device: ComputeDevice
}

export interface Progress {
  phase: 'downloading' | 'verifying' | 'installing'
  received: number
  total: number
  bytesPerSecond: number
}

export interface ComponentState {
  installed: boolean
  /** Present while a download or install is running. */
  progress: Progress | null
  error: string | null
}

export interface GpuInfo {
  /** An NVIDIA driver is installed (nvcuda.dll found). */
  nvidia: boolean
  name: string | null
  vramMb: number | null
}

export interface SetupState {
  models: Record<string, ComponentState>
  vad: ComponentState
  gpuPack: ComponentState
  gpu: GpuInfo
  /** Bundled CPU engine files are present. */
  engineReady: boolean
}

export type Unsubscribe = () => void

export interface TaskOrganizerApi {
  app: {
    info(): Promise<AppInfo>
    openFolder(which: 'data' | 'models'): Promise<void>
    setTitleBarTheme(theme: { background: string; symbols: string }): void
  }
  data: {
    load(): Promise<LoadDataResult>
    save(data: AppData): Promise<void>
    /** Blocking save for the moment the window closes. */
    saveSync(data: AppData): boolean
    exportToFile(data: AppData): Promise<string | null>
    importFromFile(): Promise<AppData | null>
  }
  settings: {
    load(): Promise<Settings>
    save(settings: Settings): Promise<void>
  }
  speech: {
    getStatus(): Promise<SpeechStatus>
    /** Starts loading the model in the background (called when recording starts). */
    prepare(): Promise<void>
    transcribe(request: TranscribeRequest): Promise<TranscribeResult>
    onStatus(listener: (status: SpeechStatus) => void): Unsubscribe
  }
  setup: {
    getState(): Promise<SetupState>
    downloadModel(modelId: string): Promise<void>
    removeModel(modelId: string): Promise<void>
    installGpuPack(): Promise<void>
    removeGpuPack(): Promise<void>
    /** Cancels a running download: a model id, 'vad' or 'gpu'. */
    cancel(componentId: string): Promise<void>
    onChange(listener: (state: SetupState) => void): Unsubscribe
  }
}
