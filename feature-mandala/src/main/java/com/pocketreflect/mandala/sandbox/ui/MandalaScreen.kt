// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.animation.Crossfade
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pocketreflect.mandala.sandbox.engine.MandalaEngine
import com.pocketreflect.mandala.sandbox.engine.MandalaPhase
import com.pocketreflect.mandala.sandbox.haptic.HapticFeedback
import com.pocketreflect.mandala.sandbox.haptic.rememberHapticFeedback
import kotlinx.coroutines.isActive

@Composable
fun MandalaScreen() {
    val engine = remember { MandalaEngine() }
    val context = LocalContext.current
    val haptics = rememberHapticFeedback()
    val lifecycleOwner = LocalLifecycleOwner.current

    val introUiTick = engine.uiSecondTick.longValue

    var layoutApplied by remember { mutableStateOf(false) }

    var fps by remember { mutableFloatStateOf(0f) }
    var frameCounter by remember { mutableIntStateOf(0) }
    var fpsWindowStart by remember { mutableLongStateOf(0L) }

    var debugPanelVisible by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner, engine, layoutApplied) {
        if (!layoutApplied) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = withFrameNanos { it }
            fpsWindowStart = lastNanos
            frameCounter = 0
            while (isActive) {
                if (engine.phase == MandalaPhase.Complete) {
                    kotlinx.coroutines.delay(200)
                    lastNanos = withFrameNanos { it }
                    continue
                }
                withFrameNanos { frameNanos ->
                    val dtSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.032f)
                    lastNanos = frameNanos

                    when (engine.phase) {
                        MandalaPhase.IntroFocus,
                        MandalaPhase.Playing,
                        MandalaPhase.CoreGlow,
                        MandalaPhase.WindDestroy,
                        -> engine.update(dtSec)

                        MandalaPhase.Complete -> Unit
                    }

                    if (engine.consumeCoreCapturedFlag()) {
                        haptics.sandFlowCoreCapture()
                    }
                    if (engine.consumeRingPassFlags()) {
                        haptics.sandFlowPass()
                    }
                    haptics.sandFlowRotation(engine.consumeRotationHapticIntensity())

                    frameCounter++
                    if (frameCounter >= 30) {
                        val elapsedSec = (frameNanos - fpsWindowStart) / 1_000_000_000f
                        if (elapsedSec > 0f) {
                            fps = frameCounter / elapsedSec
                        }
                        frameCounter = 0
                        fpsWindowStart = frameNanos
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        MandalaCanvas(
            engine = engine,
            onLayout = { width, height ->
                engine.onLayout(width, height)
                layoutApplied = true
            },
            onTap = {
                when (engine.phase) {
                    MandalaPhase.IntroFocus -> engine.skipIntro()
                    MandalaPhase.Complete -> engine.restartSession()
                    else -> Unit
                }
            },
            onDragStart = { touchX, touchY ->
                engine.startDrag(touchX, touchY)
            },
            onDragEnd = {
                engine.endDrag()
            },
            onRotationDrag = { touchX, touchY, dragX, dragY ->
                engine.applyRotationImpulse(touchX, touchY, dragX, dragY)
            },
        )

        PhaseOverlay(
            engine = engine,
            introUiTick = introUiTick,
            onIntroSkip = engine::skipIntro,
            onRestart = engine::restartSession,
        )

        DebugOverlay(
            fps = fps,
            engine = engine,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 12.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { debugPanelVisible = !debugPanelVisible },
                    )
                },
        ) {
            DebugTuningPanel(
                visible = debugPanelVisible,
                engine = engine,
                onCycleGravity = {
                    engine.tuning.gravity = when {
                        engine.tuning.gravity >= 550f -> 300f
                        else -> engine.tuning.gravity + 50f
                    }
                },
                onCycleSpawn = {
                    engine.tuning.spawnIntervalMs = when {
                        engine.tuning.spawnIntervalMs <= 180L -> 400L
                        else -> engine.tuning.spawnIntervalMs - 20L
                    }
                },
                onCycleFriction = {
                    engine.tuning.friction = when {
                        engine.tuning.friction >= 0.98f -> 0.90f
                        else -> engine.tuning.friction + 0.01f
                    }
                },
                onToggleBreathing = {
                    engine.tuning.breathingSyncEnabled = !engine.tuning.breathingSyncEnabled
                },
            )
        }

        // МЕДИТАТИВНЫЕ АФФИРМАЦИИ И ЦИТАТЫ В НИЖНЕЙ ЧАСТИ ЭКРАНА
        if (engine.phase == MandalaPhase.Playing || engine.phase == MandalaPhase.CoreGlow) {
            CalmingQuotesOverlay(
                engine = engine,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp)
            )
        }
    }
}

