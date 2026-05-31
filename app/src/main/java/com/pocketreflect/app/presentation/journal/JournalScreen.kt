// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.pocketreflect.app.presentation.components.CalmLoadingIndicator
import com.pocketreflect.app.presentation.components.CalmSuccessBanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.core.haptic.rememberHapticFeedback
import com.pocketreflect.app.core.time.DateFormats
import com.pocketreflect.app.core.time.DayBucket
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.pocketreflect.app.presentation.journal.JournalContract.MAX_TOMORROW_TASK_LINES
import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.breathing.BreathingPattern
import com.pocketreflect.app.domain.breathing.BreathingSessionController
import com.pocketreflect.app.presentation.components.AiStatusDialog
import com.pocketreflect.app.presentation.components.AiEngineStatusIcon
import com.pocketreflect.app.presentation.journal.components.BreathingDialog
import androidx.compose.material.icons.outlined.SelfImprovement
import com.pocketreflect.app.presentation.components.bottomInputBarInsets
import com.pocketreflect.app.presentation.journal.components.AiMentorCard
import com.pocketreflect.app.presentation.journal.components.WeeklyTrendSection
import com.pocketreflect.app.presentation.journal.components.MoodTagChips
import com.pocketreflect.app.presentation.journal.components.SectionCard
import com.pocketreflect.app.presentation.journal.components.SoftTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import com.pocketreflect.app.ui.theme.PocketReflectShapes
import com.pocketreflect.app.ui.theme.PocketReflectTypographyAccent

