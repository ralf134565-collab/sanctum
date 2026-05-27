// SPDX-License-Identifier: GPL-3.0-or-later
/*
 * Copyright (c) 2026 Viacheslav Zhukov
 *
 * Окружающий Kotlin-код этого файла распространяется по лицензии
 * GPL-3.0-or-later (см. ./LICENSE в корне репозитория).
 *
 * Однако СОДЕРЖИМОЕ строковых корпусов (системные промпты, few-shot
 * примеры, мок-пулы откликов, тексты «промптов дня») — это
 * самостоятельные литературные произведения. Они НЕ являются частью
 * GPL-распространения и охраняются авторским правом отдельно
 * (ст. 1259 ГК РФ; Berne Convention, art. 2).
 *
 * Разрешено:
 *  - использовать тексты в личных, исследовательских и образовательных целях;
 *  - цитировать с указанием авторства в обзорах и научных публикациях.
 *
 * Запрещено без письменного разрешения:
 *  - воспроизводить корпуса целиком или фрагментами в других приложениях;
 *  - использовать как обучающий датасет;
 *  - адаптировать (translate/rewrite) с сохранением смысловой структуры.
 *
 * Контакт: ralf.134565@gmail.com
 */
package com.pocketreflect.app.domain.prompts

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.model.MoodTag
import kotlin.random.Random

/**
 * Источник «промптов дня» — коротких вопросов, которые мягко смещают фокус
 * с «оценить день» на «зафиксировать наблюдение».
 *
 * Начиная с версии Блока 4, промпты связаны с аффективными тегами (Блок 1)
 * через гибридный контекстно-зависимый выбор.
 */
object DailyPrompts {

    /** Спокойствие (CALM) — Savoring & Somatic Anchoring. */
    val CALM: List<String> = listOf(
        "Какой образ из сегодняшнего спокойного момента вы уносите с собой в сон?",
        "В какой момент дня вам было спокойнее всего просто дышать и быть?",
        "Где сейчас в теле больше всего ощущается это вечернее спокойствие?",
        "Какая деталь вокруг вас сейчас помогает продлить это чувство тишины?",
        "Какое приятное ощущение от сегодняшнего дня хочется сохранить до утра?",
        "С кем или с чем сегодня было спокойнее всего разделять тишину?",
    )

    /** Радость (JOY) — Savoring & Positive Amplification. */
    val JOY: List<String> = listOf(
        "Какая мимолётная деталь сегодня вызвала у вас искреннюю улыбку?",
        "Где в теле отозвалась сегодняшняя радость — теплом, легкостью или движением?",
        "Если бы сегодняшнюю радость можно было описать цветом, какой бы вы выбрали?",
        "Каким маленьким моментом сегодняшнего дня вам хотелось бы поделиться?",
        "Что приятное случилось сегодня совершенно неожиданно для вас?",
        "Какому звуку или вкусу вы сегодня позволили себя порадовать?",
    )

    /** Благодарность (GRATITUDE) — Savoring & Interpersonal Connection. */
    val GRATITUDE: List<String> = listOf(
        "Кому из близких или случайных прохожих вы мысленно благодарны сегодня?",
        "Какая вещь или удобство сегодня незаметно облегчили вам жизнь?",
        "За какое бережное слово или жест в ваш адрес хочется сказать спасибо?",
        "Какому событию этого дня, даже самому крошечному, вы рады?",
        "Что природа или городская среда подарили вам сегодня для вдохновения?",
        "Какую поддержку от других — явную или негласную — вы сегодня заметили?",
    )

    /** Сфокусированность (FOCUSED) — ACT Value-Alignment & Closeness. */
    val FOCUSED: List<String> = listOf(
        "Какое одно решение сегодня вы приняли, опираясь на свои ценности?",
        "В какое дело сегодня вы погрузились с наибольшим интересом и отдачей?",
        "Каким маленьким шагом сегодня вы заложили основу для будущих дней?",
        "Что из сделанного сегодня принесло вам честное чувство ясности?",
        "От какого отвлекающего фактора вам удалось мягко уберечь свое внимание?",
        "Какая важная мысль помогала вам сохранять фокус в течение дня?",
    )

