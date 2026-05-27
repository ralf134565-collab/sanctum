// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра «глубокая ночь».
 *
 * Принципы:
 *  - Все цвета подобраны для тёмной темы — чтобы не «бить» по глазам перед сном.
 *  - Контрастность текста проверяется по WCAG AA на фоне Background.
 *  - Акценты приглушённые (никакого неонового пурпура из дефолтного шаблона):
 *    цель — атмосфера медитации, а не «приложения для тасок».
 */

// Базовые фоны
val MidnightBackground = Color(0xFF0B1020)   // основной фон
val MidnightSurface    = Color(0xFF131A2E)   // карточки
val MidnightSurfaceVar = Color(0xFF1B2440)   // выделенные блоки
val MidnightOutline    = Color(0xFF2A3457)

// Текст
val TextPrimary   = Color(0xFFEAEAF4)
val TextSecondary = Color(0xFFB4B8C9)
val TextMuted     = Color(0xFF8086A1)

// Акценты (медитативные)
val LavenderPrimary   = Color(0xFFB9A6FF)   // основной акцент
val LavenderOnPrimary = Color(0xFF1E1839)
val LavenderContainer = Color(0xFF2D2451)
val LavenderOnContainer = Color(0xFFDDD3FF)

val MintSecondary   = Color(0xFF8FD8C4)     // вторичный (позитивные теги, success)
val MintOnSecondary = Color(0xFF103128)
val MintContainer   = Color(0xFF1F4A3F)
val MintOnContainer = Color(0xFFD5F1E7)

val AmberWarning   = Color(0xFFE6B873)      // нейтральные теги (усталость)
val AmberOnWarning = Color(0xFF3A2A0A)

val CoralCritical    = Color(0xFFE89093)    // только для критических ошибок
val CoralOnCritical  = Color(0xFF3D0E10)

// Светлая тема — мягкий дневной фон, те же lavender/mint, без неона
val DawnBackground = Color(0xFFF3F4FA)
val DawnSurface    = Color(0xFFFFFFFF)
val DawnSurfaceVar = Color(0xFFE8EBF5)
val DawnOutline    = Color(0xFFC8D0E3)

val DawnTextPrimary   = Color(0xFF1A1F33)
val DawnTextSecondary = Color(0xFF5C6478)

val DawnLavenderPrimary     = Color(0xFF6B5BD6)
val DawnLavenderOnPrimary   = Color(0xFFFFFFFF)
val DawnLavenderContainer   = Color(0xFFE8E4FF)
val DawnLavenderOnContainer = Color(0xFF2D2451)

val DawnMintSecondary     = Color(0xFF2F8F78)
val DawnMintOnSecondary   = Color(0xFFFFFFFF)
val DawnMintContainer     = Color(0xFFD5F1E7)
val DawnMintOnContainer   = Color(0xFF103128)

val DawnAmberWarning   = Color(0xFF9A6F24)
val DawnAmberOnWarning = Color(0xFFFFFFFF)

val DawnCoralCritical   = Color(0xFFB8484C)
val DawnCoralOnCritical = Color(0xFFFFFFFF)
