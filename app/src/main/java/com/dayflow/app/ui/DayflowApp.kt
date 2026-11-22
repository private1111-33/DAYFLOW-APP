package com.dayflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dayflow.app.ui.screens.*
import com.dayflow.app.ui.theme.Background
import com.dayflow.app.ui.theme.Surface
import com.dayflow.app.ui.theme.SurfaceElevated
import com.dayflow.app.ui.theme.TextSecondary

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DayflowApp() {
    val navController = rememberNavController()
    // Five main sections. Progress and Settings are secondary screens reached from Home.
    val items = listOf(
        NavItem("home", "Home", Icons.Filled.Home),
        NavItem("planner", "Planner", Icons.Filled.CalendarMonth),
        NavItem("alarms", "Alarms", Icons.Filled.Alarm),
        NavItem("vocabulary", "Vocabulary", Icons.Filled.MenuBook),
        NavItem("music", "Music", Icons.Filled.MusicNote)
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
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = SurfaceElevated,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { it / 12 }) },
            exitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 16 }) },
            popEnterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { -it / 12 }) },
            popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { it / 16 }) }) {
            composable("home") {
                HomeScreen(
                    onOpenProgress = { navController.navigate("progress") { launchSingleTop = true } },
                    onOpenSettings = { navController.navigate("settings") { launchSingleTop = true } },
                    onOpenPlanner = { openTab("planner") },
                    onOpenAlarms = { openTab("alarms") },
                    onOpenVocabulary = { openTab("vocabulary") }
                )
            }
            composable("planner") { PlannerScreen() }
            composable("alarms") { AlarmsScreen() }
            composable("vocabulary") { VocabularyScreen() }
            composable("music") { MusicScreen() }
            composable("progress") { ProgressScreen(onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
