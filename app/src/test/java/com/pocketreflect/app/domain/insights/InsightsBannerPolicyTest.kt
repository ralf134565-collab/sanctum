// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.insights

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsBannerPolicyTest {

    private val now = 1_000_000L

    @Test
    fun showsWhenEnoughEntriesAndNeverOpenedTab() {
        assertTrue(
            InsightsBannerPolicy.shouldShow(
                entriesLast30Days = 12,
                tabEverOpened = false,
                tabLastOpenedAtMs = null,
                bannerLastShownMs = null,
                nowMs = now,
                isShortRitualActive = false,
            ),
        )
    }

    @Test
    fun hidesWhenShortRitual() {
        assertFalse(
            InsightsBannerPolicy.shouldShow(
                entriesLast30Days = 20,
                tabEverOpened = false,
                tabLastOpenedAtMs = null,
                bannerLastShownMs = null,
                nowMs = now,
                isShortRitualActive = true,
            ),
        )
    }

    @Test
    fun hidesWithinBannerCooldown() {
        assertFalse(
            InsightsBannerPolicy.shouldShow(
                entriesLast30Days = 20,
                tabEverOpened = true,
                tabLastOpenedAtMs = now,
                bannerLastShownMs = now - 1,
                nowMs = now,
                isShortRitualActive = false,
            ),
        )
    }
}
