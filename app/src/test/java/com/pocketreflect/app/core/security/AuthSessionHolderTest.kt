// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import com.pocketreflect.app.testing.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthSessionHolderTest {

    private lateinit var clock: FakeClock
    private lateinit var databaseProvider: TrackingDatabaseProvider
    private lateinit var holder: AuthSessionHolder

    @Before
    fun setUp() {
        clock = FakeClock(fixedNowMillis = 1_000_000L)
        databaseProvider = TrackingDatabaseProvider()
        holder = AuthSessionHolder(clock, databaseProvider)
    }

    @Test
    fun runtimeLockDisabled_background_doesNotLockDatabase() {
        holder.setRuntimeLockEnabled(false)
        holder.markAuthenticated()
        databaseProvider.lockInvocations = 0

        holder.onAppBackgrounded()

        assertEquals(0, databaseProvider.lockInvocations)
        assertTrue(holder.isAuthenticated.value)
    }

    @Test
    fun runtimeLockEnabled_background_locksDatabase() {
        holder.setRuntimeLockEnabled(true)
        holder.markAuthenticated()
        databaseProvider.lockInvocations = 0

        holder.onAppBackgrounded()

        assertEquals(1, databaseProvider.lockInvocations)
    }

    @Test
    fun runtimeLockDisabled_requiresAuth_neverPrompts() {
        holder.setRuntimeLockEnabled(false)
        holder.markAuthenticated()
        databaseProvider.unlockInvocations = 0

        val requiresAuth = holder.requiresAuth(timeoutMs = 1L)

        assertFalse(requiresAuth)
        assertTrue(holder.isAuthenticated.value)
        assertTrue(databaseProvider.unlockInvocations >= 1)
    }

    @Test
    fun runtimeLockEnabled_sessionExpired_requiresAuth() {
        holder.setRuntimeLockEnabled(true)
        holder.markAuthenticated()
        holder.onAppBackgrounded()
        clock.fixedNowMillis += 120_000L

        val requiresAuth = holder.requiresAuth(timeoutMs = 60_000L)

        assertTrue(requiresAuth)
        assertFalse(holder.isAuthenticated.value)
    }

    @Test
    fun runtimeLockEnabled_withinTimeout_unlocksWithoutPrompt() {
        holder.setRuntimeLockEnabled(true)
        holder.markAuthenticated()
        holder.onAppBackgrounded()
        clock.fixedNowMillis += 30_000L
        databaseProvider.unlockInvocations = 0

        val requiresAuth = holder.requiresAuth(timeoutMs = 60_000L)

        assertFalse(requiresAuth)
        assertTrue(holder.isAuthenticated.value)
        assertTrue(databaseProvider.unlockInvocations >= 1)
    }

    @Test
    fun requireLockAfterEnabling_clearsSessionAndLocks() {
        holder.setRuntimeLockEnabled(true)
        holder.markAuthenticated()

        holder.requireLockAfterEnabling()

        assertFalse(holder.isAuthenticated.value)
        assertTrue(databaseProvider.isLocked.value)
    }
}
