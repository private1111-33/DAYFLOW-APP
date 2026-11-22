package com.dayflow.app.ui.screens

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val PRIVATE_ROOM_PASSWORD = "13225"

@Composable
fun PrivateRoomScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val folder = remember { File(context.filesDir, "private_room").apply { mkdirs() } }
    var unlocked by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val app = context.applicationContext as Application
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) { if (activity === context) unlocked = false }
            override fun onActivityCreated(activity: Activity, state: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        app.registerActivityLifecycleCallbacks(callbacks)
        onDispose { app.unregisterActivityLifecycleCallbacks(callbacks) }
    }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val files = remember(refresh) { folder.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching {
            val original = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: ("private_" + System.currentTimeMillis())
            val safeName = original.replace(Regex("[^A-Za-z0-9._-]"), "_")
            var destination = File(folder, safeName)
            if (destination.exists()) destination = File(folder, System.currentTimeMillis().toString() + "_" + safeName)
            context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } } ?: error("Unable to read selected file")
        } }
        refresh++
    }
    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Private Room", "Your private media space")
        if (!unlocked) {
            DayflowCard {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                Text("Enter password", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(value = password, onValueChange = { password = it; error = false }, label = { Text("Password") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                if (error) Text("Incorrect password", color = MaterialTheme.colorScheme.error)
                AccentButton("Unlock") { if (password == PRIVATE_ROOM_PASSWORD) unlocked = true else error = true }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("Private files", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); TextButton(onClick = { unlocked = false; password = "" }) { Icon(Icons.Default.Lock, null); Text("Lock") } }
            AccentButton("Add photos or videos") { picker.launch(arrayOf("image/*", "video/*")) }
            if (files.isEmpty()) DayflowCard { Text("No private files yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    DayflowCard { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (file.extension.lowercase() in listOf("mp4", "mkv", "3gp", "webm")) Icons.Default.Videocam else Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(file.name, maxLines = 1, color = MaterialTheme.colorScheme.onSurface); Text(android.text.format.Formatter.formatFileSize(context, file.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { runCatching { val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file); val mime = if (file.extension.lowercase() in listOf("mp4", "mkv", "3gp", "webm")) "video/*" else "image/*"; context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }) { Icon(Icons.Default.Visibility, "View") }
                        IconButton(onClick = { file.delete(); refresh++ }) { Icon(Icons.Default.Delete, "Delete") }
                    } }
                }
            }
        }
    }
}