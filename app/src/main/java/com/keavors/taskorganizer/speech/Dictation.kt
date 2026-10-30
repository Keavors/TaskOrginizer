package com.keavors.taskorganizer.speech

import android.os.SystemClock
import com.keavors.taskorganizer.data.Store
import com.keavors.taskorganizer.text.Cleanup
import com.keavors.taskorganizer.text.Dictionary
import com.keavors.taskorganizer.text.RecognitionPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DictationPhase {
    data object Idle : DictationPhase
    data class Recording(val startedAt: Long) : DictationPhase
    data object Transcribing : DictationPhase
}

/**
 * Record -> recognize -> clean up -> hand the text to the input field.
 * One quick press records until the next press; holding the button is push-to-talk.
 */
class DictationController(private val engine: SpeechEngine, private val store: Store, private val scope: CoroutineScope) {
    private val _phase = MutableStateFlow<DictationPhase>(DictationPhase.Idle)
    val phase: StateFlow<DictationPhase> = _phase.asStateFlow()

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _results = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Recognized and cleaned text for the input field. */
    val results: SharedFlow<String> = _results.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Short messages for the user (nothing heard, microphone problems, ...). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var recorder: AudioRecorder? = null
    private var levelJob: Job? = null
    private var limitJob: Job? = null
    private var pressedAt = 0L
    private var pressStarted = false

    fun press() {
        pressedAt = SystemClock.elapsedRealtime()
        if (_phase.value == DictationPhase.Idle) {
            pressStarted = true
            start()
        } else {
            pressStarted = false
            if (_phase.value is DictationPhase.Recording) finish()
        }
    }

    fun release() {
        if (pressStarted && SystemClock.elapsedRealtime() - pressedAt > HOLD_MS) finish()
        pressStarted = false
    }

    private fun start() {
        if (engine.state.value == EngineState.NotInstalled) {
            _messages.tryEmit("Сначала скачайте модель распознавания в настройках.")
            return
        }
        engine.prepare() // the model loads while the user speaks
        val rec = AudioRecorder()
        if (!rec.start()) {
            _messages.tryEmit("Не удалось включить микрофон. Возможно, он занят другим приложением.")
            return
        }
        recorder = rec
        _phase.value = DictationPhase.Recording(SystemClock.elapsedRealtime())
        levelJob = scope.launch { rec.level.collect { _level.value = it } }
        limitJob = scope.launch {
            delay(MAX_RECORDING_MS)
            finish()
        }
    }

    fun finish() {
        val rec = recorder ?: return
        recorder = null
        levelJob?.cancel()
        limitJob?.cancel()
        _level.value = 0f
        _phase.value = DictationPhase.Transcribing
        scope.launch {
            try {
                val recording = withContext(Dispatchers.IO) { rec.stop() }
                when {
                    recording.seconds < MIN_SECONDS -> _messages.emit("Слишком короткая запись.")
                    recording.peakRms < SILENT_PEAK -> _messages.emit("Микрофон не передаёт звук. Проверьте, не занят ли он другим приложением.")
                    else -> recognize(recording)
                }
            } catch (e: SpeechException) {
                _messages.emit(e.message ?: "Ошибка распознавания.")
            } catch (e: Exception) {
                _messages.emit("Ошибка распознавания: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _phase.value = DictationPhase.Idle
            }
        }
    }

    private suspend fun recognize(recording: Recording) {
        val data = store.current
        val speech = store.settings.value.speech
        val result = engine.transcribe(
            recording.pcm,
            speech.language,
            RecognitionPrompt.build(data.dictionary.map { it.text }),
            speech.fastMode,
            speech.threads
        )
        val text = Dictionary.apply(Cleanup.clean(result.text), data.dictionary)
        if (text.isBlank()) _messages.emit("Ничего не расслышал. Попробуйте ещё раз, ближе к телефону.")
        else _results.emit(text)
    }

    fun cancel() {
        limitJob?.cancel()
        levelJob?.cancel()
        recorder?.cancel()
        recorder = null
        _level.value = 0f
        if (_phase.value is DictationPhase.Transcribing) engine.cancelRunning()
        else _phase.value = DictationPhase.Idle
    }

    private companion object {
        const val HOLD_MS = 450L
        const val MAX_RECORDING_MS = 5 * 60_000L
        const val MIN_SECONDS = 0.4
        const val SILENT_PEAK = 0.002f
    }
}
