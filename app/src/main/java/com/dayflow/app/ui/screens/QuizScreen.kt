package com.dayflow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.dayflow.app.data.LocalStore
import com.dayflow.app.data.WordEntry
import com.dayflow.app.ui.theme.*
import kotlin.random.Random

@Composable
fun QuizScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val words = remember { LocalStore.words(context).filter { it.meaning.isNotBlank() } }
    var questionIndex by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    val questions = remember(words) { words.shuffled().take(10) }
    val current = questions.getOrNull(questionIndex)
    val choices = remember(current, words) {
        if (current == null) emptyList()
        else (listOf(current.meaning) + words.filter { it.id != current.id }.map { it.meaning }.distinct().shuffled().take(3)).shuffled()
    }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back", color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.weight(1f))
            Text(if (finished) "Quiz Results" else "Quiz", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (questions.isEmpty()) "0 / 0" else "${(questionIndex + 1).coerceAtMost(questions.size)} / ${questions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (questions.isEmpty()) {
            DayflowCard {
                Text("Add at least one vocabulary word with its meaning to start a quiz.", color = MaterialTheme.colorScheme.onSurface)
                AccentButton("Back to words", onBack)
            }
        } else if (finished) {
            DayflowCard {
                Text("Your score", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$score / ${questions.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("Keep practicing to improve your accuracy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                AccentButton("Try Again") { questionIndex = 0; selected = null; score = 0; finished = false }
            }
        } else if (current != null) {
            LinearProgressIndicator(
                progress = { (questionIndex + 1).toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            DayflowCard {
                Text("What does this word mean?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(current.word, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            choices.forEach { choice ->
                val isSelected = selected == choice
                val correct = choice == current.meaning
                OutlinedButton(
                    onClick = { if (selected == null) { selected = choice; if (correct) score++ } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected && correct) MaterialTheme.colorScheme.surfaceVariant else if (isSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected && correct) MaterialTheme.colorScheme.primary else Divider
                    )
                ) {
                    Text(choice, modifier = Modifier.weight(1f))
                    if (isSelected) Text(if (correct) "✓" else "×", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (questionIndex == questions.lastIndex) finished = true
                    else { questionIndex++; selected = null }
                },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (questionIndex == questions.lastIndex) "Finish Quiz" else "Next  ›") }
        }
    }
}
