// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.insights

/**
 * Когда показывать баннер на «Сегодня» с приглашением открыть «Картину».
 */
object InsightsBannerPolicy {

    const val MIN_ENTRIES_LAST_30 = 12
    private const val BANNER_COOLDOWN_MS = 14L * 24 * 60 * 60 * 1000

    fun shouldShow(
        entriesLast30Days: Int,
        tabEverOpened: Boolean,
        tabLastOpenedAtMs: Long?,
        bannerLastShownMs: Long?,
        nowMs: Long,
        isShortRitualActive: Boolean,
    ): Boolean {
        if (isShortRitualActive) return false
        if (entriesLast30Days < MIN_ENTRIES_LAST_30) return false
        if (bannerLastShownMs != null && nowMs - bannerLastShownMs < BANNER_COOLDOWN_MS) {
            return false
        }
        if (!tabEverOpened) return true
        val lastTab = tabLastOpenedAtMs ?: return true
        return nowMs - lastTab >= BANNER_COOLDOWN_MS
    }
}
