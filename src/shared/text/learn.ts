// Finds corrections the user made to a dictated text, so the app can offer
// to remember them: "докер" -> "Docker".
import { hasCyrillic, hasLatin, stripPhraseEnding } from './russian'

export interface CorrectionSuggestion {
  /** Misrecognized form, lower case, without a noun ending ("докер"). */
  from: string
  /** What the user typed instead ("Docker"). */
  to: string
}

const WORD = /[\p{L}\p{N}]+(?:[-'’+.#][\p{L}\p{N}]+)*[+#]*/gu

// Frequent Russian words: replacing them everywhere would do more harm than good.
const STOP_WORDS = new Set(
  (
    'и в во не что он на я с со как а то все она так его но да ты к у же вы за бы по только ее мне было вот от меня еще нет о из ему ' +
    'теперь когда даже ну вдруг ли если уже или ни быть был него до вас нибудь опять уж вам ведь там потом себя ничего ей может они ' +
    'тут где есть надо ней для мы тебя их чем была сам чтоб без будто чего раз тоже себе под будет ж тогда кто этот того потому этого ' +
    'какой совсем ним здесь этом один почти мой тем чтобы нее сейчас были куда зачем всех никогда можно при наконец два об другой хоть ' +
    'после над больше тот через эти нас про всего них какая много разве три эту моя впрочем хорошо свою этой перед иногда лучше чуть ' +
    'том нельзя такой им более всегда конечно всю между четыре пять шесть семь восемь девять десять сто тысяча день неделя час'
  ).split(' ')
)

function words(s: string): string[] {
  return s.match(WORD) ?? []
}

interface Hunk {
  removed: string[]
  added: string[]
}

/** Word-level diff (LCS) grouped into hunks of consecutive changes. */
function diffHunks(a: string[], b: string[]): Hunk[] {
  const n = a.length
  const m = b.length
  const al = a.map((w) => w.toLowerCase())
  const bl = b.map((w) => w.toLowerCase())
  const lcs: number[][] = Array.from({ length: n + 1 }, () => new Array<number>(m + 1).fill(0))
  for (let i = n - 1; i >= 0; i--)
    for (let j = m - 1; j >= 0; j--)
      lcs[i][j] = al[i] === bl[j] ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1])

  const hunks: Hunk[] = []
  let cur: Hunk | null = null
  const flush = (): void => {
    if (cur && (cur.removed.length || cur.added.length)) hunks.push(cur)
    cur = null
  }
  let i = 0
  let j = 0
  while (i < n || j < m) {
    if (i < n && j < m && al[i] === bl[j]) {
      flush()
      i++
      j++
    } else if (j < m && (i === n || lcs[i][j + 1] >= lcs[i + 1][j])) {
      cur ??= { removed: [], added: [] }
      cur.added.push(b[j++])
    } else {
      cur ??= { removed: [], added: [] }
      cur.removed.push(a[i++])
    }
  }
  flush()
  return hunks
}

/**
 * Compares the recognized text with what the user finally saved and returns
 * corrections worth remembering: Cyrillic words replaced with Latin ones.
 */
export function suggestCorrections(recognized: string, edited: string): CorrectionSuggestion[] {
  const a = words(recognized)
  const b = words(edited)
  if (!a.length || !b.length || a.length > 300 || b.length > 300) return []

  const out: CorrectionSuggestion[] = []
  const seen = new Set<string>()
  for (const h of diffHunks(a, b)) {
    if (h.removed.length < 1 || h.removed.length > 3 || h.added.length < 1 || h.added.length > 3) continue
    const fromRaw = h.removed.join(' ')
    const to = h.added.join(' ')
    if (!hasCyrillic(fromRaw) || hasLatin(fromRaw) || !hasLatin(to)) continue
    if (h.removed.length === 1 && STOP_WORDS.has(fromRaw.toLowerCase())) continue
    const from = stripPhraseEnding(fromRaw.toLowerCase())
    const key = `${from}\u0000${to.toLowerCase()}`
    if (!seen.has(key)) {
      seen.add(key)
      out.push({ from, to })
    }
  }
  return out
}
