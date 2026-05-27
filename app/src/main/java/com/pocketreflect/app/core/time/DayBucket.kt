// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Утилита для построения «ключа дня» — стабильного YYYY-MM-DD в локальной TZ.
 *
 * Зачем выделено отдельно:
 *  - Чтобы единый источник истины определял, что считать «сегодняшним днём»
 *    (важно для логики «уже записал/нет»).
 *  - Чтобы тесты могли подменить `Clock` и `ZoneId` без правок продакшн-кода.
 */
object DayBucket {

    private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    /** Ключ дня для произвольного момента времени. */
    fun of(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .format(FORMATTER)

    /** Ключ сегодняшнего дня. */
    fun today(zone: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zone).format(FORMATTER)

    fun fromLocalDate(date: LocalDate): String = date.format(FORMATTER)

    fun toLocalDate(dayBucket: String): LocalDate = LocalDate.parse(dayBucket, FORMATTER)

    /** Полдень выбранного дня — стабильный timestamp для прошлых записей. */
    fun toNoonEpochMillis(dayBucket: String, zone: ZoneId = ZoneId.systemDefault()): Long =
        toLocalDate(dayBucket)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
