// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обёртка над [BiometricManager], которая отвечает на один вопрос:
 * «может ли пользователь прямо сейчас разблокировать приложение?».
 *
 * Authenticators намеренно объединяют `BIOMETRIC_STRONG` и `DEVICE_CREDENTIAL`:
 *  - пользователь с отпечатком/лицом проходит биометрию;
 *  - пользователь, который предпочитает PIN/паттерн/пароль системы, проходит
 *    через системный экран ввода кода блокировки;
 *  - в результате нам НЕ нужно держать собственный PIN-экран
 *    (он остаётся отдельным PR, как и оговорено в брифе фазы A).
 */
@Singleton
class BiometricAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Проверка ровно того набора аутентификаторов, которым потом будем разблокировать.
     * Делать иначе небезопасно: статус по `BIOMETRIC_STRONG` не равен статусу
     * по `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`.
     */
    fun status(): Status {
        val raw = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return when (raw) {
            BiometricManager.BIOMETRIC_SUCCESS -> Status.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Status.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Status.Unavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Status.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Status.Unavailable
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Status.Unavailable
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Status.Unavailable
            else -> Status.Unavailable
        }
    }

    /**
     * Развёрнутый sealed-результат вместо raw `Int` — чтобы UI-слой не зависел
     * напрямую от констант `BiometricManager`.
     */
    sealed interface Status {
        /** Можно показывать prompt и включать переключатель. */
        data object Available : Status

        /** Устройство в принципе не умеет биометрию И не имеет screen lock. */
        data object NoHardware : Status

        /** Биометрия есть, но ни одного датчика/credential не зарегистрировано. */
        data object NoneEnrolled : Status

        /** Аппаратно временно недоступно или ОС в неконсистентном состоянии. */
        data object Unavailable : Status
    }
}
