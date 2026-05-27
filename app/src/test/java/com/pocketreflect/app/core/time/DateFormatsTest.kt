// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.time

import com.pocketreflect.app.core.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class DateFormatsTest {

    @Test
    fun monthHeader_english() {
        val text = DateFormats.monthHeader(YearMonth.of(2026, 5), DateFormats.javaLocale(AppLanguage.EN))
        assertEquals("May 2026", text)
    }

    @Test
    fun monthHeader_russian() {
        val text = DateFormats.monthHeader(YearMonth.of(2026, 5), DateFormats.javaLocale(AppLanguage.RU))
        assertEquals("Май 2026", text)
    }
}
