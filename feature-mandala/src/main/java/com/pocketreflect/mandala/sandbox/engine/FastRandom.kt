// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

object FastRandom {
    private var state: Int = 0x1357_9BDF

    fun reseed(seed: Int) {
        state = seed
    }

    fun nextInt(bound: Int): Int {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        if (bound <= 0) return 0
        val positive = state and Int.MAX_VALUE
        return positive % bound
    }

    fun nextSignedFloat(range: Float): Float {
        val raw = nextInt(1000)
        return (raw / 500f - 1f) * range
    }
}
