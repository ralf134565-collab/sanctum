// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.core.time.Clock

/**
 * Детерминированные часы для тестов. Время и день можно подменять руками
 * без таблеток-обёрток над `Instant.now()`.
 */
class FakeClock(
    var fixedNowMillis: Long = 0L,
    var fixedToday: String = "2026-05-19",
) : Clock {
    override fun nowMillis(): Long = fixedNowMillis
    override fun today(): String = fixedToday
}
