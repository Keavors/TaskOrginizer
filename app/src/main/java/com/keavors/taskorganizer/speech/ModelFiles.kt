package com.keavors.taskorganizer.speech

import android.content.Context
import java.io.File

/** The speech model and the voice activity detector on disk. */
class ModelFiles(private val context: Context) {
    /** App-specific external storage: no permission needed, removed with the app, reachable over USB for setup. */
    val dir: File = (context.getExternalFilesDir("models") ?: File(context.filesDir, "models")).apply { mkdirs() }

    val model: File get() = File(dir, MODEL_FILE)
    val partial: File get() = File(dir, "$MODEL_FILE.part")

    fun modelInstalled(): Boolean = model.length() == MODEL_SIZE

    /** Silero VAD ships inside the APK (885 KB) and is copied out once. */
    fun vadModel(): File? {
        val target = File(context.filesDir, VAD_ASSET)
        if (target.length() == VAD_SIZE) return target
        return runCatching {
            context.assets.open(VAD_ASSET).use { input -> target.outputStream().use { input.copyTo(it) } }
            target
        }.getOrNull()?.takeIf { it.length() == VAD_SIZE }
    }

    fun deleteModel() {
        model.delete()
        partial.delete()
    }

    companion object {
        /** Whisper Large-v3-Turbo, 8-bit: chosen in docs/ASR-BENCHMARK.md. */
        const val MODEL_TITLE = "Whisper Large-v3-Turbo"
        const val MODEL_FILE = "ggml-large-v3-turbo-q8_0.bin"
        const val MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q8_0.bin"
        const val MODEL_SIZE = 874_188_075L
        const val MODEL_SHA256 = "317eb69c11673c9de1e1f0d459b253999804ec71ac4c23c17ecf5fbe24e259a1"

        const val VAD_ASSET = "ggml-silero-v6.2.0.bin"
        const val VAD_SIZE = 885_098L
    }
}
