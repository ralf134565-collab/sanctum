// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pocketreflect.app.core.security.BiometricGate
import com.pocketreflect.app.data.repository.AppThemeMode
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.presentation.bootstrap.DatabaseGate
import com.pocketreflect.app.presentation.bootstrap.WarmupGate
import com.pocketreflect.app.presentation.navigation.RootScaffold
import com.pocketreflect.app.ui.theme.PocketReflectTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        }
        super.recreate()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        observeScreenshotProtectionPreference()

        setContent {
            val themeMode by userPreferencesRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = AppThemeMode.DEFAULT,
            )

            PocketReflectTheme(themeMode = themeMode) {
                DatabaseGate {
                    BiometricGate {
                        WarmupGate {
                            RootScaffold()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Повторно синхронизируем флаг после возврата из фона / recreate —
        // SideEffect здесь больше не используется.
        lifecycleScope.launch {
            applyScreenshotProtection(
                userPreferencesRepository.screenshotProtectionEnabled.first(),
            )
        }
    }

    private fun observeScreenshotProtectionPreference() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferencesRepository.screenshotProtectionEnabled.collect { enabled ->
                    applyScreenshotProtection(enabled)
                }
            }
        }
    }

    private fun applyScreenshotProtection(enabled: Boolean) {
        val apply = {
            if (enabled) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE,
                )
            } else {
                // setFlags(0, mask) надёжнее clearFlags на части OEM-прошивок.
                window.setFlags(0, WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        if (window.decorView.isAttachedToWindow) {
            apply()
        } else {
            window.decorView.post(apply)
        }
    }
}
