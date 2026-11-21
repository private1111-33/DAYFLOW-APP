package com.dayflow.app.receiver

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
import com.dayflow.app.data.DayAlarm

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            AlarmScheduler.rescheduleAll(context)
            return
        }

        val id = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label").orEmpty().ifBlank { "DAYFLOW alarm" }
        if (id < 0) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("dayflow_alarms", "DAYFLOW alarms", NotificationManager.IMPORTANCE_HIGH))
        }
        val openApp = PendingIntent.getActivity(context, id,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "dayflow_alarms")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(id, notification)

        val alarm = AlarmStore.all(context).firstOrNull { it.id == id && it.enabled }
        if (alarm != null) AlarmScheduler.schedule(context, alarm)
    }
}
