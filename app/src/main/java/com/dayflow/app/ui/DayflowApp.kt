package com.dayflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dayflow.app.ui.screens.*
import com.dayflow.app.ui.theme.Background
import com.dayflow.app.ui.theme.Surface
import com.dayflow.app.ui.theme.Burgundy
import com.dayflow.app.ui.theme.TextSecondary

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DayflowApp() {
    val navController = rememberNavController()
    val items = listOf(
        NavItem("home", "Home", Icons.Filled.Home),
        NavItem("vocabulary", "Learn", Icons.Filled.MenuBook),
        NavItem("progress", "Stats", Icons.Filled.BarChart),
        NavItem("settings", "Settings", Icons.Filled.Settings)
    )
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    fun openTab(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = Surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route,
                        onClick = { openTab(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Burgundy,
                            selectedTextColor = Burgundy,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    onOpenProgress = { openTab("progress") },
                    onOpenSettings = { openTab("settings") },
                    onOpenPlanner = { navController.navigate("planner") },
                    onOpenAlarms = { navController.navigate("alarms") },
                    onOpenVocabulary = { openTab("vocabulary") },
                    onOpenQuiz = { navController.navigate("quiz") },
                    onOpenMusic = { navController.navigate("music") }
                )
            }
            composable("planner") { PlannerScreen() }
            composable("alarms") { AlarmsScreen() }
            composable("vocabulary") { VocabularyScreen() }
            composable("quiz") { QuizScreen(onBack = { openTab("home") }) }
            composable("music") { MusicScreen() }
            composable("progress") { ProgressScreen(onBack = { openTab("home") }) }
            composable("settings") { SettingsScreen(onBack = { openTab("home") }) }
        }
    }
}
