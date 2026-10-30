package com.keavors.taskorganizer.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.keavors.taskorganizer.data.SpeechSettings
import com.keavors.taskorganizer.data.Store
import com.keavors.taskorganizer.text.Dictionary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DictationPhase {
    data object Idle : DictationPhase
    /** The microphone is on; recognized words appear in [DictationController.partial] while the user speaks. */
    data class Listening(val startedAt: Long) : DictationPhase
    /** The user has stopped speaking; the recognizer is finishing the text. */
    data object Finishing : DictationPhase
}

/**
 * Dictation through the phone's own speech recognizer (android.speech.SpeechRecognizer),
 * the same one the keyboard's voice input uses. Text streams in while the user speaks.
 *
 * A quick press listens until the user stops talking or presses again; holding the
 * button listens until it is released. Call everything on the main thread: SpeechRecognizer requires it.
 */
class DictationController(private val context: Context, private val store: Store, private val scope: CoroutineScope) {
    private val _phase = MutableStateFlow<DictationPhase>(DictationPhase.Idle)
    val phase: StateFlow<DictationPhase> = _phase.asStateFlow()

    private val _level = MutableStateFlow(0f)
    /** Loudness 0..1 for the animation. */
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _partial = MutableStateFlow("")
    /** Text recognized so far in the current phrase. */
    val partial: StateFlow<String> = _partial.asStateFlow()

    private val _results = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Final text of a phrase, with the dictionary applied, for the input field. */
    val results: SharedFlow<String> = _results.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Short messages for the user (nothing heard, no recognizer, ...). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    /** Grows with every phrase, so late callbacks from an old recognizer are ignored. */
    private var session = 0
    /** Last partial text: kept if the recognizer fails or never sends the final one. */
    private var heard = ""
    private var stopRequested = false
    private var watchdog: Job? = null
    private var pressedAt = 0L
    private var pressStarted = false

    fun press() {
        pressedAt = SystemClock.elapsedRealtime()
        pressStarted = false
        when (_phase.value) {
            DictationPhase.Idle -> pressStarted = start()
            is DictationPhase.Listening -> finish()
            DictationPhase.Finishing -> Unit
        }
    }

    fun release() {
        if (pressStarted && SystemClock.elapsedRealtime() - pressedAt > HOLD_MS) finish()
        pressStarted = false
    }

    private fun start(): Boolean {
        val speech = store.settings.value.speech
        val rec = try {
            RecognitionServices.create(context, speech.service)
        } catch (e: Exception) {
            Log.e(TAG, "cannot create a recognizer", e)
            null
        }
        if (rec == null) {
            _messages.tryEmit("На телефоне не найдена служба распознавания речи. Проверьте, что включён голосовой ввод Google или Samsung.")
            return false
        }
        val id = ++session
        recognizer = rec
        heard = ""
        stopRequested = false
        _partial.value = ""
        rec.setRecognitionListener(Listener(id))
        _phase.value = DictationPhase.Listening(SystemClock.elapsedRealtime())
        try {
            rec.startListening(recognitionIntent(speech))
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            end()
            _messages.tryEmit("Не удалось запустить распознавание: ${e.message ?: e.javaClass.simpleName}")
            return false
        }
        return true
    }

