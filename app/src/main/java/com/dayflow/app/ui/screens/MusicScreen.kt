package com.dayflow.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.net.Uri

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dayflow_music", android.content.Context.MODE_PRIVATE) }
    val tracks = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("tracks", emptySet())?.toList().orEmpty())
        }
    }
    val player = remember { ExoPlayer.Builder(context).build() }
    var playing by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
            }
        }
        player.addListener(listener)
        if (tracks.isNotEmpty()) {
            player.setMediaItems(tracks.map { MediaItem.fromUri(Uri.parse(it)) })
            player.prepare()
        }
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                if (!tracks.contains(uri.toString())) tracks.add(uri.toString())
            }
            prefs.edit().putStringSet("tracks", tracks.toSet()).apply()
            player.setMediaItems(tracks.map { MediaItem.fromUri(Uri.parse(it)) })
            player.prepare()
            player.seekTo(tracks.lastIndex, 0L)
            currentIndex = tracks.lastIndex
            player.play()
        }
    }

    fun titleOf(value: String): String {
        val uri = Uri.parse(value)
        return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank { "Audio track" } ?: "Audio track"
    }

    ScreenColumn {
        SectionTitle("Music", "Your offline library")
        DayflowCard {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp))
            }
            Text(if (tracks.isEmpty()) "Nothing playing" else titleOf(tracks.getOrNull(currentIndex) ?: ""),
                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(if (tracks.isEmpty()) "Import audio files from your device" else "Track ${currentIndex + 1} of ${tracks.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = tracks.isNotEmpty(), onClick = { player.seekToPreviousMediaItem() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                FilledIconButton(enabled = tracks.isNotEmpty(), onClick = {
                    if (player.isPlaying) player.pause() else {
                        if (player.playbackState == Player.STATE_IDLE) player.prepare()
                        player.play()
                    }
                }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play") }
                IconButton(enabled = tracks.isNotEmpty(), onClick = { player.seekToNextMediaItem() }) {
                    Icon(Icons.Default.SkipNext, "Next")
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
            tracks.forEachIndexed { index, uri ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(titleOf(uri), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = {
                            player.seekTo(index, 0L)
                            player.play()
                            currentIndex = index
                        }) { Icon(Icons.Default.PlayArrow, "Play track") }
                        IconButton(onClick = {
                            tracks.remove(uri)
                            prefs.edit().putStringSet("tracks", tracks.toSet()).apply()
                            player.setMediaItems(tracks.map { MediaItem.fromUri(Uri.parse(it)) })
                            player.prepare()
                            currentIndex = 0
                        }) { Icon(Icons.Default.Delete, "Remove track") }
                    }
                }
            }
        }
    }
}
