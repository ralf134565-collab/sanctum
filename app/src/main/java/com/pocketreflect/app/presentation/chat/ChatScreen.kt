// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketreflect.app.R
import com.pocketreflect.app.domain.chat.ChatCustomPersonaPolicy
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.ai.AiEngineStatus
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.presentation.components.AiEngineStatusIcon
import com.pocketreflect.app.presentation.components.AiStatusDialog
import com.pocketreflect.app.presentation.components.bottomInputBarInsets
import com.pocketreflect.app.domain.chat.ChatRole
import com.pocketreflect.app.presentation.components.CalmTypingIndicator
import com.pocketreflect.app.presentation.components.screenAtmosphereGradient
import com.pocketreflect.app.presentation.journal.components.SoftTextField
import com.pocketreflect.app.ui.theme.PocketReflectShapes
import com.pocketreflect.app.ui.theme.PocketReflectTypographyAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToModelSettings: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAiStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ChatContract.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    if (!state.disclaimerAccepted) {
        ChatDisclaimerScreen(onAccept = { viewModel.onIntent(ChatContract.Intent.AcceptDisclaimer) })
        return
    }

    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ChatContract.Intent.DismissClearChat) },
            title = { Text(stringResource(R.string.chat_clear_confirm_title)) },
            text = { Text(stringResource(R.string.chat_clear_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(ChatContract.Intent.ConfirmClearChat) }) {
                    Text(stringResource(R.string.chat_clear_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(ChatContract.Intent.DismissClearChat) }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    if (state.showPersonaSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(ChatContract.Intent.ClosePersonaSheet) },
            sheetState = sheetState,
        ) {
            PersonaSheetContent(
                selected = state.persona,
                customPersonaEnabled = state.customPersonaEnabled,
                customPersonaPrompt = state.customPersonaPrompt,
                journalEnabled = state.journalContextEnabled,
                journalDays = state.journalContextDays,
                manifestoEnabled = state.manifestoContextEnabled,
                aiEngineStatus = state.aiEngineStatus,
                onPersona = { viewModel.onIntent(ChatContract.Intent.SelectPersona(it)) },
                onJournalEnabled = { viewModel.onIntent(ChatContract.Intent.SetJournalContextEnabled(it)) },
                onJournalDays = { viewModel.onIntent(ChatContract.Intent.SetJournalContextDays(it)) },
                onManifestoEnabled = { viewModel.onIntent(ChatContract.Intent.SetManifestoContextEnabled(it)) },
                onNavigateToModelSettings = {
                    viewModel.onIntent(ChatContract.Intent.ClosePersonaSheet)
                    onNavigateToModelSettings()
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // Как JournalScreen / HistoryScreen: insets уже в innerPadding NavHost.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ChatTopBar(
                contextPercent = state.contextPercent,
                isContextFull = state.isContextFull,
                isCompacting = state.isCompacting,
                persona = state.persona,
                personaChipLabel = state.personaChipLabel,
                aiEngineStatus = state.aiEngineStatus,
                onOpenPersona = { viewModel.onIntent(ChatContract.Intent.OpenPersonaSheet) },
                onClear = { viewModel.onIntent(ChatContract.Intent.RequestClearChat) },
                onCompact = { viewModel.onIntent(ChatContract.Intent.CompactChat) },
                onAiStatusClick = { showAiStatusDialog = true },
            )
        },
        bottomBar = {
            ChatInputBar(
                text = state.inputText,
                enabled = !state.isContextFull && !state.isStreaming,
                onTextChange = { viewModel.onIntent(ChatContract.Intent.UpdateInput(it)) },
                onSend = { viewModel.onIntent(ChatContract.Intent.SendMessage) },
                onCancel = { viewModel.onIntent(ChatContract.Intent.CancelStreaming) },
                isStreaming = state.isStreaming,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .screenAtmosphereGradient(),
        ) {
            ChatMessageList(
                messages = state.messages,
                streamingPreview = state.streamingPreview,
                isStreaming = state.isStreaming,
                persona = state.persona,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    if (showAiStatusDialog) {
        AiStatusDialog(
            status = state.aiEngineStatus,
            onDismiss = { showAiStatusDialog = false },
            onNavigateToModelSettings = onNavigateToModelSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    contextPercent: Int,
    isContextFull: Boolean,
    isCompacting: Boolean,
    persona: ChatPersona,
    personaChipLabel: String,
    aiEngineStatus: AiEngineStatus,
    onOpenPersona: () -> Unit,
    onClear: () -> Unit,
    onCompact: () -> Unit,
    onAiStatusClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.chat_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                    Text(
                        text = stringResource(R.string.chat_context_label, contextPercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isContextFull) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            },
            actions = {
                IconButton(onClick = onAiStatusClick) {
                    AiEngineStatusIcon(status = aiEngineStatus)
                }
                Spacer(modifier = Modifier.width(4.dp))
                AssistChip(
                    onClick = onOpenPersona,
                    label = {
                        Text(
                            text = persona.topBarLabel(personaChipLabel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.chat_clear),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        LinearProgressIndicator(
            progress = { contextPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = if (isContextFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
        )
        if (isContextFull) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.chat_context_full),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = onCompact,
                    enabled = !isCompacting
                ) {
                    Text(
                        text = if (isCompacting) "..." else stringResource(R.string.chat_compact),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    streamingPreview: String?,
    isStreaming: Boolean,
    persona: ChatPersona,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val displayItems = buildList {
        addAll(messages)
        if (isStreaming && !streamingPreview.isNullOrBlank()) {
            add(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = streamingPreview,
                    timestamp = 0L,
                ),
            )
        }
    }
    LaunchedEffect(displayItems.size, streamingPreview) {
        if (displayItems.isNotEmpty()) {
            listState.animateScrollToItem(displayItems.lastIndex)
        }
    }
    if (displayItems.isEmpty() && !isStreaming) {
        val hintRes = when (persona) {
            ChatPersona.GENTLE_MENTOR -> R.string.chat_empty_hint_gentle_mentor
            ChatPersona.EXPERIENCED_FRIEND -> R.string.chat_empty_hint_experienced_friend
            ChatPersona.SUPPORTIVE_COACH -> R.string.chat_empty_hint_supportive_coach
            ChatPersona.FREE_DIALOG -> R.string.chat_empty_hint_free_dialog
            ChatPersona.CUSTOM -> R.string.chat_empty_hint
        }
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(hintRes),
                style = PocketReflectTypographyAccent.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(displayItems, key = { "${it.id}_${it.timestamp}_${it.content.hashCode()}" }) { message ->
            ChatBubble(message = message)
        }
        if (isStreaming && streamingPreview.isNullOrBlank()) {
            item(key = "typing") {
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CalmTypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 4.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 16.dp,
        )
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .background(color, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(0.88f),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, end = 8.dp)
                        .width(2.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(color, shape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isStreaming: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomInputBarInsets(keyboardClosedBottomPadding = 10.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SoftTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = stringResource(R.string.chat_input_hint),
            modifier = Modifier.weight(1f),
            enabled = enabled,
            minLines = 1,
            maxLines = 3,
            minHeight = 48.dp,
        )
        if (isStreaming) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.chat_cancel),
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.chat_send),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonaSheetContent(
    selected: ChatPersona,
    customPersonaEnabled: Boolean,
    customPersonaPrompt: String,
    journalEnabled: Boolean,
    journalDays: Int,
    manifestoEnabled: Boolean,
    aiEngineStatus: AiEngineStatus,
    onPersona: (ChatPersona) -> Unit,
    onJournalEnabled: (Boolean) -> Unit,
    onJournalDays: (Int) -> Unit,
    onManifestoEnabled: (Boolean) -> Unit,
    onNavigateToModelSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (aiEngineStatus) {
            AiEngineStatus.FALLBACK -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.chat_model_standby_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            AiEngineStatus.MODEL_OFFLINE -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.chat_model_offline_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(
                            onClick = onNavigateToModelSettings,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.chat_model_inactive_action),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            AiEngineStatus.REAL_READY, AiEngineStatus.WARMING -> Unit
        }

        Text(
            text = stringResource(R.string.chat_persona_title),
            style = MaterialTheme.typography.titleMedium,
        )
        val selectablePersonas = buildList {
            addAll(ChatCustomPersonaPolicy.BUILT_IN_PERSONAS)
            if (ChatCustomPersonaPolicy.isConfigured(customPersonaEnabled, customPersonaPrompt)) {
                add(ChatPersona.CUSTOM)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectablePersonas.forEach { persona ->
                FilterChip(
                    selected = persona == selected,
                    onClick = { onPersona(persona) },
                    label = { Text(persona.label()) },
                    leadingIcon = {
                        Icon(
                            imageVector = persona.icon(),
                            contentDescription = null,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chat_journal_context_switch),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = journalEnabled, onCheckedChange = onJournalEnabled)
        }
        if (journalEnabled) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to R.string.chat_journal_days_1, 3 to R.string.chat_journal_days_3, 7 to R.string.chat_journal_days_7)
                    .forEach { (days, labelRes) ->
                        FilterChip(
                            selected = journalDays == days,
                            onClick = { onJournalDays(days) },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.chat_manifesto_context_switch),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = manifestoEnabled, onCheckedChange = onManifestoEnabled)
        }
    }
}

@Composable
private fun ChatPersona.icon() = when (this) {
    ChatPersona.GENTLE_MENTOR -> Icons.Outlined.Favorite
    ChatPersona.EXPERIENCED_FRIEND -> Icons.Outlined.Visibility
    ChatPersona.SUPPORTIVE_COACH -> Icons.AutoMirrored.Outlined.TrendingUp
    ChatPersona.FREE_DIALOG -> Icons.Outlined.Forum
    ChatPersona.CUSTOM -> Icons.Outlined.Edit
}

@Composable
private fun ChatPersona.topBarLabel(customChipLabel: String): String = when (this) {
    ChatPersona.CUSTOM -> customChipLabel
    else -> label()
}

@Composable
fun ChatDisclaimerScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_disclaimer_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.chat_disclaimer_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_disclaimer_checkbox_row")
                    .clickable { checked = !checked },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("chat_disclaimer_checkbox"),
                )
                Text(
                    text = stringResource(R.string.chat_disclaimer_checkbox),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Button(
                onClick = onAccept,
                enabled = checked,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .testTag("chat_disclaimer_continue"),
            ) {
                Text(stringResource(R.string.chat_disclaimer_continue))
            }
        }
    }
}

@Composable
private fun ChatPersona.label(): String = when (this) {
    ChatPersona.GENTLE_MENTOR -> stringResource(R.string.chat_persona_gentle_mentor)
    ChatPersona.EXPERIENCED_FRIEND -> stringResource(R.string.chat_persona_experienced_friend)
    ChatPersona.SUPPORTIVE_COACH -> stringResource(R.string.chat_persona_supportive_coach)
    ChatPersona.FREE_DIALOG -> stringResource(R.string.chat_persona_free_dialog)
    ChatPersona.CUSTOM -> stringResource(R.string.chat_persona_custom)
}
