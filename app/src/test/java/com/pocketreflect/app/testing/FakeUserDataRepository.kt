// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.repository.UserDataRepository

class FakeUserDataRepository : UserDataRepository {
    var wipeInvocations: Int = 0
        private set
    var shouldThrowOnWipe: Throwable? = null

    override suspend fun wipeAllUserContent() {
        shouldThrowOnWipe?.let { throw it }
        wipeInvocations++
    }
}
