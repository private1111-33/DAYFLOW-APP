package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(onOpenProgress: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val tasks = remember { LocalStore.tasks(context) }
    val words = remember { LocalStore.words(context) }
    val alarms = remember { AlarmStore.all(context).filter { it.enabled } }
    val completed = tasks.count { it.done }
    val pending = tasks.filter { !it.done }.sortedBy { it.time }
    val nextAlarm = alarms.sortedWith(compareBy({ it.hour }, { it.minute })).firstOrNull()
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning."
        in 12..17 -> "Good afternoon."
        else -> "Good evening."
    }
    ScreenColumn {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()).uppercase(),
                    color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Text(greeting, color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
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
                CircularProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size },
                    color = BurgundyLight, trackColor = SurfaceElevated)
            }
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth(), color = BurgundyLight, trackColor = SurfaceElevated)
            Text("$completed of ${tasks.size} tasks completed", color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DayflowCardWeighted(Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, null, tint = BurgundyLight)
                Text(completed.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Completed", color = TextSecondary)
            }
            DayflowCardWeighted(Modifier.weight(1f)) {
                Icon(Icons.Default.Alarm, null, tint = BurgundyLight)
                Text(nextAlarm?.let { String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute) } ?: "--:--",
                    style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Next alarm", color = TextSecondary)
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Upcoming tasks", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("${pending.size} left", color = BurgundyLight)
            }
            if (pending.isEmpty()) Text("No pending tasks. Your schedule is clear.", color = TextSecondary)
            else pending.take(3).forEach { task ->
                TaskLine(task.title, task.time, task.done)
            }
        }
        DayflowCard {
            Text("Vocabulary", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${words.size} saved · ${words.count { !it.learned }} to review", color = TextSecondary)
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Your progress", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("See your activity from real saved data", color = TextSecondary)
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
