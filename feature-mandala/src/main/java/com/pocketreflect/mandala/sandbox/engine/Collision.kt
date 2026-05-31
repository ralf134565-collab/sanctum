// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

import kotlin.math.atan2
import kotlin.math.sqrt

object Collision {
    const val RING_THICKNESS = 15f

    fun isAngleInGap(angle: Float, ring: RingModel): Boolean {
        val normalizedAngle = normalizeAngle(angle)
        val gapStart = normalizeAngle(ring.gapStartAngle + ring.currentAngle)
        val gapEnd = normalizeAngle(gapStart + ring.gapWidth)

        return if (gapStart <= gapEnd) {
            normalizedAngle in gapStart..gapEnd
        } else {
            normalizedAngle >= gapStart || normalizedAngle <= gapEnd
        }
    }

    fun particleAngleDegrees(particleX: Float, particleY: Float, centerX: Float, centerY: Float): Float {
        val dx = particleX - centerX
        val dy = particleY - centerY
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) {
            angle += 360f
        }
        return angle
    }

    fun distanceFromCenter(particleX: Float, particleY: Float, centerX: Float, centerY: Float): Float {
        val dx = particleX - centerX
        val dy = particleY - centerY
        return sqrt(dx * dx + dy * dy)
    }

    fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) {
            normalized += 360f
        }
        return normalized
    }
}
