// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticlePoolTest {

    @Test
    fun acquire_reusesDeadParticle() {
        val pool = ParticlePool(capacity = 2)
        val first = pool.acquire()
        assertNotNull(first)
        first!!.isAlive = true

        val second = pool.acquire()
        assertNotNull(second)
        second!!.isAlive = true

        first.isAlive = false

        val reused = pool.acquire()
        assertTrue(reused === first)
    }

    @Test
    fun acquire_returnsNullWhenPoolFull() {
        val pool = ParticlePool(capacity = 2)
        pool.acquire()?.let { it.isAlive = true }
        pool.acquire()?.let { it.isAlive = true }
        assertNull(pool.acquire())
    }

    @Test
    fun resetAll_clearsAliveFlags() {
        val pool = ParticlePool(capacity = 3)
        pool.acquire()?.let { it.isAlive = true }
        pool.acquire()?.let { it.isAlive = true }
        pool.resetAll()
        assertEquals(0, pool.aliveCount())
    }

    @Test
    fun spawnCursor_wrapsAcrossCapacity() {
        val pool = ParticlePool(capacity = 2)
        val first = pool.acquire()
        val second = pool.acquire()
        first?.isAlive = false
        second?.isAlive = false
        val third = pool.acquire()
        assertNotNull(third)
    }
}
