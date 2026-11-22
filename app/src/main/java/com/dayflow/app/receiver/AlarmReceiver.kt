package com.dayflow.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dayflow.app.MainActivity
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.service.AlarmRingService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                AlarmScheduler.rescheduleAll(context)
                return
            }
        }

        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        if (id < 0) return
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty().ifBlank { "DAYFLOW alarm" }
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)

        // Schedule tomorrow's occurrence before ringing this one.
        if (!isSnooze) {
            AlarmStore.all(context).firstOrNull { it.id == id && it.enabled }?.let {
                AlarmScheduler.schedule(context, it)
            }
        }

        val service = Intent(context, AlarmRingService::class.java)
            .putExtra(AlarmScheduler.EXTRA_ID, id)
            .putExtra(AlarmScheduler.EXTRA_LABEL, label)
        try {
            ContextCompat.startForegroundService(context, service)
        } catch (_: Exception) {
            // The system refused a foreground service: fall back to a loud notification.
            showFallbackNotification(context, id, label)
        }
    }

    private fun showFallbackNotification(context: Context, id: Int, label: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channel = NotificationChannel(FALLBACK_CHANNEL, "DAYFLOW alarms (backup)", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
        val open = android.app.PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, FALLBACK_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time")
            .setContentText(label)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        manager.notify(id, notification)
    }

    companion object {
        private const val FALLBACK_CHANNEL = "dayflow_alarm_fallback"
    }
}