    /** Усталость (TIRED) — Self-Compassion & Somatic Validation. */
    val TIRED: List<String> = listOf(
        "Где в теле сейчас сильнее всего копится усталость и просит заботы?",
        "Какое дело на этот вечер вы с легким сердцем разрешаете себе не делать?",
        "Какая бережная мелочь помогла бы вашему телу расслабиться прямо сейчас?",
        "Если бы ваша усталость была мягким сигналом, к чему бы она вас призывала?",
        "Как вы можете поблагодарить себя за то, что выдержали этот длинный день?",
        "Какую мысль вы готовы отпустить до завтра, чтобы дать голове отдохнуть?",
    )

    /** Перегруз (OVERWHELMED) — Boundaries & Cognitive Offloading. */
    val OVERWHELMED: List<String> = listOf(
        "Какое одно маленькое действие вы можете вычеркнуть из списка дел на завтра?",
        "Что сейчас важнее всего оставить в покое и просто выдохнуть?",
        "Если упростить этот вечер до одной простой задачи, какой бы она была?",
        "Какую часть сегодняшней ноши вы можете без вины перепоручить времени?",
        "Что из происходящего сейчас находится вне вашего прямого контроля?",
        "Какое физическое ощущение прямо сейчас возвращает вас в настоящий момент?",
    )

    /** Тревога (ANXIETY) — Grounding & Defusion. */
    val ANXIETY: List<String> = listOf(
        "Если тревога — это сигнал защиты, что ценное она пытается уберечь?",
        "Какая простая опора под вашим телом ощущается прямо сейчас?",
        "Какое самое неприятное последствие тревожной мысли вы можете отложить до утра?",
        "Если бы эта тревога была временным облаком в небе, куда бы дул ветер?",
        "Что в этой комнате прямо сейчас остается стабильным и безопасным?",
        "Какое бережное слово вы сказали бы близкому, если бы он так же тревожился?",
    )

    /** Грусть (SADNESS) — Somatic Acceptance & Self-Compassion. */
    val SADNESS: List<String> = listOf(
        "Какое бережное отношение к себе сейчас нужнее всего вашей грусти?",
        "Где в теле живет эта грусть — тяжестью в груди, слезами или тишиной?",
        "Какое теплое воспоминание или образ греют вас, когда вам грустно?",
        "Какую часть этого дня вы грустите, что пришлось отпустить?",
        "Как вы можете побыть на своей стороне, пока это чувство не пройдет?",
        "Кому или чему сегодня хотелось бы сопереживать?",
    )

    /** Раздражение (IRRITATION) — Boundaries & Cognitive Defusion. */
    val IRRITATION: List<String> = listOf(
        "Какую важную границу сегодня задело ваше раздражение?",
        "Если бы раздражение могло говорить без крика, о какой потребности оно бы заявило?",
        "Что в теле сжимается при этой злости — и как помочь ему расслабиться?",
        "Какую ситуацию вы готовы мысленно закрыть до утра, не пытаясь исправить?",
        "Где сегодня ожидания не совпали с реальностью — и можете ли вы это принять?",
        "Какое маленькое действие поможет вам вернуть контроль над своим вечером?",
    )

    /** Универсальные / Fallback промпты. */
    val UNIVERSAL: List<String> = listOf(
        "Какой образ или звук сегодняшнего вечера вы уносите с собой в ночь?",
        "Какое дело сегодня завершилось, оставив приятную точку?",
        "О чём сегодня было важнее всего промолчать, чтобы сберечь силы?",
        "Какая мысль сегодня крутилась в голове, как заезженная пластинка?",
        "С каким главным чувством вы закрываете этот день?",
        "Что сегодня шло не по плану, но в итоге принесло облегчение?",
        "Какое маленькое открытие о себе вы сделали за сегодняшний день?",
        "Где сегодня было больше всего вашего живого присутствия?",
        "Что на этой странице вы хотите оставить, чтобы не нести в завтрашний день?",
        "Какой разговор сегодня оставил приятное послевкусие?",
        "На какую деталь вокруг себя вы сегодня обратили внимание впервые?",
        "Что сегодня потребовало от вас больше терпения, чем обычно?",
    )

