package com.dayflow.app.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.DayAlarm
import java.util.Locale

@Composable
fun AlarmsScreen() {
    val context = LocalContext.current
    val alarms = remember { mutableStateListOf<DayAlarm>().apply { addAll(AlarmStore.all(context)) } }
    var label by remember { mutableStateOf("") }
    var pickedHour by remember { mutableIntStateOf(8) }
    var pickedMinute by remember { mutableIntStateOf(30) }
    var showAdd by remember { mutableStateOf(false) }

    fun persist() { AlarmStore.save(context, alarms.toList()) }
    fun permissionMissing(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()

    ScreenColumn {
        SectionTitle("Alarms", "Stay on time")
        DayflowCard {
            Text("ACTIVE ALARMS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${alarms.count { it.enabled }}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Alarms are saved on this device and repeat daily.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (permissionMissing()) {
            DayflowCard {
                Text("Allow exact alarms", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Android needs this permission to trigger alarms at the selected time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }) { Text("Grant permission") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your alarms", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null); Text(" Add") }
        }
        if (alarms.isEmpty()) {
            DayflowCard {
                Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                Text("No alarms yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Create your first daily reminder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            alarms.forEach { alarm ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(alarm.label.ifBlank { "Daily alarm" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (alarm.enabled) "Repeats every day" else "Disabled",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(checked = alarm.enabled, onCheckedChange = { enabled ->
                            AlarmScheduler.cancel(context, alarm)
                            val updated = alarm.copy(enabled = enabled)
                            val index = alarms.indexOfFirst { it.id == alarm.id }
                            if (index >= 0) alarms[index] = updated
                            persist()
                            if (enabled && !permissionMissing()) AlarmScheduler.schedule(context, updated)
                        })
                        IconButton(onClick = {
                            AlarmScheduler.cancel(context, alarm)
                            alarms.removeAll { it.id == alarm.id }
                            persist()
                        }) { Icon(Icons.Default.Delete, "Delete alarm") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New daily alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(12))) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Alarm label") }, singleLine = true)
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, h, m -> pickedHour = h; pickedMinute = m }, pickedHour, pickedMinute, false).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(String.format(Locale.getDefault(), "%02d:%02d", pickedHour, pickedMinute))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val alarm = DayAlarm((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), pickedHour, pickedMinute, label.trim().ifBlank { "Daily alarm" })
                    alarms.add(alarm)
                    persist()
                    if (!permissionMissing()) AlarmScheduler.schedule(context, alarm)
                    label = ""
                    showAdd = false
                }) { Text("Save alarm") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
