package com.dayflow.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dayflow.app.ui.screens.*
import com.dayflow.app.ui.theme.Background

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DayflowApp() {
    val navController = rememberNavController()
    val items = listOf(
        NavItem("home", "Home", Icons.Filled.Home),
        NavItem("planner", "Planner", Icons.Filled.CalendarMonth),
        NavItem("alarms", "Alarms", Icons.Filled.Alarm),
        NavItem("vocabulary", "Words", Icons.Filled.MenuBook),
        NavItem("music", "Music", Icons.Filled.MusicNote)
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = com.dayflow.app.ui.theme.Surface) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = com.dayflow.app.ui.theme.SurfaceElevated,
                            unselectedIconColor = com.dayflow.app.ui.theme.TextSecondary,
                            unselectedTextColor = com.dayflow.app.ui.theme.TextSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(onOpenProgress = { navController.navigate("progress") }, onOpenSettings = { navController.navigate("settings") }) }
            composable("planner") { PlannerScreen() }
            composable("alarms") { AlarmsScreen() }
            composable("vocabulary") { VocabularyScreen() }
            composable("music") { MusicScreen() }
            composable("progress") { ProgressScreen(onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
