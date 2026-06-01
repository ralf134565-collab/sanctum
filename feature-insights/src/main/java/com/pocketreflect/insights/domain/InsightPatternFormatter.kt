// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.insights.domain

data class InsightPatternUi(
    val title: String,
    val body: String,
    val evidenceLine: String,
)

object InsightPatternFormatter {

    fun format(pattern: InsightPattern, english: Boolean): InsightPatternUi {
        val title = titleFor(pattern, english)
        val body = bodyFor(pattern, english)
        val evidence = if (english) {
            "${pattern.support} matches · ${pattern.base} in sample · Tap to see evenings"
        } else {
            "${pattern.support} совпадений · ${pattern.base} в выборке · Нажмите, чтобы увидеть вечера"
        }
        return InsightPatternUi(title = title, body = body, evidenceLine = evidence)
    }

    private fun titleFor(pattern: InsightPattern, english: Boolean): String = when (pattern.type) {
        InsightPatternType.CO_OCCURRENCE -> {
            val a = pattern.tagA!!.displayName(english)
            val b = pattern.tagB!!.displayName(english)
            if (english) "$a and $b often on the same evening"
            else "«$a» и «$b» часто в один вечер"
        }
        InsightPatternType.SEQUENCE -> {
            val a = pattern.tagA!!.displayName(english)
            val b = pattern.tagB!!.displayName(english)
            if (english) "After \"$a\", next evening often \"$b\""
            else "После «$a» на следующий вечер часто «$b»"
        }
        InsightPatternType.STREAK -> {
            val t = pattern.tag!!.displayName(english)
            if (english) "Several evenings in a row with \"$t\""
            else "Несколько вечеров подряд с «$t»"
        }
        InsightPatternType.RECOVERY_MICRO_WINS -> if (english) {
            "After a heavy evening, small wins sometimes followed"
        } else {
            "После тяжёлого вечера иногда находили опору"
        }
        InsightPatternType.DOMINANT_TAG -> {
            val t = pattern.tag!!.displayName(english)
            if (english) "Most often marked: \"$t\""
            else "Чаще всего отмечали: «$t»"
        }
        InsightPatternType.WEEKEND_AFFINITY -> {
            val t = pattern.tag!!.displayName(english)
            if (english) "On weekends (Sat–Sun), often \"$t\""
            else "По выходным (сб–вс) чаще отмечали «$t»"
        }
        InsightPatternType.WEEKDAY_AFFINITY -> {
            val t = pattern.tag!!.displayName(english)
            if (english) "On weekdays, often \"$t\""
            else "В будни чаще отмечали «$t»"
        }
    }

    private fun bodyFor(pattern: InsightPattern, english: Boolean): String = when (pattern.type) {
        InsightPatternType.CO_OCCURRENCE -> {
            val a = pattern.tagA!!.displayName(english)
            val b = pattern.tagB!!.displayName(english)
            if (english) {
                "On ${pattern.support} of ${pattern.base} evenings with \"$a\" you also marked \"$b\"."
            } else {
                "В ${pattern.support} из ${pattern.base} вечеров с «$a» вы также отмечали «$b»."
            }
        }
        InsightPatternType.SEQUENCE -> {
            val a = pattern.tagA!!.displayName(english)
            val b = pattern.tagB!!.displayName(english)
            if (english) {
                "This happened ${pattern.support} of ${pattern.base} times when the next day had an entry."
            } else {
                "Так было в ${pattern.support} из ${pattern.base} раз, когда на следующий день была запись."
            }
        }
        InsightPatternType.STREAK -> {
            val t = pattern.tag!!.displayName(english)
            if (english) {
                "\"$t\" appeared in the last ${pattern.support} entries in a row."
            } else {
                "«$t» встречался в последних ${pattern.support} записях подряд."
            }
        }
        InsightPatternType.RECOVERY_MICRO_WINS -> if (english) {
            "On ${pattern.support} of ${pattern.base} days after a heavier evening, micro-wins were filled in."
        } else {
            "В ${pattern.support} из ${pattern.base} случаев после тяжёлого вечера на следующий день были микро-победы."
        }
        InsightPatternType.DOMINANT_TAG -> {
            val t = pattern.tag!!.displayName(english)
            if (english) {
                "You marked \"$t\" on ${pattern.support} of ${pattern.base} evenings in this period."
            } else {
                "Вы отмечали «$t» в ${pattern.support} из ${pattern.base} вечеров за период."
            }
        }
        InsightPatternType.WEEKEND_AFFINITY -> {
            val t = pattern.tag!!.displayName(english)
            if (english) {
                "On ${pattern.support} of ${pattern.base} weekend evenings you marked \"$t\"."
            } else {
                "В ${pattern.support} из ${pattern.base} вечеров в сб–вс вы отмечали «$t»."
            }
        }
        InsightPatternType.WEEKDAY_AFFINITY -> {
            val t = pattern.tag!!.displayName(english)
            if (english) {
                "On ${pattern.support} of ${pattern.base} weekday evenings you marked \"$t\"."
            } else {
                "В ${pattern.support} из ${pattern.base} вечеров в будни вы отмечали «$t»."
            }
        }
    }
}
