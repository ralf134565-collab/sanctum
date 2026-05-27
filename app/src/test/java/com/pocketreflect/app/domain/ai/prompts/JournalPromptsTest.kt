// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ai.prompts

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты на [JournalPrompts] — структура входа для Gemma-инференса.
 *
 * Ключевые инварианты, защищаемые этими тестами:
 *  1. `tomorrowTasks` **попадает** в промпт когда заполнен (главный фикс PR).
 *  2. `tomorrowTasks` стоит **выше** рефлексии и микро-побед — это hot-zone
 *     attention'а, без этого порядка модель эмпирически игнорирует поле.
 *  3. Пустые поля **не** попадают в промпт (иначе модель «дополняет»
 *     отсутствующее).
 *  4. SYSTEM_INSTRUCTION содержит ключевые тон-якоря и негативный список клише.
 */
class JournalPromptsTest {

    private fun entry(
        tags: List<MoodTag> = emptyList(),
        microWins: String = "",
        tomorrowTasks: String = "",
        customFieldQuestion: String = "",
        customFieldAnswer: String = "",
        reflection: String = "",
        promptShown: String = "",
    ) = JournalEntry(
        timestamp = 0L,
        dayBucket = "2026-05-19",
        moodTags = tags,
        microWins = microWins,
        tomorrowTasks = tomorrowTasks,
        reflection = reflection,
        promptShown = promptShown,
        aiReflection = null,
        customFieldQuestion = customFieldQuestion,
        customFieldAnswer = customFieldAnswer,
    )

