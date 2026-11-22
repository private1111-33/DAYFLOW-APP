package com.dayflow.app.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.DayAlarm
import com.dayflow.app.service.AlarmRingService
import com.dayflow.app.ui.theme.SurfaceElevated
import java.util.Locale

@Composable
fun AlarmsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alarms = remember { mutableStateListOf<DayAlarm>().apply { addAll(AlarmStore.all(context)) } }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DayAlarm?>(null) }
    var pendingDelete by remember { mutableStateOf<DayAlarm?>(null) }
    var permissionMissing by remember { mutableStateOf(!AlarmScheduler.canScheduleExact(context)) }

    // Re-check the permission when the user comes back from system settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionMissing = !AlarmScheduler.canScheduleExact(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persist() { AlarmStore.save(context, alarms.toList()) }

    ScreenColumn {
        SectionTitle("Alarms", "Stay on time")
        DayflowCard {
            Text("ACTIVE ALARMS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${alarms.count { it.enabled }}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Alarms are saved on this device. Tap an alarm to edit its time or days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingService::class.java)
                        .putExtra(AlarmScheduler.EXTRA_ID, 0)
                        .putExtra(AlarmScheduler.EXTRA_LABEL, "Test alarm")
                        .putExtra(AlarmRingService.EXTRA_DURATION_MS, 8_000L)
                )
            }) { Text("Test alarm sound") }
            Text("If you hear nothing, raise the Alarm volume in your phone's sound settings.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (permissionMissing) {
            DayflowCard {
                Text("Allow exact alarms", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Without this permission Android may delay alarms by several minutes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                    }
                }) { Text("Grant permission") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your alarms", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, null); Text(" Add") }
        }
        if (alarms.isEmpty()) {
            DayflowCard {
                Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                Text("No alarms yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Create your first daily reminder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            alarms.forEach { alarm ->
                DayflowCard(onClick = { editing = alarm; showDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(alarm.label.ifBlank { "Daily alarm" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (alarm.enabled) AlarmScheduler.daysSummary(alarm.days) else "Disabled",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(checked = alarm.enabled, onCheckedChange = { enabled ->
                            AlarmScheduler.cancel(context, alarm)
                            val updated = alarm.copy(enabled = enabled)
                            val index = alarms.indexOfFirst { it.id == alarm.id }
                            if (index >= 0) alarms[index] = updated
                            persist()
                            if (enabled) AlarmScheduler.schedule(context, updated)
                        })
                        IconButton(onClick = { pendingDelete = alarm }) { Icon(Icons.Default.Delete, "Delete alarm") }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlarmEditorDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { hour, minute, label, days ->
                val old = editing
                if (old == null) {
                    val alarm = DayAlarm((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), hour, minute, label, true, days)
                    alarms.add(alarm)
                    AlarmScheduler.schedule(context, alarm)
                } else {
                    AlarmScheduler.cancel(context, old)
                    val updated = old.copy(hour = hour, minute = minute, label = label, days = days)
                    val index = alarms.indexOfFirst { it.id == old.id }
                    if (index >= 0) alarms[index] = updated
                    if (updated.enabled) AlarmScheduler.schedule(context, updated)
                }
                persist()
                showDialog = false
            }
        )
    }

    pendingDelete?.let { alarm ->
        ConfirmDialog(
            title = "Delete alarm?",
            message = "${String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)} · ${alarm.label.ifBlank { "Daily alarm" }}",
            onConfirm = {
                AlarmScheduler.cancel(context, alarm)
                alarms.removeAll { it.id == alarm.id }
                persist()
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun AlarmEditorDialog(
    initial: DayAlarm?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, days: Int) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var hour by remember { mutableIntStateOf(initial?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(initial?.minute ?: 30) }
    var days by remember { mutableIntStateOf(initial?.days ?: DayAlarm.ALL_DAYS) }
    val letters = listOf("S", "M", "T", "W", "T", "F", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New alarm" else "Edit alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Alarm label") }, singleLine = true)
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute), style = MaterialTheme.typography.titleLarge)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    letters.forEachIndexed { i, letter ->
                        val on = days and (1 shl i) != 0
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primary else SurfaceElevated)
                                .clickable {
                                    val next = days xor (1 shl i)
                                    if (next != 0) days = next // keep at least one day selected
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letter, fontWeight = FontWeight.SemiBold,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(AlarmScheduler.daysSummary(days), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(hour, minute, label.trim().ifBlank { "Daily alarm" }, days) }) { Text("Save alarm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
