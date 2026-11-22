package com.dayflow.app.ui.screens

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dayflow.app.data.DayflowLyrics
import com.dayflow.app.data.DayflowTrackLyrics
import com.dayflow.app.data.MusicStore
import com.dayflow.app.data.MusicTrack
import com.dayflow.app.service.DayflowPlaybackService
import com.dayflow.app.ui.theme.BurgundyLight

private data class LocalTrackInfo(val artist: String, val artwork: Bitmap?)

@Composable
fun MusicScreen(onOpenSearch: () -> Unit = {}) {
    val context = LocalContext.current
    val tracks = remember {
        mutableStateListOf<MusicTrack>().apply {
            addAll(MusicStore.load(context).map { track ->
                if (track.name.isBlank()) track.copy(name = MusicStore.displayName(context, Uri.parse(track.uri))) else track
            })
        }
    }
    val trackInfo by produceState(initialValue = emptyMap<String, LocalTrackInfo>(), tracks.toList()) {
        value = withContext(Dispatchers.IO) {
            tracks.associate { track ->
                val retriever = MediaMetadataRetriever()
                val info = try {
                    retriever.setDataSource(context, Uri.parse(track.uri))
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
                    val art = retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    track.uri to LocalTrackInfo(artist.ifBlank { "Unknown artist" }, art)
                } catch (_: Exception) {
                    track.uri to LocalTrackInfo("Unknown artist", null)
                } finally {
                    runCatching { retriever.release() }
                }
                info
            }
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
    var currentLyrics by remember { mutableStateOf<DayflowTrackLyrics?>(null) }
    var activeLyricIndex by remember { mutableIntStateOf(-1) }
    var lyricsLoading by remember { mutableStateOf(false) }

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

    LaunchedEffect(currentIndex, tracks.size) {
        val track = tracks.getOrNull(currentIndex)
        currentLyrics = null
        activeLyricIndex = -1
        if (track != null) {
            lyricsLoading = true
            currentLyrics = runCatching { DayflowLyrics.identifyAndLoad(context, track.uri) }.getOrNull()
            lyricsLoading = false
        }
    }

    LaunchedEffect(playing, currentLyrics) {
        while (playing && currentLyrics?.lines?.isNotEmpty() == true) {
            val now = player?.currentPosition ?: 0L
            activeLyricIndex = currentLyrics?.lines?.indexOfLast { it.timeMs <= now } ?: -1
            delay(120)
        }
    }

    fun persist() = MusicStore.save(context, tracks.toList())

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

    val currentTrack = tracks.getOrNull(currentIndex)
    val currentInfo = currentTrack?.let { trackInfo[it.uri] }
    val lyricAccent = currentInfo?.artwork?.let { bitmap ->
        runCatching { Color(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)) }.getOrNull()
    } ?: BurgundyLight
    val vinylTransition = rememberInfiniteTransition(label = "vinyl-spin")
    val vinylRotation by vinylTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Restart),
        label = "vinyl-rotation"
    )

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle("Music", "Your personal music library")
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search music online", tint = MaterialTheme.colorScheme.primary)
            }
        }
        DayflowCard {
            Box(Modifier.fillMaxWidth().height(178.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(158.dp).graphicsLayer { rotationZ = if (playing) vinylRotation else 0f }
                        .clip(CircleShape).background(Color(0xFF101013))
                        .border(5.dp, Color(0xFF29252A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    repeat(3) { ring ->
                        Box(
                            Modifier.size((132 - ring * 22).dp)
                                .border(1.dp, Color(0xFF373239), CircleShape)
                        )
                    }
                    Box(
                        Modifier.size(54.dp).clip(CircleShape)
                            .background(lyricAccent).border(3.dp, Color(0xFF171416), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentInfo?.artwork != null) {
                            Image(currentInfo.artwork.asImageBitmap(), "Album cover",
                                Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                    }
                }
            }
            Text(
                if (tracks.isEmpty()) "Nothing playing" else currentTrack?.name ?: "Audio track",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
            Text(currentInfo?.artist ?: if (tracks.isEmpty()) "Add music to get started" else "Unknown artist",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(
                if (tracks.isEmpty()) "Import audio files from your device" else "Track ${currentIndex + 1} of ${tracks.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = (if (dragging) dragValue else position.toFloat()).coerceIn(0f, maxOf(duration.toFloat(), 1f)),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    player?.seekTo(dragValue.toLong())
                    position = dragValue.toLong()
                    dragging = false
                },
                valueRange = 0f..maxOf(duration.toFloat(), 1f),
                enabled = player != null && duration > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(if (dragging) dragValue.toLong() else position), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(clock(duration), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(enabled = player != null, onClick = { player?.shuffleModeEnabled = !shuffle }) {
                    Icon(Icons.Default.Shuffle, "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = {
                    player?.let { active ->
                        loadPlaylistIfEmpty(active)
                        active.seekToPreviousMediaItem()
                    }
                }) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                FilledIconButton(enabled = player != null && tracks.isNotEmpty(), onClick = {
                    player?.let { active ->
                        if (active.isPlaying) active.pause() else {
                            loadPlaylistIfEmpty(active)
                            if (active.playbackState == Player.STATE_IDLE) active.prepare()
                            active.play()
                        }
                    }
                }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play")
                }
                IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = {
                    player?.let { active ->
                        loadPlaylistIfEmpty(active)
                        active.seekToNextMediaItem()
                    }
                }) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
                IconButton(enabled = player != null, onClick = {
                    player?.repeatMode = if (repeatAll) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
                }) {
                    Icon(Icons.Default.Repeat, "Repeat all",
                        tint = if (repeatAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (player == null) {
                Text("Connecting to player…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        if (lyricsLoading) {
            Text("Finding lyrics…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        currentLyrics?.let { lyric ->
            DayflowCard {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(lyricAccent.copy(alpha = 0.16f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("LYRICS", style = MaterialTheme.typography.labelMedium, color = lyricAccent)
                    Text(lyric.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (lyric.artist.isNotBlank()) Text(lyric.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (lyric.lines.isNotEmpty()) {
                        lyric.lines.forEachIndexed { index, line ->
                            Text(line.text,
                                color = if (index == activeLyricIndex) lyricAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (index == activeLyricIndex) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                style = if (index == activeLyricIndex) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    player?.seekTo(line.timeMs)
                                    position = line.timeMs
                                    activeLyricIndex = index
                                }.padding(vertical = 5.dp))
                        }
                    } else {
                        Text(lyric.plainText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        AccentButton("＋  Import songs") { picker.launch(arrayOf("audio/*")) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Library", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("${tracks.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (tracks.isEmpty()) {
            PlaceholderNote("Choose audio files from your phone. Your library stays available offline.")
        } else {
            trackInfo.entries.groupBy { it.value.artist }.toSortedMap().forEach { (artist, artistTracks) ->
                Text(artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                artistTracks.forEach { (uri, info) ->
                    val index = tracks.indexOfFirst { it.uri == uri }
                    val track = tracks.getOrNull(index) ?: return@forEach
                    DayflowCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (info.artwork != null) {
                                Image(info.artwork.asImageBitmap(), "Album artwork",
                                    Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
                            } else {
                                Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(track.name, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                            IconButton(enabled = player != null, onClick = {
                                player?.let { active ->
                                    active.setMediaItems(mediaItems(), index, 0L)
                                    active.prepare()
                                    active.play()
                                    currentIndex = index
                                }
                            }) { Icon(Icons.Default.PlayArrow, "Play track") }
                            IconButton(onClick = {
                                player?.let { active ->
                                    if (active.mediaItemCount > index) active.removeMediaItem(index)
                                }
                                tracks.removeAt(index)
                                persist()
                                currentIndex = if (tracks.isEmpty()) 0 else currentIndex.coerceAtMost(tracks.lastIndex)
                            }) { Icon(Icons.Default.Delete, "Remove track") }
                        }
                    }
                }
            }
        }
    }
}
