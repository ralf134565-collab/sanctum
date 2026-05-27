// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketreflect.app.R
import com.pocketreflect.app.presentation.settings.backup.BackupSection
import com.pocketreflect.app.presentation.settings.export.DataExportSection
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_hub_data_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        DataSettingsContent(
            padding = padding,
            onShowSnackbar = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )
    }
}

@Composable
private fun DataSettingsContent(
    padding: PaddingValues,
    onShowSnackbar: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackupSection(onShowSnackbar = onShowSnackbar)
        DataExportSection(onShowSnackbar = onShowSnackbar)
    }
}
