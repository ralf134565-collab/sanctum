// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.time

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Инжектируемая абстракция над «сейчас».
 *
 * Зачем не использовать `System.currentTimeMillis()` напрямую:
 *  - В тестах не получится подменить «сегодня», и `JournalViewModel`,
 *    который опирается на `today()`, станет непроверяемым на границах суток.
 *  - Единое место для смены TZ-стратегии в будущем (например, «день
 *    заканчивается в 04:00 для сов» — частая фича в дневниковых приложениях).
 *
 * Реализация по умолчанию — [SystemClock]; в тестах подменяем на `FakeClock`.
 */
interface Clock {
    /** Эпоха в миллисекундах. */
    fun nowMillis(): Long

    /** Локальный «ключ дня» YYYY-MM-DD (в TZ устройства). */
    fun today(): String
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): String = DayBucket.today()
}
