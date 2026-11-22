package com.dayflow.app.ui.screens

import android.app.DownloadManager
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.content.ComponentName
import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dayflow.app.data.MusicStore
import com.dayflow.app.data.MusicTrack
import com.dayflow.app.service.DayflowPlaybackService

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val tracks = remember {
        mutableStateListOf<MusicTrack>().apply {
            addAll(MusicStore.load(context).map { track ->
                if (track.name.isBlank()) track.copy(name = MusicStore.displayName(context, Uri.parse(track.uri))) else track
            })
        }
    }
    val token = remember {
        SessionToken(context, ComponentName(context, DayflowPlaybackService::class.java))
    }
    val controllerFuture = remember(token) {
        MediaController.Builder(context, token).buildAsync()
    }
    var player by remember { mutableStateOf<MediaController?>(null) }
    var playing by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatAll by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<ArchiveTrack>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var currentArtist by remember { mutableStateOf("Unknown artist") }
    var artwork by remember { mutableStateOf<Bitmap?>(null) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var plainLyrics by remember { mutableStateOf<String?>(null) }
    var lyricsMessage by remember { mutableStateOf("Play a song to find its lyrics.") }
    val scope = rememberCoroutineScope()

    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
    }

    LaunchedEffect(player) {
        while (true) {
            player?.let { active ->
                if (!dragging) {
                    position = active.currentPosition.coerceAtLeast(0L)
                    duration = active.duration.coerceAtLeast(0L) // unknown duration is a negative sentinel
                }
            }
            delay(500)
        }
    }

    fun persist() = MusicStore.save(context, tracks.toList())

    LaunchedEffect(currentIndex, tracks.size) {
        val track = tracks.getOrNull(currentIndex)
        if (track == null) {
            currentArtist = "Unknown artist"; artwork = null; lyrics = emptyList(); plainLyrics = null
            lyricsMessage = "Play a song to find its lyrics."
        } else {
            val details = withContext(Dispatchers.IO) { readAudioDetails(context, Uri.parse(track.uri)) }
            currentArtist = details.artist.ifBlank { "Unknown artist" }
            artwork = details.artwork
            val found = withContext(Dispatchers.IO) { fetchLyrics(details.title.ifBlank { track.name }, details.artist, duration) }
            lyrics = found.first
            plainLyrics = found.second
            lyricsMessage = if (found.first.isEmpty() && found.second.isNullOrBlank()) "Lyrics are not available for this track." else ""
        }
    }

    fun mediaItems(): List<MediaItem> = tracks.map { track ->
        MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.name).build())
            .build()
    }

    // After the app restarts the library is shown but the player is empty: load it before playing.
    fun loadPlaylistIfEmpty(active: MediaController) {
        if (active.mediaItemCount == 0 && tracks.isNotEmpty()) {
            active.setMediaItems(mediaItems(), currentIndex.coerceIn(0, tracks.lastIndex), 0L)
            active.prepare()
        }
    }

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener({
            player = runCatching { controllerFuture.get() }.getOrNull()
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            MediaController.releaseFuture(controllerFuture)
            player = null
        }
    }

    DisposableEffect(player) {
        val activePlayer = player
        if (activePlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { shuffle = shuffleModeEnabled }
                override fun onRepeatModeChanged(repeatMode: Int) { repeatAll = repeatMode == Player.REPEAT_MODE_ALL }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (activePlayer.mediaItemCount > 0) currentIndex = activePlayer.currentMediaItemIndex.coerceAtLeast(0)
                }
            }
            activePlayer.addListener(listener)
            playing = activePlayer.isPlaying
            shuffle = activePlayer.shuffleModeEnabled
            repeatAll = activePlayer.repeatMode == Player.REPEAT_MODE_ALL
            if (activePlayer.mediaItemCount > 0) currentIndex = activePlayer.currentMediaItemIndex.coerceAtLeast(0)
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                if (tracks.none { it.uri == uri.toString() }) {
                    tracks.add(MusicTrack(uri.toString(), MusicStore.displayName(context, uri)))
                }
            }
            persist()
            player?.let { active ->
                active.setMediaItems(mediaItems(), tracks.lastIndex, 0L)
                active.prepare()
                active.play()
                currentIndex = tracks.lastIndex
            }
        }
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Music Player", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Icon(Icons.Default.MoreVert, "Import music", tint = MaterialTheme.colorScheme.primary) }
        }
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search by song name or link") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, trailingIcon = {
            IconButton(enabled = searchQuery.isNotBlank() && !searching, onClick = {
                scope.launch {
                    searching = true
                    searchMessage = null
                    val outcome = runCatching { searchArchiveAudio(searchTermFromInput(searchQuery.trim())) }
                    onlineResults = outcome.getOrDefault(emptyList())
                    searchMessage = when { outcome.isFailure -> "Search is temporarily unavailable."; onlineResults.isEmpty() -> "No tracks with an explicit download-friendly license were found."; else -> null }
                    searching = false
                }
            }) { Icon(Icons.Default.Search, contentDescription = "Search") }
        })
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        searchMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        onlineResults.forEach { result ->
            DayflowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(result.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 2)
                        Text("${result.creator} · ${result.license}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }
                    TextButton(onClick = {
                        val request = DownloadManager.Request(Uri.parse(result.downloadUrl)).setTitle(result.title).setDescription("Downloading full audio from Internet Archive").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setAllowedOverMetered(true).setAllowedOverRoaming(false).setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, result.fileName)
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.enqueue(request)
                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Download") }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.onPrimary.copy(alpha=.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(112.dp))
                }
                Text(if(tracks.isEmpty()) "Nothing playing" else tracks.getOrNull(currentIndex)?.name ?: "Audio track", color=MaterialTheme.colorScheme.onPrimary, style=MaterialTheme.typography.headlineSmall, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold, maxLines=1)
                Text(if(tracks.isEmpty()) "Your offline music" else "$currentArtist • Track ${currentIndex+1} of ${tracks.size}", color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.8f))
                Slider(value=(if(dragging)dragValue else position.toFloat()).coerceIn(0f,maxOf(duration.toFloat(),1f)),onValueChange={dragging=true;dragValue=it},onValueChangeFinished={player?.seekTo(dragValue.toLong());position=dragValue.toLong();dragging=false},valueRange=0f..maxOf(duration.toFloat(),1f),enabled=player!=null&&duration>0,colors=SliderDefaults.colors(thumbColor=MaterialTheme.colorScheme.onPrimary,activeTrackColor=MaterialTheme.colorScheme.onPrimary,inactiveTrackColor=MaterialTheme.colorScheme.onPrimary.copy(alpha=.25f)))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(clock(if(dragging)dragValue.toLong() else position),color=MaterialTheme.colorScheme.onPrimary,style=MaterialTheme.typography.labelSmall);Text(clock(duration),color=MaterialTheme.colorScheme.onPrimary,style=MaterialTheme.typography.labelSmall)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
                    IconButton(enabled=player!=null,onClick={player?.shuffleModeEnabled=!shuffle}){Icon(Icons.Default.Shuffle,"Shuffle",tint=MaterialTheme.colorScheme.onPrimary.copy(alpha=if(shuffle)1f else .65f))}
                    IconButton(enabled=player!=null&&tracks.isNotEmpty(),onClick={player?.let{loadPlaylistIfEmpty(it);it.seekToPreviousMediaItem()}}){Icon(Icons.Default.SkipPrevious,"Previous",tint=MaterialTheme.colorScheme.onPrimary)}
                    FilledIconButton(enabled=player!=null&&tracks.isNotEmpty(),onClick={player?.let{active->if(active.isPlaying)active.pause() else{loadPlaylistIfEmpty(active);if(active.playbackState==Player.STATE_IDLE)active.prepare();active.play()}}},modifier=Modifier.size(62.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.onPrimary,contentColor=MaterialTheme.colorScheme.primary)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,if(playing)"Pause" else "Play",modifier=Modifier.size(30.dp))}
                    IconButton(enabled=player!=null&&tracks.isNotEmpty(),onClick={player?.let{loadPlaylistIfEmpty(it);it.seekToNextMediaItem()}}){Icon(Icons.Default.SkipNext,"Next",tint=MaterialTheme.colorScheme.onPrimary)}
                    IconButton(enabled=player!=null,onClick={player?.repeatMode=if(repeatAll)Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL}){Icon(Icons.Default.Repeat,"Repeat",tint=MaterialTheme.colorScheme.onPrimary.copy(alpha=if(repeatAll)1f else .65f))}
                }
                if(player==null) Text("Connecting to player…",color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.75f),style=MaterialTheme.typography.labelSmall)
            }
        }
        DayflowCard {
            Text("Lyrics", color=MaterialTheme.colorScheme.onSurface, style=MaterialTheme.typography.titleLarge, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
            val activeLine = lyrics.lastOrNull { it.timeMs <= position }?.text
            if (lyrics.isNotEmpty()) {
                lyrics.take(80).forEach { line ->
                    Text(line.text, color=if (line.text == activeLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight=if (line.text == activeLine) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal, style=MaterialTheme.typography.bodyMedium)
                }
            } else Text(plainLyrics ?: lyricsMessage, color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Column { Text("Playlist",style=MaterialTheme.typography.titleLarge,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface); Text("${tracks.size} songs",color=MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick={picker.launch(arrayOf("audio/*"))}){Icon(Icons.Default.Add,null);Text("Import")}
        }
        if(tracks.isEmpty()) {
            DayflowCard { Icon(Icons.Default.MusicNote,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(34.dp)); Text("Your playlist is empty",color=MaterialTheme.colorScheme.onSurface,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold); Text("Import audio files from your phone to listen offline.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        } else tracks.forEachIndexed{index,track->
            DayflowCard {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=MaterialTheme.colorScheme.primary)}
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){Text(track.name,color=MaterialTheme.colorScheme.onSurface,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1);Text(if(index==currentIndex)"Now playing" else "Audio track",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelSmall)}
                    IconButton(enabled=player!=null,onClick={player?.let{active->active.setMediaItems(mediaItems(),index,0L);active.prepare();active.play();currentIndex=index}}){Icon(if(index==currentIndex&&playing)Icons.Default.Pause else Icons.Default.PlayArrow,"Play track",tint=MaterialTheme.colorScheme.primary)}
                    IconButton(onClick={player?.let{if(it.mediaItemCount>index)it.removeMediaItem(index)};tracks.removeAt(index);persist();currentIndex=if(tracks.isEmpty())0 else currentIndex.coerceAtMost(tracks.lastIndex)}){Icon(Icons.Default.Delete,"Remove track",tint=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
            }
        }
    }
}

private data class ArchiveTrack(val title: String, val creator: String, val license: String, val downloadUrl: String, val fileName: String)

/** Search Internet Archive and allow full downloads only for explicitly licensed CC/public-domain audio. */
private suspend fun searchArchiveAudio(query: String): List<ArchiveTrack> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode("mediatype:audio AND title:$query", "UTF-8")
    val searchUrl = URL("https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier&fl[]=title&fl[]=creator&rows=12&page=1&output=json")
    val searchConnection = (searchUrl.openConnection() as HttpURLConnection).apply { connectTimeout = 12000; readTimeout = 12000 }
    val docs = try { JSONObject(searchConnection.inputStream.bufferedReader().use { it.readText() }).getJSONObject("response").getJSONArray("docs") } finally { searchConnection.disconnect() }
    val accepted = mutableListOf<ArchiveTrack>()
    for (i in 0 until minOf(docs.length(), 12)) {
        val doc = docs.getJSONObject(i)
        val identifier = doc.optString("identifier")
        if (identifier.isBlank()) continue
        val connection = (URL("https://archive.org/metadata/$identifier").openConnection() as HttpURLConnection).apply { connectTimeout = 10000; readTimeout = 10000 }
        val metadata = try { JSONObject(connection.inputStream.bufferedReader().use { it.readText() }) } catch (_: Exception) { continue } finally { connection.disconnect() }
        val itemMeta = metadata.optJSONObject("metadata") ?: continue
        val licenseUrl = itemMeta.optString("licenseurl")
        val licenseText = when {
            licenseUrl.contains("creativecommons.org/publicdomain", ignoreCase = true) -> "Public domain"
            licenseUrl.contains("creativecommons.org/licenses/by-sa", ignoreCase = true) -> "CC BY-SA"
            licenseUrl.contains("creativecommons.org/licenses/by/", ignoreCase = true) -> "CC BY"
            licenseUrl.contains("creativecommons.org/publicdomain/zero", ignoreCase = true) -> "CC0"
            else -> continue
        }
        val files = metadata.optJSONArray("files") ?: continue
        var mp3: String? = null
        for (j in 0 until files.length()) {
            val file = files.optJSONObject(j) ?: continue
            val name = file.optString("name")
            if (name.endsWith(".mp3", ignoreCase = true) && file.optString("format").contains("mp3", ignoreCase = true)) { mp3 = name; break }
        }
        val fileName = mp3 ?: continue
        val safeName = fileName.substringAfterLast("/").replace(Regex("[^A-Za-z0-9._-]"), "_")
        accepted.add(ArchiveTrack(doc.optString("title", identifier), doc.optString("creator", "Unknown artist"), licenseText, "https://archive.org/download/$identifier/${Uri.encode(fileName, "/")}", safeName))
    }
    accepted
}


private data class AudioDetails(val title: String, val artist: String, val artwork: Bitmap?)
private data class LyricLine(val timeMs: Long, val text: String)

private fun readAudioDetails(context: Context, uri: Uri): AudioDetails {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty()
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
        val art = retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        AudioDetails(title, artist, art)
    } catch (_: Exception) {
        AudioDetails("", "", null)
    } finally { runCatching { retriever.release() } }
}

private suspend fun fetchLyrics(title: String, artist: String, durationMs: Long): Pair<List<LyricLine>, String?> =
    withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext emptyList<LyricLine>() to null
        val params = "track_name=" + URLEncoder.encode(title, "UTF-8") +
            if (artist.isNotBlank() && artist != "Unknown artist") "&artist_name=" + URLEncoder.encode(artist, "UTF-8") else ""
        val durationParam = if (durationMs > 0) "&duration=" + (durationMs / 1000) else ""
        val connection = (URL("https://lrclib.net/api/get?" + params + durationParam).openConnection() as HttpURLConnection).apply {
            connectTimeout = 9000; readTimeout = 9000
            setRequestProperty("User-Agent", "DAYFLOW Android Music Player")
        }
        try {
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            parseLrc(json.optString("syncedLyrics")) to json.optString("plainLyrics").takeIf { it.isNotBlank() }
        } catch (_: Exception) { emptyList<LyricLine>() to null }
        finally { connection.disconnect() }
    }

private fun parseLrc(value: String): List<LyricLine> {
    val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
    return value.lineSequence().mapNotNull { line ->
        val match = regex.find(line) ?: return@mapNotNull null
        val min = match.groupValues[1].toLongOrNull() ?: 0L
        val sec = match.groupValues[2].toLongOrNull() ?: 0L
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        LyricLine((min * 60 + sec) * 1000 + fraction, match.groupValues[4].trim())
    }.sortedBy { it.timeMs }.toList()
}

private fun searchTermFromInput(input: String): String {
    val uri = runCatching { Uri.parse(input) }.getOrNull()
    if (uri?.scheme == "http" || uri?.scheme == "https") {
        return uri.lastPathSegment.orEmpty().substringBefore('?').replace('-', ' ').replace('_', ' ').ifBlank { input }
    }
    return input
}
