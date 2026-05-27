// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Обертка для безопасного хранения пароля в оперативной памяти Compose.
 * Вместо String использует реактивный SnapshotStateList<Char>, что предотвращает
 * оседание иммутабельных строк в JVM String Pool и куче.
 */
class SecurePasswordState(
    val chars: SnapshotStateList<Char> = mutableStateListOf()
) {
    val text: String
        get() = chars.joinToString("")

    val length: Int
        get() = chars.size

    fun isNotEmpty(): Boolean = chars.isNotEmpty()

    fun update(newText: String) {
        chars.clear()
        chars.addAll(newText.toList())
    }

    fun toCharArray(): CharArray {
        val result = CharArray(chars.size)
        for (i in chars.indices) {
            result[i] = chars[i]
        }
        return result
    }

    fun clear() {
        chars.clear()
    }
}

@Composable
fun rememberSecurePasswordState(): SecurePasswordState {
    return remember { SecurePasswordState() }
}
