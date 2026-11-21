package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dayflow.app.data.LocalStore
import com.dayflow.app.data.WordEntry
import com.dayflow.app.ui.theme.BurgundyLight
import com.dayflow.app.ui.theme.TextPrimary
import com.dayflow.app.ui.theme.TextSecondary

@Composable
fun VocabularyScreen() {
    val context = LocalContext.current
    val words = remember { mutableStateListOf<WordEntry>().apply { addAll(LocalStore.words(context)) } }
    var showAdd by remember { mutableStateOf(false) }
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var revealId by remember { mutableStateOf<Long?>(null) }
    val learned = words.count { it.learned }

    ScreenColumn {
        SectionTitle("Vocabulary", "Build your English, one word at a time")
        DayflowCard {
            Text("YOUR WORD LIBRARY", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text("${words.size} words", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(
                progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size },
                modifier = Modifier.fillMaxWidth(), color = BurgundyLight
            )
            Text("$learned learned · ${words.size - learned} to review", color = TextSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("My words", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add word")
            }
        }
        if (words.isEmpty()) {
            DayflowCard {
                Text("Start your vocabulary library", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("Save English words with their Arabic meanings and examples. They stay on this device.", color = TextSecondary)
            }
        } else {
            words.forEach { entry ->
                DayflowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.word, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                            if (revealId == entry.id) {
                                Text(entry.meaning, color = BurgundyLight, style = MaterialTheme.typography.titleMedium)
                                if (entry.example.isNotBlank()) Text(entry.example, color = TextSecondary)
                            } else {
                                Text("Tap to reveal meaning", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { revealId = if (revealId == entry.id) null else entry.id }) {
                            Text(if (revealId == entry.id) "Hide" else "Reveal")
                        }
                        IconButton(onClick = {
                            words.removeAll { it.id == entry.id }
                            LocalStore.saveWords(context, words.toList())
                            if (revealId == entry.id) revealId = null
                        }) { Icon(Icons.Default.Delete, contentDescription = "Delete word", tint = TextSecondary) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = entry.learned, onCheckedChange = { checked ->
                            val index = words.indexOfFirst { it.id == entry.id }
                            if (index >= 0) words[index] = entry.copy(learned = checked)
                            LocalStore.saveWords(context, words.toList())
                        }, colors = CheckboxDefaults.colors(checkedColor = BurgundyLight))
                        Text("Learned", color = TextSecondary)
                    }
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
                    OutlinedTextField(value = word, onValueChange = { word = it },
                        label = { Text("English word") }, singleLine = true)
                    OutlinedTextField(value = meaning, onValueChange = { meaning = it },
                        label = { Text("Arabic meaning") }, singleLine = true)
                    OutlinedTextField(value = example, onValueChange = { example = it },
                        label = { Text("Example (optional)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (word.isNotBlank() && meaning.isNotBlank()) {
                        words.add(0, WordEntry(System.currentTimeMillis(), word.trim(), meaning.trim(), example.trim()))
                        LocalStore.saveWords(context, words.toList())
                        word = ""; meaning = ""; example = ""; showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
