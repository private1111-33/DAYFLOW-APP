package com.dayflow.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class DayTask(
    val id: Long,
    val title: String,
    val time: String,
    val done: Boolean = false,
    /** Day the task belongs to, as LocalDate.toEpochDay(). */
    val date: Long = LocalDate.now().toEpochDay()
)

data class MusicTrack(val uri: String, val name: String)

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
                    item.optString("time"), item.optBoolean("done"),
                    item.optLong("date", LocalDate.now().toEpochDay()))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveTasks(context: Context, tasks: List<DayTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().put("id", task.id).put("title", task.title)
                .put("time", task.time).put("done", task.done).put("date", task.date))
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

object MusicStore {
    private const val PREFS = "dayflow_music"
    private const val KEY_JSON = "tracks_json"
    private const val KEY_LEGACY = "tracks"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Loads the library in the order the user imported it. */
    fun load(context: Context): List<MusicTrack> {
        val raw = prefs(context).getString(KEY_JSON, null)
        if (raw != null) {
            return try {
                val array = JSONArray(raw)
                (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    MusicTrack(item.getString("uri"), item.optString("name"))
                }
            } catch (_: Exception) { emptyList() }
        }
        // Older versions stored an unordered set of URIs.
        return prefs(context).getStringSet(KEY_LEGACY, emptySet()).orEmpty().map { MusicTrack(it, "") }
    }

    fun save(context: Context, tracks: List<MusicTrack>) {
        val array = JSONArray()
        tracks.forEach { array.put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        prefs(context).edit().putString(KEY_JSON, array.toString()).apply()
    }

    fun displayName(context: Context, uri: Uri): String {
        val raw = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        return (raw ?: uri.lastPathSegment ?: "Audio track").substringBeforeLast('.').ifBlank { "Audio track" }
    }
}
