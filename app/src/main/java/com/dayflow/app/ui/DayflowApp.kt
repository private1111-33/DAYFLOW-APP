package com.dayflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dayflow.app.ui.screens.*

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DayflowApp() {
    val navController = rememberNavController()
    val items = listOf(
        NavItem("home", "Home", Icons.Rounded.Home),
        NavItem("vocabulary", "Learn", Icons.Rounded.MenuBook),
        NavItem("progress", "Stats", Icons.Rounded.BarChart),
        NavItem("settings", "Settings", Icons.Rounded.Settings)
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != "splash") NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route,
                        onClick = { openTab(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "splash", modifier = Modifier.padding(padding)) {
            composable("splash") {
                LaunchedEffect(Unit) {
                    delay(1700)
                    navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                }
                SplashScreen()
            }
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
            composable("settings") { SettingsScreen(onBack = { openTab("home") }, onOpenMusic = { navController.navigate("music") }) }
        }
    }
}
