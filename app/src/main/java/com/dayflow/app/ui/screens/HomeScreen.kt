package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val learned = words.count { it.learned }
    val percent = if (tasks.isEmpty()) 0 else completed * 100 / tasks.size
    val nextAlarm = alarms.sortedWith(compareBy({ it.hour }, { it.minute })).firstOrNull()
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning,"
        in 12..17 -> "Good Afternoon,"
        else -> "Good Evening,"
    }
    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TaskAlt, null, tint = BurgundyLight, modifier = Modifier.size(42.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("DAYFLOW", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your Personal Daily System", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextPrimary) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(greeting, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Let's make today productive.", color = TextSecondary)
            }
            Text(SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(Date()), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 9.dp,
                        color = BurgundyLight,
                        trackColor = SurfaceElevated
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percent%", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Today", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("Complete", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricLine(Success, completed.toString(), "Completed")
                    MetricLine(BurgundyLight, tasks.size.toString(), "Total tasks")
                    MetricLine(Warning, (tasks.size - completed).toString(), "Remaining")
                }
            }
        }
        DayflowCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = BurgundyLight, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Next Alarm", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(nextAlarm?.let { String.format(Locale.ENGLISH, "%d:%02d %s", if (it.hour % 12 == 0) 12 else it.hour % 12, it.minute, if (it.hour < 12) "AM" else "PM") } ?: "No alarm set",
                        color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(nextAlarm?.label?.ifBlank { "Daily reminder" } ?: "Create a reminder in Alarms", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
            }
        }
        DayflowCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = BurgundyLight, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vocabulary", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("$words.size words".replace("WordEntry", "words") + " · $learned learned", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size },
                        modifier = Modifier.fillMaxWidth(), color = BurgundyLight, trackColor = SurfaceElevated
                    )
                }
                Text(if (words.isEmpty()) "0%" else "${learned * 100 / words.size}%", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Upcoming Tasks", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenProgress) { Text("See All →", color = BurgundyLight) }
        }
        if (pending.isEmpty()) {
            DayflowCard { Text("No pending tasks. Your schedule is clear.", color = TextSecondary) }
        } else {
            pending.take(3).forEach { task ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = BurgundyLight)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(task.time.ifBlank { "Today" }, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("Open", color = BurgundyLight, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLine(color: Color, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).then(Modifier), contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxSize(), shape = CircleShape, color = color) {}
        }
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
