package com.dayflow.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DayAlarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true
)

object AlarmStore {
    private const val PREFS = "dayflow_alarms"
    private const val KEY = "items"

    fun all(context: Context): List<DayAlarm> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                DayAlarm(o.getInt("id"), o.getInt("hour"), o.getInt("minute"),
                    o.optString("label"), o.optBoolean("enabled", true))
            }.sortedWith(compareBy<DayAlarm> { it.hour }.thenBy { it.minute })
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, alarms: List<DayAlarm>) {
        val a = JSONArray()
        alarms.forEach { alarm ->
            a.put(JSONObject().put("id", alarm.id).put("hour", alarm.hour)
                .put("minute", alarm.minute).put("label", alarm.label)
                .put("enabled", alarm.enabled))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, a.toString()).apply()
    }
}
