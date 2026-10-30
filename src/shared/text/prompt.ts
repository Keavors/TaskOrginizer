// Builds the text prompt Whisper sees before the audio.
//
// Benchmark (docs/ASR-BENCHMARK.md): with this style sentence alone, English
// names in Latin script went from 68% to 87.5%; adding the user's dictionary
// terms raised it to 97%.

/** Shows Whisper a Russian sentence that keeps English names in Latin script. */
export const STYLE_PROMPT = 'Список дел: открыть Slack, проверить задачи в Jira, обновить Chrome и макет в Figma.'

/** Whisper keeps only the last ~224 prompt tokens; stay well below that. */
export const MAX_PROMPT_CHARS = 450

export function buildRecognitionPrompt(terms: string[], maxChars = MAX_PROMPT_CHARS): string {
  const seen = new Set<string>()
  const unique: string[] = []
  for (const raw of terms) {
    const t = raw.trim().replace(/\s+/g, ' ')
    const key = t.toLowerCase()
    if (t && !seen.has(key)) {
      seen.add(key)
      unique.push(t)
    }
  }
  if (unique.length === 0) return STYLE_PROMPT

  const head = `${STYLE_PROMPT} Термины: `
  let list = ''
  for (const t of unique) {
    const next = list ? `${list}, ${t}` : t
    if (head.length + next.length + 1 > maxChars) break
    list = next
  }
  return list ? `${head}${list}.` : STYLE_PROMPT
}
