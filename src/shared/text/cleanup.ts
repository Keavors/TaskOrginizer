// Removes text Whisper is known to invent on silence or noise.
// Russian Whisper models learned these from subtitle credits on YouTube.

// A name with initials, as in the credits: "А.Синецкая", "А. Егорова".
const INITIALS_NAME = String.raw`(?:\s*[а-яё]\.\s?[а-яё]+)`

const REMOVE_ANYWHERE: RegExp[] = [
  new RegExp(String.raw`редактор субтитров${INITIALS_NAME}*[.!?]?`, 'giu'),
  new RegExp(String.raw`корректор${INITIALS_NAME}+[.!?]?`, 'giu'),
  /субтитры\s+(?:сделал|сделаны|создавал|создал|делал|подготовил|подогнал|создавались)[^.!?\n]*[.!?]?/giu,
  /dima\s?torzok/giu,
  /\[(?:музыка|смех|аплодисменты|шум|тишина|music|laughter|applause|silence|blank_audio|noise)[^\]]*\]/giu,
  /\((?:музыка|смех|аплодисменты|шум|тишина|music|laughter|applause)[^)]*\)/giu,
  /[♪♫]+/gu
]

// Dropped only when they are the whole result: a person could say them on purpose inside a longer note.
const DROP_IF_WHOLE: RegExp[] = [
  /^продолжение следует$/iu,
  /^спасибо за (?:просмотр|внимание)$/iu,
  /^(?:подписывайтесь|подпишитесь) на (?:наш )?канал$/iu,
  /^ставьте лайки?(?: и подписывайтесь на канал)?$/iu,
  /^thank you(?: for watching)?$/iu
]

export function cleanTranscript(raw: string): string {
  let t = raw
  for (const re of REMOVE_ANYWHERE) t = t.replace(re, ' ')
  t = t
    .replace(/\s+/gu, ' ')
    .replace(/\s+([,.!?;:…])/gu, '$1')
    .trim()
  const bare = t.replace(/[.!?…\s]+$/u, '').trim()
  if (bare === '' || DROP_IF_WHOLE.some((re) => re.test(bare))) return ''
  return t
}
