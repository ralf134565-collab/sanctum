// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    @Test
    fun isAngleInGap_returnsTrueInsideSimpleGap() {
        val ring = RingModel(radius = 100f, gapStartAngle = 10f, gapWidth = 20f, currentAngle = 0f)
        assertTrue(Collision.isAngleInGap(15f, ring))
    }

    @Test
    fun isAngleInGap_returnsFalseOutsideSimpleGap() {
        val ring = RingModel(radius = 100f, gapStartAngle = 10f, gapWidth = 20f, currentAngle = 0f)
        assertFalse(Collision.isAngleInGap(45f, ring))
    }

    @Test
    fun isAngleInGap_respectsRingRotation() {
        val ring = RingModel(radius = 100f, gapStartAngle = 0f, gapWidth = 30f, currentAngle = 90f)
        assertTrue(Collision.isAngleInGap(100f, ring))
    }

    @Test
    fun isAngleInGap_handlesWrapAroundGap() {
        val ring = RingModel(radius = 100f, gapStartAngle = 350f, gapWidth = 20f, currentAngle = 0f)
        assertTrue(Collision.isAngleInGap(355f, ring))
        assertTrue(Collision.isAngleInGap(5f, ring))
        assertFalse(Collision.isAngleInGap(180f, ring))
    }

    @Test
    fun normalizeAngle_wrapsNegativeAndOverflow() {
        assertTrue(Collision.normalizeAngle(-10f) == 350f)
        assertTrue(Collision.normalizeAngle(370f) == 10f)
    }
}
