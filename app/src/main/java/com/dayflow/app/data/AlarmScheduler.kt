package com.dayflow.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayflow.app.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    fun schedule(context: Context, alarm: DayAlarm) {
        if (!alarm.enabled) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) return
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("alarm_id", alarm.id)
            .putExtra("alarm_label", alarm.label)
        val pending = PendingIntent.getBroadcast(context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    fun cancel(context: Context, alarm: DayAlarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        pending.cancel()
    }

    fun rescheduleAll(context: Context) {
        AlarmStore.all(context).filter { it.enabled }.forEach { schedule(context, it) }
    }
}
