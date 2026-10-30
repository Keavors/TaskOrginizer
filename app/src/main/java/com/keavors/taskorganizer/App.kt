package com.keavors.taskorganizer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.keavors.taskorganizer.data.AppStorage
import com.keavors.taskorganizer.data.Store
import com.keavors.taskorganizer.reminders.Reminders
import com.keavors.taskorganizer.speech.DictationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide objects; they live as long as the process. */
class Services(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store = Store(AppStorage(app), scope)
    val dictation = DictationController(app, store, scope)
    val reminders = Reminders(app, store, scope)
}

class App : Application() {
    lateinit var services: Services
        private set

    override fun onCreate() {
        super.onCreate()
        services = Services(this)
        services.reminders.start()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                services.store.flushBlocking()
                services.dictation.cancel()
            }
        })
    }
}
