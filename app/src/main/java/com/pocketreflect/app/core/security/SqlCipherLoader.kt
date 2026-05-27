// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

/**
 * Загружает нативную библиотеку SQLCipher один раз на процесс (только на Android).
 */
object SqlCipherLoader {
    @Volatile
    private var loaded = false

    fun ensureLoaded() {
        if (!DatabaseEncryptionPolicy.useSqlCipherInThisProcess()) return
        if (loaded) return
        synchronized(this) {
            if (!loaded) {
                System.loadLibrary("sqlcipher")
                loaded = true
            }
        }
    }
}
