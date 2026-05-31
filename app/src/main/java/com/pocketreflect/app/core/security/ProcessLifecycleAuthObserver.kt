// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkManager
import com.pocketreflect.app.core.work.ModelWarmupWorker
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Слушает `ProcessLifecycleOwner` и сообщает [AuthSessionHolder]
 * о моменте ухода приложения в background и возврата в foreground.
 *
 * В on-demand режиме (прогрев при запуске выключен) освобождает native engine
 * при сворачивании приложения, чтобы не удерживать ~2.6 GB RAM в фоне.
 */
@Singleton
class ProcessLifecycleAuthObserver @Inject constructor(
    private val authSessionHolder: AuthSessionHolder,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val workManager: WorkManager,
    private val gemmaLocalEngine: GemmaLocalEngine,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStart(owner: LifecycleOwner) {
        runBlocking {
            val lockEnabled = userPreferencesRepository.biometricLockEnabled.first()
            authSessionHolder.setRuntimeLockEnabled(lockEnabled)
            if (lockEnabled) {
                val timeoutMs = userPreferencesRepository.autoLockTimeout.first().millis
                authSessionHolder.tryUnlockIfSessionValid(timeoutMs)
            } else {
                authSessionHolder.ensureAccessibleWithoutRuntimeLock()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        runBlocking {
            authSessionHolder.setRuntimeLockEnabled(
                userPreferencesRepository.biometricLockEnabled.first(),
            )
        }
        authSessionHolder.onAppBackgrounded()
        scope.launch {
            if (!userPreferencesRepository.warmupOnLaunchEnabled.first()) {
                workManager.cancelUniqueWork(ModelWarmupWorker.UNIQUE_WORK_NAME)
                runCatching { gemmaLocalEngine.release() }
            }
        }
    }
}
