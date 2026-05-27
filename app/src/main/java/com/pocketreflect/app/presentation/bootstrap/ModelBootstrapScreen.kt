// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.components.CalmPulseCircle

/**
 * Экран прогрева локальной модели (Sub-PR #4).
 *
 * UX-инварианты:
 *  - **Никакого процента прогресса**: пользователь не ждёт прогресса,
 *    он ждёт пока приложение прогреется. Цифры на bootstrap'е создают
 *    ложное обещание точности и нервируют, если шкала «застряла» на 67%.
 *  - **Pulse-анимация**: спокойный вдох-выдох (4 с цикл) визуально
 *    соответствует «дыхательной» эстетике дневника, не дёргается.
 *  - **Текст**: «Готовим локальный ИИ-ментор. Это займёт около 30 секунд.»
 *    — даём временной якорь, чтобы человек не подумал что приложение зависло.
 *  - **Failed-режим**: вместо «ошибка/повторить» показываем мягкое
 *    «работаем в режиме поддержки». На этом этапе пользователь уже
 *    собирается продолжить, нам важнее его не расстроить, чем добиться
 *    повторного запуска.
 *
 * Экран не имеет кнопки «продолжить»: переход в основной UI происходит
 * автоматически по `state.isFinished` (см. [WarmupGate]).
 */
@Composable
fun ModelBootstrapScreen(
    state: ModelBootstrapContract.State,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            CalmPulseCircle(size = 96.dp)
            Text(
                text = stringResource(R.string.bootstrap_preparing_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (state.hasFailed) {
                    stringResource(R.string.bootstrap_failed_subtitle)
                } else {
                    stringResource(R.string.bootstrap_preparing_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
