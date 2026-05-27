// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import dagger.Lazy

/** Обёртка для unit-тестов, где [RoomJournalRepository] ожидает `dagger.Lazy`. */
fun <T> testDaggerLazy(value: T): Lazy<T> = object : Lazy<T> {
    override fun get(): T = value
}
