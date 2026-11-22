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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val PRIVATE_ROOM_PASSWORD = "13225"
private const val VAULT_FOLDER = "Download/DAYFLOW Private/"
private const val VAULT_MIME = "application/octet-stream"

private data class VaultEntry(val uri: android.net.Uri, val name: String, val size: Long)

private fun deriveVaultKey(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, 180_000, 256)
    val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    spec.clearPassword()
    return SecretKeySpec(key, "AES")
}

private fun encryptVaultFile(input: InputStream, output: java.io.OutputStream, password: String) {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveVaultKey(password, salt), GCMParameterSpec(128, iv))
    output.write(byteArrayOf(68, 70, 86, 49))
    output.write(salt)
    output.write(iv)
    javax.crypto.CipherOutputStream(output, cipher).use { encrypted -> input.use { it.copyTo(encrypted) } }
}

private fun decryptVaultFile(input: InputStream, output: java.io.OutputStream, password: String) {
    val header = ByteArray(32)
    var offset = 0
    while (offset < header.size) {
        val count = input.read(header, offset, header.size - offset)
        if (count < 0) throw java.io.IOException("Incomplete encrypted file")
        offset += count
    }
    require(header.copyOfRange(0, 4).contentEquals(byteArrayOf(68, 70, 86, 49))) { "Invalid vault file" }
    val salt = header.copyOfRange(4, 20)
    val iv = header.copyOfRange(20, 32)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, deriveVaultKey(password, salt), GCMParameterSpec(128, iv))
    javax.crypto.CipherInputStream(input, cipher).use { decrypted -> output.use { decrypted.copyTo(it) } }
}

@Composable
fun PrivateRoomScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
    val resolver = context.contentResolver
    val vaultCollection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

    fun loadEntries(): List<VaultEntry> {
        val result = mutableListOf<VaultEntry>()
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
        resolver.query(vaultCollection, projection, "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(VAULT_FOLDER), "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                if (!name.endsWith(".dfvault")) continue
                val id = cursor.getLong(idCol)
                result += VaultEntry(android.content.ContentUris.withAppendedId(vaultCollection, id), name.removeSuffix(".dfvault"), cursor.getLong(sizeCol))
            }
        }
        return result
    }

    val files = remember(refresh) { loadEntries() }

    val deleteOriginals = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        message = if (result.resultCode == Activity.RESULT_OK) "Encrypted safely. Original media removed from Gallery." else "Encrypted copy saved. Original remains in Gallery."
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val savedUris = mutableListOf<android.net.Uri>()
        val mediaStoreUris = mutableListOf<android.net.Uri>()
        var failed = 0
        uris.forEach { sourceUri ->
            runCatching {
                val originalName = resolver.query(sourceUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: "private_${System.currentTimeMillis()}.bin"
                val safeName = originalName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "private.bin" }
                val source = resolver.openInputStream(sourceUri) ?: error("Cannot read selected file")
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}_$safeName.dfvault")
                    put(MediaStore.MediaColumns.MIME_TYPE, VAULT_MIME)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, VAULT_FOLDER)
                }
                val target = resolver.insert(vaultCollection, values) ?: error("Cannot create encrypted vault item")
                val targetStream = resolver.openOutputStream(target) ?: error("Cannot save encrypted vault item")\n                encryptVaultFile(source, targetStream, PRIVATE_ROOM_PASSWORD)
                savedUris += sourceUri
                if (sourceUri.authority == "media") mediaStoreUris += sourceUri
            }.onFailure { failed++ }
        }
        refresh++
        if (mediaStoreUris.isNotEmpty() && Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val request = MediaStore.createDeleteRequest(resolver, mediaStoreUris)
                deleteOriginals.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { message = "Encrypted copy saved. Android could not request removal of the original; remove it manually from Gallery if desired." }
        } else if (savedUris.isNotEmpty()) {
            message = "Encrypted copy saved. Android did not provide a deletable Gallery item for this picker selection; remove the original manually if you want it hidden."
        } else message = "Could not encrypt selected media."
        if (failed > 0) message = "${message ?: ""} ($failed file(s) failed.)"
    }

    fun restoreToGallery(entry: VaultEntry) {
        runCatching {
            val encryptedInput = resolver.openInputStream(entry.uri) ?: error("Cannot read encrypted file")
            val originalName = entry.name.substringAfter('_', entry.name).ifBlank { "restored_media" }
            val isVideo = originalName.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "3gp", "webm", "mov")
            val collection = if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val mime = java.net.URLConnection.guessContentTypeFromName(originalName) ?: if (isVideo) "video/mp4" else "image/jpeg"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) Environment.DIRECTORY_MOVIES + "/DAYFLOW Restored" else Environment.DIRECTORY_PICTURES + "/DAYFLOW Restored")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val outputUri = resolver.insert(collection, values) ?: error("Could not create gallery item")
            val restoredOutput = resolver.openOutputStream(outputUri) ?: error("Could not write restored file")\n            decryptVaultFile(encryptedInput, restoredOutput, PRIVATE_ROOM_PASSWORD)
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.update(outputUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
            resolver.delete(entry.uri, null, null)
            refresh++
            message = "Decrypted and restored to Gallery."
        }.onFailure {
            message = if (it is javax.crypto.AEADBadTagException) "Wrong password or damaged encrypted file." else "Restore failed: ${it.localizedMessage ?: "unknown error"}"
        }
    }

    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Private Room", "AES-256 encrypted media • survives app uninstall")
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
                Text("Encrypted files", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { unlocked = false; password = "" }) { Icon(Icons.Default.Lock, null); Text("Lock") }
            }
            Text("Files are encrypted using your Private Room password before being stored in Downloads/DAYFLOW Private. Android asks before deleting originals from Gallery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AccentButton("Encrypt photos or videos") { picker.launch(arrayOf("image/*", "video/*")) }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            if (files.isEmpty()) DayflowCard { Text("No encrypted files yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(files, key = { it.uri.toString() }) { entry ->
                    DayflowCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (entry.name.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "3gp", "webm", "mov")) Icons.Default.Videocam else Icons.Default.EnhancedEncryption, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(entry.name.substringAfter('_', entry.name), maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(android.text.format.Formatter.formatFileSize(context, entry.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { restoreToGallery(entry) }) { Icon(Icons.Default.Restore, "Decrypt and restore to Gallery") }
                        }
                    }
                }
            }
        }
    }
}