    /** Полный плоский пул для обратной совместимости и тестов. */
    private val POOL: List<String> = buildList {
        addAll(CALM)
        addAll(JOY)
        addAll(GRATITUDE)
        addAll(FOCUSED)
        addAll(TIRED)
        addAll(OVERWHELMED)
        addAll(ANXIETY)
        addAll(SADNESS)
        addAll(IRRITATION)
        addAll(UNIVERSAL)
    }

    /**
     * Контекстно-зависимый выбор промпта с учётом выбранных тегов и истории.
     */
    fun forContext(
        language: AppLanguage,
        selectedTags: Set<MoodTag>,
        history: Set<String>,
        random: Random = Random.Default
    ): String {
        val dominantTag = MoodPromptPolicy.resolveDominantTag(selectedTags, random)

        // 1. Пытаемся выбрать из пула доминантного тега
        if (dominantTag != null) {
            val tagPool = MoodPromptPolicy.getPoolForTag(language, dominantTag)
            val availableFromTag = tagPool.filterNot { it in history }
            if (availableFromTag.isNotEmpty()) {
                return availableFromTag[random.nextInt(availableFromTag.size)]
            }

            // 2. Если пул тега исчерпан историей, ищем по полярности тега
            val polarityPool = MoodPromptPolicy.getPoolForPolarity(language, dominantTag.polarity)
            val availableFromPolarity = polarityPool.filterNot { it in history }
            if (availableFromPolarity.isNotEmpty()) {
                return availableFromPolarity[random.nextInt(availableFromPolarity.size)]
            }
        }

        // 3. Fallback на универсальный пул
        val universalPool = MoodPromptPolicy.getUniversalPool(language)
        val availableFromUniversal = universalPool.filterNot { it in history }
        if (availableFromUniversal.isNotEmpty()) {
            return availableFromUniversal[random.nextInt(availableFromUniversal.size)]
        }

        // 4. Абсолютный fallback — случайный из всего языкового пула (игнорируем историю во избежание сбоев)
        val fullPool = MoodPromptPolicy.getFullLanguagePool(language)
        return fullPool[random.nextInt(fullPool.size)]
    }

    /**
     * Случайный промпт с учётом истории показов.
     * Оставлен для обратной совместимости, вызывает контекстно-зависимый выбор с пустыми тегами.
     */
    fun random(
        language: AppLanguage = AppLanguage.RU,
        history: Set<String> = emptySet(),
        random: Random = Random.Default,
    ): String {
        return forContext(language, emptySet(), history, random)
    }

    /** Полный пул — для тестов и debug-инспекции. */
    fun all(language: AppLanguage = AppLanguage.RU): List<String> = poolFor(language)

    private fun poolFor(language: AppLanguage): List<String> =
        if (language.isEnglish) DailyPromptsEn.POOL else POOL

    fun poolForTag(language: AppLanguage, tag: MoodTag): List<String> =
        if (language.isEnglish) DailyPromptsEn.poolFor(tag) else when (tag) {
            MoodTag.CALM -> CALM
            MoodTag.JOY -> JOY
            MoodTag.GRATITUDE -> GRATITUDE
            MoodTag.FOCUSED -> FOCUSED
            MoodTag.TIRED -> TIRED
            MoodTag.OVERWHELMED -> OVERWHELMED
            MoodTag.ANXIETY -> ANXIETY
            MoodTag.SADNESS -> SADNESS
            MoodTag.IRRITATION -> IRRITATION
        }
}
