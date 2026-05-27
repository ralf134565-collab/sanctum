// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import com.pocketreflect.app.data.local.AppDatabase
import kotlinx.coroutines.flow.StateFlow

/**
 * Жизненный цикл зашифрованной Room-БД в процессе.
 *
 * [lock] закрывает инстанс и стирает ключ из RAM; [unlock] собирает новый
 * Room и инкрементирует [revision], чтобы репозитории переподписали Flow'ы.
 */
interface DatabaseProvider {
    val revision: StateFlow<Long>

    /** `true`, пока ключ в RAM стёрт и Room закрыт (background / auto-lock). */
    val isLocked: StateFlow<Boolean>

    fun get(): AppDatabase

    fun lock()

    fun unlock()
}
