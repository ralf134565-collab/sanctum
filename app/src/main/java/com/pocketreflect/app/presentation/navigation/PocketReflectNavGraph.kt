// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.pocketreflect.app.presentation.history.HistoryScreen
import com.pocketreflect.app.presentation.insights.InsightsScreen
import com.pocketreflect.app.presentation.history.detail.EntryDetailScreen
import com.pocketreflect.app.presentation.chat.ChatScreen
import com.pocketreflect.app.presentation.journal.JournalScreen
import com.pocketreflect.app.presentation.settings.AppearanceSettingsScreen
import com.pocketreflect.app.presentation.settings.ChatSettingsScreen
import com.pocketreflect.app.presentation.settings.DataSettingsScreen
import com.pocketreflect.app.presentation.settings.PrivacySettingsScreen
import com.pocketreflect.app.presentation.settings.RitualSettingsScreen
import com.pocketreflect.app.presentation.settings.SettingsScreen
import com.pocketreflect.app.presentation.settings.model.ModelSettingsScreen

/**
 * Главный навигационный граф приложения.
 *
 * Архитектурное замечание про deepLinks:
 *  - в Local-First приложении извне DeepLink не приходит;
 *    точки расширения оставлены на будущее (quick tile и т.п.).
 */
@Composable
fun PocketReflectNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Today,
        modifier = modifier.padding(innerPadding),
    ) {
        composable(Routes.Today) {
            JournalScreen(
                onNavigateToModelSettings = {
                    navController.navigate(Routes.SettingsModel)
                },
                onNavigateToInsights = {
                    navController.navigate(Routes.Insights) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToEntryDetail = { id ->
                    navController.navigate(Routes.entryDetail(id))
                },
            )
        }

        composable(Routes.History) {
            HistoryScreen(
                onOpenEntry = { id ->
                    navController.navigate(Routes.entryDetail(id))
                },
            )
        }

        composable(Routes.Insights) {
            InsightsScreen(
                onOpenEntry = { id ->
                    navController.navigate(Routes.entryDetail(id))
                },
            )
        }

        composable(Routes.Chat) {
            ChatScreen(
                onNavigateToModelSettings = {
                    navController.navigate(Routes.SettingsModel)
                }
            )
        }

        composable(
            route = Routes.EntryDetailPattern,
            arguments = listOf(
                navArgument(Routes.EntryDetailArg) { type = NavType.LongType },
            ),
        ) {
            EntryDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.Settings) {
            SettingsScreen(
                onOpenPrivacy = { navController.navigate(Routes.SettingsPrivacy) },
                onOpenAppearance = { navController.navigate(Routes.SettingsAppearance) },
                onOpenRitual = { navController.navigate(Routes.SettingsRitual) },
                onOpenChat = { navController.navigate(Routes.SettingsChat) },
                onOpenModelSettings = { navController.navigate(Routes.SettingsModel) },
                onOpenData = { navController.navigate(Routes.SettingsData) },
            )
        }

        composable(Routes.SettingsPrivacy) {
            PrivacySettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SettingsRitual) {
            RitualSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SettingsChat) {
            ChatSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SettingsData) {
            DataSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SettingsAppearance) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SettingsModel) {
            ModelSettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
