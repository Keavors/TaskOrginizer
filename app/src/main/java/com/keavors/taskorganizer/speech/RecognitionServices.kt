package com.keavors.taskorganizer.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/** A speech recognizer the user can pick in the settings. */
data class RecognitionServiceOption(val id: String, val label: String, val detail: String?)

/** Speech recognition services on the phone (Google, Samsung, ...). */
object RecognitionServices {
    /** Setting value for Android's on-device recognizer (Android 12+). Empty setting = the phone's default service. */
    const val ON_DEVICE = "on-device"

    /** Every installed recognition service. Needs the <queries> entry in the manifest. */
    fun installed(context: Context): List<RecognitionServiceOption> {
        val pm = context.packageManager
        val found = runCatching { pm.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0) }.getOrDefault(emptyList())
        return found.mapNotNull { info ->
            val service = info.serviceInfo ?: return@mapNotNull null
            val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault("").ifBlank { service.packageName }
            RecognitionServiceOption(ComponentName(service.packageName, service.name).flattenToString(), label, service.packageName)
        }.distinctBy { it.id }.sortedBy { it.label.lowercase() }
    }

    fun onDeviceAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 31 && runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

    /** Choices for the settings besides the phone's default. */
    fun options(context: Context): List<RecognitionServiceOption> {
        val onDevice = if (onDeviceAvailable(context)) listOf(RecognitionServiceOption(ON_DEVICE, "Android на устройстве", "Работает только без интернета")) else emptyList()
        return onDevice + installed(context)
    }

    /** The service the phone uses by default, if Android lets the app read it. */
    fun systemDefault(context: Context): RecognitionServiceOption? {
        val id = runCatching { Settings.Secure.getString(context.contentResolver, "voice_recognition_service") }.getOrNull()
        if (id.isNullOrBlank()) return null
        return installed(context).firstOrNull { it.id == id }
    }

    /** Creates the recognizer chosen in the settings; a missing choice falls back to the phone's default. */
    fun create(context: Context, choice: String): SpeechRecognizer? {
        if (choice == ON_DEVICE && Build.VERSION.SDK_INT >= 31 && onDeviceAvailable(context)) {
            return SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        }
        if (choice.isNotEmpty() && choice != ON_DEVICE && installed(context).any { it.id == choice }) {
            ComponentName.unflattenFromString(choice)?.let { return SpeechRecognizer.createSpeechRecognizer(context, it) }
        }
        return if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
}

/** Language tag for the recognizer. Settings saved by the old Whisper version hold "ru", "auto" or "en". */
fun speechLanguageTag(language: String): String = when (language) {
    "", "ru", "auto" -> "ru-RU"
    "en" -> "en-US"
    else -> language
}
