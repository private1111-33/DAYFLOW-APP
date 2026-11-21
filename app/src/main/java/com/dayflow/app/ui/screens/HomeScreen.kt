package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.*

@Composable
fun HomeScreen(onOpenProgress: () -> Unit, onOpenSettings: () -> Unit) {
    ScreenColumn {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("SATURDAY, NOVEMBER 21", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text("Good afternoon.", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextPrimary) }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TODAY'S FOCUS", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text("Keep your day moving", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text("Your daily overview", color = TextSecondary)
                }
                CircularProgressIndicator(progress = { 0.75f }, color = BurgundyLight, trackColor = SurfaceElevated)
            }
            LinearProgressIndicator(progress = { 0.75f }, modifier = Modifier.fillMaxWidth(), color = BurgundyLight, trackColor = SurfaceElevated)
            Text("3 of 4 tasks completed", color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DayflowCardWeighted(Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, null, tint = BurgundyLight)
                Text("03", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Completed", color = TextSecondary)
            }
            DayflowCardWeighted(Modifier.weight(1f)) {
                Icon(Icons.Default.Alarm, null, tint = BurgundyLight)
                Text("08:30", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Next alarm", color = TextSecondary)
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Upcoming tasks", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("View all", color = BurgundyLight)
            }
            TaskLine("Review project notes", "10:30 AM", false)
            TaskLine("English vocabulary", "12:00 PM", true)
        }
        DayflowCard {
            Text("Vocabulary today", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("12 words waiting for review", color = TextSecondary)
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Your progress", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("See your weekly activity", color = TextSecondary)
                }
                IconButton(onClick = onOpenProgress) { Icon(Icons.Default.ArrowForward, null, tint = BurgundyLight) }
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Now playing", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Choose music from your library", color = TextSecondary)
                }
                Icon(Icons.Default.MusicNote, null, tint = BurgundyLight)
            }
        }
    }
}

@Composable
private fun TaskLine(title: String, time: String, done: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                tint = if (done) Success else TextSecondary)
            Text(title, color = TextPrimary)
        }
        Text(time, color = TextSecondary)
    }
}
