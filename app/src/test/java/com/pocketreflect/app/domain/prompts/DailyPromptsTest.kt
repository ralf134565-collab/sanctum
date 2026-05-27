// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.prompts

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Тесты на пул и ротацию [DailyPrompts], а также на политику выбора [MoodPromptPolicy].
 */
class DailyPromptsTest {

    @Test
    fun `english pool has at least 40 prompts without duplicates`() {
        val pool = DailyPrompts.all(AppLanguage.EN)
        assertTrue(pool.size >= 40)
        assertEquals(pool.size, pool.toSet().size)
        pool.forEach { prompt ->
            assertTrue(prompt.trim().endsWith("?"))
        }
    }

    @Test
    fun `pool has at least 40 prompts`() {
        assertTrue(
            "Pool промптов должен быть ≥ 40 после расширения Блока 4",
            DailyPrompts.all().size >= 40,
        )
    }

    @Test
    fun `pool contains no exact duplicate strings`() {
        val pool = DailyPrompts.all()
        val unique = pool.toSet()
        assertEquals(
            "В POOL не должно быть точных текстовых дублей",
            pool.size,
            unique.size,
        )
    }

    @Test
    fun `every prompt is non-blank and ends with question mark`() {
        DailyPrompts.all().forEach { prompt ->
            assertTrue("Промпт не должен быть пустым: '$prompt'", prompt.isNotBlank())
            assertTrue(
                "Промпт должен быть открытым вопросом (заканчиваться на '?'): '$prompt'",
                prompt.trim().endsWith("?"),
            )
        }
    }

    @Test
    fun `random respects single-element history`() {
        val pool = DailyPrompts.all()
        val excluded = pool.first()
        // 50 итераций с фиксированным excluded — ни разу не должно вернуть его.
        repeat(50) {
            val result = DailyPrompts.random(history = setOf(excluded))
            assertNotEquals(
                "random не должен возвращать промпт из history",
                excluded,
                result,
            )
        }
    }

    @Test
    fun `random with 7-item history never returns any of them`() {
        val history = DailyPrompts.all().take(7).toSet()
        // 200 итераций на пуле при history=7 — ни одной коллизии.
        repeat(200) {
            val result = DailyPrompts.random(history = history)
            assertFalse(
                "random не должен возвращать ни один из 7 history-промптов",
                result in history,
            )
        }
    }

    @Test
    fun `random falls back to pool when history exhausts everything`() {
        val everythingInHistory = DailyPrompts.all().toSet()
        // Все промпты «использованы» — fallback должен сработать без NPE и бесконечного цикла.
        val result = DailyPrompts.random(history = everythingInHistory)
        assertTrue(
            "Fallback должен вернуть промпт из POOL даже когда history покрывает всё",
            result in everythingInHistory,
        )
    }

    @Test
    fun `random is deterministic with fixed Random seed`() {
        val seededA = Random(42)
        val seededB = Random(42)
        val a = DailyPrompts.random(history = emptySet(), random = seededA)
        val b = DailyPrompts.random(history = emptySet(), random = seededB)
        assertEquals(
            "Одинаковый seed должен давать одинаковый промпт — нужно для воспроизводимых тестов",
            a,
            b,
        )
    }

    @Test
    fun `100 sequential calls with history filter produce no repeats in first 5`() {
        // Эмулируем работу JournalViewModel: каждый раз обновляем history
        // последними 7 показанными.
        val seen = ArrayDeque<String>(7)
        val firstFive = mutableListOf<String>()
        val rng = Random(System.currentTimeMillis())
        repeat(100) { idx ->
            val historySet = seen.toSet()
            val pick = DailyPrompts.random(history = historySet, random = rng)
            if (idx < 5) firstFive += pick
            seen.addLast(pick)
            if (seen.size > 7) seen.removeFirst()
        }
        assertEquals(
            "Первые 5 промптов должны быть уникальны (history-механика работает)",
            firstFive.toSet().size,
            firstFive.size,
        )
    }

    // --- НОВЫЕ ТЕСТЫ ДЛЯ БЛОКА 4 (Mood-linked prompts) ---

    @Test
    fun `every mood tag has at least 6 unique prompts in Russian and English`() {
        MoodTag.entries.forEach { tag ->
            val ruPool = DailyPrompts.poolForTag(AppLanguage.RU, tag)
            val enPool = DailyPrompts.poolForTag(AppLanguage.EN, tag)

            assertTrue("Тег ${tag.name} (RU) должен иметь >= 6 промптов", ruPool.size >= 6)
            assertTrue("Тег ${tag.name} (EN) должен иметь >= 6 промптов", enPool.size >= 6)

            assertEquals("Тег ${tag.name} (RU) содержит дубликаты", ruPool.size, ruPool.toSet().size)
            assertEquals("Тег ${tag.name} (EN) содержит дубликаты", enPool.size, enPool.toSet().size)
        }
    }

    @Test
    fun `universal pool has at least 12 unique prompts in Russian and English`() {
        val ruUniversal = MoodPromptPolicy.getUniversalPool(AppLanguage.RU)
        val enUniversal = MoodPromptPolicy.getUniversalPool(AppLanguage.EN)

        assertTrue("Universal pool (RU) должен иметь >= 12 промптов", ruUniversal.size >= 12)
        assertTrue("Universal pool (EN) должен иметь >= 12 промптов", enUniversal.size >= 12)

        assertEquals("Universal pool (RU) содержит дубликаты", ruUniversal.size, ruUniversal.toSet().size)
        assertEquals("Universal pool (EN) содержит дубликаты", enUniversal.size, enUniversal.toSet().size)
    }

