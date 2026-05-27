// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

/**
 * На устройстве/эмуляторе Room открывается через SQLCipher.
 * На JVM (Robolectric unit-тесты) нативной `libsqlcipher` нет — используем plain SQLite.
 */
object DatabaseEncryptionPolicy {

    fun useSqlCipherInThisProcess(): Boolean = !isRobolectricUnitTest()

    /** WorkManager / periodic workers — только на реальном Android, не на JVM-тестах. */
    fun shouldStartApplicationBackgroundWork(): Boolean = !isRobolectricUnitTest()

    fun isRobolectricUnitTest(): Boolean =
        try {
            Class.forName("org.robolectric.RuntimeEnvironment")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
}
