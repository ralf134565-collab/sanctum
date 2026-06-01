// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.pocketreflect.app.R

/**
 * Декларативный реестр маршрутов.
 *
 * Используем строковый DSL Navigation Compose (не Type-Safe Nav 2.8) ради двух причин:
 *  - стабильное API, не привязано к kotlinx-serialization;
 *  - явные `route`-строки удобно матчить в DeepLink и логах.
 *
 * Все маршруты табов сгруппированы как [TopLevelDestination] — это то, что
 * показывается в [BottomNavBar]. [Routes.EntryDetail] не в табах: открывается
 * push-нав внутри стека вкладки History.
 */
object Routes {
    const val Today: String = "today"
    const val History: String = "history"
    const val Insights: String = "insights"
    const val Chat: String = "chat"
    const val Settings: String = "settings"

    /** Подэкраны хаба [Settings] — не входят в bottom-nav. */
    const val SettingsPrivacy: String = "settings/privacy"
    const val SettingsRitual: String = "settings/ritual"
    const val SettingsData: String = "settings/data"
    const val SettingsAppearance: String = "settings/appearance"
    const val SettingsChat: String = "settings/chat"

    /**
     * Экран выбора и подключения локальной модели Gemma 4.
     * Открывается из [Settings] и не входит в bottom-nav.
     */
    const val SettingsModel: String = "settings/model"

    /** Параметризованный маршрут с id записи. */
    const val EntryDetailPattern: String = "entry/{entryId}"
    const val EntryDetailArg: String = "entryId"

    fun entryDetail(id: Long): String = "entry/$id"
}

enum class TopLevelDestination(
    val route: String,
    @get:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    TODAY(route = Routes.Today, titleRes = R.string.nav_today, icon = Icons.Outlined.Edit),
    HISTORY(route = Routes.History, titleRes = R.string.nav_history, icon = Icons.Outlined.History),
    INSIGHTS(route = Routes.Insights, titleRes = R.string.nav_insights, icon = Icons.Outlined.AutoGraph),
    CHAT(route = Routes.Chat, titleRes = R.string.nav_chat, icon = Icons.Outlined.Forum),
    SETTINGS(route = Routes.Settings, titleRes = R.string.nav_settings, icon = Icons.Outlined.Settings),
}
