// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.locale

import com.pocketreflect.app.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AppLanguageResolver @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    val resolved: Flow<AppLanguage> =
        userPreferencesRepository.appLanguage.map { AppLanguage.resolve(it) }

    suspend fun resolvedNow(): AppLanguage =
        AppLanguage.resolve(userPreferencesRepository.appLanguage.first())
}
