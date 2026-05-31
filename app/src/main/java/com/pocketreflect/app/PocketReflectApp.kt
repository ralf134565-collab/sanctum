// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.pocketreflect.app.core.locale.AppLocales
import com.pocketreflect.app.core.locale.LanguageBootstrap
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DatabaseBootstrap
import com.pocketreflect.app.core.security.DatabaseEncryptionPolicy
import com.pocketreflect.app.core.security.ProcessLifecycleAuthObserver
import androidx.work.WorkManager
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Корневой Application-класс. Точка входа Hilt и кастомного WorkManager.
 *
 * Hilt-генерированный код подключается аннотацией [HiltAndroidApp].
 * Реализуем [Configuration.Provider], чтобы WorkManager-задачи
 * (прогрев модели) могли инжектить зависимости через Hilt.
 */
@HiltAndroidApp
class PocketReflectApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Слушатель `ProcessLifecycleOwner`, который сообщает [AuthSessionHolder]
     * о моменте ухода приложения в background — нужен для auto-lock биометрии.
     */
    @Inject
    lateinit var processLifecycleAuthObserver: ProcessLifecycleAuthObserver

    /**
     * Хилт-связанный `GemmaLocalEngine` (на самом деле `EngineCoordinator`).
     * Нужен, чтобы при системных сигналах нехватки памяти освободить
     * нативный буфер LiteRT-LM (~2.6 GB для E2B). На следующий вызов
     * движок проинициализируется заново (cold start ~10 c), что
     * приемлемая цена за то, чтобы Android не убил процесс целиком.
     */
    @Inject
    lateinit var gemmaLocalEngine: GemmaLocalEngine

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var databaseBootstrap: DatabaseBootstrap

    @Inject
    lateinit var languageBootstrap: LanguageBootstrap

    @Inject
    lateinit var authSessionHolder: AuthSessionHolder

    /**
     * Application-scoped корутинный scope для fire-and-forget задач уровня
     * процесса (release движка по `onTrimMemory`).
     *  - `SupervisorJob` — падение одной задачи не валит весь scope.
     *  - `Dispatchers.Default` — engine.release() выполняет нативный close,
     *    это compute-bound, а не I/O.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        databaseBootstrap.ensureReadyOnMainThread()
        val lockEnabled = runBlocking { userPreferencesRepository.biometricLockEnabled.first() }
        authSessionHolder.setRuntimeLockEnabled(lockEnabled)
        if (lockEnabled) {
            authSessionHolder.lockDatabase()
        } else {
            authSessionHolder.markAuthenticated()
        }
        // Регистрируется один раз на процесс. Сам observer не удерживает Activity,
        // поэтому утечек нет, а отписка не требуется.
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleAuthObserver)
        runBlocking {
            languageBootstrap.runIfNeeded()
            AppLocales.apply(userPreferencesRepository.appLanguage.first())
        }
        if (!DatabaseEncryptionPolicy.shouldStartApplicationBackgroundWork()) {
            return
        }
        cancelLegacyBackgroundWork()
    }

    /** Снимает фоновые задачи, удалённые из приложения. */
    private fun cancelLegacyBackgroundWork() {
        WorkManager.getInstance(this).apply {
            cancelUniqueWork(LEGACY_WEEKLY_SUMMARY_PERIODIC)
            cancelUniqueWork(LEGACY_WEEKLY_SUMMARY_ONE_TIME)
            cancelUniqueWork(LEGACY_TOMORROW_REMINDER_PERIODIC)
        }
    }

    /**
     * При сигнале нехватки памяти от системы (`TRIM_MEMORY_RUNNING_LOW` или
     * жёстче) аккуратно отпускаем нативный engine. Lower-severity сигналы
     * (`TRIM_MEMORY_UI_HIDDEN`, `TRIM_MEMORY_BACKGROUND`) сознательно
     * игнорируем — приложение скорее всего скоро снова станет переднимом,
     * а перезагружать 2.6 GB модели после простой минимизации — UX-провал.
     *
     * **Почему `@Suppress("DEPRECATION")`:** в Android 15 (API 35) большинство
     * level-констант `onTrimMemory` помечены deprecated, потому что новый
     * memory pressure API через `Process.lowMemoryReason()` точнее. НО:
     *  - `TRIM_MEMORY_RUNNING_LOW` остаётся **единственным** надёжным сигналом
     *    «released soon» для устройств Android 14 и ниже, которые мы поддерживаем
     *    (minSdk = 28). Полный переход на новый API требует minSdk = 35.
     *  - Сам колбэк `onTrimMemory(Int)` НЕ deprecated — deprecated только
     *    некоторые числовые значения параметра. Для нашей логики
     *    (`level >= TRIM_MEMORY_RUNNING_LOW`) это безопасно.
     *  - На API 35+ система по-прежнему дёргает эти константы для legacy-кода
     *    (источник: ActivityManagerService.scheduleAppTrimMemoryUsage).
     *
     * Когда поднимем minSdk до 35 — мигрируем на `Process.lowMemoryReason()`.
     */
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Числовая ловушка Android API: TRIM_MEMORY_UI_HIDDEN (20) численно
        // БОЛЬШЕ чем TRIM_MEMORY_RUNNING_LOW (10), хотя UI_HIDDEN — это
        // просто «свернул приложение», а не memory pressure. Раньше здесь
        // стояло `level >= TRIM_MEMORY_RUNNING_LOW` — это срабатывало на
        // каждое сворачивание и заставляло пользователя ждать 1-4 сек на
        // init при разворачивании обратно. Найдено в smoke-test 19 мая 2026.
        //
        // Освобождаем когда:
        //  (a) UI на экране И есть memory pressure (RUNNING_LOW/CRITICAL);
        //  (b) приложение глубоко в фоне (BACKGROUND и выше — близко к kill).
        // UI_HIDDEN (20) и RUNNING_MODERATE (5) игнорируем как noise.
        val shouldRelease = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> true
            else -> level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        }
        if (!shouldRelease) {
            Log.i(TAG, "onTrimMemory(level=$level): ignored (not memory pressure)")
            return
        }
        Log.i(TAG, "onTrimMemory(level=$level): releasing LiteRT engine")
        applicationScope.launch {
            runCatching { gemmaLocalEngine.release() }
                .onFailure { Log.w(TAG, "Engine release failed", it) }
        }
    }

    private companion object {
        const val TAG = "PocketReflectApp"
        const val LEGACY_WEEKLY_SUMMARY_PERIODIC = "weekly_summary_periodic"
        const val LEGACY_WEEKLY_SUMMARY_ONE_TIME = "weekly_summary_one_time"
        const val LEGACY_TOMORROW_REMINDER_PERIODIC = "daily_tomorrow_reminder_periodic"
    }
}
