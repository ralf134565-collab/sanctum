// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketreflect.app.data.repository.AutoLockTimeout
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel «привратника» биометрии.
 *
 * Принимает решение "lock / unlock" комбинацией:
 *  - флаг включения lock'а из [UserPreferencesRepository];
 *  - флаг текущей аутентификации из [AuthSessionHolder] (Singleton, переживает rotate);
 *  - taймер auto-lock из [UserPreferencesRepository.autoLockTimeout].
 *
 * `authAttemptId` инкрементируется при ручном retry — это «дёргает» `LaunchedEffect`
 * в [BiometricGate], чтобы перезапустить системный `BiometricPrompt` после
 * отмены или ошибки, не пересоздавая всю композицию.
 */
@HiltViewModel
class BiometricGateViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authSessionHolder: AuthSessionHolder,
) : ViewModel() {

    private val authAttemptId = MutableStateFlow(0)

    /**
     * Кэшируем текущее значение таймаута для нужд `onAppResumed()`:
     * метод вызывается из `LifecycleEventObserver`, где Flow.collect не уместен.
     */
    private val cachedTimeoutMs = MutableStateFlow(AutoLockTimeout.DEFAULT.millis)

    val state: StateFlow<GateState> =
        combine(
            userPreferencesRepository.biometricLockEnabled,
            authSessionHolder.isAuthenticated,
            authAttemptId,
        ) { lockEnabled, isAuthenticated, attemptId ->
            GateState.Resolved(
                isLocked = lockEnabled && !isAuthenticated,
                authAttemptId = attemptId,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GateState.Loading,
        )

    init {
        viewModelScope.launch {
            userPreferencesRepository.autoLockTimeout.collect { timeout ->
                cachedTimeoutMs.value = timeout.millis
            }
        }
    }

    /**
     * Вызывается из [BiometricGate] на `ON_RESUME`.
     * Если таймаут истёк — [AuthSessionHolder] сбросит флаг `isAuthenticated`,
     * что прокатится через combine и переведёт UI на lock-экран.
     */
    fun onAppResumed() {
        authSessionHolder.requiresAuth(cachedTimeoutMs.value)
    }

    /** Вызов из callback'а успешной биометрии. */
    fun onAuthenticated() {
        authSessionHolder.markAuthenticated()
    }

    /** Кнопка «Попробовать снова» на lock-экране — перезапускает prompt. */
    fun onRetryRequested() {
        authAttemptId.update { it + 1 }
    }
}

/**
 * Состояние «привратника».
 *
 * [Loading] — короткое окно до первой эмиссии из DataStore. В это время
 * композиция рендерит пустой фон, чтобы не было «вспышки» открытого журнала
 * у пользователя с включённым lock'ом.
 */
@Immutable
sealed interface GateState {
    data object Loading : GateState

    data class Resolved(
        val isLocked: Boolean,
        val authAttemptId: Int,
    ) : GateState
}
