package com.dayflow.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AlarmsScreen() {
    ScreenColumn {
        SectionTitle("Alarms", "Stay on time")
        DayflowCard {
            Text("08:30 AM", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Daily focus", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("MON  TUE  WED  THU  FRI", color = MaterialTheme.colorScheme.primary)
        }
        DayflowCard {
            Text("Alarm engine", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            PlaceholderNote("Exact alarms, snooze, custom sounds and reboot recovery are not wired yet.")
        }
        AccentButton("＋  New alarm") {}
    }
}
