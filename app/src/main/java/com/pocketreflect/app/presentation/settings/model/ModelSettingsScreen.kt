// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.settings.model

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.data.model.ModelManifest
import com.pocketreflect.app.presentation.settings.model.components.AttachProgressBlock
import com.pocketreflect.app.presentation.settings.model.components.AttachedModelCard
import com.pocketreflect.app.presentation.settings.model.components.BackendToggle
import com.pocketreflect.app.presentation.settings.model.components.WarmupOnLaunchToggle
import com.pocketreflect.app.presentation.settings.model.components.ModelInfoCard
import com.pocketreflect.app.presentation.settings.model.components.ModelVariantCard

/**
 * Экран выбора и подключения локальной модели Gemma 4.
 *
 * Жизненный цикл:
 *  - всегда показывает обе карточки вариантов (E2B / E4B) — пользователь
 *    может в любой момент сменить вариант, даже когда один уже подключён;
 *  - при подключённой модели сверху появляется [AttachedModelCard] со
 *    статусом и кнопками «Заменить» / «Удалить»;
 *  - во время копирования файла поверх карточек появляется [AttachProgressBlock]
 *    (sealed `progress != null` запрещает второй параллельный attach).
 *
 * SAF-лаунчер запускается реактивно: как только VM переводит `pickingForVariant`
 * в не-null значение, [LaunchedEffect] стартует системный пикер. Это гарантирует
 * корректное поведение при rotate (rememberLauncherForActivityResult переживает
 * пересоздание composition).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    onBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onIntent(ModelSettingsContract.Intent.FilePicked(uri))
        } else {
            viewModel.onIntent(ModelSettingsContract.Intent.FilePickerCancelled)
        }
    }

    LaunchedEffect(state.pickingForVariant) {
        if (state.pickingForVariant != null) {
            // У `.litertlm` нет общепринятого MIME-типа. `*/*` даёт пользователю
            // увидеть любой файл в Downloads; SHA-256 верификация защитит
            // от случайного выбора неподходящего.
            pickLauncher.launch(arrayOf("*/*"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ModelSettingsContract.Effect.OpenExternalUrl -> {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
                is ModelSettingsContract.Effect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // status bar inset уже отнят RootScaffold'ом — обнуляем (см. JournalScreen).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.model_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Content(
            padding = padding,
            state = state,
            onIntent = viewModel::onIntent,
        )
    }

    if (state.isConfirmingDetach) {
        DetachConfirmDialog(
            onCancel = { viewModel.onIntent(ModelSettingsContract.Intent.CancelDetach) },
            onConfirm = { viewModel.onIntent(ModelSettingsContract.Intent.ConfirmDetach) },
        )
    }
}

@Composable
private fun Content(
    padding: PaddingValues,
    state: ModelSettingsContract.State,
    onIntent: (ModelSettingsContract.Intent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ModelInfoCard()

        state.attached?.let { attached ->
            AttachedModelCard(
                attached = attached,
                onReplace = {
                    onIntent(ModelSettingsContract.Intent.StartAttach(attached.variant))
                },
                onRequestDetach = {
                    onIntent(ModelSettingsContract.Intent.RequestDetach)
                },
            )
        }

        state.progress?.let { progress ->
            AttachProgressBlock(progress = progress)
        }

        // Тогл бэкенда показываем всегда — пользователь может выбрать GPU/CPU
        // ещё до подключения первой модели, и тогда engine инициализируется
        // под нужный бэкенд сразу при первом инференсе, без лишнего реинита.
        BackendToggle(
            selected = state.selectedBackend,
            onSelect = { backend ->
                onIntent(ModelSettingsContract.Intent.SelectBackend(backend))
            },
        )

        WarmupOnLaunchToggle(
            enabled = state.attached != null,
            checked = state.warmupOnLaunchEnabled,
            onCheckedChange = { enabled ->
                onIntent(ModelSettingsContract.Intent.SetWarmupOnLaunch(enabled))
            },
        )

        state.variants.forEach { variant ->
            ModelVariantCard(
                variant = variant,
                entry = ModelManifest.entryOf(variant),
                isSourcesExpanded = state.expandedSourcesFor == variant,
                onToggleSources = {
                    onIntent(ModelSettingsContract.Intent.ToggleSources(variant))
                },
                onOpenSource = { url ->
                    onIntent(ModelSettingsContract.Intent.OpenSource(url))
                },
                onAttach = {
                    onIntent(ModelSettingsContract.Intent.StartAttach(variant))
                },
                isAttachInProgress = state.progress != null,
            )
        }
    }
}

@Composable
private fun DetachConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.model_detach_confirm_title)) },
        text = { Text(stringResource(R.string.model_detach_confirm_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.model_detach_confirm_button),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
