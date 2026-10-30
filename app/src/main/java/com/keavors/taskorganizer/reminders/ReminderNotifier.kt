package com.keavors.taskorganizer.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.keavors.taskorganizer.MainActivity
import com.keavors.taskorganizer.R
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.ReminderSettings
import com.keavors.taskorganizer.ui.theme.priorityColor
import androidx.compose.ui.graphics.toArgb
import java.time.ZoneId

/** Reminder notifications: one per entry (tagged with its id), one channel per level so each can sound differently. */
class ReminderNotifier(private val context: Context) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannelGroup(NotificationChannelGroup(GROUP, "Напоминания"))
        for ((priority, name) in listOf(Priority.NORMAL to "Обычные", Priority.MEDIUM to "Жёлтые", Priority.HIGH to "Красные")) {
            val channel = NotificationChannel(channelId(priority), name, NotificationManager.IMPORTANCE_HIGH).apply {
                group = GROUP
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun allowed(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun show(event: ReminderEvent, now: Long, zone: ZoneId, settings: ReminderSettings) {
        if (!allowed()) return
        val t = event.target
        val item = t.item
        val text = ReminderText.describe(t, now, zone)
        val notification = NotificationCompat.Builder(context, channelId(item.priority))
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(item.text)
            .setContentText(text)
            .setSubText(t.tabTitle)
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(item.text).bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(priorityColor(item.priority).toArgb())
            .setWhen(t.dueAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openIntent(t))
            .addAction(0, "Выполнено", actionIntent(ACTION_DONE, t))
            .addAction(0, "Отложить на ${ReminderText.duration(settings.snoozeMinutes.toLong())}", actionIntent(ACTION_SNOOZE, t))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(item.id, NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "notification not allowed", e)
        }
    }

    fun cancel(itemId: String) = NotificationManagerCompat.from(context).cancel(itemId, NOTIFICATION_ID)

    /** Removes reminders of entries that no longer remind (done, deleted, without a due time). */
    fun cancelAllExcept(keep: Set<String>) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { nm.activeNotifications }.getOrDefault(emptyArray())
            .filter { it.id == NOTIFICATION_ID && it.tag != null && it.tag !in keep }
            .forEach { nm.cancel(it.tag, it.id) }
    }

    private fun itemUri(t: ReminderTarget): Uri = Uri.parse("taskorganizer://item/${Uri.encode(t.item.id)}")

    private fun openIntent(t: ReminderTarget): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN)
            .setData(itemUri(t))
            .putExtra(EXTRA_TAB, t.tabId)
            .putExtra(EXTRA_ITEM, t.item.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(action: String, t: ReminderTarget): PendingIntent {
        // The data URI makes the intent unique per entry, so actions of different entries never mix.
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(itemUri(t))
            .putExtra(EXTRA_TAB, t.tabId)
            .putExtra(EXTRA_ITEM, t.item.id)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val TAG = "ReminderNotifier"
        private const val GROUP = "reminders"
        const val NOTIFICATION_ID = 1
        const val ACTION_OPEN = "com.keavors.taskorganizer.OPEN_ITEM"
        const val ACTION_DONE = "com.keavors.taskorganizer.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.keavors.taskorganizer.REMINDER_SNOOZE"
        const val EXTRA_TAB = "tabId"
        const val EXTRA_ITEM = "itemId"

        fun channelId(priority: Priority): String = "reminders_" + priority.name.lowercase()
    }
}
