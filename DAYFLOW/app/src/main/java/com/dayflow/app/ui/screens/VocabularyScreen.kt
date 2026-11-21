package com.dayflow.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun VocabularyScreen() {
    ScreenColumn {
        SectionTitle("Vocabulary", "Build your English, one word at a time")
        DayflowCard {
            Text("TODAY'S REVIEW", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text("12 words", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { 0.35f }, color = MaterialTheme.colorScheme.primary)
            Text("4 of 12 reviewed", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Productive", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
            Text("مُنتِج", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text("Example: A productive day starts with a clear plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AccentButton("Start review") {}
        PlaceholderNote("Spaced repetition, quizzes and local word storage will be implemented in the next phase.")
    }
}
