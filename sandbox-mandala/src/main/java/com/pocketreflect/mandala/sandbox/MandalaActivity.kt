// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pocketreflect.mandala.sandbox.theme.MandalaTheme
import com.pocketreflect.mandala.sandbox.ui.MandalaScreen

class MandalaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MandalaTheme {
                MandalaScreen()
            }
        }
    }
}
