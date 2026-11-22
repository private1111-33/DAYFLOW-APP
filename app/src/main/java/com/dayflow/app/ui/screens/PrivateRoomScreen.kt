package com.dayflow.app.ui.screens

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    // Shared Pictures directory survives app uninstall. .nomedia keeps its contents out of gallery apps.
    val folder = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DAYFLOW Private").apply {
            mkdirs()
            File(this, ".nomedia").takeIf { !it.exists() }?.createNewFile()
        }
    }
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
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val files = remember(refresh) { folder.listFiles()?.filter { it.isFile && it.name != ".nomedia" }?.sortedByDescending { it.lastModified() }.orEmpty() }

    val deleteOriginals = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        message = if (result.resultCode == Activity.RESULT_OK) "Original item moved out of the gallery. A copy is safely stored in Private Room." else "Private copy saved, but the original was not removed from the gallery."
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        var copied = 0
        uris.forEach { uri ->
            runCatching {
                val original = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: ("private_" + System.currentTimeMillis())
                val safeName = original.replace(Regex("[^A-Za-z0-9._-]"), "_")
                var destination = File(folder, safeName)
                if (destination.exists()) destination = File(folder, System.currentTimeMillis().toString() + "_" + safeName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                } ?: error("Unable to read selected file")
                copied++
            }
        }
        refresh++
        if (copied > 0 && Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
                deleteOriginals.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { message = "Private copy saved. Android did not allow automatic removal of the original." }
        } else if (copied > 0) {
            message = "Private copy saved. On this Android version, remove the original from Gallery manually if you want it hidden."
        } else message = "Could not copy the selected media."
    }

    fun restoreToGallery(file: File) {
        runCatching {
            val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "3gp", "webm", "mov")
            val collection = if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val mime = java.net.URLConnection.guessContentTypeFromName(file.name) ?: if (isVideo) "video/mp4" else "image/jpeg"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) Environment.DIRECTORY_MOVIES + "/DAYFLOW Restored" else Environment.DIRECTORY_PICTURES + "/DAYFLOW Restored")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: error("Could not create gallery item")
            context.contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } } ?: error("Could not write restored file")
            if (Build.VERSION.SDK_INT >= 29) {
                val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                context.contentResolver.update(uri, ready, null, null)
            }
            file.delete()
            refresh++
            message = "Restored to Gallery."
        }.onFailure { message = "Restore failed: ${it.localizedMessage ?: "unknown error"}" }
    }

    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Private Room", "Media stored outside the app so it remains after uninstall")
        if (!unlocked) {
            DayflowCard {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                Text("Enter password", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(value = password, onValueChange = { password = it; error = false }, label = { Text("Password") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                if (error) Text("Incorrect password", color = MaterialTheme.colorScheme.error)
                AccentButton("Unlock") { if (password == PRIVATE_ROOM_PASSWORD) unlocked = true else error = true }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Private files", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { unlocked = false; password = "" }) { Icon(Icons.Default.Lock, null); Text("Lock") }
            }
            Text("Add copies to the hidden DAYFLOW Private folder. Android will ask before removing the originals from Gallery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AccentButton("Add photos or videos") { picker.launch(arrayOf("image/*", "video/*")) }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            if (files.isEmpty()) DayflowCard { Text("No private files yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    DayflowCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (file.extension.lowercase() in listOf("mp4", "mkv", "3gp", "webm", "mov")) Icons.Default.Videocam else Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(file.name, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(android.text.format.Formatter.formatFileSize(context, file.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                runCatching {
                                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                    val mime = java.net.URLConnection.guessContentTypeFromName(file.name) ?: "*/*"
                                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                }
                            }) { Icon(Icons.Default.Visibility, "View") }
                            IconButton(onClick = { restoreToGallery(file) }) { Icon(Icons.Default.Restore, "Restore to Gallery") }
                        }
                    }
                }
            }
        }
    }
}
