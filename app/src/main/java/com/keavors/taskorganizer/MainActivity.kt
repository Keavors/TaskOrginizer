package com.keavors.taskorganizer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.reminders.ReminderNotifier
import com.keavors.taskorganizer.ui.AppRoot
import com.keavors.taskorganizer.ui.MainViewModel
import com.keavors.taskorganizer.ui.theme.TaskOrganizerTheme
import com.keavors.taskorganizer.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openFromNotification(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = isDarkTheme(settings)
            // Status and navigation bar icons follow the app theme, not only the system one.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            TaskOrganizerTheme(settings) {
                // The app background also fills the area behind the system bars.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { AppRoot(vm) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromNotification(intent)
    }

    private fun openFromNotification(intent: Intent?) {
        if (intent?.action != ReminderNotifier.ACTION_OPEN) return
        val itemId = intent.getStringExtra(ReminderNotifier.EXTRA_ITEM) ?: return
        vm.openItem(intent.getStringExtra(ReminderNotifier.EXTRA_TAB), itemId)
    }
}
