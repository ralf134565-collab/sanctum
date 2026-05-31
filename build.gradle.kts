// SPDX-License-Identifier: GPL-3.0-or-later
// Корневой build-скрипт. Здесь только декларация плагинов — подключение в модулях.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
