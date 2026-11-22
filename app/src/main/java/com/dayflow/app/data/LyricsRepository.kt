package com.dayflow.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class TrackMetadata(
    val title: String,
    val artist: String,
    val durationMs: Long
)

data class LyricLine(val timeMs: Long?, val text: String)

data class TrackLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean
)

object TrackMetadataReader {
    suspend fun read(context: Context, uri: Uri, fallbackName: String): TrackMetadata =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() } ?: fallbackName.substringBeforeLast('.')
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        ?.takeIf { it.isNotBlank() }
                    ?: "Unknown artist"
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                TrackMetadata(title, artist, duration)
            } catch (_: Exception) {
                TrackMetadata(fallbackName.substringBeforeLast('.'), "Unknown artist", 0L)
            } finally {
                runCatching { retriever.release() }
            }
        }
}

object LyricsRepository {
    suspend fun find(title: String, artist: String, durationMs: Long): TrackLyrics? =
        withContext(Dispatchers.IO) {
            if (title.isBlank()) return@withContext null
            val query = buildString {
                append("track_name=").append(encode(title))
                if (artist.isNotBlank() && artist != "Unknown artist") {
                    append("&artist_name=").append(encode(artist))
                }
                if (durationMs > 0L) append("&duration=").append(durationMs / 1000L)
            }

            val exact = requestJson("https://lrclib.net/api/get?$query")
                ?.let { runCatching { parseLyrics(JSONObject(it)) }.getOrNull() }
            if (exact != null) return@withContext exact

            val search = requestJson("https://lrclib.net/api/search?$query")
                ?: return@withContext null
            runCatching {
                val results = JSONArray(search)
                (0 until results.length())
                    .asSequence()
                    .mapNotNull { index -> parseLyrics(results.optJSONObject(index) ?: return@mapNotNull null) }
                    .firstOrNull()
            }.getOrNull()
        }

    private fun requestJson(address: String): String? {
        val connection = (URL(address).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "DAYFLOW Android Music Player")
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLyrics(json: JSONObject): TrackLyrics? {
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
        val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
        return when {
            synced != null -> parseSynced(synced)?.let { TrackLyrics(it, true) }
            plain != null -> TrackLyrics(
                plain.lines().map { LyricLine(null, it.trim()) }.filter { it.text.isNotBlank() },
                false
            )
            else -> null
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun parseSynced(raw: String): List<LyricLine>? {
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?]")
        val result = mutableListOf<LyricLine>()
        raw.lines().forEach { line ->
            val matches = regex.findAll(line).toList()
            val text = regex.replace(line, "").trim()
            if (text.isNotBlank()) {
                matches.forEach { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    result += LyricLine((minutes * 60_000L) + (seconds * 1_000L) + fraction, text)
                }
            }
        }
        return result.sortedBy { it.timeMs }.takeIf { it.isNotEmpty() }
    }
}
