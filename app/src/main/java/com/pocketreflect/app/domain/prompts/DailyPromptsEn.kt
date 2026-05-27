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

import com.pocketreflect.app.domain.model.MoodTag

/**
 * English daily prompts organized by clinical mood tags and fallback.
 */
internal object DailyPromptsEn {

    val CALM = listOf(
        "What image from today’s calm moments are you carrying with you into your sleep?",
        "At what point today did you feel most at peace, just breathing and being?",
        "Where in your body do you feel this evening calm the most right now?",
        "What detail around you right now helps prolong this sense of quiet?",
        "What pleasant sensation from today would you like to keep until morning?",
        "Who or what was it easiest to share a quiet moment with today?"
    )

    val JOY = listOf(
        "What fleeting detail brought a genuine smile to your face today?",
        "Where in your body did today's joy resonate — as warmth, lightness, or movement?",
        "If you could describe today's joy with a single color, which would it be?",
        "What small moment from today would you love to share with someone?",
        "What pleasant thing happened today that was completely unexpected?",
        "Which sound or taste did you let yourself fully enjoy today?"
    )

    val GRATITUDE = listOf(
        "Who among your loved ones or strangers are you silently grateful to today?",
        "What simple object or convenience quietly made your life easier today?",
        "For which kind word or gesture directed at you would you like to say thank you?",
        "What event from today, even the tiniest one, are you glad happened?",
        "What did nature or the city gift you today for inspiration?",
        "What support from others — obvious or unspoken — did you notice today?"
    )

    val FOCUSED = listOf(
        "What is one decision you made today that aligned with your core values?",
        "Which task did you immerse yourself in with the greatest interest and flow?",
        "With what small step today did you lay a foundation for your future?",
        "Which of your actions today brought you a genuine sense of clarity?",
        "From what distraction did you manage to gently protect your attention today?",
        "What important thought helped you maintain your focus throughout the day?"
    )

    val TIRED = listOf(
        "Where in your body is tiredness resting most heavily, asking for care?",
        "What task for this evening do you whole-heartedly give yourself permission to skip?",
        "What gentle comfort could help your body relax right now?",
        "If your fatigue were a soft signal, what would it be inviting you to do?",
        "How can you thank yourself for holding up through this long day?",
        "What thought are you ready to put down until tomorrow to let your mind rest?"
    )

    val OVERWHELMED = listOf(
        "What is one small task you can cross off your to-do list for tomorrow?",
        "What is most important to let go of right now, just to take a breath?",
        "If you simplified this evening to just one basic comfort, what would it be?",
        "What part of today’s burden can you leave to time without feeling guilty?",
        "What of everything happening right now is completely outside your control?",
        "What physical sensation right now brings you back to the present moment?"
    )

    val ANXIETY = listOf(
        "If anxiety is a protective signal, what valuable thing is it trying to guard?",
        "What physical support beneath your body can you feel right now?",
        "What is the worst-case scenario that you can safely postpone thinking about until morning?",
        "If this anxiety were a passing cloud in the sky, which way would the wind blow?",
        "What in this room remains stable and safe right now?",
        "What gentle words would you offer a close friend if they felt this anxious?"
    )

    val SADNESS = listOf(
        "What kind of self-care does your sadness need most of all right now?",
        "Where in your body does this sadness live — as a heavy chest, tears, or quiet?",
        "What warm memory or mental image brings you comfort when you are sad?",
        "What part of today are you grieving having to let go of?",
        "How can you stay on your own side while this feeling passes?",
        "With whom or what did you feel a sense of shared human warmth today?"
    )

    val IRRITATION = listOf(
        "What important boundary of yours did today’s irritation point to?",
        "If your irritation could speak without yelling, what need would it declare?",
        "What in your body tenses up with this anger — and how can you help it soften?",
        "What situation are you ready to mentally close until morning, without fixing it?",
        "Where did your expectations clash with reality today — and can you let it be?",
        "What small action can help you reclaim a sense of control over your evening?"
    )

    val UNIVERSAL = listOf(
        "What image or sound of this evening are you carrying with you into the night?",
        "Which of today's finished tasks left you with a satisfying sense of completion?",
        "What was it most important to stay silent about today to save your energy?",
        "What thought kept spinning in your head today like a broken record?",
        "With what main feeling are you closing this day?",
        "What went off-script today but ended up bringing unexpected relief?",
        "What small discovery about yourself did you make over the course of today?",
        "Where did you feel most fully present and alive today?",
        "What would you like to leave on this page so you don't carry it into tomorrow?",
        "Which conversation today left you with a pleasant, warm aftertaste?",
        "What detail around you did you notice today for the very first time?",
        "What required more patience from you today than you usually need?"
    )

    val POOL: List<String> = buildList {
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

    fun poolFor(tag: MoodTag): List<String> = when (tag) {
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
