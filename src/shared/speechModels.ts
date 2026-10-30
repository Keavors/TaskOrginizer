// Catalog of speech recognition models the app can download and run offline.
// Sizes and SHA-256 come from the Hugging Face file listings.

export interface DownloadableFile {
  id: string
  file: string
  url: string
  size: number
  sha256: string
}

export interface SpeechModelSpec extends DownloadableFile {
  title: string
  summary: string
  /** Rough GPU memory needed while the model is loaded, in MB. */
  vramMb: number
}

const WHISPER_HF = 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/'

export const SPEECH_MODELS: SpeechModelSpec[] = [
  {
    id: 'large-v3',
    file: 'ggml-large-v3.bin',
    url: WHISPER_HF + 'ggml-large-v3.bin',
    size: 3095033483,
    sha256: '64d182b440b98d5203c4f9bd541544d84c605196c4f7b845dfa11fb23594d1e2',
    title: 'Whisper Large v3',
    summary: 'Самая точная. Лучше всех пишет английские названия в русской речи. Рекомендуется видеокарта NVIDIA от 6 ГБ.',
    vramMb: 4300
  },
  {
    id: 'large-v3-q5_0',
    file: 'ggml-large-v3-q5_0.bin',
    url: WHISPER_HF + 'ggml-large-v3-q5_0.bin',
    size: 1081140203,
    sha256: 'd75795ecff3f83b5faa89d1900604ad8c780abd5739fae406de19f23ecd98ad1',
    title: 'Whisper Large v3, сжатая',
    summary: 'Почти та же точность, файл втрое меньше. Подходит для видеокарт с 4 ГБ памяти.',
    vramMb: 2300
  },
  {
    id: 'large-v3-turbo',
    file: 'ggml-large-v3-turbo.bin',
    url: WHISPER_HF + 'ggml-large-v3-turbo.bin',
    size: 1624555275,
    sha256: '1fc70f774d38eb169993ac391eea357ef47c88757ef72ee5943879b7e8e2bc69',
    title: 'Whisper Large v3 Turbo',
    summary: 'Вдвое быстрее, но чаще ошибается в английских словах.',
    vramMb: 2500
  },
  {
    id: 'large-v3-turbo-q5_0',
    file: 'ggml-large-v3-turbo-q5_0.bin',
    url: WHISPER_HF + 'ggml-large-v3-turbo-q5_0.bin',
    size: 574041195,
    sha256: '394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2',
    title: 'Whisper Large v3 Turbo, сжатая',
    summary: 'Самая лёгкая. Для компьютеров без видеокарты.',
    vramMb: 1300
  }
]

/** Voice activity detection model: cuts silence so Whisper does not invent text on pauses. */
export const VAD_MODEL: DownloadableFile = {
  id: 'silero-vad',
  file: 'ggml-silero-v6.2.0.bin',
  url: 'https://huggingface.co/ggml-org/whisper-vad/resolve/main/ggml-silero-v6.2.0.bin',
  size: 885098,
  sha256: '2aa269b785eeb53a82983a20501ddf7c1d9c48e33ab63a41391ac6c9f7fb6987'
}

export const DEFAULT_MODEL_ID = 'large-v3'

export function findSpeechModel(id: string): SpeechModelSpec | undefined {
  return SPEECH_MODELS.find((m) => m.id === id)
}
