// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.time

import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
import com.pocketreflect.app.core.locale.AppLanguage
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Форматирование дат для UI (заголовки журнала, история).
 *
 * Локаль берётся из [AppLanguage] (ViewModel) или [Configuration] (Compose).
 */
object DateFormats {

    fun javaLocale(language: AppLanguage): Locale = when (AppLanguage.resolve(language)) {
        AppLanguage.EN -> Locale.forLanguageTag("en")
        else -> Locale.forLanguageTag("ru")
    }

    fun javaLocale(configuration: Configuration): Locale {
        val locales = ConfigurationCompat.getLocales(configuration)
        return when {
            !locales.isEmpty -> locales[0]!!
            else -> Locale.getDefault()
        }
    }

    /** «Tuesday, May 19» / «Вторник, 19 мая» — TopAppBar экрана дня. */
    fun dayHeader(
        epochMillis: Long,
        locale: Locale,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }

    /** Заголовок TopAppBar по ключу дня YYYY-MM-DD. */
    fun dayHeaderFromBucket(
        dayBucket: String,
        locale: Locale,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = dayHeader(DayBucket.toNoonEpochMillis(dayBucket, zone), locale, zone)

    /** «May 19» / «19 мая» — карточки истории и деталь записи. */
    fun shortDay(
        epochMillis: Long,
        locale: Locale,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("d MMMM", locale))

    /** «May 2026» / «Май 2026» — заголовок группы в истории. */
    fun monthHeader(yearMonth: YearMonth, locale: Locale): String {
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        return "${monthName.replaceFirstChar { it.titlecase(locale) }} ${yearMonth.year}"
    }

    fun yearMonthOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): YearMonth =
        YearMonth.from(
            Instant.ofEpochMilli(epochMillis)
                .atZone(zone)
                .toLocalDate(),
        )

    fun localDateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
}
