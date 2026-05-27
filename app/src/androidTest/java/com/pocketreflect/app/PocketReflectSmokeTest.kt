// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Минимальный smoke на устройстве/эмуляторе: запуск, навигация, disclaimer чата.
 *
 * Не покрывает биометрию (по умолчанию выключена) и прогрев модели
 * (WarmupGate пропускает UI при NoModel/Failed).
 */
@RunWith(AndroidJUnit4::class)
class PocketReflectSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun journalTab_isVisibleOnLaunch() {
        waitForJournal()
        composeRule.onNodeWithText("С каким чувством вы завершаете день?")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToSettings_showsSecuritySection() {
        waitForJournal()
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithText("Защита").assertIsDisplayed()
    }

    @Test
    fun chatDisclaimer_canBeAccepted() {
        waitForJournal()
        composeRule.onNodeWithTag("nav_chat").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Перед началом").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Чат").fetchSemanticsNodes().isNotEmpty()
        }
        val disclaimerVisible = composeRule.onAllNodesWithText("Перед началом")
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (disclaimerVisible) {
            composeRule.onNodeWithTag("chat_disclaimer_checkbox_row").performClick()
            composeRule.onNodeWithTag("chat_disclaimer_continue")
                .assertIsEnabled()
                .performClick()
        }
        composeRule.onNodeWithText("Чат").assertIsDisplayed()
    }

    @Test
    fun saveDay_afterSelectingMoodTag() {
        waitForJournal()
        composeRule.onNodeWithText("Спокойствие").performClick()
        composeRule.onNodeWithTag("journal_save_day")
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Обновить запись").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForJournal() {
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithText("С каким чувством вы завершаете день?")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
