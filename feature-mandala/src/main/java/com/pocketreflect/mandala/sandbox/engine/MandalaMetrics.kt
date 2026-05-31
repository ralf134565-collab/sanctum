// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

class MandalaMetrics {
    var coreFill: Int = 0
    var channelPasses: Int = 0
    var totalSpawned: Int = 0

    fun reset() {
        coreFill = 0
        channelPasses = 0
        totalSpawned = 0
    }

    fun fillPercent(threshold: Int): Float {
        if (threshold <= 0) return 0f
        return (coreFill.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
    }
}
