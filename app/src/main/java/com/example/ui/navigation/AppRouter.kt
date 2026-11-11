package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.domain.model.AppModule

/**
 * AppRouter using MaterialNavigation / Jetpack Navigation to manage transitions
 * between the main dashboard and tracker screens (Habit, Expense, Journal, Tasks,
 * Health, Guilds, Reports, Settings, Profile), with deep linking support.
 */
class AppRouter(val navController: NavHostController) {

    companion object {
        const val SCHEME = "lifetracker"
        const val HOST = "lifetracker.example.com"

        fun routeForModule(module: AppModule): String = when (module) {
            AppModule.DASHBOARD -> "dashboard"
            AppModule.TASKS -> "tasks"
            AppModule.HABITS -> "habits"
            AppModule.EXPENSES -> "expenses"
            AppModule.HEALTH -> "health"
            AppModule.JOURNAL -> "journal"
            AppModule.NOTES -> "notes"
            AppModule.GOALS -> "goals"
            AppModule.CALENDAR -> "calendar"
            AppModule.AI_ASSISTANT -> "ai_assistant"
            AppModule.REPORTS -> "reports"
            AppModule.GUILDS -> "guilds"
            AppModule.SETTINGS -> "settings"
            AppModule.PROFILE -> "profile"
            AppModule.AUTH -> "auth"
        }

        fun moduleForRoute(route: String?): AppModule = when (route?.substringBefore("?")) {
            "tasks" -> AppModule.TASKS
            "habits" -> AppModule.HABITS
            "expenses" -> AppModule.EXPENSES
            "health" -> AppModule.HEALTH
            "journal" -> AppModule.JOURNAL
            "notes" -> AppModule.NOTES
            "goals" -> AppModule.GOALS
            "calendar" -> AppModule.CALENDAR
            "ai_assistant" -> AppModule.AI_ASSISTANT
            "reports" -> AppModule.REPORTS
            "guilds" -> AppModule.GUILDS
            "settings" -> AppModule.SETTINGS
            "profile" -> AppModule.PROFILE
            "auth" -> AppModule.AUTH
            else -> AppModule.DASHBOARD
        }

        fun createDeepLinks(route: String) = listOf(
            navDeepLink { uriPattern = "$SCHEME://$route" },
            navDeepLink { uriPattern = "https://$HOST/$route" }
        )
    }

    /**
     * Navigates to a target module with state restoration enabled.
     */
    fun navigateTo(module: AppModule) {
        val targetRoute = routeForModule(module)
        val currentRoute = navController.currentDestination?.route

        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun popBackStack(): Boolean = navController.popBackStack()

    fun currentModule(): AppModule {
        val route = navController.currentBackStackEntry?.destination?.route
        return moduleForRoute(route)
    }
}

@Composable
fun rememberAppRouter(
    navController: NavHostController = rememberNavController()
): AppRouter {
    return remember(navController) {
        AppRouter(navController)
    }
}
