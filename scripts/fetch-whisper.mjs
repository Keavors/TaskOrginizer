// Downloads the pinned whisper.cpp CPU runtime into resources/whisper.
// The CUDA (GPU) pack is large, so the app downloads it on demand instead.
//
//   node scripts/fetch-whisper.mjs              always re-download
//   node scripts/fetch-whisper.mjs --if-missing skip when the right build is already there
import { createHash } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { Readable } from 'node:stream'
import { pipeline } from 'node:stream/promises'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const runtime = JSON.parse(fs.readFileSync(path.join(root, 'src/shared/whisperRuntime.json'), 'utf8'))
const targetDir = path.join(root, 'resources', 'whisper')
const markerFile = path.join(targetDir, 'version.json')

if (process.platform !== 'win32') {
  console.log('[fetch-whisper] Not Windows: skipping (only Windows builds are packaged for now).')
  process.exit(0)
}

if (process.argv.includes('--if-missing') && fs.existsSync(markerFile)) {
  const marker = JSON.parse(fs.readFileSync(markerFile, 'utf8'))
  if (marker.build === runtime.build) {
    console.log(`[fetch-whisper] whisper.cpp ${runtime.build} is already in place.`)
    process.exit(0)
  }
}

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'task-organizer-whisper-'))
try {
  const zipPath = path.join(tmp, 'whisper.zip')
  console.log(`[fetch-whisper] Downloading ${runtime.cpu.url}`)
  const res = await fetch(runtime.cpu.url)
  if (!res.ok || !res.body) throw new Error(`HTTP ${res.status} for ${runtime.cpu.url}`)
  await pipeline(Readable.fromWeb(res.body), fs.createWriteStream(zipPath))

  const hash = createHash('sha256').update(fs.readFileSync(zipPath)).digest('hex')
  if (hash !== runtime.cpu.sha256) throw new Error(`SHA-256 mismatch: got ${hash}, expected ${runtime.cpu.sha256}`)

  const extractDir = path.join(tmp, 'x')
  fs.mkdirSync(extractDir)
  // bsdtar ships with Windows 10+ and understands zip archives.
  execFileSync(path.join(process.env.SystemRoot ?? 'C:\\Windows', 'System32', 'tar.exe'), ['-xf', zipPath, '-C', extractDir])
  const releaseDir = path.join(extractDir, 'Release')

  fs.rmSync(targetDir, { recursive: true, force: true })
  fs.mkdirSync(targetDir, { recursive: true })
  const patterns = runtime.cpu.files.map((p) => new RegExp('^' + p.replace(/[.]/g, '\\.').replace(/\*/g, '.*') + '$', 'i'))
  let copied = 0
  for (const name of fs.readdirSync(releaseDir)) {
    if (patterns.some((re) => re.test(name))) {
      fs.copyFileSync(path.join(releaseDir, name), path.join(targetDir, name))
      copied++
    }
  }
  if (!fs.existsSync(path.join(targetDir, 'whisper-server.exe'))) throw new Error('whisper-server.exe not found in archive')

  // App-local Visual C++ runtime, so the engine starts even without the VC++ redistributable.
  const system32 = path.join(process.env.SystemRoot ?? 'C:\\Windows', 'System32')
  for (const dll of runtime.vcRuntime) {
    const src = path.join(system32, dll)
    if (fs.existsSync(src)) fs.copyFileSync(src, path.join(targetDir, dll))
    else console.warn(`[fetch-whisper] ${dll} not found in System32; the engine will rely on the installed VC++ runtime.`)
  }

  fs.writeFileSync(markerFile, JSON.stringify({ build: runtime.build, version: runtime.version, sha256: runtime.cpu.sha256 }, null, 2))
  console.log(`[fetch-whisper] Installed whisper.cpp ${runtime.version} (${runtime.build}): ${copied} files -> ${path.relative(root, targetDir)}`)
} finally {
  fs.rmSync(tmp, { recursive: true, force: true })
}
