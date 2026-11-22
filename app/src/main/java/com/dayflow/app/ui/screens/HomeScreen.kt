package com.dayflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenMusic: () -> Unit
) {
    val context = LocalContext.current
    val today = LocalDate.now().toEpochDay()
    val tasks = remember { LocalStore.tasks(context).filter { it.date == today } }
    val words = remember { LocalStore.words(context) }
    val alarms = remember { AlarmStore.all(context).filter { it.enabled } }
    val completed = tasks.count { it.done }
    val pending = tasks.filter { !it.done }.sortedBy { it.time }
    val learned = words.count { it.learned }
    val percent = if (tasks.isEmpty()) 0 else completed * 100 / tasks.size
    val nextAlarm = alarms.minByOrNull { AlarmScheduler.minutesUntil(it) }
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning,"
        in 12..17 -> "Good Afternoon,"
        else -> "Good Evening,"
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, tint = Burgundy, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DAYFLOW", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your Personal Daily System", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextPrimary) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(greeting, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Let's make today productive.", color = TextSecondary)
            Text(SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(Date()), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }

        DayflowCard(onClick = onOpenProgress) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 9.dp,
                        color = Burgundy,
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

        Text("Quick Access", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Tasks", Icons.Default.TaskAlt, onOpenPlanner, Modifier.weight(1f))
            QuickAction("Music", Icons.Default.MusicNote, onOpenMusic, Modifier.weight(1f))
            QuickAction("Alarm", Icons.Default.Alarm, onOpenAlarms, Modifier.weight(1f))
        }

        DayflowCard(onClick = onOpenAlarms) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = Burgundy, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Next Alarm", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        nextAlarm?.let { String.format(Locale.ENGLISH, "%d:%02d %s", if (it.hour % 12 == 0) 12 else it.hour % 12, it.minute, if (it.hour < 12) "AM" else "PM") } ?: "No alarm set",
                        color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold
                    )
                    Text(nextAlarm?.label?.ifBlank { "Daily reminder" } ?: "Create a reminder in Alarms", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
            }
        }

        DayflowCard(onClick = onOpenVocabulary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = Burgundy, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Vocabulary", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${words.size} words · $learned learned", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size },
                        modifier = Modifier.fillMaxWidth(), color = Burgundy, trackColor = SurfaceElevated
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(if (words.isEmpty()) "0%" else "${learned * 100 / words.size}%", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Upcoming Tasks", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenPlanner) { Text("See All  →", color = Burgundy) }
        }
        if (pending.isEmpty()) {
            DayflowCard { Text("No pending tasks. Your schedule is clear.", color = TextSecondary) }
        } else {
            pending.take(3).forEach { task ->
                DayflowCard(onClick = onOpenPlanner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = Burgundy)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(task.time.ifBlank { "Today" }, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("Open", color = Burgundy, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(SurfaceElevated), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Burgundy, modifier = Modifier.size(23.dp))
            }
            Text(title, color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetricLine(color: Color, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
