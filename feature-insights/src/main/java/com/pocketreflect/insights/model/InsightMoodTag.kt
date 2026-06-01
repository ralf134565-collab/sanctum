// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.model

/**
 * Зеркало [com.pocketreflect.app.domain.model.MoodTag] для автономного модуля insights.
 * При интеграции в app — маппинг 1:1 по storageKey.
 */
enum class InsightMoodTag(
    val storageKey: String,
    val displayNameRu: String,
    val displayNameEn: String,
    val polarity: Polarity,
) {
    CALM("calm", "Спокойствие", "Calm", Polarity.POSITIVE),
    JOY("joy", "Радость", "Joy", Polarity.POSITIVE),
    GRATITUDE("gratitude", "Благодарность", "Gratitude", Polarity.POSITIVE),
    FOCUSED("focused", "Сфокусированность", "Focused", Polarity.POSITIVE),

    TIRED("tired", "Усталость", "Tired", Polarity.NEUTRAL),
    OVERWHELMED("overwhelmed", "Перегруз", "Overwhelmed", Polarity.NEUTRAL),

    ANXIETY("anxiety", "Тревога", "Anxiety", Polarity.NEGATIVE),
    SADNESS("sadness", "Грусть", "Sadness", Polarity.NEGATIVE),
    IRRITATION("irritation", "Раздражение", "Irritation", Polarity.NEGATIVE),
    ;

    enum class Polarity { POSITIVE, NEUTRAL, NEGATIVE }

    fun displayName(english: Boolean): String = if (english) displayNameEn else displayNameRu

    companion object {
        val orderedForUi: List<InsightMoodTag> = entries.sortedBy { it.ordinal }
    }
}

val Collection<InsightMoodTag>.hasNegative: Boolean
    get() = any { it.polarity == InsightMoodTag.Polarity.NEGATIVE }
