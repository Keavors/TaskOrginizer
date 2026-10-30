package com.keavors.taskorganizer.speech

/** JNI bindings to whisper.cpp (app/src/main/cpp/whisper_jni.cpp). Not thread-safe: one call per context at a time. */
object WhisperNative {
    init {
        System.loadLibrary("whisperjni")
    }

    /** Loads the best CPU backend for this phone from the app's native library folder. Call once. */
    external fun initBackends(nativeLibDir: String)

    external fun systemInfo(): String

    /** Returns a context handle, or 0 if the model could not be loaded. */
    external fun initContext(modelPath: String): Long

    external fun freeContext(handle: Long)

    /** Makes a running [transcribe] return null as soon as possible. Safe to call from any thread. */
    external fun requestAbort(handle: Long)

    /** UTF-8 bytes of the recognized text, or null when cancelled or failed. */
    external fun transcribe(
        handle: Long,
        pcm: FloatArray,
        language: String,
        prompt: String?,
        vadModelPath: String?,
        threads: Int,
        audioCtx: Int
    ): ByteArray?
}
