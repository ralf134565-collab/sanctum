// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import com.pocketreflect.app.core.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-singleton, хранящий состояние «разблокировано / нет» и управляющий
 * криптографической блокировкой базы данных через [DatabaseProvider].
 *
 * Runtime lock (close Room + wipe passphrase from RAM on background) применяется
 * **только** при включённой биометрической блокировке ([runtimeLockEnabled]).
 */
@Singleton
class AuthSessionHolder @Inject constructor(
    private val clock: Clock,
    private val databaseProvider: DatabaseProvider,
) {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val isLocked: Boolean
        get() = !_isAuthenticated.value

    /**
     * Включена ли биометрическая блокировка (из [UserPreferencesRepository]).
     * Обновляется из [ProcessLifecycleAuthObserver] и настроек.
     */
    @Volatile
    var runtimeLockEnabled: Boolean = false
        private set

    /**
     * Когда в последний раз приложение ушло в background (`ON_STOP`).
     * Используется, чтобы решить, истёк ли auto-lock таймаут.
     */
    private val _lastBackgroundedAtMs = MutableStateFlow<Long?>(null)
    val lastBackgroundedAtMs: StateFlow<Long?> = _lastBackgroundedAtMs.asStateFlow()

    fun setRuntimeLockEnabled(enabled: Boolean) {
        runtimeLockEnabled = enabled
        if (!enabled) {
            ensureAccessibleWithoutRuntimeLock()
        }
    }

    /** Вызывается из [BiometricGateViewModel] после успешной аутентификации. */
    fun markAuthenticated() {
        unlockDatabase()
        _isAuthenticated.value = true
        _lastBackgroundedAtMs.value = null
    }

    /**
     * Без runtime lock: БД всегда доступна, сессия считается аутентифицированной.
     */
    fun ensureAccessibleWithoutRuntimeLock() {
        unlockDatabase()
        _isAuthenticated.value = true
        _lastBackgroundedAtMs.value = null
    }

    /**
     * Пользователь включил биометрию в настройках — закрываем БД и требуем разблокировку.
     */
    fun requireLockAfterEnabling() {
        _isAuthenticated.value = false
        _lastBackgroundedAtMs.value = null
        lockDatabase()
    }

    /** Закрывает базу данных и полностью стирает кодовую фразу из оперативной памяти. */
    fun lockDatabase() {
        databaseProvider.lock()
    }

    /** Пересобирает Room и сигнализирует репозиториям о новой версии БД. */
    fun unlockDatabase() {
        databaseProvider.unlock()
    }

    /** Вызывается из [ProcessLifecycleAuthObserver] в `ON_STOP`. */
    fun onAppBackgrounded() {
        if (!runtimeLockEnabled) return
        if (_isAuthenticated.value) {
            _lastBackgroundedAtMs.value = clock.nowMillis()
            lockDatabase()
        }
    }

    /**
     * Разблокирует БД при возврате в foreground, если сессия ещё валидна.
     * Вызывается из [ProcessLifecycleAuthObserver] в `ON_START` — до Activity `ON_RESUME`.
     */
    fun tryUnlockIfSessionValid(timeoutMs: Long) {
        if (!runtimeLockEnabled) {
            ensureAccessibleWithoutRuntimeLock()
            return
        }
        if (!_isAuthenticated.value) return
        val backgroundedAt = _lastBackgroundedAtMs.value
        if (backgroundedAt == null) {
            unlockDatabase()
            return
        }
        val elapsed = clock.nowMillis() - backgroundedAt
        if (elapsed < timeoutMs) {
            unlockDatabase()
        }
    }

    /**
     * Проверка: нужно ли при текущем `ON_RESUME` снова показать prompt.
     */
    fun requiresAuth(timeoutMs: Long): Boolean {
        if (!runtimeLockEnabled) {
            ensureAccessibleWithoutRuntimeLock()
            return false
        }

        if (!_isAuthenticated.value) {
            lockDatabase()
            return true
        }

        val backgroundedAt = _lastBackgroundedAtMs.value
        if (backgroundedAt == null) {
            unlockDatabase()
            return false
        }

        val elapsed = clock.nowMillis() - backgroundedAt
        val expired = elapsed >= timeoutMs
        if (expired) {
            _isAuthenticated.value = false
            _lastBackgroundedAtMs.value = null
            lockDatabase()
        } else {
            unlockDatabase()
        }
        return expired
    }
}
