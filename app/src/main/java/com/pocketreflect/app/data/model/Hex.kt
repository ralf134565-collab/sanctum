// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

/**
 * Hex-кодирование без зависимости от JDK 17 `HexFormat` —
 * единообразно работает на Android (compileSdk 36) и в pure-JVM юнит-тестах.
 *
 * Используем нижний регистр, чтобы вывод напрямую сравнивался со значением
 * из `ModelManifest.expectedSha256`, которое тоже хранится в нижнем регистре.
 */
internal fun ByteArray.toLowerHex(): String {
    val chars = CharArray(this.size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        chars[i * 2] = HEX_DIGITS[v ushr 4]
        chars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
    }
    return String(chars)
}

private val HEX_DIGITS: CharArray = "0123456789abcdef".toCharArray()
