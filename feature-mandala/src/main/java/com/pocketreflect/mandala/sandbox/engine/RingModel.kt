// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

class RingModel(
    var radius: Float,
    var currentAngle: Float = 0f,
    var velocity: Float = 0f,
    val gapStartAngle: Float,
    var gapWidth: Float,
) {
    var alpha: Float = 1f
}
