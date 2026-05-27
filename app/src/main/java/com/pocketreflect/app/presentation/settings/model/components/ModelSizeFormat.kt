// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model.components

import java.util.Locale

/**
 * Форматирование размера в гигабайтах с двумя знаками после запятой.
 *
 * Используется и в карточке варианта (статичный размер из манифеста),
 * и в блоке прогресса (сколько уже скопировано) — общая точность важна
 * для визуальной согласованности.
 *
 * Локаль `Locale.US` — чтобы десятичный разделитель оставался точкой
 * («2.59 GB»), это привычнее для технической характеристики, чем
 * локализованная «2,59 GB».
 */
internal fun humanGigabytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.2f GB", gb)
}
