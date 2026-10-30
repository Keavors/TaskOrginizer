// Turns typed or pasted text into separate entries: one line, one entry.

const BULLET = /^(?:[-–—•*·]\s+|\d{1,3}[.)]\s+|\[\s?[xх ]?\s?\]\s+)/iu

export function splitEntries(text: string): string[] {
  return text
    .split(/\r?\n/u)
    .map((line) => line.replace(BULLET, '').trim())
    .filter(Boolean)
}

/** Capital first letter, no extra spaces: how dictated text should look as an entry. */
export function tidyDictation(text: string): string {
  const t = text.replace(/\s+/gu, ' ').trim()
  if (!t) return t
  // Leave names like "iPhone" or "eBay" as they are.
  if (/^[a-z]+[A-Z]/u.test(t)) return t
  return t.charAt(0).toLocaleUpperCase('ru-RU') + t.slice(1)
}
