package com.dayflow.app.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.MusicStore
import com.dayflow.app.data.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class FoundTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val artwork: String,
    val preview: String,
    val durationMs: Long
)

private data class LyricsData(val text: String, val synced: Boolean)

private object MusicDiscoveryApi {
    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        connection.setRequestProperty("User-Agent", "DAYFLOW-Android/1.0 (music discovery)")
        return try { connection.inputStream.bufferedReader().use { it.readText() } }
        finally { connection.disconnect() }
    }

    suspend fun search(query: String): List<FoundTrack> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = JSONObject(get("https://itunes.apple.com/search?term=$encoded&entity=song&limit=30"))
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).mapNotNull { i ->
            val item = results.optJSONObject(i) ?: return@mapNotNull null
            val preview = item.optString("previewUrl")
            if (preview.isBlank() || preview == "null") return@mapNotNull null
            FoundTrack(
                id = item.optLong("trackId"),
                title = item.optString("trackName", "Unknown track"),
                artist = item.optString("artistName", "Unknown artist"),
                artwork = item.optString("artworkUrl100").replace("100x100", "600x600"),
                preview = preview,
                durationMs = item.optLong("trackTimeMillis")
            )
        }
    }

    suspend fun lyrics(track: FoundTrack): LyricsData? = withContext(Dispatchers.IO) {
        val name = URLEncoder.encode(track.title, "UTF-8")
        val artist = URLEncoder.encode(track.artist, "UTF-8")
        val response = get("https://lrclib.net/api/search?track_name=$name&artist_name=$artist")
        val array = org.json.JSONArray(response)
        if (array.length() == 0) return@withContext null
        val item = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            .maxByOrNull { if (it.optString("syncedLyrics").isNotBlank()) 2 else 1 }
            ?: return@withContext null
        val synced = item.optString("syncedLyrics")
        val plain = item.optString("plainLyrics")
        val text = if (synced.isNotBlank() && synced != "null") synced else plain
        if (text.isBlank() || text == "null") null else LyricsData(text, synced.isNotBlank())
    }

    suspend fun downloadPreview(track: FoundTrack, target: File) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        URL(track.preview).openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    suspend fun artwork(url: String) = withContext(Dispatchers.IO) {
        runCatching { URL(url).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
}

@Composable
fun MusicSearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoundTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<FoundTrack?>(null) }
    var lyrics by remember { mutableStateOf<LyricsData?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<Long?>(null) }
    var savedIds by remember { mutableStateOf(setOf<Long>()) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var artworkColor by remember { mutableStateOf(Color(0xFFD12D5B)) }

    DisposableEffect(Unit) {
        onDispose { runCatching { player?.stop(); player?.release() }; player = null }
    }

    fun search() {
        if (query.isBlank()) return
        loading = true
        error = null
        scope.launch {
            try {
                results = MusicDiscoveryApi.search(query.trim())
                if (results.isEmpty()) error = "No tracks with playable previews were found."
            } catch (e: Exception) { error = "Search failed. Check your internet connection." }
            finally { loading = false }
        }
    }

    ScreenColumn {
        SectionTitle("Discover", "Search songs and explore lyrics")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Song, artist or album") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { search() }) { Icon(Icons.Default.Search, "Search") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            shape = RoundedCornerShape(18.dp)
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        selected?.let { track ->
            DayflowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrackArtwork(track.artwork, Modifier.size(76.dp)) { color -> artworkColor = color }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Preview • ${formatDuration(track.durationMs)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        if (playingId == track.id) {
                            runCatching { player?.pause() }
                            playingId = null
                        } else {
                            runCatching { player?.release() }
                            player = MediaPlayer().apply {
                                setDataSource(track.preview)
                                setOnCompletionListener { playingId = null }
                                prepareAsync()
                                setOnPreparedListener { it.start(); playingId = track.id }
                            }
                        }
                    }) {
                        Icon(if (playingId == track.id) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (playingId == track.id) "Pause preview" else "Play preview")
                    }
                    Button(onClick = {
                        lyricsLoading = true
                        lyrics = null
                        scope.launch {
                            lyrics = runCatching { MusicDiscoveryApi.lyrics(track) }.getOrNull()
                            lyricsLoading = false
                        }
                    }) { Text("Lyrics") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val file = File(context.filesDir, "music-previews/${track.id}.m4a")
                            try {
                                if (!file.exists()) MusicDiscoveryApi.downloadPreview(track, file)
                                val current = MusicStore.load(context).toMutableList()
                                val uri = Uri.fromFile(file).toString()
                                if (current.none { it.uri == uri }) {
                                    current.add(MusicTrack(uri, "${track.title} — ${track.artist} (Preview)"))
                                    MusicStore.save(context, current)
                                }
                                savedIds = savedIds + track.id
                            } catch (_: Exception) { error = "Could not save this preview." }
                        }
                    }) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (track.id in savedIds) "Saved" else "Save preview")
                    }
                }
                if (lyricsLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                lyrics?.let { lyric ->
                    HorizontalDivider()
                    Text(if (lyric.synced) "Synced lyrics" else "Lyrics", fontWeight = FontWeight.SemiBold)
                    Text(lyric.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text("Results", style = MaterialTheme.typography.titleLarge)
        if (results.isEmpty() && !loading) {
            PlaceholderNote("Search for a song or artist to see matching tracks.")
        }
        results.forEach { track ->
            DayflowCard {
                Row(
                    Modifier.fillMaxWidth().clickable { selected = track; lyrics = null },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(track.artwork, Modifier.size(58.dp)) { }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { selected = track; lyrics = null }) {
                        Icon(Icons.Default.MusicNote, "Open track")
                    }
                }
            }
        }
        Text(
            "Previews are short samples supplied by the music catalog. Saving adds the sample to your DAYFLOW library; it does not download the full commercial song.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrackArtwork(url: String, modifier: Modifier, onColor: (Color) -> Unit) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = MusicDiscoveryApi.artwork(url)
        bitmap?.let { image ->
            val sample = image.getPixel(image.width / 2, image.height / 2)
            onColor(Color(sample))
        }
    }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), contentDescription = "Album artwork", modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