/**
 * Экран «Итоги дня» — единая точка входа.
 *
 * Композиция строго следует продуктовому брифу: 4 блока в фиксированном порядке.
 * Блок «микро-побед» УБИРАЕТСЯ полностью (а не disable'ится), если выбран
 * негативный тег — это часть Empathic UX.
 *
 * UX-полировка (Foundation Polish):
 *  - дата в TopAppBar + privacy-badge,
 *  - индикатор «уже записано сегодня» (зелёная точка),
 *  - градиент фона MidnightBackground → MidnightSurface (вечерняя атмосфера),
 *  - haptic на toggle тега,
 *  - hint под disabled-кнопкой сохранения,
 *  - промпт дня визуально отделён карточкой-цитатой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onNavigateToModelSettings: () -> Unit = {},
    onNavigateToEntryDetail: (Long) -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHapticFeedback(state.uiHapticEnabled)
    val displayLocale = DateFormats.javaLocale(androidx.compose.ui.platform.LocalConfiguration.current)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val dayHeader = if (state.selectedDayBucket.isNotEmpty()) {
        DateFormats.dayHeaderFromBucket(state.selectedDayBucket, displayLocale)
    } else {
        DateFormats.dayHeader(System.currentTimeMillis(), displayLocale)
    }

    // Локальный диалог-пояснение приватности (раскрывается тапом по PrivacyBadge).
    // State поднят сюда, а не внутрь TopAppBar — иначе пересоздание ToolBar
    // при rotate/recompose сбросило бы открытое окно. ViewModel в это
    // состояние не вовлечена: оно чисто UI и не выживает process death,
    // что для модального пояснения корректно.
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAiStatusDialog by remember { mutableStateOf(false) }
    var showBreathingDialog by rememberSaveable { mutableStateOf(false) }
    var showSandFlowDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveBanner by remember { mutableStateOf(false) }
    val savedSnackbarMessage = stringResource(R.string.journal_saved_snackbar)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is JournalContract.Effect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                JournalContract.Effect.DaySaved -> {
                    haptic.confirm()
                    showSaveBanner = true
                }
                JournalContract.Effect.ScrollToTop -> Unit
            }
        }
    }

    LaunchedEffect(showSaveBanner) {
        if (showSaveBanner) {
            kotlinx.coroutines.delay(3_200L)
            showSaveBanner = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // КРИТИЧНО: этот Scaffold вложен в RootScaffold, который уже
        // отнял место под status bar и BottomNavBar через innerPadding.
        // Если не обнулить contentWindowInsets — system insets отнимутся
        // ДВАЖДЫ: появится "пустая полоса" сверху (≈24dp статусбара)
        // и снизу (≈32-48dp жестового нав-бара) между SaveBar и BottomNavBar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            JournalTopBar(
                dayHeader = dayHeader,
                wasSavedForDay = state.wasSavedForDay,
                isEditingPastDay = state.isEditingPastDay,
                aiEngineStatus = state.aiEngineStatus,
                onDateClick = { showDatePicker = true },
                onPrivacyBadgeClick = { showPrivacyDialog = true },
                onAiStatusClick = { showAiStatusDialog = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!state.isLoading && !state.bootstrapFailed) {
                // CTA вынесена из прокручиваемого контента в bottomBar:
                //  - всегда видна (не уезжает вниз вместе с прокруткой),
                //  - [bottomInputBarInsets] — над клавиатурой без лишнего зазора.
                SaveBar(
                    state = state,
                    saveBannerMessage = savedSnackbarMessage,
                    showSaveBanner = showSaveBanner,
                    onSave = { viewModel.onIntent(JournalContract.Intent.SaveDay) },
                    onRetrySave = { viewModel.onIntent(JournalContract.Intent.RetrySave) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            when {
                state.isLoading -> LoadingState(innerPadding)
                state.bootstrapFailed -> BootstrapErrorState(
                    padding = innerPadding,
                    onRetry = { viewModel.onIntent(JournalContract.Intent.RetryBootstrap) },
                )
                else -> JournalContent(
                    padding = innerPadding,
                    state = state,
                    onStartBreathing = { showBreathingDialog = true },
                    onStartSandFlow = { showSandFlowDialog = true },
                    onNavigateToEntryDetail = onNavigateToEntryDetail,
                    onIntent = { intent ->
                        if (intent is JournalContract.Intent.ToggleTag) haptic.tick()
                        viewModel.onIntent(intent)
                    },
                )
            }
        }
    }

    if (showAiStatusDialog) {
        AiStatusDialog(
            status = state.aiEngineStatus,
            onDismiss = { showAiStatusDialog = false },
            onNavigateToModelSettings = onNavigateToModelSettings,
        )
    }

    if (showPrivacyDialog) {
        PrivacyExplainDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showBreathingDialog) {
        BreathingDialog(
            pattern = state.breathingPattern,
            hapticEnabled = state.breathingHapticEnabled,
            hapticIntensity = state.breathingHapticIntensity,
            cycleCount = state.breathingCycleCount,
            onDismiss = { showBreathingDialog = false },
        )
    }

    if (showSandFlowDialog) {
        com.pocketreflect.app.presentation.journal.components.SandFlowDialog(
            difficulty = state.sandFlowDifficulty,
            breathingSyncEnabled = state.sandFlowBreathingSyncEnabled,
            onDismiss = { showSandFlowDialog = false },
        )
    }

    if (showDatePicker) {
        JournalDatePickerDialog(
            selectedDayBucket = state.selectedDayBucket,
            onDismiss = { showDatePicker = false },
            onConfirm = { dayBucket ->
                showDatePicker = false
                viewModel.onIntent(JournalContract.Intent.SelectDay(dayBucket))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalTopBar(
    dayHeader: String,
    wasSavedForDay: Boolean,
    isEditingPastDay: Boolean,
    aiEngineStatus: AiEngineStatus,
    onDateClick: () -> Unit,
    onPrivacyBadgeClick: () -> Unit,
    onAiStatusClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayHeader,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable(onClick = onDateClick),
                    )
                    if (wasSavedForDay) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SavedTodayDot()
                    }
                }
                if (wasSavedForDay) {
                    Text(
                        text = if (isEditingPastDay) {
                            stringResource(R.string.journal_saved_past_day_subtitle)
                        } else {
                            stringResource(R.string.journal_saved_today_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (isEditingPastDay) {
                    Text(
                        text = stringResource(R.string.journal_past_day_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onAiStatusClick) {
                AiEngineStatusIcon(status = aiEngineStatus)
            }
            PrivacyBadge(onClick = onPrivacyBadgeClick)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        // status bar inset уже отнят RootScaffold'ом → здесь обнуляем,
        // иначе появится пустая полоса между статусбаром и TopAppBar.
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

/**
 * Privacy-badge — маленький значок «без сети», который пользователь видит ВСЕГДА.
 * Это не декоративный элемент: продукт продаёт приватность, и она должна
 * физически присутствовать в интерфейсе. По тапу открывается короткое
 * пояснение, что означает иконка — без подсказки люди легко путают её
 * с «нет связи / самолётный режим».
 */
