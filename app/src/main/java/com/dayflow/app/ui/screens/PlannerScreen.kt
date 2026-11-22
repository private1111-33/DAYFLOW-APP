package com.dayflow.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.DayTask
import com.dayflow.app.data.LocalStore
import com.dayflow.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlannerScreen() {
    val context = LocalContext.current
    val tasks = remember { mutableStateListOf<DayTask>().apply { addAll(LocalStore.tasks(context)) } }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var pendingDelete by remember { mutableStateOf<DayTask?>(null) }
    var filter by remember { mutableStateOf("All") }
    val selectedEpoch = selected.toEpochDay()
    val dayTasks = tasks.filter { it.date == selectedEpoch }
    val done = dayTasks.count { it.done }
    val visibleTasks = dayTasks.filter { when(filter) { "Pending" -> !it.done; "Completed" -> it.done; else -> true } }
    val today = LocalDate.now()

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tasks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), color = TextSecondary)
            }
            IconButton(onClick = { filter = "All" }) { Icon(Icons.Rounded.Search, "Search tasks", tint = Burgundy) }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Burgundy), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$done / ${dayTasks.size}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Tasks Completed", color = Color.White.copy(alpha = .85f))
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { if(dayTasks.isEmpty()) 0f else done.toFloat()/dayTasks.size }, color = Color.White, trackColor = Color.White.copy(alpha=.22f), strokeWidth = 6.dp, modifier = Modifier.size(54.dp))
                        Text("${if(dayTasks.isEmpty()) 0 else done*100/dayTasks.size}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Button(onClick = { editingTask = null; title = ""; time = ""; showAdd = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Task", fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Pending", "Completed").forEach { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Burgundy, selectedLabelColor = Color.White))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Select day", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected = selected.minusDays(1) }) { Icon(Icons.Rounded.ChevronLeft, null) }
                Text(if(selected == today) "Today" else selected.format(DateTimeFormatter.ofPattern("MMM d")), color = Burgundy, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { selected = selected.plusDays(1) }) { Icon(Icons.Rounded.ChevronRight, null) }
            }
        }
        if (visibleTasks.isEmpty()) {
            DayflowCard {
                Icon(Icons.Rounded.AssignmentTurnedIn, null, tint = Burgundy, modifier = Modifier.size(34.dp))
                Text(if(dayTasks.isEmpty()) "Nothing planned yet" else "No tasks here", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Add a task to organize your day.", color = TextSecondary)
            }
        } else visibleTasks.sortedWith(compareBy<DayTask>{it.done}.thenBy{it.time}).forEach { task ->
            DayflowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.done, onCheckedChange = { checked ->
                        val i = tasks.indexOfFirst { it.id == task.id }; if(i >= 0) tasks[i] = task.copy(done = checked)
                        LocalStore.saveTasks(context, tasks.toList())
                    }, colors = CheckboxDefaults.colors(checkedColor = Burgundy))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = if(task.done) TextSecondary else TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(task.time.ifBlank { "Any time" }, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { editingTask = task; title = task.title; time = task.time; showAdd = true }) { Icon(Icons.Rounded.Edit, "Edit", tint = Burgundy) }
                    IconButton(onClick = { pendingDelete = task }) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = TextSecondary) }
                }
            }
        }
    }
    if(showAdd) AlertDialog(onDismissRequest = { showAdd = false }, title = { Text(if(editingTask == null) "New task" else "Edit task") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") }, singleLine = true)
            OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> time = String.format(Locale.ENGLISH,"%02d:%02d",h,m) },9,0,false).show() }, modifier = Modifier.fillMaxWidth()) { Text(time.ifBlank { "Choose time (optional)" }) }
        }
    }, confirmButton = { TextButton(onClick = { if(title.isNotBlank()) { val old=editingTask; if(old==null) tasks.add(DayTask(System.currentTimeMillis(),title.trim(),time.trim(),false,selectedEpoch)) else { val i=tasks.indexOfFirst{it.id==old.id}; if(i>=0) tasks[i]=old.copy(title=title.trim(),time=time.trim()) }; LocalStore.saveTasks(context,tasks.toList()); showAdd=false; title=""; time=""; editingTask=null } }) { Text("Save",color=Burgundy) } }, dismissButton = { TextButton(onClick = { showAdd=false; editingTask=null }) { Text("Cancel") } })
    pendingDelete?.let { task -> ConfirmDialog(title="Delete task?",message=task.title,onConfirm={tasks.removeAll{it.id==task.id};LocalStore.saveTasks(context,tasks.toList());pendingDelete=null},onDismiss={pendingDelete=null}) }
}
