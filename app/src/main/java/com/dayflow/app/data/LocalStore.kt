package com.dayflow.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DayTask(
    val id: Long,
    val title: String,
    val time: String,
    val done: Boolean = false
)

data class WordEntry(
    val id: Long,
    val word: String,
    val meaning: String,
    val example: String,
    val learned: Boolean = false
)

object LocalStore {
    private const val PREFS = "dayflow_local_v1"
    private const val TASKS = "tasks"
    private const val WORDS = "words"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun tasks(context: Context): List<DayTask> {
        val raw = prefs(context).getString(TASKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                DayTask(item.getLong("id"), item.getString("title"),
                    item.optString("time"), item.optBoolean("done"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveTasks(context: Context, tasks: List<DayTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().put("id", task.id).put("title", task.title)
                .put("time", task.time).put("done", task.done))
        }
        prefs(context).edit().putString(TASKS, array.toString()).apply()
    }

    fun words(context: Context): List<WordEntry> {
        val raw = prefs(context).getString(WORDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                WordEntry(item.getLong("id"), item.getString("word"),
                    item.getString("meaning"), item.optString("example"),
                    item.optBoolean("learned"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveWords(context: Context, words: List<WordEntry>) {
        val array = JSONArray()
        words.forEach { word ->
            array.put(JSONObject().put("id", word.id).put("word", word.word)
                .put("meaning", word.meaning).put("example", word.example)
                .put("learned", word.learned))
        }
        prefs(context).edit().putString(WORDS, array.toString()).apply()
    }
}
