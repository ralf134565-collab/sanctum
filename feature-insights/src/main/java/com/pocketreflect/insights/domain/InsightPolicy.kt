// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

object InsightPolicy {
    const val WINDOW_30_DAYS = 30
    const val WINDOW_90_DAYS = 90

    const val MIN_ENTRIES_FULL = 12
    const val MIN_ENTRIES_PREVIEW = 8

    const val MIN_PATTERN_SUPPORT = 4
    const val MIN_PATTERN_RATE = 0.55
    const val DOMINANT_TAG_RATE = 0.45

    const val MAX_CARDS = 7
    const val VISIBLE_CARDS_COLLAPSED = 3

    /** Порог доли вечеров с ровно одним тегом — показываем нормализующий copy. */
    const val SINGLE_TAG_EVENING_RATE = 0.5f

    /** Минимум активных тегов для полного 10-угольника. */
    const val MIN_ACTIVE_TAGS_FOR_FULL_POLYGON = 3

    /** Минимум средних тегов на вечер для полного полигона. */
    const val MIN_AVG_TAGS_FOR_FULL_POLYGON = 1.3f

    const val MIN_VERTEX_SCORE_SIMPLIFIED = 0.15f
    const val POLYGON_MIN_VERTEX_FLOOR = 0.08f

    /** Минимум вечеров с записью в подвыборке (выходные / будни). */
    const val MIN_WEEKEND_EVENINGS = 3
    const val MIN_WEEKDAY_EVENINGS = 6

    /** Тег «привязан» к выходным/будням, если доля в подвыборке ≥ этого порога. */
    const val MIN_WEEKPART_TAG_RATE = 0.55f

    /** Контраст: на выходных тег заметно чаще, чем в будни. */
    const val WEEKPART_CONTRAST_DELTA = 0.22f
}
