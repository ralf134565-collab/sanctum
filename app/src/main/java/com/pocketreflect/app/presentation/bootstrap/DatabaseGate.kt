// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.bootstrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.pocketreflect.app.core.security.DatabaseAccessState
import com.pocketreflect.app.core.security.DatabaseAccessStatus
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Блокирует основной UI, если локальная БД недоступна (битый ключ / сбой миграции).
 * Должен быть **вне** [com.pocketreflect.app.core.security.BiometricGate] — recovery
 * не требует биометрии.
 */
@Composable
fun DatabaseGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val accessState = EntryPointAccessors.fromApplication(
        context.applicationContext,
        DatabaseGateEntryPoint::class.java,
    ).databaseAccessState()
    val status by accessState.status.collectAsStateWithLifecycle()

    when (status) {
        DatabaseAccessStatus.Ready -> content()
        is DatabaseAccessStatus.Blocked -> DatabaseUnavailableScreen()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseGateEntryPoint {
    fun databaseAccessState(): DatabaseAccessState
}
