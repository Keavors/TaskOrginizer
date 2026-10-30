// ASR benchmark: Russian speech with embedded English terms.
// Usage: node run_bench.mjs <binDir> <modelsDir> <benchDir> <outJson>
import { spawn, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const [binDir, modelsDir, benchDir, outJson] = process.argv.slice(2)
const sentences = JSON.parse(fs.readFileSync(path.join(benchDir, 'sentences.json'), 'utf8'))
const audioDir = path.join(benchDir, 'audio')
const clips = fs.readdirSync(audioDir).filter((f) => f.endsWith('.mp3')).sort()
const byId = Object.fromEntries(sentences.map((s) => [s.id, s]))

const allTerms = [...new Set(sentences.flatMap((s) => s.terms))]
const STYLE = 'Список дел: открыть Slack, проверить задачи в Jira, обновить Chrome и макет в Figma.'
const PROMPTS = {
  none: '',
  style: STYLE,
  dict: `${STYLE} Термины: ${allTerms.join(', ')}.`
}

const norm = (s) =>
  s.toLowerCase().replace(/ё/g, 'е').replace(/[.,!?;:«»"“”()[\]…]/g, ' ').replace(/[-–—]/g, ' ').replace(/\s+/g, ' ').trim()

function wordErrors(ref, hyp) {
  const r = norm(ref).split(' ').filter(Boolean)
  const h = norm(hyp).split(' ').filter(Boolean)
  const d = Array.from({ length: r.length + 1 }, (_, i) => [i, ...Array(h.length).fill(0)])
  for (let j = 1; j <= h.length; j++) d[0][j] = j
  for (let i = 1; i <= r.length; i++)
    for (let j = 1; j <= h.length; j++)
      d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + (r[i - 1] === h[j - 1] ? 0 : 1))
  return { errors: d[r.length][h.length], words: r.length }
}

const termHit = (hyp, term) => norm(hyp).includes(norm(term))

function score(results) {
  let errors = 0, words = 0, termsOk = 0, termsAll = 0, pureErr = 0, pureWords = 0, ms = 0
  for (const r of results) {
    const s = byId[r.id]
    const we = wordErrors(s.ref, r.text)
    errors += we.errors; words += we.words; ms += r.ms
    if (s.terms.length === 0) { pureErr += we.errors; pureWords += we.words }
    for (const t of s.terms) { termsAll++; if (termHit(r.text, t)) termsOk++ }
  }
  return {
    wer: +(100 * errors / words).toFixed(1),
    termAcc: +(100 * termsOk / termsAll).toFixed(1),
    pureRuWer: +(100 * pureErr / pureWords).toFixed(1),
    avgMs: Math.round(ms / results.length)
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function startServer(model, port, lang) {
  const args = ['-m', path.join(modelsDir, model), '--host', '127.0.0.1', '--port', String(port), '-l', lang, '-t', '8',
    '--vad-model', path.join(modelsDir, 'ggml-silero-v6.2.0.bin')]
  const proc = spawn(path.join(binDir, 'whisper-server.exe'), args, { cwd: binDir, stdio: ['ignore', 'pipe', 'pipe'] })
  let log = ''
  proc.stdout.on('data', (d) => (log += d))
  proc.stderr.on('data', (d) => (log += d))
  const t0 = Date.now()
  for (;;) {
    if (proc.exitCode !== null) throw new Error('server exited: ' + log.slice(-2000))
    try {
      const res = await fetch(`http://127.0.0.1:${port}/health`)
      if (res.ok) break
    } catch {}
    await sleep(200)
  }
  const gpu = /CUDA|cuda/.test(log) ? (log.match(/ggml_cuda_init:.*|using CUDA.*|CUDA0.*/)?.[0] ?? 'cuda?') : 'cpu'
  return { proc, loadMs: Date.now() - t0, gpu, getLog: () => log }
}

async function transcribe(port, file, { prompt, vad }) {
  const fd = new FormData()
  fd.append('file', new Blob([fs.readFileSync(file)]), path.basename(file))
  fd.append('response_format', 'json')
  fd.append('temperature', '0.0')
  fd.append('temperature_inc', '0.2')
  fd.append('no_timestamps', 'true')
  if (prompt) { fd.append('prompt', prompt); fd.append('carry_initial_prompt', 'true') }
  if (vad) fd.append('vad', 'true')
  const t0 = performance.now()
  const res = await fetch(`http://127.0.0.1:${port}/inference`, { method: 'POST', body: fd })
  const body = await res.text()
  const ms = Math.round(performance.now() - t0)
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${body}`)
  return { text: JSON.parse(body).text.trim(), ms }
}

const configs = [
  { name: 'large-v3 | ru | no prompt', model: 'ggml-large-v3.bin', lang: 'ru', prompt: 'none' },
  { name: 'large-v3 | ru | style prompt', model: 'ggml-large-v3.bin', lang: 'ru', prompt: 'style' },
  { name: 'large-v3 | ru | dictionary prompt', model: 'ggml-large-v3.bin', lang: 'ru', prompt: 'dict' },
  { name: 'large-v3 | ru | dictionary prompt + VAD', model: 'ggml-large-v3.bin', lang: 'ru', prompt: 'dict', vad: true },
  { name: 'large-v3 | auto | style prompt', model: 'ggml-large-v3.bin', lang: 'auto', prompt: 'style' },
  { name: 'large-v3-turbo | ru | no prompt', model: 'ggml-large-v3-turbo.bin', lang: 'ru', prompt: 'none' },
  { name: 'large-v3-turbo | ru | style prompt', model: 'ggml-large-v3-turbo.bin', lang: 'ru', prompt: 'style' },
  { name: 'large-v3-turbo | ru | dictionary prompt', model: 'ggml-large-v3-turbo.bin', lang: 'ru', prompt: 'dict' }
]

const only = process.env.ONLY ? new RegExp(process.env.ONLY) : null
const out = fs.existsSync(outJson) ? JSON.parse(fs.readFileSync(outJson, 'utf8')) : {}
let port = 18900
for (const cfg of configs) {
  if (only && !only.test(cfg.name)) continue
  const srv = await startServer(cfg.model, ++port, cfg.lang)
  try {
    const results = []
    for (const clip of clips) {
      const id = clip.split('_')[0]
      const r = await transcribe(port, path.join(audioDir, clip), { prompt: PROMPTS[cfg.prompt], vad: cfg.vad })
      results.push({ id, clip, ...r })
    }
    out[cfg.name] = { ...score(results), loadMs: srv.loadMs, gpu: srv.gpu, results }
    console.log(cfg.name.padEnd(45), JSON.stringify({ ...score(results), loadMs: srv.loadMs }))
    fs.writeFileSync(outJson, JSON.stringify(out, null, 1))
  } finally {
    srv.proc.kill()
    await sleep(500)
  }
}

// Parakeet (no prompt support): one CLI call per clip
if (!only || only.test('parakeet')) {
  const results = []
  for (const clip of clips) {
    const id = clip.split('_')[0]
    const t0 = performance.now()
    const r = spawnSync(path.join(binDir, 'parakeet-cli.exe'),
      ['-m', path.join(modelsDir, 'ggml-parakeet-tdt-0.6b-v3-f16.bin'), '-np', '-f', path.join(audioDir, clip)],
      { cwd: binDir, encoding: 'utf8' })
    results.push({ id, clip, text: (r.stdout || '').trim(), ms: Math.round(performance.now() - t0), err: r.status ? r.stderr.slice(-500) : undefined })
  }
  out['parakeet-tdt-0.6b-v3 (CLI, incl. model load)'] = { ...score(results), results }
  console.log('parakeet-tdt-0.6b-v3'.padEnd(45), JSON.stringify(score(results)))
  fs.writeFileSync(outJson, JSON.stringify(out, null, 1))
}
