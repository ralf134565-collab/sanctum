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
package com.pocketreflect.app.data.ai.mock

import com.pocketreflect.app.core.locale.AppLanguage

data class MockGemmaTextPools(
    val gentleMentorChat: List<String>,
    val friendChat: List<String>,
    val coachChat: List<String>,
    val freeDialogChat: List<String>,
    val journalHint: String,
    val echoLong: String,
    val anxiety: List<String>,
    val sadness: List<String>,
    val irritation: List<String>,
    val tired: List<String>,
    val generalSupport: List<String>,
    val joy: List<String>,
    val gratitude: List<String>,
    val calm: List<String>,
    val focused: List<String>,
    val neutral: List<String>,
) {
    companion object {
        fun forLanguage(language: AppLanguage): MockGemmaTextPools =
            if (language.isEnglish) EN else RU

        private val RU = MockGemmaTextPools(
        gentleMentorChat = listOf(
            "Слышу вас. То, о чём вы пишете, имеет вес — не обязательно сейчас что-то с этим делать. " +
                "Можно просто побудьте рядом с этой мыслью ещё минуту.",
            "Похоже, внутри много всего сразу. Это не слабость — это сигнал, что вам важно быть услышанным. " +
                "Я здесь, без спешки.",
            "Спасибо, что делитесь. Не нужно «правильно» формулировать — достаточно того, что вы написали.",
        ),
        friendChat = listOf(
            "Эй, я рядом. Звучит так, будто день выдался непростой — хочется просто выговориться?",
            "Понимаю. Иногда достаточно сказать вслух (хоть экрану), чтобы стало чуть легче дышать.",
            "Ты не один с этим. Давай без героизма — что сейчас больше всего давит?",
        ),
        coachChat = listOf(
            "Уже хорошо, что вы это заметили и написали. Какой один маленький шаг завтра был бы реалистичным — " +
                "не идеальным, а посильным?",
            "Звучит как момент, где много энергии уходит в ожидание. Что из сегодняшнего вы бы точно повторили?",
            "Есть опора в том, что вы здесь и формулируете. Что уже получилось, даже если это «просто дойти до вечера»?",
        ),
        freeDialogChat = listOf(
            "Интересно. А что для вас главное в этом сейчас?",
            "Понял. Хотите развернуть мысль подробнее?",
            "Слушаю. Что бы вы хотели, чтобы я уточнил?",
        ),
        journalHint = " Вижу, что вы ведёте дневник — это уже опора.",
        echoLong = " То, что вы написали, звучит важно.",
        anxiety = listOf(
            "Тревога громкая, но временная. Она не ваш характер — это сигнал тела о том, " +
                "что было слишком много. Можно сделать один медленный выдох и заметить " +
                "опору под ногами.",
            "Если тревога не уходит к ночи — это не ваша вина. Мысли о завтра иногда " +
                "становятся плотнее именно перед сном. Можно мысленно отложить их на лист " +
                "и сказать им «утром».",
            "Тревога просит внимания, а не решения. Сейчас не нужно с ней «справляться» — " +
                "достаточно её заметить и сделать один медленный выдох.",
            "Когда внутри шумно, помогает простое: пять предметов, которые видите прямо " +
                "сейчас. Это не побеждает тревогу, но возвращает в эту комнату.",
            "Тревога часто звучит «срочно», но редко правда срочно. Между ощущением и " +
                "действием есть зазор — туда можно положить один вдох и один выдох.",
            "То, что вы дошли до этой записи в тревожный день — уже навык. Многие сейчас " +
                "бы листали ленту, а вы выбрали посмотреть внутрь.",
            "Тревога — это тело, которое говорит «много». Не обязательно её разгадывать " +
                "прямо сейчас. Иногда достаточно просто признать: да, сегодня было много.",
        ),
        sadness = listOf(
            "Грусть просит, чтобы её заметили, а не починили. Сейчас не нужно никуда " +
                "бежать — я рядом, и день уже завершён.",
            "Сегодня можно ничего не достигать. Достаточно того, что вы дошли до этого " +
                "экрана и нашли минуту, чтобы посмотреть внутрь.",
            "Грусть бывает тихим знаком: что-то важное ушло или не случилось. Не " +
                "обязательно искать причину — можно просто побыть с ней рядом.",
            "Когда грустно, помогает не разговор с собой, а присутствие рядом с собой. " +
                "Вы это и делаете — это уже забота.",
            "Грусть не всегда уходит «по плану». Иногда она просто становится тише. " +
                "Сегодня можно ей позволить быть.",
        ),
        irritation = listOf(
            "Раздражение — сигнал, что какая-то ваша граница была сегодня нарушена. " +
                "Не обязательно искать виноватого — достаточно её заметить.",
            "Иногда лучшая реакция на раздражение — медленный длинный выдох. Не " +
                "подавление, а просто пауза перед следующим действием.",
            "Раздражение часто маскирует усталость, которую не разрешили себе признать. " +
                "Возможно, дело не в людях вокруг, а в ресурсе внутри.",
            "Если внутри шумно, не обязательно сразу это «обсудить». Иногда вечер — " +
                "это время, когда лучше просто закрыть дверь и побыть в тишине.",
            "Раздражение — это тоже информация о ваших границах. Хорошо, что вы её " +
                "замечаете, а не подавляете.",
        ),
        tired = listOf(
            "Усталость — честный ответ тела. Не наказывайте себя за неё — это просто " +
                "факт, не оценка.",
            "Сегодня вы и так сделали достаточно. Пусть завтрашний список будет короче, " +
                "чем привычка.",
            "Перегруз — это не «слабость», а признак того, что мощности кончились. " +
                "Можно дать им восстановиться, ничего не объясняя себе.",
            "Когда внутри «слишком много», помогает не «собраться», а наоборот — " +
                "разрешить вечеру быть пустым.",
            "Усталость к вечеру — не повод оценить день как «провальный». Многое из " +
                "сделанного станет видно только утром.",
            "Если хочется ничего не делать — это не лень, это разумный ответ системы. " +
                "Иногда отдых — самая продуктивная задача дня.",
            "Перегруз говорит о том, что границы давно были тоньше, чем хотелось бы. " +
                "Не обязательно их сейчас перестраивать — сначала просто заметить.",
        ),
        generalSupport = listOf(
            "Я заметил тяжёлый оттенок дня. Вы здесь, со своими записями — это уже " +
                "забота о себе.",
            "В сложные дни сам факт «прийти и записать» становится опорой. Вы её " +
                "только что себе создали.",
            "Когда всё разом — лучшее, что можно сделать, это не решать всё разом. " +
                "Сегодня достаточно остановиться.",
            "Тяжёлый день — не приговор завтрашнему. Он просто закрывается этой записью.",
            "В дни, когда внутри много всего, важнее всего не оценить себя, а заметить " +
                "себя. Вы это сделали.",
        ),
        joy = listOf(
            "Запомните эту радость как опору. В трудные дни к ней можно будет мысленно " +
                "вернуться.",
            "Радость — это навык внимания. Вы только что им воспользовались — заметили " +
                "хорошее и зафиксировали.",
            "Хорошие дни не случайны — они становятся такими в том числе потому, что " +
                "их замечают. Вы это сделали.",
            "Можно позволить радости побыть подольше, не торопясь к следующему. Это и " +
                "есть savoring — практика, а не случайность.",
            "Сегодняшнее ощущение можно мысленно «положить в карман» — на будущее, " +
                "когда будет нужно.",
        ),
        gratitude = listOf(
            "Благодарность сегодня — будто закладка в книге дня. Завтра по ней будет " +
                "легче открыть нужную страницу.",
            "Замечать, за что благодарен — это тренировка внимания. Чем чаще, тем " +
                "точнее становится взгляд.",
            "Сегодняшняя благодарность не отменяет тяжёлого — она просто стоит рядом. " +
                "Это нормально, что внутри бывает и то, и другое.",
            "За что благодарны сегодня — то и становится опорой завтра. Не обязательно " +
                "громко — достаточно, что вы это заметили.",
            "Благодарность — это способ остаться в контакте с реальностью, в которой " +
                "действительно есть хорошее.",
        ),
        calm = listOf(
            "Спокойствие — это не отсутствие чувств, а согласие быть с тем, что есть. " +
                "У вас сегодня получилось.",
            "Ровные дни кажутся «обычными», но именно они — фундамент. Не каждый день " +
                "обязан быть ярким.",
            "Спокойствие сегодня — навык, который не виден со стороны. А вы его только " +
                "что применили.",
            "Когда внутри тихо, можно просто побыть с этой тишиной. Без задач, без " +
                "«надо что-то с этим сделать».",
            "Ровное состояние — это и есть ресурс. Запомните его таким, какое оно " +
                "есть сейчас.",
        ),
        focused = listOf(
            "Фокус сегодня — ваш ресурс. Завтра он восстановится быстрее, если ночью " +
                "отпустите задачи.",
            "Сосредоточенность — короткий и дорогой ресурс. Хорошо, что сегодня его " +
                "удалось куда-то приложить.",
            "После сфокусированного дня важно не «продлевать» его в вечер. Можно " +
                "мягко переключиться в режим отдыха.",
            "Когда получилось сосредоточиться — это всегда сочетание усилия и " +
                "обстоятельств. Не обязательно вам этому учить себя дальше.",
            "Сфокусированный день — повод заметить, что у вас есть навык внимания. " +
                "Он работает, даже когда устаёт.",
        ),
        neutral = listOf(
            "День завершён. Спасибо, что нашли минуту на эту запись.",
            "Иногда день — это просто день. И это нормально — не каждый из них должен " +
                "быть «о чём-то».",
            "Ровный день — тоже часть жизни. Он не обязан быть значимым, чтобы быть " +
                "прожитым.",
            "Сам факт, что вы сейчас здесь, в этой записи — это и есть забота о себе. " +
                "Без громких слов.",
            "Закройте этот день мягко — не подведением итогов, а просто тем, что он " +
                "закончился.",
        ),
    )

    private val EN = MockGemmaTextPools(
        gentleMentorChat = listOf(
            "I hear you. What you're writing about matters — you don't have to do anything with it right now. " +
                "You can simply stay with this thought for another minute.",
            "It sounds like there's a lot going on inside at once. That's not weakness — it's a sign that being heard matters to you. " +
                "I'm here, without rushing.",
            "Thank you for sharing. You don't need to phrase it \"correctly\" — what you wrote is enough.",
        ),
        friendChat = listOf(
            "Hey, I'm here. Sounds like today was tough — do you just need to get it off your chest?",
            "I get it. Sometimes saying it out loud (even to a screen) makes it a little easier to breathe.",
            "You're not alone in this. No heroics — what's weighing on you most right now?",
        ),
        coachChat = listOf(
            "It's already good that you noticed this and wrote it down. What's one small step for tomorrow that would be realistic — " +
                "not perfect, but doable?",
            "Sounds like a moment where a lot of energy goes into waiting. What from today would you definitely repeat?",
            "There's support in the fact that you're here and putting it into words. What already worked out, even if it's \"just making it to evening\"?",
        ),
        freeDialogChat = listOf(
            "Interesting. What matters most to you about this right now?",
            "Got it. Would you like to expand on that thought?",
            "I'm listening. What would you like me to clarify?",
        ),
        journalHint = " I see you're keeping a journal — that's already a support.",
        echoLong = " What you wrote sounds important.",
        anxiety = listOf(
            "Anxiety is loud, but temporary. It's not your character — it's your body signaling that there was too much. " +
                "You can take one slow exhale and notice the ground under your feet.",
            "If anxiety won't let up at night — that's not your fault. Thoughts about tomorrow sometimes get denser right before sleep. " +
                "You can mentally put them on a list and tell them \"tomorrow.\"",
            "Anxiety asks for attention, not a solution. You don't need to \"fix\" it right now — " +
                "noticing it and taking one slow breath is enough.",
            "When it's noisy inside, something simple helps: five objects you can see right now. " +
                "It doesn't defeat anxiety, but it brings you back to this room.",
            "Anxiety often sounds \"urgent,\" but it's rarely truly urgent. Between the feeling and action there's a gap — " +
                "you can put one inhale and one exhale there.",
            "That you reached this entry on an anxious day is already a skill. Many people would be scrolling a feed right now; " +
                "you chose to look inward.",
            "Anxiety is the body saying \"too much.\" You don't have to decode it right now. " +
                "Sometimes it's enough to acknowledge: yes, today was a lot.",
        ),
        sadness = listOf(
            "Sadness asks to be noticed, not fixed. You don't need to run anywhere right now — " +
                "I'm here, and the day is already ending.",
            "Today you don't have to achieve anything. It's enough that you reached this screen and found a minute to look inward.",
            "Sadness can be a quiet sign that something important left or didn't happen. " +
                "You don't have to find the reason — you can simply stay with it.",
            "When you're sad, what helps isn't talking yourself through it, but being present with yourself. " +
                "That's what you're doing — that's already care.",
            "Sadness doesn't always leave \"on schedule.\" Sometimes it just grows quieter. " +
                "Today you can let it be.",
        ),
        irritation = listOf(
            "Irritation is a signal that a boundary of yours was crossed today. " +
                "You don't have to find someone to blame — noticing it is enough.",
            "Sometimes the best response to irritation is a slow, long exhale. Not suppression — " +
                "just a pause before the next action.",
            "Irritation often masks tiredness you haven't allowed yourself to admit. " +
                "Maybe it's not about the people around you, but the resource inside.",
            "When it's noisy inside, you don't have to \"discuss\" it right away. Sometimes evening is " +
                "when it's better to close the door and be in silence.",
            "Irritation is also information about your boundaries. It's good that you notice it instead of pushing it down.",
        ),
        tired = listOf(
            "Tiredness is an honest answer from the body. Don't punish yourself for it — " +
                "it's a fact, not a verdict.",
            "You've already done enough today. Let tomorrow's list be shorter than habit.",
            "Overwhelm isn't \"weakness\" — it's a sign that capacity ran out. " +
                "You can let it recover without explaining yourself.",
            "When there's \"too much\" inside, what helps isn't \"pulling yourself together,\" but the opposite — " +
                "allowing the evening to be empty.",
            "Being tired by evening isn't a reason to call the day a failure. Much of what you did will only be visible in the morning.",
            "If you want to do nothing — that's not laziness, it's a sensible response from your system. " +
                "Sometimes rest is the most productive task of the day.",
            "Overwhelm says your boundaries have been thinner than you'd like for a while. " +
                "You don't have to rebuild them now — first, just notice.",
        ),
        generalSupport = listOf(
            "I notice a heavy tone to the day. You're here, with your entries — that's already self-care.",
            "On hard days, the act of coming and writing becomes a support. You just created one for yourself.",
            "When everything hits at once — the best thing to do isn't solve everything at once. " +
                "Today it's enough to stop.",
            "A heavy day isn't a sentence for tomorrow. It simply closes with this entry.",
            "On days when there's a lot inside, what matters most isn't judging yourself, but noticing yourself. You did that.",
        ),
        joy = listOf(
            "Remember this joy as a support. On hard days you can return to it in your mind.",
            "Joy is a skill of attention. You just used it — you noticed something good and recorded it.",
            "Good days aren't random — they become good partly because people notice them. You did that.",
            "You can let joy stay a little longer without rushing to what's next. " +
                "That's savoring — a practice, not an accident.",
            "You can mentally \"put today's feeling in your pocket\" — for later, when you'll need it.",
        ),
        gratitude = listOf(
            "Gratitude today is like a bookmark in the book of the day. Tomorrow it'll be easier to open the right page.",
            "Noticing what you're grateful for trains attention. The more often, the sharper the gaze.",
            "Today's gratitude doesn't cancel what's heavy — it simply stands beside it. " +
                "It's normal to hold both inside.",
            "What you're grateful for today becomes tomorrow's support. It doesn't have to be loud — " +
                "noticing is enough.",
            "Gratitude is a way to stay in contact with a reality where good things actually exist.",
        ),
        calm = listOf(
            "Calm isn't the absence of feelings, but agreeing to be with what is. You managed that today.",
            "Steady days feel \"ordinary,\" but they're the foundation. Not every day has to be vivid.",
            "Calm today is a skill that isn't visible from the outside. You just applied it.",
            "When it's quiet inside, you can simply be with that quiet. No tasks, no \"I have to do something about this.\"",
            "A steady state is a resource in itself. Remember it as it is right now.",
        ),
        focused = listOf(
            "Focus today is your resource. Tomorrow it'll recover faster if you release tasks at night.",
            "Concentration is a short and precious resource. It's good you found somewhere to apply it today.",
            "After a focused day, what matters is not extending it into the evening. You can gently switch into rest mode.",
            "When you managed to focus — that's always a mix of effort and circumstances. " +
                "You don't have to push yourself to do more of it.",
            "A focused day is a reason to notice you have a skill of attention. It works, even when it's tired.",
        ),
        neutral = listOf(
            "The day is done. Thank you for finding a minute for this entry.",
            "Sometimes a day is just a day. That's okay — not every one has to be \"about something.\"",
            "A steady day is part of life too. It doesn't have to be meaningful to be lived.",
            "The fact that you're here, in this entry, is self-care. Without big words.",
            "Close this day gently — not with a summary, but simply with the fact that it ended.",
        ),
    )
    }
}
