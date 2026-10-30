package com.keavors.taskorganizer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.keavors.taskorganizer.data.AppStorage
import com.keavors.taskorganizer.data.Store
import com.keavors.taskorganizer.speech.DictationController
import com.keavors.taskorganizer.speech.ModelDownloader
import com.keavors.taskorganizer.speech.ModelFiles
import com.keavors.taskorganizer.speech.SpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide objects; they live as long as the process. */
class Services(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store = Store(AppStorage(app), scope)
    val modelFiles = ModelFiles(app)
    val engine = SpeechEngine(app, modelFiles, scope)
    val downloader = ModelDownloader(modelFiles, scope) { engine.onModelInstalled() }
    val dictation = DictationController(engine, store, scope)
}

class App : Application() {
    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        services = Services(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Load the model while the user looks at the list: the first dictation is then instant.
                services.engine.prepare()
            }

            override fun onStop(owner: LifecycleOwner) {
                services.store.flushBlocking()
                services.dictation.cancel()
                services.engine.unloadLater(services.store.settings.value.speech.keepLoadedMinutes)
            }
        })
    }
}
