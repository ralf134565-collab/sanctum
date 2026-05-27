// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.journal.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import com.pocketreflect.app.ui.theme.PocketReflectShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Текстовое поле, унифицированное под «мягкий» вечерний стиль:
 *  - Спокойная скругленная рамка,
 *  - Менее агрессивный focused-цвет,
 *  - Поддержка многострочного ввода с min/max heights.
 *
 * Параметр [maxLines] не сам ограничивает ввод (это слишком хрупко),
 * только подсказывает Compose, сколько строк отрисовывать без скролла.
 * Жёсткое ограничение — в ViewModel (см. `handleUpdateTomorrowTasks`).
 */
@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = PocketReflectShapes.Input,
        textStyle = MaterialTheme.typography.bodyLarge,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorCursorColor = MaterialTheme.colorScheme.error,
        ),
    )
}
