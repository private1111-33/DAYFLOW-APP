package com.dayflow.app.ui.screens

import android.content.ComponentName
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Music Player", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Icon(Icons.Default.MoreVert, "Import music", tint = MaterialTheme.colorScheme.primary) }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.onPrimary.copy(alpha=.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(112.dp))
                }
                Text(if(tracks.isEmpty()) "Nothing playing" else tracks.getOrNull(currentIndex)?.name ?: "Audio track", color=MaterialTheme.colorScheme.onPrimary, style=MaterialTheme.typography.headlineSmall, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold, maxLines=1)
                Text(if(tracks.isEmpty()) "Your offline music" else "DAYFLOW • Track ${currentIndex+1} of ${tracks.size}", color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.8f))
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
