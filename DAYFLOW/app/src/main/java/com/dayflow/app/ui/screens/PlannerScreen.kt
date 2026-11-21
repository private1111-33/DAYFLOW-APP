package com.dayflow.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*

@Composable
fun PlannerScreen() {
    ScreenColumn {
        SectionTitle("Planner", "Your schedule, in one place")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Day") })
            FilterChip(selected = false, onClick = {}, label = { Text("Week") })
            FilterChip(selected = false, onClick = {}, label = { Text("Month") })
        }
        DayflowCard {
            Text("Saturday, November 21", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("Today", color = MaterialTheme.colorScheme.primary)
        }
        DayflowCard {
            Text("10:30  •  Review project notes", color = MaterialTheme.colorScheme.onSurface)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text("12:00  •  English vocabulary", color = MaterialTheme.colorScheme.onSurface)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text("04:00  •  Personal work", color = MaterialTheme.colorScheme.onSurface)
        }
        PlaceholderNote("Planner UI foundation. Task creation, calendar data and persistence are added in the next implementation phase.")
        AccentButton("＋  Add task") {}
    }
}
