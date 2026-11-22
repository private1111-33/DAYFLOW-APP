package com.dayflow.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DayflowLyricLine(val timeMs: Long, val text: String)
data class DayflowTrackLyrics(val title: String, val artist: String, val lines: List<DayflowLyricLine>, val plainText: String)

object DayflowLyrics {
    suspend fun identifyAndLoad(context: Context, uri: String): DayflowTrackLyrics? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val (title, artist) = try {
            retriever.setDataSource(context, Uri.parse(uri))
            val name = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty()
            val performer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
            name to performer
        } catch (_: Exception) {
            "" to ""
        } finally {
            runCatching { retriever.release() }
        }
        if (title.isBlank()) return@withContext null
        val query = URLEncoder.encode(title + " " + artist, "UTF-8")
        val connection = URL("https://lrclib.net/api/search?q=" + query).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "DAYFLOW-Android/1.0")
        val response = try { connection.inputStream.bufferedReader().use { it.readText() } }
            finally { connection.disconnect() }
        val matches = JSONArray(response)
        val best = (0 until matches.length()).mapNotNull { matches.optJSONObject(it) }
            .maxByOrNull { if (it.optString("syncedLyrics").isNotBlank()) 2 else 1 }
            ?: return@withContext null
        val synced = best.optString("syncedLyrics").takeUnless { it == "null" }.orEmpty()
        val plain = best.optString("plainLyrics").takeUnless { it == "null" }.orEmpty()
        val content = if (synced.isNotBlank()) synced else plain
        if (content.isBlank()) return@withContext null
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
        val lines = content.lineSequence().flatMap { raw ->
            val found = regex.findAll(raw).toList()
            val text = regex.replace(raw, "").trim()
            found.mapNotNull { m ->
                val min = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val sec = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val ms = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                DayflowLyricLine(min * 60000 + sec * 1000 + ms, text).takeIf { text.isNotBlank() }
            }
        }.sortedBy { it.timeMs }.toList()
        DayflowTrackLyrics(title, artist, lines, content)
    }
}
