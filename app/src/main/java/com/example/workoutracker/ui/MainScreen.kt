package com.example.workoutracker.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Default.Home),
    History("history", "History", Icons.Default.DateRange),
    Exercises("exercises", "Exercises", Icons.Default.Menu)
}

private const val WORKOUT_ROUTE = "workout/{workoutId}"
private fun workoutRoute(id: Long) = "workout/$id"

/**
 * Switches to a bottom-bar tab: keeps one copy of each tab and remembers its state.
 * Every tab switch should go through here, including ones the code makes itself,
 * so the saved tab state stays consistent.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The app's top-level shell: a bottom navigation bar and the screen for the selected tab. */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The bottom bar only makes sense on the three main tabs, not during a workout.
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (tab == Tab.Home) {
                                    // Home is always at the bottom of the back stack, so going
                                    // Home just means removing everything above it. No saved
                                    // state is restored, so it can't land somewhere unexpected.
                                    navController.popBackStack(Tab.Home.route, inclusive = false)
                                } else {
                                    navController.navigateToTab(tab.route)
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            // consumeWindowInsets stops the screens' own Scaffolds adding the same padding twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    onOpenWorkout = { id ->
                        navController.navigate(workoutRoute(id)) { launchSingleTop = true }
                    }
                )
            }
            composable(Tab.History.route) { HistoryScreen() }
            composable(Tab.Exercises.route) { ExercisesScreen() }
            composable(
                route = WORKOUT_ROUTE,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) {
                ActiveWorkoutScreen(
                    onFinished = {
                        // Back to Home first, then switch tabs exactly like the bottom bar does.
                        // Jumping straight from here to History confused the tab state-saving,
                        // which is why tapping Home kept bouncing you back to History.
                        navController.popBackStack(Tab.Home.route, inclusive = false)
                        navController.navigateToTab(Tab.History.route)
                    },
                    onDiscarded = { navController.popBackStack() }
                )
            }
        }
    }
}