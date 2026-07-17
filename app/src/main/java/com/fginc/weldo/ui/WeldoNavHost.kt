package com.fginc.weldo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.notifications.NudgeDeepLink
import com.fginc.weldo.ui.detail.ItemDetailScreen
import com.fginc.weldo.ui.login.LoginScreen
import com.fginc.weldo.ui.project.ProjectScreen
import com.fginc.weldo.ui.settings.SettingsScreen
import com.fginc.weldo.ui.stats.StatsScreen

/** Route names. Detail is keyed by (type wire, id); project by id. */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    fun item(type: ItemType, id: String) = "item/${type.wire}/$id"
    const val ITEM_PATTERN = "item/{type}/{id}"
    fun project(id: String) = "project/$id"
    const val PROJECT_PATTERN = "project/{id}"
}

@Composable
fun WeldoNavHost(
    pendingDeepLink: NudgeDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val nav = rememberNavController()
    val start = if (WeldoApp.graph.session.isSignedIn) Routes.HOME else Routes.LOGIN

    // A tapped nudge notification opens the item's detail screen (if signed in).
    LaunchedEffect(pendingDeepLink) {
        val dl = pendingDeepLink ?: return@LaunchedEffect
        if (WeldoApp.graph.session.isSignedIn) {
            val type = ItemType.fromWire(dl.type) ?: ItemType.TASK
            nav.navigate(Routes.item(type, dl.itemId))
        }
        onDeepLinkConsumed()
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(onSignedIn = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            MainShell(
                onOpenItem = { type, id -> nav.navigate(Routes.item(type, id)) },
                onOpenProject = { id -> nav.navigate(Routes.project(id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            Routes.ITEM_PATTERN,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
            ),
        ) { entry ->
            val type = ItemType.fromWire(entry.arguments?.getString("type")) ?: ItemType.TASK
            val id = entry.arguments?.getString("id").orEmpty()
            ItemDetailScreen(
                type = type,
                id = id,
                onBack = { nav.popBackStack() },
                onOpenProject = { pid -> nav.navigate(Routes.project(pid)) },
            )
        }

        composable(
            Routes.PROJECT_PATTERN,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ProjectScreen(
                projectId = id,
                onBack = { nav.popBackStack() },
                onOpenItem = { type, itemId -> nav.navigate(Routes.item(type, itemId)) },
                onOpenProject = { pid -> nav.navigate(Routes.project(pid)) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenStats = { nav.navigate(Routes.STATS) },
                onSignedOut = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }
    }
}