    private fun recognitionIntent(speech: SpeechSettings): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguageTag(speech.language))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (speech.offlineOnly) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            if (Build.VERSION.SDK_INT >= 33) {
                // Names from the dictionary as hints; recognizers without hint support ignore them.
                val terms = dictionary().map { it.text.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_HINTS)
                if (terms.isNotEmpty()) putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(terms))
            }
        }

    /** Stops listening; what was said is finished and goes to the input field. */
    fun finish() {
        val rec = recognizer ?: return
        if (_phase.value !is DictationPhase.Listening) return
        stopRequested = true
        enterFinishing()
        try {
            rec.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "stopListening failed", e)
        }
    }

    /** Drops the current phrase. */
    fun cancel() {
        recognizer?.let { runCatching { it.cancel() } }
        end()
    }

    private fun enterFinishing() {
        _phase.value = DictationPhase.Finishing
        _level.value = 0f
        val id = session
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(FINISH_TIMEOUT_MS)
            if (id != session) return@launch
            // The recognizer never answered: keep what was already heard.
            val fallback = heard
            recognizer?.let { runCatching { it.cancel() } }
            end()
            if (fallback.isNotBlank()) deliver(fallback) else _messages.emit("Служба распознавания не ответила. Попробуйте ещё раз.")
        }
    }

    private fun end() {
        session++
        watchdog?.cancel()
        watchdog = null
        // Destroy outside the listener callback that may have called end().
        recognizer?.let { rec -> main.post { runCatching { rec.destroy() } } }
        recognizer = null
        heard = ""
        _level.value = 0f
        _partial.value = ""
        _phase.value = DictationPhase.Idle
    }

    private fun dictionary() = store.data.value?.dictionary.orEmpty()

    private fun deliver(raw: String) {
        val text = Dictionary.apply(raw.replace(WHITESPACE, " ").trim(), dictionary())
        if (text.isBlank()) _messages.tryEmit(NOTHING_HEARD) else _results.tryEmit(text)
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NOTHING_HEARD
        SpeechRecognizer.ERROR_CLIENT ->
            if (stopRequested) NOTHING_HEARD else "Служба распознавания не запустилась. Попробуйте ещё раз или выберите другую в настройках."
        SpeechRecognizer.ERROR_AUDIO -> "Не удалось включить микрофон. Возможно, он занят другим приложением."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет доступа к микрофону. Разрешите его в настройках телефона."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
            if (store.settings.value.speech.offlineOnly) NO_OFFLINE_LANGUAGE
            else "Нет связи со службой распознавания. Проверьте интернет."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание занято другим приложением. Попробуйте через пару секунд."
        ERROR_TOO_MANY_REQUESTS -> "Служба распознавания перегружена. Попробуйте через минуту."
        ERROR_SERVER_DISCONNECTED -> "Служба распознавания перестала отвечать. Попробуйте ещё раз."
        ERROR_LANGUAGE_NOT_SUPPORTED -> "Эта служба распознавания не знает выбранный язык. Выберите другую в настройках."
        ERROR_LANGUAGE_UNAVAILABLE -> NO_OFFLINE_LANGUAGE
        else -> "Ошибка распознавания речи (код $error)."
    }

    private inner class Listener(private val id: Int) : RecognitionListener {
        private val current get() = id == session

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onRmsChanged(rmsdB: Float) {
            if (current && _phase.value is DictationPhase.Listening) {
                _level.value = ((rmsdB - RMS_QUIET) / (RMS_LOUD - RMS_QUIET)).coerceIn(0f, 1f)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!current) return
            val text = firstResult(partialResults)
            if (text.isBlank()) return
            heard = text
            _partial.value = Dictionary.apply(text, dictionary())
        }

        override fun onEndOfSpeech() {
            if (current && _phase.value is DictationPhase.Listening) enterFinishing()
        }

        override fun onResults(results: Bundle?) {
            if (!current) return
            val text = firstResult(results).ifBlank { heard }
            end()
            deliver(text)
        }

        override fun onError(error: Int) {
            if (!current) return
            Log.w(TAG, "recognizer error $error")
            val fallback = heard
            val message = errorMessage(error)
            end()
            // Whatever was already recognized is worth more than an error message.
            if (fallback.isNotBlank()) deliver(fallback) else _messages.tryEmit(message)
        }
    }

    private companion object {
        const val TAG = "Dictation"
        const val HOLD_MS = 450L
        const val FINISH_TIMEOUT_MS = 8_000L
        const val MAX_HINTS = 100
        /** onRmsChanged reports roughly -2..10 dB. */
        const val RMS_QUIET = -2f
        const val RMS_LOUD = 10f
        // Error codes added in Android 12 (API 31).
        const val ERROR_TOO_MANY_REQUESTS = 10
        const val ERROR_SERVER_DISCONNECTED = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
        const val NOTHING_HEARD = "Ничего не расслышал. Попробуйте ещё раз."
        const val NO_OFFLINE_LANGUAGE =
            "Без интернета эта служба не знает выбранный язык. Скачайте его для голосового ввода в настройках телефона или выберите другую службу в настройках приложения."
        val WHITESPACE = Regex("\\s+")

        fun firstResult(bundle: Bundle?): String =
            bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull { it.isNotBlank() }.orEmpty()
    }
}
