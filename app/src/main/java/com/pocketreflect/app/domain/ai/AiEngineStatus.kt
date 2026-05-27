// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ai

/**
 * Публичный статус локального ИИ для chip'ов на Journal и Chat.
 */
enum class AiEngineStatus {
    /** Gemma прогрета, инференс идёт через LiteRT. */
    REAL_READY,

    /** Прогрев в WorkManager. */
    WARMING,

    /** Файл модели не подключён — ответы через встроенный сценарий без Gemma. */
    MODEL_OFFLINE,

    /** Модель подключена, но прогрев не удался — упрощённые отклики. */
    FALLBACK,
}
