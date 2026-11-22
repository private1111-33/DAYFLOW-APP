package com.dayflow.app.ui.screens
import androidx.compose.material.icons.filled.Search

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import com.dayflow.app.data.ExcelVocabularyImporter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dayflow.app.data.LocalStore
import com.dayflow.app.data.WordEntry
import com.dayflow.app.ui.theme.*
import java.util.Locale

@Composable
fun VocabularyScreen() {
    val context = LocalContext.current
    val words = remember { mutableStateListOf<WordEntry>().apply { addAll(LocalStore.words(context)) } }
    var showAdd by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<WordEntry?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    fun save() = LocalStore.saveWords(context, words.toList())
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { ExcelVocabularyImporter.read(context, uri) }
            .onSuccess { imported ->
                val existing = words.map { it.word.lowercase() to it.meaning.lowercase() }.toSet()
                val fresh = imported.filterNot { (it.word.lowercase() to it.meaning.lowercase()) in existing }
                words.addAll(0, fresh); save(); importMessage = "${fresh.size} words imported"
            }.onFailure { importMessage = "Import failed: ${it.message ?: "Invalid Excel file"}" }
    }
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WordEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(0) }
    val learned = words.count { it.learned }
    val due = words.filter { !it.learned }
    val safeIndex = if (due.isEmpty()) 0 else index.coerceIn(0, due.lastIndex)
    val current = due.getOrNull(safeIndex)

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) engine?.setLanguage(Locale.US)
        }
        tts = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
            tts = null
        }
    }

    // Used by "Hard": move on to the next word that is still being reviewed.
    fun advance() { reveal = false; index = if (due.size <= 1) 0 else (safeIndex + 1) % due.size }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Vocabulary", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Your daily English practice", color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) }) { Icon(Icons.Default.FileUpload, "Import Excel", tint = BurgundyLight) }
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add word", tint = BurgundyLight) }
            }
        }
        DayflowCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TODAY'S PROGRESS", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text("$learned / ${words.size}", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text("★", color = Warning, style = MaterialTheme.typography.headlineMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = BurgundyLight, trackColor = SurfaceElevated
                )
                Spacer(Modifier.width(12.dp))
                Text(if (words.isEmpty()) "0%" else "${learned * 100 / words.size}%", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (current == null) {
            DayflowCard {
                Text(if (words.isEmpty()) "Your vocabulary starts here" else "You're done for today",
                    color = TextPrimary, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text(if (words.isEmpty()) "Add your first English word and its meaning." else "All saved words are marked as learned. Add more words to keep going.",
                    color = TextSecondary, textAlign = TextAlign.Center)
                AccentButton("＋  Add a word") { showAdd = true }
            }
        } else {
            DayflowCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("WORD ${safeIndex + 1} OF ${due.size}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(current.word, color = TextPrimary, style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    IconButton(onClick = { tts?.speak(current.word, TextToSpeech.QUEUE_FLUSH, null, "dayflow_word") }) {
                        Icon(Icons.Default.VolumeUp, "Pronounce", tint = BurgundyLight)
                    }
                }
                if (reveal) {
                    Text(current.meaning, color = BurgundyLight, style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    if (current.example.isNotBlank()) {
                        Text(current.example, color = TextSecondary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Text("Think of the meaning, then reveal it", color = TextSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = { reveal = !reveal }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(if (reveal) "Hide meaning" else "Reveal meaning", color = TextPrimary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val i = words.indexOfFirst { it.id == current.id }
                        if (i >= 0) words[i] = current.copy(learned = true)
                        // The learned word leaves the review list, so the next one slides into this slot.
                        reveal = false
                        save()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("I Know")
                }
                OutlinedButton(
                    onClick = { advance() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Favorite, null, tint = TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Hard", color = TextPrimary)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Word List", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("${words.size} saved", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search vocabulary") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
            shape = RoundedCornerShape(18.dp)
        )
        words.filter { it.word.contains(searchQuery, true) || it.meaning.contains(searchQuery, true) }.forEach { entry ->
            DayflowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.word, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(entry.meaning, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (entry.learned) "Learned" else "Review", color = if (entry.learned) Success else BurgundyLight,
                        style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = { editingWord = entry; word = entry.word; meaning = entry.meaning; example = entry.example; showAdd = true }) {
                        Icon(Icons.Default.Edit, "Edit word", tint = BurgundyLight)
                    }
                    IconButton(onClick = { pendingDelete = entry }) {
                        Icon(Icons.Default.Delete, "Delete word", tint = TextSecondary)
                    }
                }
            }
        }
    }

    if (importMessage != null) AlertDialog(onDismissRequest = { importMessage = null }, title = { Text("Excel import") }, text = { Text(importMessage!!) }, confirmButton = { TextButton(onClick = { importMessage = null }) { Text("OK") } })

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(if (editingWord == null) "Add vocabulary" else "Edit vocabulary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = word, onValueChange = { word = it }, label = { Text("English word") }, singleLine = true)
                    OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("Arabic meaning") }, singleLine = true)
                    OutlinedTextField(value = example, onValueChange = { example = it }, label = { Text("Example (optional)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (word.isNotBlank() && meaning.isNotBlank()) {
                        val old = editingWord
                        if (old == null) words.add(0, WordEntry(System.currentTimeMillis(), word.trim(), meaning.trim(), example.trim()))
                        else { val i = words.indexOfFirst { it.id == old.id }; if (i >= 0) words[i] = old.copy(word = word.trim(), meaning = meaning.trim(), example = example.trim()) }
                        save(); word = ""; meaning = ""; example = ""; editingWord = null; index = 0; showAdd = false
                    }
                }) { Text("Save", color = BurgundyLight) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editingWord = null; word = ""; meaning = ""; example = "" }) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { entry ->
        ConfirmDialog(
            title = "Delete word?",
            message = "${entry.word} — ${entry.meaning}",
            onConfirm = {
                words.removeAll { it.id == entry.id }
                save()
                index = 0
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
