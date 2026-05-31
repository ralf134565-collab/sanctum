// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import com.pocketreflect.app.testing.FakeClock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultDatabaseAccessTest {

    private lateinit var databaseProvider: TrackingDatabaseProvider
    private lateinit var authSessionHolder: AuthSessionHolder
    private lateinit var databaseAccess: DefaultDatabaseAccess

    @Before
    fun setUp() {
        databaseProvider = TrackingDatabaseProvider()
        authSessionHolder = AuthSessionHolder(FakeClock(), databaseProvider)
        databaseAccess = DefaultDatabaseAccess(databaseProvider, authSessionHolder)
    }

    @Test
    fun isReady_falseWhenLocked() = runTest {
        authSessionHolder.setRuntimeLockEnabled(true)
        authSessionHolder.requireLockAfterEnabling()

        assertFalse(databaseAccess.isReady.first())
    }

    @Test
    fun isReady_trueWhenAuthenticatedAndUnlocked() = runTest {
        authSessionHolder.setRuntimeLockEnabled(false)
        authSessionHolder.markAuthenticated()

        assertTrue(databaseAccess.isReady.first())
    }

    @Test
    fun observeWhenReady_emitsSentinelWhileNotReady() = runTest {
        authSessionHolder.setRuntimeLockEnabled(true)
        authSessionHolder.requireLockAfterEnabling()

        val value = databaseAccess.observeWhenReady(emptyList<String>()) {
            flowOf(listOf("live"))
        }.first()

        assertEquals(emptyList<String>(), value)
    }

    @Test
    fun whenReady_executesAfterUnlock() = runTest {
        authSessionHolder.setRuntimeLockEnabled(true)
        authSessionHolder.requireLockAfterEnabling()

        val deferred = async {
            databaseAccess.whenReady { "ok" }
        }
        authSessionHolder.markAuthenticated()

        assertEquals("ok", deferred.await())
    }
}