@Composable
fun CalmingQuotesOverlay(
    engine: MandalaEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRu = remember(context) {
        val locale = context.resources.configuration.locales.get(0)
        locale?.language?.lowercase()?.startsWith("ru") ?: false
    }

    val quotes = remember(isRu) {
        if (isRu) {
            listOf(
                // Стиль 1: Мягкое отпускание (ACT)
                "Мысли — как эти песчинки. Позвольте им осыпаться.",
                "Вам не нужно удерживать этот мир. Пусть он течёт сам.",
                "Каждая светящаяся искра — это мысль, пролетающая мимо.",
                "Гравитация сделает всё сама. Просто наблюдайте.",
                "Отпустите контроль, как песок сквозь пальцы.",
                "Ветру не нужно сопротивляться. Пусть он уносит лишнее.",
                "Вы — это небо. А мысли — лишь пылинки на ветру.",
                "Позвольте вещам просто происходить прямо сейчас.",
                "Ничего не нужно решать в эту самую секунду.",
                "Тревога тоже утекает, капля за каплей.",
                "Как песчинки ложатся на дно, так утихает и ум.",
                "В этом потоке нет ошибок. Всё движется как должно.",
                "Наблюдайте, как форма меняется, не пытаясь её удержать.",
                "Можно перестать стараться. Просто будьте в потоке.",
                "Пусть вчера и завтра растворятся в этом свете.",

                // Стиль 2: Телесное заземление (Somatic Grounding)
                "Позвольте дыханию стать чуть медленнее.",
                "Заметьте, как тяжелеют ваши плечи, опускаясь вниз.",
                "Вдох и выдох. Как ритм этих вращающихся колец.",
                "Почувствуйте опору под собой. Она надежна.",
                "Лицо расслабляется с каждой падающей песчинкой.",
                "Воздух прохладный на вдохе и теплый на выдохе.",
                "Ваши руки могут немного отдохнуть прямо сейчас.",
                "Представьте, как напряжение стекает вниз, в песок.",
                "Пульс замедляется, подстраиваясь под этот ритм.",
                "Мягкий вдох. И долгий, спокойный выдох.",
                "Заметьте тишину между концом выдоха и новым вдохом.",
                "Ваше тело здесь, в безопасности.",
                "Челюсть расслабляется, язык мягко опускается.",
                "Позвольте векам стать чуть тяжелее.",
                "Дыхание течет так же свободно, как этот свет.",

                // Стиль 3: Мудрые цитаты (Philosophy & Impermanence)
                "Всё течёт, всё меняется. И это тоже пройдет.",
                "Река никогда не бывает прежней. Как и этот миг.",
                "В пустоте ума рождается истинная ясность.",
                "Принимая непостоянство, мы обретаем покой.",
                "Дерево не сопротивляется осени, сбрасывая листья.",
                "Самая глубокая вода течет с наименьшим шумом.",
                "Время — это просто песок. Не пытайтесь его сжать.",
                "То, что мы отпускаем, перестает нами управлять.",
                "Там, где кончается контроль, начинается свобода.",
                "Свет звезд доходит до нас даже сквозь темноту.",
                "Мягкое всегда побеждает твердое. Будьте как вода.",
                "Ничто не вечно, кроме самого потока перемен.",
                "Спокойствие — это не отсутствие бури, а тишина в ее центре.",
                "Если чаша полна, в нее больше ничего не налить.",
                "Не толкайте реку, она течет сама по себе.",

                // Стиль 4: Бережное присутствие (Validation & Compassion)
                "Вы проделали большой путь сегодня. Можно выдохнуть.",
                "Уставать — это нормально. Вы имеете право на отдых.",
                "Даже если день был трудным, сейчас вы в безопасности.",
                "Вам не нужно быть идеальными. Достаточно быть здесь.",
                "Отложите доспехи. В этом пространстве они не нужны.",
                "Сегодня вы сделали всё, что могли. Этого достаточно.",
                "Позвольте себе просто быть. Без целей и ожиданий.",
                "Ваши чувства имеют значение, какими бы они ни были.",
                "Мысленно обнимите себя за всё, что пришлось пройти.",
                "Слабость — это не провал, а просьба тела о заботе.",
                "Вы заслуживаете тишины и этой мягкой темноты.",
                "Совершенно нормально, если сейчас нет сил.",
                "Никто ничего от вас не ждёт в эти минуты.",
                "Пусть это время станет вашим безопасным убежищем.",
                "Я побуду здесь с вами, пока свет струится вниз."
            )
        } else {
            listOf(
                // Style 1: Gentle Letting Go (ACT)
                "Thoughts are like these grains. Let them fall.",
                "You don't need to hold the world. Let it flow.",
                "Every glowing spark is a thought passing by.",
                "Gravity will do the work. Just watch.",
                "Let go of control, like sand slipping through fingers.",
                "No need to resist the wind. Let it carry the weight.",
                "You are the sky. Thoughts are just dust in the wind.",
                "Allow things to simply happen right now.",
                "Nothing needs to be solved in this exact second.",
                "Anxiety flows away too, drop by drop.",
                "As grains settle at the bottom, the mind quiets down.",
                "There are no mistakes in this flow. All moves as it should.",
                "Watch the shape change without trying to hold it.",
                "You can stop trying now. Just be in the flow.",
                "Let yesterday and tomorrow dissolve in this light.",

                // Style 2: Somatic Grounding
                "Allow your breathing to become slightly slower.",
                "Notice how your shoulders grow heavy, dropping down.",
                "Inhale and exhale. Like the rhythm of these rings.",
                "Feel the support beneath you. It is secure.",
                "Your face relaxes with every falling grain.",
                "The air is cool as you breathe in, warm as you breathe out.",
                "Your hands can rest a little bit right now.",
                "Imagine the tension flowing down into the sand.",
                "Your pulse is slowing, tuning into this rhythm.",
                "A gentle breath in. And a long, calm breath out.",
                "Notice the silence between the exhale and the next breath.",
                "Your body is right here, safe and sound.",
                "Your jaw relaxes, letting go of all tension.",
                "Allow your eyelids to grow just a bit heavier.",
                "Your breath flows as freely as this light.",

                // Style 3: Wise Quotes (Philosophy & Impermanence)
                "Everything flows, everything changes. This too shall pass.",
                "A river is never the same twice. Just like this moment.",
                "In the emptiness of the mind, true clarity is born.",
                "By embracing impermanence, we find deep peace.",
                "A tree does not resist autumn when dropping its leaves.",
                "The deepest waters flow with the least amount of noise.",
                "Time is just sand. Do not try to grasp it tightly.",
                "What we let go of ceases to control us.",
                "Where control ends, true freedom begins.",
                "Starlight reaches us even through the darkest night.",
                "The soft overcomes the hard. Be like water.",
                "Nothing is eternal, except the flow of change itself.",
                "Peace is not the absence of storm, but stillness within it.",
                "If the cup is full, nothing more can be poured in.",
                "Do not push the river, it flows by itself.",

                // Style 4: Gentle Presence (Validation & Compassion)
                "You've come a long way today. It's okay to exhale.",
                "Being tired is natural. You have every right to rest.",
                "Even if the day was hard, you are safe right now.",
                "You don't need to be perfect. Being here is enough.",
                "Set aside your armor. It is not needed in this space.",
                "You did the best you could today. That is enough.",
                "Allow yourself to simply be. No goals, no expectations.",
                "Your feelings are valid, whatever they might be.",
                "Mentally embrace yourself for all you have been through.",
                "Weakness is not a failure, but a body's plea for care.",
                "You deserve the silence and this gentle darkness.",
                "It is completely okay if you have no energy right now.",
                "Nobody expects anything from you in these moments.",
                "Let this time become your safe and quiet haven.",
                "I will sit right here with you while the light flows down."
            )
        }
    }

    val quotesByStyle = remember(isRu) {
        if (isRu) {
            mapOf(
                "Все" to (0..59).toList(),
                "Отпускание" to (0..14).toList(),
                "Заземление" to (15..29).toList(),
                "Цитаты" to (30..44).toList(),
                "Присутствие" to (45..59).toList()
            )
        } else {
            mapOf(
                "All" to (0..59).toList(),
                "Letting Go" to (0..14).toList(),
                "Grounding" to (15..29).toList(),
                "Quotes" to (30..44).toList(),
                "Presence" to (45..59).toList()
            )
        }
    }

    val stylesList = remember(isRu) {
        if (isRu) {
            listOf("Все", "Отпускание", "Заземление", "Цитаты", "Присутствие")
        } else {
            listOf("All", "Letting Go", "Grounding", "Quotes", "Presence")
        }
    }

    var selectedStyleIndex by remember { mutableStateOf(0) }
    val currentStyle = stylesList[selectedStyleIndex]

    // Фильтруем и перемешиваем цитаты на основе выбранного стиля
    val filteredQuotes = remember(currentStyle, isRu) {
        val indices = quotesByStyle[currentStyle] ?: (0..59).toList()
        val rawList = indices.map { quotes[it] }
        rawList.shuffled()
    }

    var currentQuoteIndex by remember { mutableStateOf(0) }

    LaunchedEffect(filteredQuotes) {
        currentQuoteIndex = 0
        while (isActive) {
            delay(9000) // 9 секунд — научно обоснованное золотое сечение (интеграция + дыхание)
            currentQuoteIndex = (currentQuoteIndex + 1) % filteredQuotes.size
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            // ПРЕМИАЛЬНАЯ АНИМАЦИЯ: Мягкое растворение (Fade) + легкое масштабирование (Scale) + размытие (Blur)
            Crossfade(
                targetState = if (filteredQuotes.isNotEmpty()) filteredQuotes[currentQuoteIndex % filteredQuotes.size] else "",
                animationSpec = tween(durationMillis = 1200, easing = EaseInOutSine),
                label = "quote-fade"
            ) { quote ->
                Text(
                    text = quote,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shadowElevation = 2f
                        }
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        // Ненавязчивая, полупрозрачная кнопка переключения стиля
        androidx.compose.material3.TextButton(
            onClick = {
                selectedStyleIndex = (selectedStyleIndex + 1) % stylesList.size
            },
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            ),
            modifier = Modifier.graphicsLayer { alpha = 0.75f }
        ) {
            Text(
                text = if (isRu) "Настроение: $currentStyle" else "Focus: $currentStyle",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
