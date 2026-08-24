package com.divyasrikarri.migrainejournal.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.divyasrikarri.migrainejournal.ui.dailycheckin.DailyCheckInScreen
import com.divyasrikarri.migrainejournal.ui.history.HistoryScreen
import com.divyasrikarri.migrainejournal.ui.home.HomeScreen
import com.divyasrikarri.migrainejournal.ui.insights.InsightsScreen
import com.divyasrikarri.migrainejournal.ui.logmigraine.LogMigraineScreen
import com.divyasrikarri.migrainejournal.ui.navigation.BottomDestination
import com.divyasrikarri.migrainejournal.ui.navigation.Routes
import com.divyasrikarri.migrainejournal.ui.settings.SettingsScreen

@Composable
fun MigraineApp(
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    // A notification tap arrives as a route to open once the graph exists.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let {
            navController.navigate(it)
            onPendingRouteConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomDestination.entries.any { it.route == currentRoute }

    Scaffold(
        // Each screen's own Scaffold applies the status-bar inset via its top bar, so the
        // host must not apply it a second time. The bottom bar still contributes its height
        // (and its own navigation-bar inset) to the content padding below.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onLogMigraine = { navController.navigate(Routes.logMigraine()) },
                    onOpenMigraine = { id -> navController.navigate(Routes.logMigraine(id)) },
                    onOpenCheckIn = { navController.navigate(Routes.checkIn()) },
                    onSeeAllHistory = { navController.navigate(Routes.HISTORY) }
                )
            }

            composable(
                route = Routes.LOG_MIGRAINE,
                arguments = listOf(
                    navArgument(Routes.ARG_ENTRY_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                val entryId = entry.arguments?.getLong(Routes.ARG_ENTRY_ID) ?: 0L
                LogMigraineScreen(
                    entryId = entryId,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.CHECK_IN,
                arguments = listOf(
                    navArgument(Routes.ARG_DATE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                DailyCheckInScreen(
                    dateKey = entry.arguments?.getString(Routes.ARG_DATE),
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenMigraine = { id -> navController.navigate(Routes.logMigraine(id)) },
                    onOpenCheckIn = { dateKey -> navController.navigate(Routes.checkIn(dateKey)) }
                )
            }

            composable(Routes.INSIGHTS) { InsightsScreen() }

            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}
