// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DayBucketCalendar {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun isWeekend(dayBucket: String): Boolean {
        val dow = LocalDate.parse(dayBucket, formatter).dayOfWeek
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
    }

    fun isWeekday(dayBucket: String): Boolean = !isWeekend(dayBucket)
}
