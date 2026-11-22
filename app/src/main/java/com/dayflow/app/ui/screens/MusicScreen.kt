package com.dayflow.app.ui.screens

import android.content.ComponentName
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        SectionTitle("Music", "Your offline library")
        DayflowCard {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp))
            }
            Text(
                if (tracks.isEmpty()) "Nothing playing" else tracks.getOrNull(currentIndex)?.name ?: "Audio track",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
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
        AccentButton("＋  Import songs") { picker.launch(arrayOf("audio/*")) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Library", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("${tracks.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (tracks.isEmpty()) {
            PlaceholderNote("Choose audio files from your phone. Your library stays available offline.")
        } else {
            tracks.forEachIndexed { index, track ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
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