@Composable
private fun PrivacyBadge(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = stringResource(R.string.cd_privacy_local_only),
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Короткий диалог-пояснение: «что значит этот значок».
 *
 * Дизайн-решения:
 *  - Заголовок без местоимений и без слов «политика/конфиденциальность» —
 *    это не legal-текст, а человеческое пояснение в одно предложение.
 *  - Иконка щита в заголовке — визуальный мостик к Settings/«Приватность»,
 *    где живёт полная техническая версия.
 *  - Одна кнопка «Понятно». Не делаем «Подробнее → Настройки», потому что
 *    переход из IconButton в TopAppBar требует протаскивания NavController
 *    через несколько слоёв ради одного клика — пользователь сам дойдёт
 *    через нижнее меню «Настройки».
 */
@Composable
private fun PrivacyExplainDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = stringResource(R.string.cd_privacy_offline_icon),
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        title = { Text(stringResource(R.string.journal_privacy_dialog_title)) },
        text = {
            Text(text = stringResource(R.string.journal_privacy_dialog_body))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_understood)) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SavedTodayDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary),
    )
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CalmLoadingIndicator()
    }
}

@Composable
private fun BootstrapErrorState(
    padding: PaddingValues,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.journal_bootstrap_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun JournalContent(
    padding: PaddingValues,
    state: JournalContract.State,
    onStartBreathing: () -> Unit,
    onStartSandFlow: () -> Unit,
    onNavigateToEntryDetail: (Long) -> Unit,
    onIntent: (JournalContract.Intent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            // vertical = 4 (было 8) — отдаём ещё ~8dp прокручиваемому контенту.
            // horizontal остаётся 16dp (стандарт Material 3 для phones).
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        // === TIME ECHO (запись год назад) ============================================
        state.timeEcho?.let { echo ->
            com.pocketreflect.app.presentation.journal.components.TimeEchoCard(
                echo = echo,
                onClick = { onNavigateToEntryDetail(echo.entry.id) },
                onDismiss = { onIntent(JournalContract.Intent.DismissTimeEcho) }
            )
        }

        val isShortModeActive = state.ritualMode == com.pocketreflect.app.domain.ritual.RitualMode.SHORT && !state.isShortRitualOverridden

        // === БАННЕР КОРОТКОГО ВЕЧЕРА (Feature 2) =====================================
        if (isShortModeActive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ritual_short_banner_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.ritual_short_banner_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = { onIntent(JournalContract.Intent.ExpandFullRitual) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(R.string.ritual_short_expand))
                    }
                }
            }
        }

        // === ДЫХАТЕЛЬНЫЙ МОСТ И ПЕСОЧНЫЙ ПОТОК ========================================
        if (!isShortModeActive) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BreathingBannerCard(
                    onStart = onStartBreathing,
                    pattern = state.breathingPattern,
                    cycleCount = state.breathingCycleCount,
                )
                if (state.sandFlowEnabled) {
                    SandFlowBannerCard(
                        onStart = onStartSandFlow,
                        difficulty = state.sandFlowDifficulty,
                    )
                }
            }
        }

        // === БЛОК 1. АФФЕКТИВНЫЙ ЛЕЙБЛИНГ ===========================================
        SectionCard(
            title = stringResource(R.string.affective_question),
            subtitle = stringResource(R.string.affective_subtitle),
        ) {
            MoodTagChips(
                tags = state.availableTags,
                selected = state.selectedTags,
                onToggle = { onIntent(JournalContract.Intent.ToggleTag(it)) },
            )
        }

        // === БЛОК 2. КРИСТАЛЛИЗАЦИЯ УСИЛИЙ (скрывается при негативе и в коротком режиме) ===
        if (!isShortModeActive) {
            AnimatedVisibility(
                visible = !state.isMicroWinsHidden,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SectionCard(
                    title = stringResource(R.string.micro_wins_title),
                    subtitle = stringResource(R.string.micro_wins_hint),
                ) {
                    SoftTextField(
                        value = state.microWins,
                        onValueChange = { onIntent(JournalContract.Intent.UpdateMicroWins(it)) },
                        placeholder = stringResource(R.string.journal_micro_wins_placeholder),
                        minLines = 2,
                        maxLines = 6,
                    )
                }
            }
        }

        // === БЛОК 3. ЭКСТЕРНАЛИЗАЦИЯ ЗАДАЧ (скрывается в коротком режиме) =============
        if (!isShortModeActive) {
            SectionCard(
                title = stringResource(R.string.tasks_title),
                subtitle = stringResource(R.string.tasks_hint),
            ) {
                SoftTextField(
                    value = state.tomorrowTasks,
                    onValueChange = { onIntent(JournalContract.Intent.UpdateTomorrowTasks(it)) },
                    placeholder = "1) …\n2) …\n3) …",
                    minLines = 3,
                    maxLines = 5,
                    isError = state.isTomorrowTasksLimitExceeded,
                )
                AnimatedVisibility(
                    visible = state.tomorrowTasks.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.tomorrow_tasks_empathic_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                AnimatedVisibility(
                    visible = state.isTomorrowTasksLimitExceeded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.journal_tasks_limit_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.journal_tasks_lines,
                        state.tomorrowTaskLineCount,
                        MAX_TOMORROW_TASK_LINES,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isTomorrowTasksLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.showCustomField) {
            SectionCard(
                title = state.customFieldQuestion,
                subtitle = state.customFieldHint.takeIf { it.isNotBlank() },
            ) {
                SoftTextField(
                    value = state.customFieldAnswer,
                    onValueChange = { onIntent(JournalContract.Intent.UpdateCustomField(it)) },
                    placeholder = state.customFieldHint.ifBlank {
                        stringResource(R.string.custom_field_answer_placeholder)
                    },
                    minLines = 2,
                    maxLines = 6,
                )
            }
        }

        // === БЛОК 4. ДИНАМИЧЕСКАЯ ПЕРСПЕКТИВА (промпт дня) ===========================
        SectionCard(title = stringResource(R.string.prompt_title)) {
            PromptQuoteCard(prompt = state.dailyPrompt)

            SoftTextField(
                value = state.reflection,
                onValueChange = { onIntent(JournalContract.Intent.UpdateReflection(it)) },
                placeholder = stringResource(R.string.reflection_hint),
                minLines = 2,
                maxLines = 8,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onIntent(JournalContract.Intent.ReshufflePrompt) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.cd_reshuffle_prompt),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.prompt_reshuffle))
                }
            }
        }

        // === ОТКЛИК ИИ-МЕНТОРА (вне 4-блочной структуры, ambient-карточка) ===========
        AiMentorCard(
            isThinking = state.isAiThinking,
            text = state.aiResponse,
            supportiveMode = state.isSupportiveModeActive,
            aiEngineStatus = state.aiEngineStatus,
            canRequest = state.canRequestAiReflection,
            requestButtonLabel = if (state.hasAiResponse) {
                stringResource(R.string.ai_mentor_refresh)
            } else {
                stringResource(R.string.ai_mentor_request)
            },
            hintNoTags = stringResource(R.string.ai_mentor_hint_no_tags),
            hintRequest = stringResource(R.string.ai_mentor_hint_request),
            onRequestClick = { onIntent(JournalContract.Intent.RequestAiReflection) },
        )

        WeeklyTrendSection(
            summaryText = state.weeklyTrendSummary,
            isBuilding = state.isWeeklyTrendBuilding,
            canRequest = state.canRequestWeeklyTrend,
            hasSummary = state.hasWeeklyTrendSummary,
            aiEngineStatus = state.aiEngineStatus,
            requestButtonLabel = if (state.hasWeeklyTrendSummary) {
                stringResource(R.string.weekly_trend_refresh)
            } else {
                stringResource(R.string.weekly_trend_request)
            },
            hintNeedEntries = stringResource(R.string.weekly_trend_need_more_entries),
            hintRequest = stringResource(R.string.weekly_trend_hint_request),
            onRequestClick = { onIntent(JournalContract.Intent.RequestWeeklyTrend) },
        )

        // Раньше здесь стоял Spacer(8.dp) от прилипания к SaveBar.
        // Теперь не нужен: SaveBar сам имеет vertical padding и
        // полупрозрачный градиент сверху, который создаёт визуальный «зазор».
    }
}

