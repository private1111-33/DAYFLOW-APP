package com.dayflow.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.dayflow.app.data.AlarmScheduler
import com.dayflow.app.data.AlarmStore
import com.dayflow.app.data.DayAlarm
import com.dayflow.app.data.LocalStore
import com.dayflow.app.data.MusicStore
import com.dayflow.app.data.MusicTrack
import com.dayflow.app.data.DayTask
import com.dayflow.app.data.WordEntry
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var pendingBackup by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingBackup) }
                    ?: error("Unable to open the selected file")
            }.onSuccess { statusMessage = "Backup saved successfully." }
                .onFailure { statusMessage = "Could not save backup: ${it.localizedMessage}" }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read the selected file")
                val root = JSONObject(raw)
                require(root.optInt("schema_version") == 1) { "Unsupported backup version" }

                val tasksJson = root.optJSONArray("tasks") ?: JSONArray()
                val tasks = (0 until tasksJson.length()).map { i ->
                    val item = tasksJson.getJSONObject(i)
                    DayTask(item.getLong("id"), item.getString("title"), item.optString("time"), item.optBoolean("done"),
                        item.optLong("date", java.time.LocalDate.now().toEpochDay()))
                }
                val wordsJson = root.optJSONArray("words") ?: JSONArray()
                val words = (0 until wordsJson.length()).map { i ->
                    val item = wordsJson.getJSONObject(i)
                    WordEntry(item.getLong("id"), item.getString("word"), item.optString("meaning"),
                        item.optString("example"), item.optBoolean("learned"))
                }
                val alarmsJson = root.optJSONArray("alarms") ?: JSONArray()
                val alarms = (0 until alarmsJson.length()).map { i ->
                    val item = alarmsJson.getJSONObject(i)
                    DayAlarm(item.getInt("id"), item.getInt("hour"), item.getInt("minute"),
                        item.optString("label"), item.optBoolean("enabled", true))
                }
                val musicJson = root.optJSONArray("music_tracks") ?: JSONArray()
                val tracks = (0 until musicJson.length()).map { i ->
                    when (val entry = musicJson.get(i)) {
                        is JSONObject -> MusicTrack(entry.getString("uri"), entry.optString("name"))
                        else -> MusicTrack(entry.toString(), "")
                    }
                }

                AlarmStore.all(context).forEach { AlarmScheduler.cancel(context, it) }
                LocalStore.saveTasks(context, tasks)
                LocalStore.saveWords(context, words)
                AlarmStore.save(context, alarms)
                AlarmScheduler.rescheduleAll(context)
                MusicStore.save(context, tracks)
            }.onSuccess { statusMessage = "Backup restored. Reopen the app sections to refresh their data." }
                .onFailure { statusMessage = "Could not restore backup: ${it.localizedMessage}" }
        }
    }

    fun buildBackup(): String {
        val tasks = JSONArray().apply {
            LocalStore.tasks(context).forEach { task ->
                put(JSONObject().put("id", task.id).put("title", task.title)
                    .put("time", task.time).put("done", task.done).put("date", task.date))
            }
        }
        val words = JSONArray().apply {
            LocalStore.words(context).forEach { word ->
                put(JSONObject().put("id", word.id).put("word", word.word)
                    .put("meaning", word.meaning).put("example", word.example).put("learned", word.learned))
            }
        }
        val alarms = JSONArray().apply {
            AlarmStore.all(context).forEach { alarm ->
                put(JSONObject().put("id", alarm.id).put("hour", alarm.hour)
                    .put("minute", alarm.minute).put("label", alarm.label).put("enabled", alarm.enabled))
            }
        }
        val tracks = JSONArray().apply {
            MusicStore.load(context).forEach { put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        }
        return JSONObject().put("schema_version", 1).put("app", "DAYFLOW")
            .put("exported_at", System.currentTimeMillis())
            .put("tasks", tasks).put("words", words).put("alarms", alarms)
            .put("music_tracks", tracks).toString(2)
    }

    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Settings", "Learn. Improve. Repeat.")
        DayflowCard {
            Text("Dayflow User", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("Learn. Improve. Repeat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Learning Preferences", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Your vocabulary and daily learning activity are stored on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Reminders & alarms", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Alarms with repeat days, exact scheduling and 10-minute snooze.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            val notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val exactOn = AlarmScheduler.canScheduleExact(context)
            Text("Notifications: " + if (notificationsOn) "allowed" else "blocked",
                color = if (notificationsOn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            Text("Exact alarms: " + if (exactOn) "allowed" else "not allowed",
                color = if (exactOn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            }) { Text("Notification settings") }
        }
        DayflowCard {
            Text("Offline data", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Export or restore your tasks, vocabulary, alarms and music library references.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AccentButton("Export backup") {
                pendingBackup = buildBackup()
                exportLauncher.launch("DAYFLOW-backup.json")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }) {
                Text("Restore backup")
            }
        }
        statusMessage?.let { message ->
            DayflowCard {
                Text(message, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { statusMessage = null }) { Text("Dismiss") }
            }
        }
        PlaceholderNote("Backups are stored wherever you choose. Keep a copy somewhere safe.")
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        }
        PlaceholderNote("DAYFLOW $version · All data stays on this device. No account, ads or tracking.")
    }
}
