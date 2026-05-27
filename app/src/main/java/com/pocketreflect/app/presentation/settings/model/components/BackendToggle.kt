// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pocketreflect.app.R
import com.pocketreflect.app.data.ai.EngineBackend
import com.pocketreflect.app.presentation.journal.components.SectionCard

/**
 * Тоггл аппаратного бэкенда для LiteRT-LM (Sub-PR #3c).
 *
 * UX-решения:
 *  - **Material3 SingleChoiceSegmentedButtonRow** — нативный way для эксклюзивного
 *    выбора из 2–5 опций. Выглядит как тогглы в системных настройках, а не как
 *    «настройка для гиков».
 *  - **Без префикса «Backend:»** — название секции говорит само за себя
 *    («Ускорение»), не нужно второй раз дублировать.
 *  - **Описание под тогглом**, не как tooltip. Tooltip на тач-устройстве — это
 *    клик-долгое-нажатие, его никто не делает. Описание один раз прочитают и
 *    забудут — это нормально.
 *  - **Empathic tone**: «если ИИ-ментор молчит дольше обычного» вместо
 *    «если приложение тормозит» — приложение не виновато, мы просто заботимся
 *    о пользователе.
 *  - **Без иконок** в кнопках: визуально проще, аббревиатуры «GPU»/«CPU» уже
 *    однозначны для аудитории, способной добраться до этого экрана.
 *
 * Поведенческий контракт:
 *  - VM получает интент `SelectBackend(backend)`, дальше сама пишет в DataStore.
 *  - При следующем `generatePromptResponse` `LiteRtGemmaEngine.ensureEngine`
 *    увидит рассогласование `activeBackend != selectedBackend`, закроет
 *    существующий native engine и пересоздаст под новым бэкендом (10–15 с
 *    на E2B). UI про это не знает — там просто следующий ответ придёт чуть
 *    дольше первого, а в логе будет запись о реинициализации.
 *
 *  Если backend ещё не зафиксирован (или DataStore пустой), VM подаст сюда
 *  дефолт `EngineBackend.GPU` — см. `readBackend` в DataStore-репозитории.
 */
@Composable
fun BackendToggle(
    selected: EngineBackend,
    onSelect: (EngineBackend) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.model_backend_section_title),
        modifier = modifier,
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            EngineBackend.entries.forEachIndexed { index, backend ->
                SegmentedButton(
                    selected = backend == selected,
                    onClick = { onSelect(backend) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = EngineBackend.entries.size,
                    ),
                ) {
                    Text(text = backend.label())
                }
            }
        }
        Text(
            text = stringResource(R.string.model_backend_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EngineBackend.label(): String = when (this) {
    EngineBackend.GPU -> stringResource(R.string.model_backend_gpu_label)
    EngineBackend.CPU -> stringResource(R.string.model_backend_cpu_label)
}
