package com.dayflow.app.ui.screens

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

@Composable
fun VocabularyScreen() {
    val context = LocalContext.current
    val words = remember { mutableStateListOf<WordEntry>().apply { addAll(LocalStore.words(context)) } }
    var showAdd by remember { mutableStateOf(false) }
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    val learned = words.count { it.learned }
    val due = words.filter { !it.learned }
    val current = due.getOrNull(index.coerceIn(0, (due.size - 1).coerceAtLeast(0)))

    fun save() = LocalStore.saveWords(context, words.toList())
    fun advance() { reveal = false; index = if (due.isEmpty()) 0 else (index + 1) % due.size }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Vocabulary", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Your daily English practice", color = TextSecondary)
            }
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add word", tint = BurgundyLight) }
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
                    Text("WORD ${index + 1} OF ${due.size}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(14.dp))
                Text(current.word, color = TextPrimary, style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                        save(); advance()
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
            Text("My words", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("${words.size} saved", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        words.forEach { entry ->
            DayflowCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.word, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(entry.meaning, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (entry.learned) "Learned" else "Review", color = if (entry.learned) Success else BurgundyLight,
                        style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = {
                        words.removeAll { it.id == entry.id }; save(); index = 0
                    }) { Icon(Icons.Default.Delete, "Delete word", tint = TextSecondary) }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add vocabulary") },
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
                        words.add(0, WordEntry(System.currentTimeMillis(), word.trim(), meaning.trim(), example.trim()))
                        save(); word = ""; meaning = ""; example = ""; index = 0; showAdd = false
                    }
                }) { Text("Save", color = BurgundyLight) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
