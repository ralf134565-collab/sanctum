// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.work

/**
 * Общие константы weekly summary (worker + UI).
 */
object WeeklySummaryPolicy {
    const val MIN_ENTRIES_FOR_SUMMARY = 3
    const val SUMMARY_WINDOW_DAYS = 7

    /** На «Сегодня» показываем текст профиля, если он не старше 8 суток. */
    const val DISPLAY_PROFILE_MAX_AGE_MS = 8L * 24 * 60 * 60 * 1000
}
