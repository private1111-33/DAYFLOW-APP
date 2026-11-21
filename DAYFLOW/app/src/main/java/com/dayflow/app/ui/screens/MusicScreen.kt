package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MusicScreen() {
    ScreenColumn {
        SectionTitle("Music", "Your offline library")
        DayflowCard {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(96.dp))
            }
            Text("Nothing playing", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("Import audio files stored on your device", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, "Previous") }
                IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, "Next") }
            }
        }
        AccentButton("＋  Import songs") {}
        PlaceholderNote("Playback, playlists, background service and lock-screen controls will be implemented with Android Media3.")
    }
}
