import { describe, expect, it } from 'vitest'
import type { DictionaryTerm } from '@shared/model'
import { applyDictionary } from '@shared/text/replacements'
import { suggestCorrections } from '@shared/text/learn'
import { cleanTranscript } from '@shared/text/cleanup'
import { buildRecognitionPrompt, STYLE_PROMPT, MAX_PROMPT_CHARS } from '@shared/text/prompt'
import { splitEntries, tidyDictation } from '@shared/text/entries'
import { stripNounEnding } from '@shared/text/russian'

const term = (text: string, ...variants: string[]): DictionaryTerm => ({ id: text, text, variants, createdAt: 0 })

describe('applyDictionary', () => {
  const dict = [term('Docker', 'докер'), term('Google Drive', 'гугл драйв'), term('Steam', 'стим'), term('Notion', 'наушен')]

  it('replaces whole words, ignoring case', () => {
    expect(applyDictionary('Настроить Докер на сервере', dict)).toBe('Настроить Docker на сервере')
  })

  it('handles Russian case endings', () => {
    expect(applyDictionary('урок по докеру и в докере', dict)).toBe('урок по Docker и в Docker')
    expect(applyDictionary('файлы в гугл драйве', dict)).toBe('файлы в Google Drive')
  })

  it('does not touch longer words that only start the same way', () => {
    expect(applyDictionary('нужен стимул поиграть в стим', dict)).toBe('нужен стимул поиграть в Steam')
  })

  it('accepts hyphens and spaces between words of a phrase', () => {
    expect(applyDictionary('скинуть в гугл-драйв', dict)).toBe('скинуть в Google Drive')
  })

  it('treats е and ё the same', () => {
    expect(applyDictionary('заметки из наушён', dict)).toBe('заметки из Notion')
  })

  it('can match exact forms only', () => {
    expect(applyDictionary('урок по докеру', dict, false)).toBe('урок по докеру')
  })

  it('leaves text alone when the dictionary is empty', () => {
    expect(applyDictionary('просто текст', [])).toBe('просто текст')
  })

  it('does not interpret $ in replacements', () => {
    expect(applyDictionary('купить кеш', [term('$cache', 'кеш')])).toBe('купить $cache')
  })
})

describe('suggestCorrections', () => {
  it('suggests a Cyrillic word replaced with a Latin one, without the ending', () => {
    expect(suggestCorrections('Посмотреть урок по докеру', 'Посмотреть урок по Docker')).toEqual([{ from: 'докер', to: 'Docker' }])
  })

  it('handles multi-word replacements', () => {
    expect(suggestCorrections('Скинуть файлы в гугл драйв', 'Скинуть файлы в Google Drive')).toEqual([
      { from: 'гугл драйв', to: 'Google Drive' }
    ])
  })

  it('ignores ordinary edits and frequent words', () => {
    expect(suggestCorrections('Купить молоко', 'Купить хлеб')).toEqual([])
    expect(suggestCorrections('нажать три раза', 'нажать Retry раза')).toEqual([])
  })

  it('returns nothing for identical text', () => {
    expect(suggestCorrections('Обновить Windows', 'Обновить Windows')).toEqual([])
  })
})

describe('cleanTranscript', () => {
  it('drops typical Whisper hallucinations', () => {
    expect(cleanTranscript('Продолжение следует...')).toBe('')
    expect(cleanTranscript('Субтитры сделал DimaTorzok')).toBe('')
    expect(cleanTranscript('Купить хлеб. Редактор субтитров А.Синецкая Корректор А.Егорова')).toBe('Купить хлеб.')
    expect(cleanTranscript('[Музыка]')).toBe('')
  })

  it('keeps normal text, including phrases that are only suspicious alone', () => {
    expect(cleanTranscript('  Позвонить   маме ')).toBe('Позвонить маме')
    expect(cleanTranscript('Сказать спасибо за внимание коллегам')).toBe('Сказать спасибо за внимание коллегам')
  })
})

describe('buildRecognitionPrompt', () => {
  it('uses only the style sentence without terms', () => {
    expect(buildRecognitionPrompt([])).toBe(STYLE_PROMPT)
  })

  it('lists unique terms and stays under the limit', () => {
    const p = buildRecognitionPrompt(['Docker', 'docker', ' GitHub '])
    expect(p).toBe(`${STYLE_PROMPT} Термины: Docker, GitHub.`)
    const many = Array.from({ length: 200 }, (_, i) => `Term${i}`)
    expect(buildRecognitionPrompt(many).length).toBeLessThanOrEqual(MAX_PROMPT_CHARS)
  })
})

describe('entries', () => {
  it('splits lines and strips list bullets', () => {
    expect(splitEntries('- купить хлеб\n\n2) позвонить\n• написать\n[ ] прочитать')).toEqual([
      'купить хлеб',
      'позвонить',
      'написать',
      'прочитать'
    ])
  })

  it('capitalizes dictation but keeps names like iPhone', () => {
    expect(tidyDictation('  купить   хлеб ')).toBe('Купить хлеб')
    expect(tidyDictation('iPhone зарядить')).toBe('iPhone зарядить')
  })

  it('strips noun endings only when a stem remains', () => {
    expect(stripNounEnding('докера')).toBe('докер')
    expect(stripNounEnding('гитхабе')).toBe('гитхаб')
    expect(stripNounEnding('зуме')).toBe('зум')
    expect(stripNounEnding('ему')).toBe('ему')
    expect(stripNounEnding('Docker')).toBe('Docker')
  })
})
