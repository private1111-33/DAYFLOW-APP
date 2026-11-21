package com.dayflow.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Progress", "A clear view of your consistency")
        DayflowCard {
            Text("Tasks completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("18", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("This week", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Vocabulary", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Words learned and review accuracy will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PlaceholderNote("Statistics will be calculated from real local database records.")
    }
}
