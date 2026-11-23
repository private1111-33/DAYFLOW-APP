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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    // Restore the original four-section navigation.
    val items = listOf(
        NavItem("home", "Home", Icons.Filled.Home),
        NavItem("planner", "Tasks", Icons.Filled.CalendarMonth),
        NavItem("alarms", "Alarm", Icons.Filled.Alarm),
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
            NavigationBar(containerColor = Color(0xFF100D10), contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route || (item.route == "music" && route == "discover"),
                        onClick = { openTab(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFB3C8),
                            selectedTextColor = Color(0xFFFFB3C8),
                            indicatorColor = Color(0xFF58172F),
                            unselectedIconColor = Color(0xFF8E858B),
                            unselectedTextColor = Color(0xFF8E858B)
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
                    onOpenVocabulary = { navController.navigate("vocabulary") { launchSingleTop = true } },
                    onOpenMusic = { openTab("music") }
                )
            }
            composable("planner") { PlannerScreen() }
            composable("alarms") { AlarmsScreen() }
            composable("vocabulary") { VocabularyScreen() }
            composable("music") { MusicScreen(onOpenSearch = { navController.navigate("discover") { launchSingleTop = true } }) }
            composable("discover") { MusicSearchScreen() }
            composable("progress") { ProgressScreen(onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