    @Test
    fun `buildPromptInput includes tomorrowTasks when not blank`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.ANXIETY),
                tomorrowTasks = "созвон с командой\nотчёт",
            ),
        )
        assertTrue(
            "Должен быть строка 'Задачи на завтра:' с самим текстом",
            input.contains("Задачи на завтра: созвон с командой\nотчёт"),
        )
    }

    @Test
    fun `buildPromptInput omits tomorrowTasks when blank`() {
        val input = JournalPrompts.buildPromptInput(
            entry(tags = listOf(MoodTag.JOY), tomorrowTasks = "   "),
        )
        assertFalse(
            "Пустые задачи не должны попадать в промпт",
            input.contains("Задачи на завтра"),
        )
    }

    @Test
    fun `buildPromptInput places tomorrowTasks before reflection`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.TIRED),
                tomorrowTasks = "разобрать почту",
                reflection = "устал, день был длинный",
            ),
        )
        val tasksIdx = input.indexOf("Задачи на завтра:")
        val reflectionIdx = input.indexOf("Рефлексия пользователя:")
        assertTrue("Оба поля должны присутствовать", tasksIdx >= 0 && reflectionIdx >= 0)
        assertTrue(
            "tomorrowTasks обязан стоять выше reflection (hot-zone attention'а)",
            tasksIdx < reflectionIdx,
        )
    }

    @Test
    fun `buildPromptInput places tomorrowTasks before microWins`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.JOY),
                tomorrowTasks = "забрать вещи из химчистки",
                microWins = "позвонил отцу",
            ),
        )
        val tasksIdx = input.indexOf("Задачи на завтра:")
        val winsIdx = input.indexOf("Микро-победы:")
        assertTrue(tasksIdx >= 0 && winsIdx >= 0)
        assertTrue(
            "tomorrowTasks обязан стоять выше microWins",
            tasksIdx < winsIdx,
        )
    }

    @Test
    fun `buildPromptInput includes final instruction referencing tomorrowTasks`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.ANXIETY),
                tomorrowTasks = "созвон",
            ),
        )
        assertTrue(
            "Финальная инструкция должна явно требовать отражения задач во 2-м предложении",
            input.contains("задачи на завтра"),
        )
    }

    @Test
    fun `buildPromptInput includes custom field when question and answer present`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.JOY),
                tomorrowTasks = "созвон",
                customFieldQuestion = "За что благодарен?",
                customFieldAnswer = "за тишину утром",
            ),
        )
        assertTrue(
            input.contains("За что благодарен?: <user_data>за тишину утром</user_data>"),
        )
    }

    @Test
    fun `buildPromptInput omits custom field when answer blank`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.JOY),
                customFieldQuestion = "За что благодарен?",
                customFieldAnswer = "   ",
            ),
        )
        assertFalse(input.contains("За что благодарен?"))
    }

    @Test
    fun `buildPromptInput places custom field after tomorrowTasks and before prompt`() {
        val input = JournalPrompts.buildPromptInput(
            entry(
                tags = listOf(MoodTag.TIRED),
                tomorrowTasks = "разобрать почту",
                customFieldQuestion = "Что помогло?",
                customFieldAnswer = "прогулка",
                promptShown = "Как прошёл день?",
                reflection = "устал",
            ),
        )
        val tasksIdx = input.indexOf("Задачи на завтра:")
        val customIdx = input.indexOf("Что помогло?:")
        val promptIdx = input.indexOf("Промпт дня:")
        assertTrue(tasksIdx >= 0 && customIdx >= 0 && promptIdx >= 0)
        assertTrue(tasksIdx < customIdx)
        assertTrue(customIdx < promptIdx)
    }

    @Test
    fun `buildPromptInput marks polarity based on negative tag`() {
        val negative = JournalPrompts.buildPromptInput(entry(tags = listOf(MoodTag.ANXIETY)))
        val positive = JournalPrompts.buildPromptInput(entry(tags = listOf(MoodTag.JOY)))
        val neutral = JournalPrompts.buildPromptInput(entry(tags = listOf(MoodTag.TIRED)))

        assertTrue("Должен распознать негативную тональность", negative.contains("Тональность: негативный"))
        assertTrue("Должен распознать позитивную тональность", positive.contains("Тональность: позитивный"))
        assertTrue("TIRED — нейтральный (см. MoodTag.polarity)", neutral.contains("Тональность: нейтральный"))
    }

    @Test
    fun `system instruction contains anti-cliche guardrails`() {
        val sys = JournalPrompts.SYSTEM_INSTRUCTION
        listOf("ты молодец", "всё наладится", "позаботься о себе", "ты заслужил отдых").forEach { cliche ->
            assertTrue(
                "SYSTEM_INSTRUCTION должна явно запрещать клише '$cliche'",
                sys.contains(cliche),
            )
        }
    }

    @Test
    fun `system instruction contains tomorrowTasks-specific rule`() {
        val sys = JournalPrompts.SYSTEM_INSTRUCTION
        assertTrue(
            "SYSTEM_INSTRUCTION должна содержать спец-правило про «Задачи на завтра»",
            sys.contains("Задачи на завтра") && sys.contains("планирования"),
        )
    }

    @Test
    fun `system instruction contains few-shot examples`() {
        val sys = JournalPrompts.SYSTEM_INSTRUCTION
        assertTrue(
            "SYSTEM_INSTRUCTION должна содержать секцию ПРИМЕРОВ для Gemma",
            sys.contains("ПРИМЕРЫ КАЧЕСТВЕННЫХ ОТКЛИКОВ"),
        )
        assertTrue("Должно быть как минимум 3 примера", sys.split("Пример ").size >= 4)
    }

    @Test
    fun `buildPromptInput omits manifesto when null`() {
        val input = JournalPrompts.buildPromptInput(
            entry(tags = listOf(MoodTag.JOY)),
            personalManifesto = null,
        )
        assertFalse(input.contains("Личные ориентиры пользователя"))
    }

    @Test
    fun `buildPromptInput includes manifesto when provided`() {
        val input = JournalPrompts.buildPromptInput(
            entry(tags = listOf(MoodTag.JOY)),
            personalManifesto = "после 21:00 телефон не со мной",
        )
        assertTrue(input.contains("Личные ориентиры пользователя"))
        assertTrue(input.contains("<user_data>после 21:00 телефон не со мной</user_data>"))
    }

    @Test
    fun `buildSummaryInput includes manifesto when provided`() {
        val input = JournalPrompts.buildSummaryInput(
            entries = listOf(entry(tags = listOf(MoodTag.TIRED))),
            personalManifesto = "я склонен к переработкам",
        )
        assertTrue(input.contains("Личные ориентиры пользователя"))
        assertTrue(input.contains("<user_data>я склонен к переработкам</user_data>"))
    }

    @Test
    fun `buildSummaryInput omits manifesto when null`() {
        val input = JournalPrompts.buildSummaryInput(
            entries = listOf(entry(tags = listOf(MoodTag.TIRED))),
            personalManifesto = null,
        )
        assertFalse(input.contains("Личные ориентиры пользователя"))
    }
}
