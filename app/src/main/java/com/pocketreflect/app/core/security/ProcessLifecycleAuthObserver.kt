// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Слушает `ProcessLifecycleOwner` и сообщает [AuthSessionHolder]
 * о моменте ухода приложения в background и возврата в foreground.
 *
 * `ON_START` разблокирует БД до Activity `ON_RESUME`, чтобы репозитории
 * не падали в окне между возвратом из SAF и BiometricGate.
 */
@Singleton
class ProcessLifecycleAuthObserver @Inject constructor(
    private val authSessionHolder: AuthSessionHolder,
    private val userPreferencesRepository: UserPreferencesRepository,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        runBlocking {
            val timeoutMs = userPreferencesRepository.autoLockTimeout.first().millis
            authSessionHolder.tryUnlockIfSessionValid(timeoutMs)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        authSessionHolder.onAppBackgrounded()
    }
}
