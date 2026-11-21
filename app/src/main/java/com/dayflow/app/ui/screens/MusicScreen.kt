package com.dayflow.app.ui.screens

import android.content.ComponentName
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dayflow.app.service.DayflowPlaybackService

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dayflow_music", Context.MODE_PRIVATE) }
    val tracks = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("tracks", emptySet())?.toList().orEmpty())
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
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentIndex = activePlayer.currentMediaItemIndex.coerceAtLeast(0)
                }
            }
            activePlayer.addListener(listener)
            playing = activePlayer.isPlaying
            currentIndex = activePlayer.currentMediaItemIndex.coerceAtLeast(0)
            onDispose { activePlayer.removeListener(listener) }
        }
    }

    fun titleOf(value: String): String {
        val uri = Uri.parse(value)
        return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            ?.ifBlank { "Audio track" } ?: "Audio track"
    }

    fun mediaItems(values: List<String>): List<MediaItem> = values.map { value ->
        MediaItem.Builder()
            .setUri(Uri.parse(value))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(titleOf(value)).build())
            .build()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                if (!tracks.contains(uri.toString())) tracks.add(uri.toString())
            }
            prefs.edit().putStringSet("tracks", tracks.toSet()).apply()
            player?.let { active ->
                active.setMediaItems(mediaItems(tracks))
                active.prepare()
                active.seekTo(tracks.lastIndex, 0L)
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
                if (tracks.isEmpty()) "Nothing playing" else titleOf(tracks.getOrNull(currentIndex) ?: ""),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                if (tracks.isEmpty()) "Import audio files from your device" else "Track ${currentIndex + 1} of ${tracks.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = { player?.seekToPreviousMediaItem() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                FilledIconButton(enabled = player != null && tracks.isNotEmpty(), onClick = {
                    player?.let { active ->
                        if (active.isPlaying) active.pause() else {
                            if (active.playbackState == Player.STATE_IDLE) active.prepare()
                            active.play()
                        }
                    }
                }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play")
                }
                IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = { player?.seekToNextMediaItem() }) {
                    Icon(Icons.Default.SkipNext, "Next")
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
            tracks.forEachIndexed { index, uri ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(titleOf(uri), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        IconButton(enabled = player != null, onClick = {
                            player?.let { active ->
                                active.setMediaItems(mediaItems(tracks), index, 0L)
                                active.prepare()
                                active.play()
                                currentIndex = index
                            }
                        }) { Icon(Icons.Default.PlayArrow, "Play track") }
                        IconButton(enabled = player != null, onClick = {
                            val wasCurrent = index == currentIndex
                            tracks.remove(uri)
                            prefs.edit().putStringSet("tracks", tracks.toSet()).apply()
                            player?.let { active ->
                                active.setMediaItems(mediaItems(tracks))
                                if (tracks.isNotEmpty()) {
                                    active.prepare()
                                    active.seekTo((index.coerceAtMost(tracks.lastIndex)), 0L)
                                } else {
                                    active.stop()
                                }
                            }
                            currentIndex = if (tracks.isEmpty()) 0 else index.coerceAtMost(tracks.lastIndex)
                            if (wasCurrent) playing = false
                        }) { Icon(Icons.Default.Delete, "Remove track") }
                    }
                }
            }
        }
    }
}