/**
 * Карточка-цитата под промпт дня. Курсив + приглушённый фон визуально
 * выделяют «слова от приложения» среди ввода пользователя.
 */
@Composable
private fun PromptQuoteCard(prompt: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PocketReflectShapes.Card,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "“",
                style = PocketReflectTypographyAccent.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = prompt,
                style = PocketReflectTypographyAccent.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Закреплённая снизу панель с CTA.
 *
 * Дизайн-решения:
 *  - Полупрозрачный градиент сверху (Transparent → background) создаёт
 *    «дверь» между прокручиваемым контентом и закреплённой кнопкой,
 *    чтобы было видно, что выше есть ещё информация.
 *  - [bottomInputBarInsets] поднимает над клавиатурой без двойного сдвига
 *    (манифест: adjustNothing + imePadding; nav-bar padding только при IME).
 *  - Подсказка `disabledSaveHint` — над кнопкой, чтобы пользователь сразу
 *    видел «что меня блокирует», а не угадывал из серого цвета.
 */
@Composable
private fun SaveBar(
    state: JournalContract.State,
    saveBannerMessage: String,
    showSaveBanner: Boolean,
    onSave: () -> Unit,
    onRetrySave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .bottomInputBarInsets()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CalmSuccessBanner(
            message = saveBannerMessage,
            visible = showSaveBanner,
        )
        AnimatedVisibility(
            visible = state.showSaveHintNoTags,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.journal_save_hint_no_tags),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        AnimatedVisibility(
            visible = state.saveFailed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.journal_save_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetrySave) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("journal_save_day"),
            shape = PocketReflectShapes.Button,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = when {
                        state.wasSavedForDay -> stringResource(R.string.journal_update_entry)
                        state.ritualMode == com.pocketreflect.app.domain.ritual.RitualMode.SHORT && !state.isShortRitualOverridden -> stringResource(R.string.ritual_short_save)
                        else -> stringResource(R.string.action_save)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun RitualBannerCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onStart: () -> Unit,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    gradient: Brush,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonContentColor,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

/**
 * Изящный баннер для запуска дыхательной сессии «Дыхательный мост».
 */
@Composable
private fun BreathingBannerCard(
    onStart: () -> Unit,
    pattern: BreathingPattern,
    cycleCount: Int,
    modifier: Modifier = Modifier,
) {
    val normalizedCycles = BreathingSessionController.normalizeCycleCount(cycleCount)
    val durationSeconds = (
        BreathingSessionController.cycleDurationMs(pattern) * normalizedCycles / 1000L
        ).toInt()
    val durationMinutes = (durationSeconds + 59) / 60
    val colorScheme = MaterialTheme.colorScheme

    RitualBannerCard(
        title = stringResource(R.string.breathing_banner_title),
        subtitle = stringResource(R.string.breathing_banner_subtitle, durationMinutes),
        actionLabel = stringResource(R.string.breathing_banner_action),
        onStart = onStart,
        icon = Icons.Outlined.SelfImprovement,
        iconBackgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.6f),
        iconTint = colorScheme.secondary,
        buttonContainerColor = colorScheme.secondaryContainer,
        buttonContentColor = colorScheme.onSecondaryContainer,
        gradient = Brush.verticalGradient(
            colors = listOf(
                colorScheme.secondaryContainer.copy(alpha = 0.25f),
                colorScheme.tertiaryContainer.copy(alpha = 0.15f),
            ),
        ),
        modifier = modifier,
    )
}

/**
 * Изящный баннер для запуска кинетической сессии «Песочный поток» (Sand Flow).
 */
@Composable
private fun SandFlowBannerCard(
    onStart: () -> Unit,
    difficulty: Int,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    RitualBannerCard(
        title = stringResource(R.string.sand_flow_banner_title),
        subtitle = stringResource(R.string.sand_flow_banner_subtitle),
        actionLabel = stringResource(R.string.breathing_banner_action),
        onStart = onStart,
        icon = Icons.Outlined.SelfImprovement,
        iconBackgroundColor = colorScheme.primaryContainer.copy(alpha = 0.6f),
        iconTint = colorScheme.primary,
        buttonContainerColor = colorScheme.primaryContainer,
        buttonContentColor = colorScheme.onPrimaryContainer,
        gradient = Brush.verticalGradient(
            colors = listOf(
                colorScheme.primaryContainer.copy(alpha = 0.25f),
                colorScheme.tertiaryContainer.copy(alpha = 0.15f),
            ),
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalDatePickerDialog(
    selectedDayBucket: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val initialMillis = if (selectedDayBucket.isNotEmpty()) {
        DayBucket.toNoonEpochMillis(selectedDayBucket, zone)
    } else {
        Instant.now().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                return !date.isAfter(today)
            }

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: return@TextButton
                    val dayBucket = DayBucket.of(millis, zone)
                    onConfirm(dayBucket)
                },
            ) {
                Text(stringResource(R.string.journal_date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.journal_date_picker_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
