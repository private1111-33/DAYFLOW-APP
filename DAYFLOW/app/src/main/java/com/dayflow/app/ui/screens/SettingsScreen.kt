package com.dayflow.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    ScreenColumn {
        TextButton(onClick = onBack) { Text("← Back") }
        SectionTitle("Settings", "Make DAYFLOW work your way")
        DayflowCard {
            Text("Appearance", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Dark theme", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Reminders & alarms", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Alarm permissions and reminder preferences", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DayflowCard {
            Text("Data", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Offline backup, restore and clear local data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PlaceholderNote("Settings are visual foundations. Persistent preferences will be added with DataStore.")
    }
}
