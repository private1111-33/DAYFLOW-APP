package com.dayflow.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dayflow.app.data.LocalStore

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tasks = remember { LocalStore.tasks(context) }
    val words = remember { LocalStore.words(context) }
    val done = tasks.count { it.done }
    val learned = words.count { it.learned }
    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Progress", "A clear view of your consistency")
        DayflowCard {
            Text("Tasks completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(done.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${tasks.size} total tasks · ${tasks.size - done} remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
                color = MaterialTheme.colorScheme.primary)
        }
        DayflowCard {
            Text("Vocabulary", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("${words.size} saved words", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("$learned learned · ${words.size - learned} to review", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size },
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
