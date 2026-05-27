// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.locale

import com.pocketreflect.app.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageBootstrap @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend fun runIfNeeded() {
        userPreferencesRepository.ensureFirstRunLanguageBootstrap()
    }
}
