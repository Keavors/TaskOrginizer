package com.keavors.taskorganizer.speech

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.ceil

sealed interface EngineState {
    data object NotInstalled : EngineState
    data object Unloaded : EngineState
    data object Loading : EngineState
    data object Ready : EngineState
    data object Busy : EngineState
    data class Failed(val message: String) : EngineState
}

class SpeechException(message: String) : Exception(message)

class Transcription(val text: String, val audioSeconds: Double, val processingMs: Long)

/**
 * Owns the whisper.cpp context. All native calls run on one dedicated thread.
 * The model is loaded in the background (when the app opens or recording
 * starts) so that a dictation does not wait for it.
 */
class SpeechEngine(private val context: Context, private val files: ModelFiles, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<EngineState>(if (files.modelInstalled()) EngineState.Unloaded else EngineState.NotInstalled)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    /** Last recognition time, shown in the settings. */
    private val _lastRun = MutableStateFlow<Transcription?>(null)
    val lastRun: StateFlow<Transcription?> = _lastRun.asStateFlow()

    var systemInfo: String? = null
        private set

    private val native = Executors.newSingleThreadExecutor { Thread(it, "whisper").apply { priority = Thread.MAX_PRIORITY } }
        .asCoroutineDispatcher()
    @Volatile private var handle = 0L
    private var backendsReady = false
    private var loadJob: Job? = null
    private var unloadJob: Job? = null

    fun onModelInstalled() {
        if (_state.value == EngineState.NotInstalled) _state.value = EngineState.Unloaded
        prepare()
    }

    fun onModelRemoved() {
        unload()
        _state.value = EngineState.NotInstalled
    }

    /** Starts loading the model in the background if it is not loaded yet. */
    fun prepare() {
        unloadJob?.cancel()
        if (!files.modelInstalled() || handle != 0L || loadJob?.isActive == true) return
        loadJob = scope.launch(native) { ensureLoaded() }
    }

    private fun ensureLoaded(): Boolean {
        if (handle != 0L) return true
        if (!files.modelInstalled()) {
            _state.value = EngineState.NotInstalled
            return false
        }
        _state.value = EngineState.Loading
        val started = SystemClock.elapsedRealtime()
        val h = try {
            if (!backendsReady) {
                WhisperNative.initBackends(context.applicationInfo.nativeLibraryDir)
                systemInfo = WhisperNative.systemInfo()
                Log.i(TAG, "system: $systemInfo")
                backendsReady = true
            }
            WhisperNative.initContext(files.model.absolutePath)
        } catch (e: Throwable) {
            // A missing or incompatible native library must not take the whole app down.
            Log.e(TAG, "native init failed", e)
            _state.value = EngineState.Failed("Движок распознавания не запустился на этом телефоне: ${e.javaClass.simpleName}")
            return false
        }
        if (h == 0L) {
            _state.value = EngineState.Failed("Не удалось загрузить модель. Возможно, файл повреждён: удалите её в настройках и скачайте заново.")
            return false
        }
        handle = h
        Log.i(TAG, "model loaded in ${SystemClock.elapsedRealtime() - started} ms")
        _state.value = EngineState.Ready
        return true
    }

    /**
     * Recognizes 16 kHz mono PCM. [fast] shrinks Whisper's 30-second encoder
     * window to the length of the phrase, which is several times faster for short phrases.
     */
    suspend fun transcribe(pcm: FloatArray, language: String, prompt: String, fast: Boolean, threads: Int): Transcription =
        withContext(native) {
            unloadJob?.cancel()
            if (!ensureLoaded()) throw SpeechException((state.value as? EngineState.Failed)?.message ?: "Модель распознавания не скачана.")
            _state.value = EngineState.Busy
            val seconds = pcm.size / AudioRecorder.SAMPLE_RATE.toDouble()
            val started = SystemClock.elapsedRealtime()
            try {
                val bytes = WhisperNative.transcribe(
                    handle, pcm, language, prompt, files.vadModel()?.absolutePath,
                    if (threads > 0) threads else defaultThreads(), if (fast) audioContextFor(seconds) else 0
                ) ?: throw SpeechException("Распознавание прервано.")
                Transcription(bytes.toString(Charsets.UTF_8), seconds, SystemClock.elapsedRealtime() - started).also {
                    Log.i(TAG, "%.1f s of audio in %d ms (fast=%b)".format(seconds, it.processingMs, fast))
                    _lastRun.value = it
                }
            } finally {
                _state.value = if (handle != 0L) EngineState.Ready else EngineState.Unloaded
            }
        }

    /** Stops a running recognition; safe from any thread. */
    fun cancelRunning() {
        val h = handle
        if (h != 0L) WhisperNative.requestAbort(h)
    }

    /** Frees the memory of the model after [minutes] (the app is in the background). */
    fun unloadLater(minutes: Int) {
        unloadJob?.cancel()
        if (minutes < 0) return
        unloadJob = scope.launch {
            delay(minutes * 60_000L)
            unload()
        }
    }

    fun unload() {
        unloadJob?.cancel()
        scope.launch(native) {
            if (handle != 0L) {
                WhisperNative.freeContext(handle)
                handle = 0L
            }
            if (_state.value != EngineState.NotInstalled) _state.value = EngineState.Unloaded
        }
    }

    companion object {
        private const val TAG = "SpeechEngine"

        /** Big and middle cores; the small ones only slow the team down. */
        fun defaultThreads(): Int = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 8)

        /** Encoder frames for a clip: 50 per second plus a 2-second margin; 1500 = full 30 s window. */
        fun audioContextFor(seconds: Double): Int {
            if (seconds >= 28) return 0
            val frames = ceil(seconds * 50).toInt() + 100
            return ((frames + 63) / 64 * 64).coerceAtMost(1500)
        }
    }
}
