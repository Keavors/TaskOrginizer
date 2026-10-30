package com.keavors.taskorganizer.speech

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val received: Long, val total: Long, val bytesPerSecond: Long) : DownloadState
    data class Verifying(val checked: Long, val total: Long) : DownloadState
    data class Failed(val message: String) : DownloadState
}

/**
 * One-time download of the speech model. Resumes from `.part` with an HTTP
 * Range request and checks SHA-256 before the file is used.
 */
class ModelDownloader(private val files: ModelFiles, private val scope: CoroutineScope, private val onInstalled: () -> Unit) {
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private var job: Job? = null

    val running: Boolean get() = job?.isActive == true

    fun start() {
        if (running || files.modelInstalled()) return
        job = scope.launch(Dispatchers.IO) {
            try {
                download()
                verify()
                _state.value = DownloadState.Idle
                withContext(Dispatchers.Main) { onInstalled() }
            } catch (e: CancellationException) {
                _state.value = DownloadState.Idle
            } catch (e: Exception) {
                Log.w(TAG, "download failed", e)
                _state.value = DownloadState.Failed(describe(e))
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun download() {
        val part = files.partial
        var start = if (part.exists()) part.length() else 0L
        if (start > ModelFiles.MODEL_SIZE) {
            part.delete()
            start = 0
        }
        if (start == ModelFiles.MODEL_SIZE) return

        val connection = (URL(ModelFiles.MODEL_URL).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            if (start > 0) setRequestProperty("Range", "bytes=$start-")
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK && start > 0) start = 0 // the server ignored the range
            else if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) throw IOException("HTTP $code")

            var received = start
            var lastEmit = 0L
            var windowStart = System.currentTimeMillis()
            var windowBytes = received
            var speed = 0L
            connection.inputStream.use { input ->
                FileOutputStream(part, start > 0).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        coroutineContextEnsureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        received += n
                        val now = System.currentTimeMillis()
                        if (now - windowStart >= 1000) {
                            speed = (received - windowBytes) * 1000 / (now - windowStart)
                            windowStart = now
                            windowBytes = received
                        }
                        if (now - lastEmit > 200) {
                            lastEmit = now
                            _state.value = DownloadState.Downloading(received, ModelFiles.MODEL_SIZE, speed)
                        }
                    }
                }
            }
            if (received != ModelFiles.MODEL_SIZE) throw IOException("incomplete: $received")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun verify() {
        val part = files.partial
        val digest = MessageDigest.getInstance("SHA-256")
        val total = part.length()
        var checked = 0L
        var lastEmit = 0L
        part.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                coroutineContextEnsureActive()
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
                checked += n
                val now = System.currentTimeMillis()
                if (now - lastEmit > 200) {
                    lastEmit = now
                    _state.value = DownloadState.Verifying(checked, total)
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (hash != ModelFiles.MODEL_SHA256) {
            part.delete()
            throw IOException("checksum")
        }
        if (!part.renameTo(files.model)) throw IOException("rename")
    }

    private suspend fun coroutineContextEnsureActive() = kotlin.coroutines.coroutineContext.ensureActive()

    private fun describe(e: Exception): String = when {
        e is UnknownHostException -> "Нет подключения к интернету. Модель скачивается один раз — подключитесь к Wi-Fi и повторите."
        e is SocketTimeoutException -> "Соединение оборвалось. Нажмите «Скачать» ещё раз — загрузка продолжится с того же места."
        e.message == "checksum" -> "Файл повредился при загрузке. Скачайте ещё раз."
        e.message?.startsWith("incomplete") == true -> "Загрузка прервалась. Нажмите «Скачать» ещё раз — она продолжится с того же места."
        e.message?.contains("ENOSPC") == true -> "На телефоне не хватает места (нужно около 900 МБ)."
        else -> "Не удалось скачать модель: ${e.message ?: e.javaClass.simpleName}"
    }

    private companion object {
        const val TAG = "ModelDownloader"
    }
}
