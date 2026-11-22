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
            TextButton(onClick = onBack) { Text("‹ Back", color = Burgundy) }
            Spacer(Modifier.weight(1f))
            Text(if (finished) "Quiz Results" else "Quiz", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (questions.isEmpty()) "0 / 0" else "${(questionIndex + 1).coerceAtMost(questions.size)} / ${questions.size}", color = TextSecondary)
        }
        if (questions.isEmpty()) {
            DayflowCard {
                Text("Add at least one vocabulary word with its meaning to start a quiz.", color = TextPrimary)
                AccentButton("Back to words", onBack)
            }
        } else if (finished) {
            DayflowCard {
                Text("Your score", color = TextSecondary)
                Text("$score / ${questions.size}", color = Burgundy, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("Keep practicing to improve your accuracy.", color = TextSecondary)
                AccentButton("Try Again") { questionIndex = 0; selected = null; score = 0; finished = false }
            }
        } else if (current != null) {
            LinearProgressIndicator(
                progress = { (questionIndex + 1).toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = Burgundy, trackColor = SurfaceElevated
            )
            DayflowCard {
                Text("What does this word mean?", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(current.word, color = Burgundy, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            choices.forEach { choice ->
                val isSelected = selected == choice
                val correct = choice == current.meaning
                OutlinedButton(
                    onClick = { if (selected == null) { selected = choice; if (correct) score++ } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected && correct) SurfaceElevated else if (isSelected) MaterialTheme.colorScheme.errorContainer else Surface,
                        contentColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected && correct) Burgundy else Divider
                    )
                ) {
                    Text(choice, modifier = Modifier.weight(1f))
                    if (isSelected) Text(if (correct) "✓" else "×", color = Burgundy, fontWeight = FontWeight.Bold)
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
                colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
            ) { Text(if (questionIndex == questions.lastIndex) "Finish Quiz" else "Next  ›") }
        }
    }
}
