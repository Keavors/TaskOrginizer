package com.keavors.taskorganizer.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.keavors.taskorganizer.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reminder alarm, "Done"/"Snooze" buttons, and system events after which alarms must be set again. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val services = (context.applicationContext as App).services
        val pending = goAsync()
        services.scope.launch(Dispatchers.Default) {
            try {
                services.reminders.handle(
                    intent.action,
                    intent.getStringExtra(ReminderNotifier.EXTRA_TAB),
                    intent.getStringExtra(ReminderNotifier.EXTRA_ITEM)
                )
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "failed: ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }
}
