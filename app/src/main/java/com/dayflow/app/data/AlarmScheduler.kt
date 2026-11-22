package com.dayflow.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayflow.app.MainActivity
import com.dayflow.app.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_LABEL = "alarm_label"
    const val EXTRA_SNOOZE = "alarm_snooze"
    const val SNOOZE_MINUTES = 10
    private const val SNOOZE_REQUEST_OFFSET = 1_000_000

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun pending(context: Context, id: Int, label: String, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_SNOOZE, snooze)
        val code = if (snooze) id + SNOOZE_REQUEST_OFFSET else id
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAt(context: Context, triggerAt: Long, pending: PendingIntent) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (canScheduleExact(context)) {
                val show = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    /** Next time this alarm should ring, honouring its repeat days. */
    fun nextTriggerMillis(alarm: DayAlarm): Long? {
        val days = if (alarm.days and DayAlarm.ALL_DAYS == 0) DayAlarm.ALL_DAYS else alarm.days
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val bit = 1 shl (c.get(Calendar.DAY_OF_WEEK) - 1)
            if (c.timeInMillis > now && (days and bit) != 0) return c.timeInMillis
        }
        return null
    }

    fun schedule(context: Context, alarm: DayAlarm) {
        if (!alarm.enabled) return
        val next = nextTriggerMillis(alarm) ?: return
        setAt(context, next, pending(context, alarm.id, alarm.label, false))
    }

    fun scheduleSnooze(context: Context, id: Int, label: String) {
        val triggerAt = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        setAt(context, triggerAt, pending(context, id, label, true))
    }

    fun cancel(context: Context, alarm: DayAlarm) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pending(context, alarm.id, alarm.label, false))
        manager.cancel(pending(context, alarm.id, alarm.label, true))
    }

    fun rescheduleAll(context: Context) {
        AlarmStore.all(context).filter { it.enabled }.forEach { schedule(context, it) }
    }

    /** Whole minutes until the alarm next rings; Int.MAX_VALUE if it never will. */
    fun minutesUntil(alarm: DayAlarm): Int {
        val next = nextTriggerMillis(alarm) ?: return Int.MAX_VALUE
        return ((next - System.currentTimeMillis()) / 60_000L).toInt()
    }

    /** "Every day", "Weekdays" or "Mon, Wed". */
    fun daysSummary(days: Int): String {
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return when (days and DayAlarm.ALL_DAYS) {
            0, DayAlarm.ALL_DAYS -> "Every day"
            DayAlarm.WEEKDAYS -> "Weekdays"
            else -> names.filterIndexed { i, _ -> days and (1 shl i) != 0 }.joinToString(", ")
        }
    }
}
