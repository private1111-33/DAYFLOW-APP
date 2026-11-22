package com.dayflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.*
import java.time.LocalDate
import java.text.SimpleDateFormat
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
    val words = remember { LocalStore.words(context) }
    val tasks = remember { LocalStore.tasks(context).filter { it.date == LocalDate.now().toEpochDay() } }
    val learned = words.count { it.learned }
    val accuracy = if (words.isEmpty()) 0 else learned * 100 / words.size
    val alarms = remember { AlarmStore.all(context).filter { it.enabled } }
    val nextAlarm = alarms.minByOrNull { AlarmScheduler.minutesUntil(it) }
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning!"
        in 12..17 -> "Good Afternoon!"
        else -> "Good Evening!"
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(greeting, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Keep going, you're doing great!", color = TextSecondary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = Burgundy)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Burgundy)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 19.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stat(Icons.Default.MenuBook, words.size.toString(), "Words")
                Stat(Icons.Default.LocalFireDepartment, tasks.count { it.done }.toString(), "Tasks Done")
                Stat(Icons.Default.EmojiEvents, "$accuracy%", "Accuracy")
            }
        }

        Button(
            onClick = onOpenVocabulary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Continue Learning", fontWeight = FontWeight.SemiBold)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Access", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenProgress) { Text("See All  ›", color = Burgundy) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Word List", Icons.Default.MenuBook, onOpenVocabulary, Modifier.weight(1f))
                QuickAction("Quizzes", Icons.Default.SportsEsports, onOpenVocabulary, Modifier.weight(1f))
                QuickAction("Progress", Icons.Default.BarChart, onOpenProgress, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Tasks", Icons.Default.AssignmentTurnedIn, onOpenPlanner, Modifier.weight(1f))
                QuickAction("Music", Icons.Default.MusicNote, onOpenMusic, Modifier.weight(1f))
                QuickAction("Alarm", Icons.Default.Notifications, onOpenAlarms, Modifier.weight(1f))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("You're making progress!", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("$learned words learned", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Burgundy)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Daily Reminder", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenAlarms) { Text("Manage", color = Burgundy) }
        }
        DayflowCard(onClick = onOpenAlarms) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = Burgundy, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        nextAlarm?.let { String.format(Locale.ENGLISH, "%d:%02d %s", if (it.hour % 12 == 0) 12 else it.hour % 12, it.minute, if (it.hour < 12) "AM" else "PM") } ?: "No alarm set",
                        color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                    )
                    Text(nextAlarm?.label?.ifBlank { "Daily reminder" } ?: "Add an alarm to stay on track", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
            }
        }
        Text(SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(Date()), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(27.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun QuickAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceElevated), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Burgundy, modifier = Modifier.size(23.dp))
            }
            Text(title, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
