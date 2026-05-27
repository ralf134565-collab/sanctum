// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

/**
 * Выбор аппаратного бэкенда LiteRT-LM для локальной Gemma 4.
 *
 *  - [GPU] — задействует OpenCL через `libOpenCL.so` (см. `AndroidManifest`).
 *    Быстрее prefill/decode на топ-устройствах; требует драйвер OpenCL.
 *    На эмуляторе без OpenCL и на ряде устройств низкого класса не
 *    инициализируется — в этом случае `LiteRtGemmaEngine` молча делает
 *    fallback на [CPU].
 *  - [CPU] — универсальный путь, работает везде, но медленнее (особенно
 *    для E4B).
 *
 * Перечисление сознательно живёт в `data/ai/`, а не в domain: это
 * runtime-аспект конкретной реализации движка. Пользователю он становится
 * виден только через настройку «GPU/CPU» в Sub-PR #3c — там добавится
 * persistence в `ModelSelectionRepository` и UI toggle.
 *
 * NPU-бэкенд (Snapdragon 8 Elite SM8750, QCS8275, Intel PTL) сюда
 * сознательно не добавлен — для него нужна отдельная фаза с детектированием
 * `Build.SOC_MODEL` и упаковкой нативных библиотек (Sub-PR #6).
 */
enum class EngineBackend { GPU, CPU }
