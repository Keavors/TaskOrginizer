package com.keavors.taskorganizer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.keavors.taskorganizer.MainActivity
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.DataOps
import com.keavors.taskorganizer.data.Settings
import com.keavors.taskorganizer.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * Keeps one exact alarm set for the next reminder of all entries. When it fires,
 * every reminder due since the last run is shown and the next alarm is set.
 *
 * "delivered" is the moment up to which reminders were shown. Changes made in the
 * app move it to now, so editing an entry never brings up reminders from the past;
 * after a reboot the missed ones are shown (up to a day back).
 */
class Reminders(private val context: Context, private val store: Store, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val notifier = ReminderNotifier(context)
    private val lock = Mutex()

    /** Creates the channels and follows every change of the entries and settings. */
    @OptIn(FlowPreview::class)
    fun start() {
        notifier.createChannels()
        scope.launch {
            combine(store.data.filterNotNull(), store.settings) { data, settings -> data to settings }
                .debounce(SYNC_DELAY_MS)
                .collectLatest { (data, settings) ->
                    withContext(Dispatchers.Default) { runCatching { run(data, settings, deliver = false) }.onFailure { Log.e(TAG, "sync failed", it) } }
                }
        }
    }

    /** Alarm, notification button or system event (boot, time change). */
    suspend fun handle(action: String?, tabId: String?, itemId: String?) {
        store.ensureLoaded()
        when (action) {
            ReminderNotifier.ACTION_DONE -> if (tabId != null && itemId != null) {
                withContext(Dispatchers.Main) { store.update { DataOps.setDone(it, findTab(it, tabId, itemId), itemId, true) } }
                withContext(Dispatchers.IO) { store.flushBlocking() }
                notifier.cancel(itemId)
            }
            ReminderNotifier.ACTION_SNOOZE -> if (itemId != null) {
                val minutes = store.settings.value.reminders.snoozeMinutes
                prefs.edit().putLong(SNOOZE + itemId, System.currentTimeMillis() + minutes * ReminderPlanner.MINUTE).apply()
                notifier.cancel(itemId)
            }
        }
        val data = store.data.value ?: return
        val deliver = action != ReminderNotifier.ACTION_DONE && action != ReminderNotifier.ACTION_SNOOZE
        run(data, store.settings.value, deliver)
    }

    /** The entry may have moved to another tab since the notification was shown. */
    private fun findTab(data: AppData, tabId: String, itemId: String): String =
        data.tabs.firstOrNull { tab -> data.itemsOf(tab.id).any { it.id == itemId } }?.id ?: tabId

    private suspend fun run(data: AppData, settings: Settings, deliver: Boolean) = lock.withLock {
        val s = settings.reminders
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val targets = ReminderPlanner.targets(data, s, zone)
        val snoozes = snoozes()
        val delivered = prefs.getLong(KEY_DELIVERED, 0L)
        val scheduled = prefs.getLong(KEY_SCHEDULED, 0L)
        // An alarm that should already have fired counts as firing now.
        val behind = scheduled in 1..now
        if ((deliver || behind) && delivered > 0) {
            val oldest = now - if (s.showMissed) MISSED_LIMIT_MS else LATE_LIMIT_MS
            ReminderPlanner.due(targets, s, snoozes, maxOf(delivered, oldest), now).forEach { notifier.show(it, now, zone, s) }
        }
        notifier.cancelAllExcept(targets.map { it.item.id }.toSet())
        val next = ReminderPlanner.next(targets, s, snoozes, now)
        prefs.edit().apply {
            putLong(KEY_DELIVERED, now)
            putLong(KEY_SCHEDULED, next ?: 0L)
            snoozes.filterValues { it <= now }.keys.forEach { remove(SNOOZE + it) }
        }.apply()
        if (next == null) cancelAlarm() else setAlarm(next, s.alarmClock)
    }

    private fun snoozes(): Map<String, Long> =
        prefs.all.filterKeys { it.startsWith(SNOOZE) }.mapNotNull { (key, value) -> (value as? Long)?.let { key.removePrefix(SNOOZE) to it } }.toMap()

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun setAlarm(at: Long, alarmClock: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val operation = alarmIntent()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            when {
                exact && alarmClock -> {
                    val show = PendingIntent.getActivity(
                        context, 1,
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
                }
                exact -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                else -> am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarms are not allowed", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun cancelAlarm() {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent())
    }

    companion object {
        private const val TAG = "Reminders"
        const val ACTION_FIRE = "com.keavors.taskorganizer.REMINDER_FIRE"
        private const val KEY_DELIVERED = "delivered"
        private const val KEY_SCHEDULED = "scheduled"
        private const val SNOOZE = "snooze:"
        private const val SYNC_DELAY_MS = 400L
        /** Reminders of the last day missed while the phone was off are still shown. */
        private const val MISSED_LIMIT_MS = 24 * 60 * ReminderPlanner.MINUTE
        /** A late alarm (deep sleep) still shows its reminder. */
        private const val LATE_LIMIT_MS = 15 * ReminderPlanner.MINUTE
    }
}