    @Test
    fun `dominant tag resolution respects clinical priority queue`() {
        // Пустой выбор -> null
        assertNull(MoodPromptPolicy.resolveDominantTag(emptySet()))

        // Только позитивные -> один из позитивных
        val positivesOnly = setOf(MoodTag.JOY, MoodTag.GRATITUDE)
        val resolvedPositive = MoodPromptPolicy.resolveDominantTag(positivesOnly)
        assertTrue(resolvedPositive in positivesOnly)

        // Позитивные + нейтральный -> нейтральный побеждает
        val positiveAndNeutral = setOf(MoodTag.JOY, MoodTag.TIRED)
        val resolvedNeutral = MoodPromptPolicy.resolveDominantTag(positiveAndNeutral)
        assertEquals(MoodTag.TIRED, resolvedNeutral)

        // Нейтральный + негативный -> негативный побеждает
        val neutralAndNegative = setOf(MoodTag.OVERWHELMED, MoodTag.ANXIETY)
        val resolvedNegative = MoodPromptPolicy.resolveDominantTag(neutralAndNegative)
        assertEquals(MoodTag.ANXIETY, resolvedNegative)

        // Позитивный + нейтральный + негативный -> негативный побеждает
        val allTypes = setOf(MoodTag.CALM, MoodTag.TIRED, MoodTag.SADNESS)
        val resolvedAll = MoodPromptPolicy.resolveDominantTag(allTypes)
        assertEquals(MoodTag.SADNESS, resolvedAll)
    }

    @Test
    fun `never returns a savor-only prompt when negative tag is active`() {
        val selected = setOf(MoodTag.ANXIETY, MoodTag.JOY) // Должен разрешиться в ANXIETY
        val ruCalmPool = DailyPrompts.poolForTag(AppLanguage.RU, MoodTag.CALM)
        val ruJoyPool = DailyPrompts.poolForTag(AppLanguage.RU, MoodTag.JOY)
        val ruGratitudePool = DailyPrompts.poolForTag(AppLanguage.RU, MoodTag.GRATITUDE)
        val ruSavorPools = ruCalmPool + ruJoyPool + ruGratitudePool

        repeat(100) {
            val prompt = DailyPrompts.forContext(
                language = AppLanguage.RU,
                selectedTags = selected,
                history = emptySet()
            )
            // Промпт не должен быть из пулов радости, благодарности или спокойствия
            assertFalse(
                "При тревоге выбран savoring-промпт: '$prompt'",
                prompt in ruSavorPools
            )
        }
    }

    @Test
    fun `forContext respects history exclusion`() {
        val selected = setOf(MoodTag.CALM)
        val tagPool = DailyPrompts.poolForTag(AppLanguage.RU, MoodTag.CALM)
        
        // Помещаем все промпты CALM, кроме одного, в историю
        val excluded = tagPool.first()
        val history = tagPool.drop(1).toSet()

        repeat(50) {
            val prompt = DailyPrompts.forContext(
                language = AppLanguage.RU,
                selectedTags = selected,
                history = history
            )
            assertEquals("Должен вернуться единственный оставшийся вне истории промпт", excluded, prompt)
        }
    }

    @Test
    fun `forContext falls back to polarity pool then universal then full pool when tag pool is exhausted`() {
        val selected = setOf(MoodTag.CALM)
        val tagPool = DailyPrompts.poolForTag(AppLanguage.RU, MoodTag.CALM)
        
        // Полностью исчерпываем историю для конкретного тега CALM
        val historyExhaustingTag = tagPool.toSet()

        // Проверяем, что вернётся промпт другой позитивной полярности (например, из JOY, GRATITUDE или FOCUSED), которого нет в истории
        val promptFromPolarity = DailyPrompts.forContext(
            language = AppLanguage.RU,
            selectedTags = selected,
            history = historyExhaustingTag
        )
        assertFalse(promptFromPolarity in historyExhaustingTag)
        
        val ruPositivePool = MoodPromptPolicy.getPoolForPolarity(AppLanguage.RU, MoodTag.Polarity.POSITIVE)
        assertTrue(promptFromPolarity in ruPositivePool)

        // Теперь исчерпываем всю полярность POSITIVE
        val historyExhaustingPolarity = ruPositivePool.toSet()
        val promptFromUniversal = DailyPrompts.forContext(
            language = AppLanguage.RU,
            selectedTags = selected,
            history = historyExhaustingPolarity
        )
        assertFalse(promptFromUniversal in historyExhaustingPolarity)
        val ruUniversalPool = MoodPromptPolicy.getUniversalPool(AppLanguage.RU)
        assertTrue(promptFromUniversal in ruUniversalPool)

        // И наконец исчерпываем всю историю полностью (включая универсальный пул)
        val historyExhaustingAll = (ruPositivePool + ruUniversalPool).toSet()
        val promptFromFullPool = DailyPrompts.forContext(
            language = AppLanguage.RU,
            selectedTags = selected,
            history = historyExhaustingAll
        )
        // Должно вернуть хоть какой-то валидный промпт из полного пула (безопасный абсолютный fallback)
        assertTrue(promptFromFullPool.isNotBlank())
    }
}
