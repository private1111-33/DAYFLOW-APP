package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.DayTask
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.BurgundyLight
import com.dayflow.app.ui.theme.TextPrimary
import com.dayflow.app.ui.theme.TextSecondary

@Composable
fun PlannerScreen() {
    val context = LocalContext.current
    val tasks = remember { mutableStateListOf<DayTask>().apply { addAll(LocalStore.tasks(context)) } }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    val completed = tasks.count { it.done }

    ScreenColumn {
        SectionTitle("Planner", "Your schedule, in one place")
        DayflowCard {
            Text("TODAY", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date()),
                color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth(), color = BurgundyLight
            )
            Text("$completed of ${tasks.size} tasks completed", color = TextSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Tasks", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add task")
            }
        }
        if (tasks.isEmpty()) {
            DayflowCard {
                Text("Your day is clear", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("Add your first task to start planning.", color = TextSecondary)
            }
        } else {
            tasks.sortedWith(compareBy<DayTask> { it.done }.thenBy { it.time }).forEach { task ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { checked ->
                                val index = tasks.indexOfFirst { it.id == task.id }
                                if (index >= 0) tasks[index] = task.copy(done = checked)
                                LocalStore.saveTasks(context, tasks.toList())
                            },
                            colors = CheckboxDefaults.colors(checkedColor = BurgundyLight)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = if (task.done) TextSecondary else TextPrimary,
                                style = MaterialTheme.typography.titleMedium)
                            if (task.time.isNotBlank()) Text(task.time, color = TextSecondary)
                        }
                        IconButton(onClick = {
                            tasks.removeAll { it.id == task.id }
                            LocalStore.saveTasks(context, tasks.toList())
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = TextSecondary) }
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it },
                        label = { Text("Task name") }, singleLine = true)
                    OutlinedTextField(value = time, onValueChange = { time = it },
                        label = { Text("Time (optional)") }, singleLine = true,
                        placeholder = { Text("e.g. 10:30 AM") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        tasks.add(DayTask(System.currentTimeMillis(), title.trim(), time.trim()))
                        LocalStore.saveTasks(context, tasks.toList())
                        title = ""; time = ""; showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }
}
