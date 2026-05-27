// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R

/**
 * Гейт перед основным контентом приложения.
 *
 * Контракт:
 *  - Пока [BiometricGateViewModel] не отдал первое значение — рендерится пустой
 *    тёмный фон. Это убирает «вспышку» открытого журнала у пользователя
 *    с включённым lock'ом на cold start.
 *  - Если lock включён и сессия не аутентифицирована — рендерится [LockScreen]
 *    и автоматически открывается системный `BiometricPrompt`.
 *  - При успешной аутентификации — рендерится переданный [content].
 *
 * Регистрация `ON_RESUME` нужна, чтобы при возврате из background проверить
 * истёк ли auto-lock таймаут (см. [BiometricGateViewModel.onAppResumed]).
 */
@Composable
fun BiometricGate(
    viewModel: BiometricGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val current = state) {
        GateState.Loading -> EmptyLoadingBackground()

        is GateState.Resolved -> {
            if (current.isLocked) {
                LockScreen(
                    authAttemptId = current.authAttemptId,
                    onAuthenticated = viewModel::onAuthenticated,
                    onRetry = viewModel::onRetryRequested,
                )
            } else {
                content()
            }
        }
    }
}

@Composable
private fun EmptyLoadingBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

/**
 * Lock-экран PocketReflect.
 *
 * Стиль соответствует «тихой вечерней» эстетике дневника:
 *  - чистый тёмный фон, никаких ярких акцентов;
 *  - один смысловой блок по центру;
 *  - кнопка «Разблокировать» крупная, но без агрессии (filled, не tonal).
 *
 * При первой композиции (и при каждом инкременте [authAttemptId])
 * автоматически открывает `BiometricPrompt`. Пользователь может закрыть
 * системный диалог — и тогда останется на этом экране с кнопкой ручного retry.
 */
@Composable
private fun LockScreen(
    authAttemptId: Int,
    onAuthenticated: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptDescription = stringResource(R.string.biometric_prompt_description)

    LaunchedEffect(authAttemptId) {
        val activity = context.findFragmentActivity() ?: return@LaunchedEffect
        showBiometricPrompt(
            activity = activity,
            title = promptTitle,
            description = promptDescription,
            onSuccess = onAuthenticated,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.lock_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.lock_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.lock_screen_unlock_button),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * Один проход системного `BiometricPrompt`.
 *
 * Authenticators: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` — допускаем
 * fallback на системный PIN/паттерн/пароль. Поэтому `setNegativeButtonText`
 * НЕ задаём (биометрия + device credential несовместимы с custom negative button).
 *
 * Колбэки `onAuthenticationError` и `onAuthenticationFailed` намеренно пустые:
 * по UX-сценарию пользователь должен сам нажать «Попробовать снова»,
 * чтобы не «бомбить» его повторным системным диалогом при случайной отмене.
 */
private fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    description: String,
    onSuccess: () -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    }
    val prompt = BiometricPrompt(activity, executor, callback)
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setDescription(description)
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(info)
}

/**
 * Раскручивает [ContextWrapper] до [FragmentActivity].
 *
 * Compose отдаёт нам обёрнутый контекст (тема + другие декораторы),
 * поэтому прямой каст в Activity не сработает. Цикл из ContextWrapper'ов —
 * стандартный идиом для Compose.
 */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
