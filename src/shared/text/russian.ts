// Small helpers for Russian word forms.

/**
 * Case and number endings that Russian adds to borrowed nouns
 * ("докер" -> "докера", "докеру", "докером", "докере"). Longest first.
 */
export const RU_NOUN_ENDINGS = [
  'ами', 'ями',
  'ов', 'ев', 'ей', 'ам', 'ям', 'ах', 'ях', 'ом', 'ем', 'ём', 'ой', 'ою', 'ую',
  'а', 'я', 'у', 'ю', 'е', 'ы', 'и'
] as const

const CYRILLIC_WORD = /^[а-яё]+$/iu

/** Strips one noun ending if a stem of at least `minStem` letters remains. */
export function stripNounEnding(word: string, minStem = 3): string {
  if (!CYRILLIC_WORD.test(word)) return word
  const lower = word.toLowerCase()
  for (const ending of RU_NOUN_ENDINGS) {
    if (lower.endsWith(ending) && lower.length - ending.length >= minStem) {
      return word.slice(0, word.length - ending.length)
    }
  }
  return word
}

/** Strips the ending of the last word of a phrase ("гугл драйве" -> "гугл драйв"). */
export function stripPhraseEnding(phrase: string): string {
  const words = phrase.trim().split(/\s+/u)
  words[words.length - 1] = stripNounEnding(words[words.length - 1])
  return words.join(' ')
}

export const hasCyrillic = (s: string): boolean => /[а-яё]/iu.test(s)
export const hasLatin = (s: string): boolean => /[a-z]/iu.test(s)
