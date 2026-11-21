package com.dayflow.app.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dayflow.app.MainActivity
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                AlarmScheduler.rescheduleAll(context)
                return
            }
            ACTION_SNOOZE -> {
                scheduleSnooze(context, intent)
                return
            }
            ACTION_SNOOZE_FIRE -> {
                showAlarmNotification(context, intent.getIntExtra("alarm_id", -1),
                    intent.getStringExtra("alarm_label").orEmpty())
                return
            }
        }

        val id = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label").orEmpty().ifBlank { "DAYFLOW alarm" }
        if (id < 0) return

        // Schedule the next daily occurrence before showing this occurrence.
        AlarmStore.all(context).firstOrNull { it.id == id && it.enabled }?.let {
            AlarmScheduler.schedule(context, it)
        }
        showAlarmNotification(context, id, label)
    }

    private fun scheduleSnooze(context: Context, source: Intent) {
        val id = source.getIntExtra("alarm_id", -1)
        if (id < 0) return
        val label = source.getStringExtra("alarm_label").orEmpty().ifBlank { "DAYFLOW alarm" }
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            putExtra("alarm_id", id)
            putExtra("alarm_label", label)
        }
        val pending = PendingIntent.getBroadcast(
            context, id + SNOOZE_REQUEST_OFFSET, fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun showAlarmNotification(context: Context, id: Int, label: String) {
        if (id < 0) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "DAYFLOW alarms", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarm_id", id)
            putExtra("alarm_label", label)
        }
        val snooze = PendingIntent.getBroadcast(
            context, id + SNOOZE_ACTION_OFFSET, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time")
            .setContentText(label.ifBlank { "DAYFLOW alarm" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10 min", snooze)
            .build()
        manager.notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "dayflow_alarms"
        private const val ACTION_SNOOZE = "com.dayflow.app.action.SNOOZE"
        private const val ACTION_SNOOZE_FIRE = "com.dayflow.app.action.SNOOZE_FIRE"
        private const val SNOOZE_MINUTES = 10
        private const val SNOOZE_ACTION_OFFSET = 2_000_000
        private const val SNOOZE_REQUEST_OFFSET = 1_000_000
    }
}