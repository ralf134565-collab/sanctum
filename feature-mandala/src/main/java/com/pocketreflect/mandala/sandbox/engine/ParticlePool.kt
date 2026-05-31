// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

class ParticlePool(private val capacity: Int = 300) {
    val items = Array(capacity) { Particle() }
    private var spawnCursor = 0

    fun acquire(): Particle? {
        var scanned = 0
        while (scanned < capacity) {
            val particle = items[spawnCursor]
            spawnCursor = (spawnCursor + 1) % capacity
            scanned++
            if (!particle.isAlive) {
                return particle
            }
        }
        return null
    }

    fun resetAll() {
        var index = 0
        while (index < capacity) {
            items[index].isAlive = false
            items[index].alpha = 1f
            index++
        }
        spawnCursor = 0
    }

    fun aliveCount(): Int {
        var count = 0
        var index = 0
        while (index < capacity) {
            if (items[index].isAlive) {
                count++
            }
            index++
        }
        return count
    }
}
