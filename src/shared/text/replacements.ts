import type { DictionaryTerm } from '../model'
import { RU_NOUN_ENDINGS } from './russian'

const WORD_CHAR = '[\\p{L}\\p{N}]'
const SEPARATOR = '[\\s\\-–—]+'

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function charPattern(ch: string): string {
  return ch === 'е' || ch === 'ё' ? '[её]' : escapeRegExp(ch)
}

/**
 * Regex source that finds `variant` as a whole word or phrase, ignoring case,
 * treating "е"/"ё" as equal and spaces/hyphens as interchangeable.
 * With `inflections`, a Russian noun ending after the last word also matches.
 */
export function variantPattern(variant: string, inflections: boolean): string | null {
  const words = variant.toLowerCase().split(/[\s\-–—]+/u).filter(Boolean)
  if (words.length === 0) return null
  const body = words.map((w) => Array.from(w).map(charPattern).join('')).join(SEPARATOR)
  const last = words[words.length - 1]
  const ending = inflections && /[а-яё]$/u.test(last) ? `(?:${RU_NOUN_ENDINGS.join('|')})?` : ''
  return `(?<!${WORD_CHAR})${body}${ending}(?!${WORD_CHAR})`
}

interface Rule {
  re: RegExp
  to: string
}

function compileRules(terms: DictionaryTerm[], inflections: boolean): Rule[] {
  const pairs: { from: string; to: string }[] = []
  for (const term of terms) {
    const to = term.text.trim()
    if (!to) continue
    for (const raw of term.variants) {
      const from = raw.trim()
      if (from && from.toLowerCase() !== to.toLowerCase()) pairs.push({ from, to })
    }
  }
  // Longer variants first, so "гугл драйв" wins over "гугл".
  pairs.sort((a, b) => b.from.length - a.from.length)
  const rules: Rule[] = []
  for (const p of pairs) {
    const src = variantPattern(p.from, inflections)
    if (src) rules.push({ re: new RegExp(src, 'giu'), to: p.to })
  }
  return rules
}

/** Replaces every known misrecognition with the correct spelling from the dictionary. */
export function applyDictionary(text: string, terms: DictionaryTerm[], inflections = true): string {
  let out = text
  for (const rule of compileRules(terms, inflections)) {
    out = out.replace(rule.re, () => rule.to)
  }
  return out
}
