package com.dayflow.app.ui.screens

import android.content.ComponentName
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dayflow.app.data.LyricsRepository
import com.dayflow.app.data.LyricLine
import com.dayflow.app.data.MusicStore
import com.dayflow.app.data.MusicTrack
import com.dayflow.app.data.TrackLyrics
import com.dayflow.app.data.TrackMetadata
import com.dayflow.app.data.TrackMetadataReader
import com.dayflow.app.service.DayflowPlaybackService
import kotlinx.coroutines.delay

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
    val token = remember { SessionToken(context, ComponentName(context, DayflowPlaybackService::class.java)) }
    val controllerFuture = remember(token) { MediaController.Builder(context, token).buildAsync() }
    var player by remember { mutableStateOf<MediaController?>(null) }
    var playing by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatAll by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf<TrackMetadata?>(null) }
    val trackMetadata = remember { mutableStateMapOf<String, TrackMetadata>() }
    var lyrics by remember { mutableStateOf<TrackLyrics?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    val lyricsListState = rememberLazyListState()

    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
    }

    fun persist() = MusicStore.save(context, tracks.toList())

    fun mediaItems(): List<MediaItem> = tracks.map { track ->
        MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.name).build())
            .build()
    }

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

    LaunchedEffect(player) {
        while (true) {
            player?.let { active ->
                if (!dragging) {
                    position = active.currentPosition.coerceAtLeast(0L)
                    duration = active.duration.coerceAtLeast(0L)
                }
            }
            delay(350)
        }
    }

    val currentTrack = tracks.getOrNull(currentIndex)
    LaunchedEffect(tracks.map { it.uri }) {
        val activeUris = tracks.map { it.uri }.toSet()
        trackMetadata.keys.toList().filterNot { it in activeUris }.forEach(trackMetadata::remove)
        tracks.forEach { track ->
            if (track.uri !in trackMetadata) {
                trackMetadata[track.uri] = TrackMetadataReader.read(context, Uri.parse(track.uri), track.name)
            }
        }
    }

    LaunchedEffect(currentTrack?.uri, currentTrack?.name) {
        metadata = null
        lyrics = null
        if (currentTrack != null) {
            lyricsLoading = true
            val info = TrackMetadataReader.read(context, Uri.parse(currentTrack.uri), currentTrack.name)
            metadata = info
            lyrics = LyricsRepository.find(info.title, info.artist, info.durationMs)
            lyricsLoading = false
        } else {
            lyricsLoading = false
        }
    }

    val lyricLines = lyrics?.lines.orEmpty()
    val activeLyricIndex = remember(lyricLines, position) {
        lyricLines.indexOfLast { it.timeMs != null && it.timeMs <= position }.coerceAtLeast(0)
    }
    LaunchedEffect(activeLyricIndex, showLyrics) {
        if (showLyrics && lyrics?.isSynced == true && lyricLines.isNotEmpty()) {
            lyricsListState.animateScrollToItem(activeLyricIndex.coerceIn(0, lyricLines.lastIndex))
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            Column(Modifier.weight(1f)) {
                Text("Music", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your offline library", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Default.Add, "Import music", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(if (showLyrics) Icons.Default.Album else Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (showLyrics) "PLAYER" else "LYRICS", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (showLyrics) {
                    LyricsPanel(
                        lyrics = lyrics,
                        loading = lyricsLoading,
                        title = metadata?.title ?: currentTrack?.name.orEmpty(),
                        artist = metadata?.artist ?: " ",
                        activeIndex = activeLyricIndex,
                        listState = lyricsListState,
                        onSeek = { time -> player?.seekTo(time); position = time }
                    )
                } else {
                    VinylDisc(
                        modifier = Modifier.padding(vertical = 4.dp),
                        isPlaying = playing
                    )
                    Text(
                        metadata?.title ?: if (tracks.isEmpty()) "Nothing playing" else currentTrack?.name ?: "Audio track",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        metadata?.artist ?: if (tracks.isEmpty()) "Import music from your phone" else "Reading track details…",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
                        thumbColor = MaterialTheme.colorScheme.onPrimary,
                        activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .25f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(clock(if (dragging) dragValue.toLong() else position), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                    Text(clock(duration), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = player != null, onClick = { player?.shuffleModeEnabled = !shuffle }) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (shuffle) 1f else .65f))
                    }
                    IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = { player?.let { loadPlaylistIfEmpty(it); it.seekToPreviousMediaItem() } }) {
                        Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    FilledIconButton(
                        enabled = player != null && tracks.isNotEmpty(),
                        onClick = {
                            player?.let { active ->
                                if (active.isPlaying) active.pause() else {
                                    loadPlaylistIfEmpty(active)
                                    if (active.playbackState == Player.STATE_IDLE) active.prepare()
                                    active.play()
                                }
                            }
                        },
                        modifier = Modifier.size(62.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play", modifier = Modifier.size(30.dp))
                    }
                    IconButton(enabled = player != null && tracks.isNotEmpty(), onClick = { player?.let { loadPlaylistIfEmpty(it); it.seekToNextMediaItem() } }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(enabled = player != null, onClick = { player?.repeatMode = if (repeatAll) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL }) {
                        Icon(Icons.Default.Repeat, "Repeat", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (repeatAll) 1f else .65f))
                    }
                }
                if (player == null) Text("Connecting to player…", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${tracks.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Icon(Icons.Default.Add, null); Text("Import") }
        }
        if (tracks.isEmpty()) {
            DayflowCard {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Text("Your playlist is empty", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("Import audio files from your phone to listen offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            tracks.withIndex()
                .groupBy { (_, track) ->
                    val detectedArtist = trackMetadata[track.uri]?.artist
                        ?.takeIf { it.isNotBlank() && !it.equals("Unknown artist", ignoreCase = true) }
                        ?: inferredArtistFromFileName(track.name)
                    detectedArtist?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()
                        ?: "__unknown_artist__"
                }
                .forEach { (_, artistTracks) ->
                    val artistLabel = artistTracks.firstOrNull()?.value?.let { track ->
                        trackMetadata[track.uri]?.artist
                            ?.takeIf { it.isNotBlank() && !it.equals("Unknown artist", ignoreCase = true) }
                            ?: inferredArtistFromFileName(track.name)
                    } ?: "Unknown artist"
                    Text(
                        artistLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                    )
                    artistTracks.forEach { indexedTrack ->
                        val index = indexedTrack.index
                        val track = indexedTrack.value
                        DayflowCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(if (index == currentIndex) "Now playing" else artistLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(enabled = player != null, onClick = {
                                    player?.let { active -> active.setMediaItems(mediaItems(), index, 0L); active.prepare(); active.play(); currentIndex = index }
                                }) {
                                    Icon(if (index == currentIndex && playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play track", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    player?.let { if (it.mediaItemCount > index) it.removeMediaItem(index) }
                                    tracks.removeAt(index)
                                    persist()
                                    currentIndex = if (tracks.isEmpty()) 0 else currentIndex.coerceAtMost(tracks.lastIndex)
                                }) { Icon(Icons.Default.Delete, "Remove track", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun VinylDisc(modifier: Modifier = Modifier, isPlaying: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
                )
            }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(226.dp).rotate(rotation.value)) {
            val radius = size.minDimension / 2f
            drawCircle(Color(0xFF171216), radius = radius)
            for (fraction in listOf(.25f, .34f, .43f, .52f, .61f, .70f, .79f, .88f)) {
                drawCircle(Color(0xFF51414A).copy(alpha = .7f), radius = radius * fraction, style = Stroke(width = 1.4.dp.toPx()))
            }
            drawCircle(Color(0xFF7B2948), radius = radius * .31f)
            drawCircle(Color(0xFFB85A77), radius = radius * .24f)
            drawCircle(Color(0xFFF0DDE4), radius = radius * .045f)
        }
    }
}

@Composable
private fun LyricsPanel(
    lyrics: TrackLyrics?,
    loading: Boolean,
    title: String,
    artist: String,
    activeIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeek: (Long) -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LYRICS", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(title, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
            lyrics == null || lyrics.lines.isEmpty() -> Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                Text("Lyrics not found for this track", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), textAlign = TextAlign.Center)
            }
            else -> {
                if (!lyrics.isSynced) {
                    Text("These lyrics are not time-synced. Tap-to-seek is unavailable.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .65f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentPadding = PaddingValues(vertical = 64.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val selected = lyrics.isSynced && index == activeIndex
                        val isArabic = remember(line.text) { Regex("[\\u0600-\\u06FF]").containsMatchIn(line.text) }
                        Text(
                            line.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = line.timeMs != null) { line.timeMs?.let(onSeek) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = if (lyrics.isSynced) .48f else .82f),
                            style = if (selected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isArabic) FontWeight.Bold else if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


private fun inferredArtistFromFileName(fileName: String): String? {
    val baseName = fileName.substringBeforeLast('.', fileName).trim()
    val separators = listOf(" - ", " – ", " — ", " _ ")
    val separator = separators.firstOrNull { baseName.contains(it) } ?: return null
    val artist = baseName.substringBefore(separator).trim()
    val title = baseName.substringAfter(separator).trim()
    return artist.takeIf { it.length >= 2 && title.isNotBlank() }
}
