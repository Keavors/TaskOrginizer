package com.keavors.taskorganizer.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class Recording(val pcm: FloatArray, val peakRms: Float) {
    val seconds: Double get() = pcm.size / AudioRecorder.SAMPLE_RATE.toDouble()
}

/** Microphone capture: 16 kHz mono, the format Whisper expects, so no resampling is needed. */
class AudioRecorder {
    private val _level = MutableStateFlow(0f)
    /** Loudness 0..1 for the on-screen meter. */
    val level: StateFlow<Float> = _level.asStateFlow()

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private val chunks = ArrayList<ShortArray>()
    private var peak = 0f

    /** Needs the RECORD_AUDIO permission. Returns false if the microphone could not be opened. */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(minBuffer, SAMPLE_RATE) // at least 0.5 s
        val rec = openRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, bufferSize)
            ?: openRecord(MediaRecorder.AudioSource.MIC, bufferSize)
            ?: return false
        record = rec
        synchronized(chunks) { chunks.clear() }
        peak = 0f
        running = true
        rec.startRecording()
        thread = Thread({ readLoop(rec) }, "mic").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun openRecord(source: Int, bufferSize: Int): AudioRecord? = try {
        val rec = AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (rec.state == AudioRecord.STATE_INITIALIZED) rec else {
            rec.release()
            null
        }
    } catch (e: Exception) {
        Log.w(TAG, "cannot open source $source", e)
        null
    }

    private fun readLoop(rec: AudioRecord) {
        val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms
        while (running) {
            val n = rec.read(buffer, 0, buffer.size)
            if (n <= 0) continue
            var sum = 0.0
            for (i in 0 until n) {
                val s = buffer[i] / 32768.0
                sum += s * s
            }
            val rms = sqrt(sum / n).toFloat()
            if (rms > peak) peak = rms
            _level.value = minOf(1f, sqrt(rms) * 2.2f)
            synchronized(chunks) { chunks.add(buffer.copyOf(n)) }
        }
    }

    /** Stops and returns everything recorded so far. */
    fun stop(): Recording {
        release()
        val all = synchronized(chunks) { chunks.toList().also { chunks.clear() } }
        val total = all.sumOf { it.size }
        val pcm = FloatArray(total)
        var offset = 0
        for (chunk in all) {
            for (s in chunk) pcm[offset++] = s / 32768f
        }
        return Recording(pcm, peak)
    }

    fun cancel() {
        release()
        synchronized(chunks) { chunks.clear() }
    }

    private fun release() {
        running = false
        thread?.join(500)
        thread = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
        _level.value = 0f
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val TAG = "AudioRecorder"
    }
}
