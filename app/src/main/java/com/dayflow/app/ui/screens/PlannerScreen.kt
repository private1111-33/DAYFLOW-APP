package com.dayflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.DayTask
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasks = remember { mutableStateListOf<DayTask>().apply { addAll(LocalStore.tasks(context)) } }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val done = tasks.count { it.done }
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val days = month.lengthOfMonth()

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Previous month", tint = TextPrimary) }
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Next month", tint = TextPrimary) }
                TextButton(onClick = { month = YearMonth.now(); selected = LocalDate.now() }) { Text("Today") }
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach {
                    Text(it, color = TextSecondary, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            for (week in 0 until 6) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    for (weekday in 0 until 7) {
                        val day = week * 7 + weekday - firstDayOffset + 1
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val active = date == selected
                            val today = date == LocalDate.now()
                            Box(Modifier.weight(1f).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (active) Burgundy else androidx.compose.ui.graphics.Color.Transparent)
                                        .then(if (today && !active) Modifier.background(SurfaceElevated, CircleShape) else Modifier)
                                        .clickable { selected = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(day.toString(), color = if (active) TextPrimary else TextPrimary,
                                        fontWeight = if (today || active) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).height(44.dp))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(selected.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)),
                    color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Daily schedule", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, null, tint = BurgundyLight)
                Text("Add task", color = BurgundyLight)
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today's progress", color = TextPrimary)
                Text("$done / ${tasks.size}", color = BurgundyLight)
            }
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth(), color = BurgundyLight, trackColor = SurfaceElevated
            )
        }
        if (tasks.isEmpty()) {
            DayflowCard {
                Text("Nothing planned yet", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("Add a task to build your daily schedule.", color = TextSecondary)
            }
        } else {
            tasks.sortedWith(compareBy<DayTask> { it.done }.thenBy { it.time }).forEach { task ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.done, onCheckedChange = { checked ->
                            val i = tasks.indexOfFirst { it.id == task.id }
                            if (i >= 0) tasks[i] = task.copy(done = checked)
                            LocalStore.saveTasks(context, tasks.toList())
                        }, colors = CheckboxDefaults.colors(checkedColor = BurgundyLight))
                        Column(Modifier.weight(1f)) {
                            Text(task.time.ifBlank { "Any time" }, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(task.title, color = if (task.done) TextSecondary else TextPrimary)
                        }
                        IconButton(onClick = {
                            tasks.removeAll { it.id == task.id }
                            LocalStore.saveTasks(context, tasks.toList())
                        }) { Icon(Icons.Default.Delete, "Delete task", tint = TextSecondary) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") }, singleLine = true)
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        tasks.add(DayTask(System.currentTimeMillis(), title.trim(), time.trim()))
                        LocalStore.saveTasks(context, tasks.toList())
                        title = ""; time = ""; showAdd = false
                    }
                }) { Text("Save", color = BurgundyLight) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
