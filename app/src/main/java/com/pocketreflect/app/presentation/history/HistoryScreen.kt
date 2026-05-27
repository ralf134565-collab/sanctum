// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.pocketreflect.app.presentation.components.CalmEmptyState
import com.pocketreflect.app.presentation.components.CalmLoadingIndicator
import com.pocketreflect.app.presentation.components.calmCardBorderStroke
import com.pocketreflect.app.presentation.components.screenAtmosphereGradient
import com.pocketreflect.app.ui.theme.PocketReflectShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import java.time.YearMonth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.pocketreflect.app.presentation.journal.components.SoftTextField
import com.pocketreflect.app.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.history.JournalSearchMatcher
import com.pocketreflect.app.domain.history.MonthTagFrequency
import androidx.compose.material.icons.outlined.Edit
import com.pocketreflect.app.core.time.DateFormats
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.presentation.journal.components.displayLabel

/**
 * Экран «История» — лента записей, сгруппированная по месяцам.
 *
 * Sticky-header вместо обычных карточек месяца — пользователь всегда видит,
 * к какому периоду относятся видимые записи (классический паттерн дневников
 * вроде Day One). Это важно, потому что иначе при скролле теряется контекст.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onOpenEntry: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryContract.Effect.NavigateToDetail -> onOpenEntry(effect.id)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // status bar inset уже отнят RootScaffold'ом — обнуляем оба измерения,
        // чтобы system insets не учитывались дважды (см. JournalScreen).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.nav_history),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = stringResource(R.string.history_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
                HistorySearchBar(
                    query = state.searchQuery,
                    onQueryChange = {
                        viewModel.onIntent(HistoryContract.Intent.UpdateSearchQuery(it))
                    },
                    onClear = { viewModel.onIntent(HistoryContract.Intent.ClearSearch) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (state.isSearchActive && !state.isSearchEmpty) {
                    Text(
                        text = stringResource(
                            R.string.history_search_results_count,
                            state.searchResultCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .screenAtmosphereGradient(),
        ) {
            when {
                state.isLoading -> LoadingState(innerPadding)
                state.loadFailed -> HistoryLoadErrorState(
                    padding = innerPadding,
                    onRetry = { viewModel.onIntent(HistoryContract.Intent.RetryLoad) },
                )
                else -> HistoryList(
                    padding = innerPadding,
                    groups = state.grouped,
                    personalManifesto = state.personalManifesto,
                    mentorIncludeManifesto = state.mentorIncludeManifesto,
                    weeklyIncludeManifesto = state.weeklyIncludeManifesto,
                    isSearchActive = state.isSearchActive,
                    isSearchEmpty = state.isSearchEmpty,
                    isEmpty = state.isEmpty,
                    searchFilterQuery = state.searchFilterQuery,
                    contentLanguage = state.contentLanguage,
                    onIntent = viewModel::onIntent,
                )
            }
        }
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CalmLoadingIndicator()
    }
}

@Composable
private fun HistoryLoadErrorState(
    padding: PaddingValues,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.history_load_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    padding: PaddingValues,
    groups: List<HistoryContract.MonthGroup>,
    personalManifesto: String,
    mentorIncludeManifesto: Boolean,
    weeklyIncludeManifesto: Boolean,
    isSearchActive: Boolean,
    isSearchEmpty: Boolean,
    isEmpty: Boolean,
    searchFilterQuery: String,
    contentLanguage: AppLanguage,
    onIntent: (HistoryContract.Intent) -> Unit,
) {
    var collapsedMonths by rememberSaveable { mutableStateOf(emptySet<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!isSearchActive) {
            item(key = "personal-manifesto") {
                PersonalManifestoCard(
                    manifesto = personalManifesto,
                    mentorIncludeManifesto = mentorIncludeManifesto,
                    weeklyIncludeManifesto = weeklyIncludeManifesto,
                    onSave = { onIntent(HistoryContract.Intent.UpdatePersonalManifesto(it)) },
                    onMentorIncludeChange = {
                        onIntent(HistoryContract.Intent.SetMentorIncludeManifesto(it))
                    },
                    onWeeklyIncludeChange = {
                        onIntent(HistoryContract.Intent.SetWeeklyIncludeManifesto(it))
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (isSearchEmpty) {
            item(key = "search-empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CalmEmptyState(
                        title = stringResource(R.string.history_search_empty_title),
                        body = stringResource(R.string.history_search_empty_body),
                        icon = Icons.Outlined.Search,
                    )
                }
            }
        } else if (isEmpty) {
            item(key = "empty-history") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CalmEmptyState(
                        title = stringResource(R.string.history_empty_title),
                        body = stringResource(R.string.history_empty_body),
                        icon = Icons.Outlined.Edit,
                    )
                }
            }
        }

        groups.forEach { group ->
            val monthKey = group.yearMonth.toString()
            val isCollapsed = !isSearchActive && collapsedMonths.contains(monthKey)

            stickyHeader(key = "month-$monthKey") {
                MonthHeader(
                    title = group.title,
                    count = group.entries.size,
                    topTagsLine = MonthTagFrequency.formatLine(group.topTags, contentLanguage),
                    isCollapsed = isCollapsed,
                    onToggleCollapse = {
                        collapsedMonths = if (isCollapsed) {
                            collapsedMonths - monthKey
                        } else {
                            collapsedMonths + monthKey
                        }
                    },
                )
            }
            if (!isCollapsed) {
                items(items = group.entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        searchFilterQuery = if (isSearchActive) searchFilterQuery else "",
                        contentLanguage = contentLanguage,
                        onClick = { onIntent(HistoryContract.Intent.OpenEntry(entry.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        placeholder = {
            Text(stringResource(R.string.history_search_hint))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.cd_history_search),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_history_search_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun MonthHeader(
    title: String,
    count: Int,
    topTagsLine: String?,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggleCollapse)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Icon(
                imageVector = if (isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                contentDescription = stringResource(if (isCollapsed) R.string.cd_expand else R.string.cd_collapse),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }
        if (!topTagsLine.isNullOrBlank()) {
            Text(
                text = topTagsLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, end = 28.dp),
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    searchFilterQuery: String,
    contentLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    val accentTag = entry.moodTags.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = PocketReflectShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = calmCardBorderStroke(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (accentTag != null) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .padding(start = 0.dp)
                        .width(3.dp)
                        .height(48.dp)
                        .background(moodStripeColor(accentTag)),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val locale = DateFormats.javaLocale(LocalConfiguration.current)
                Text(
                    text = DateFormats.shortDay(entry.timestamp, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TagsPreview(tags = entry.moodTags)
                val preview = if (searchFilterQuery.isNotBlank()) {
                    JournalSearchMatcher.matchPreview(entry, searchFilterQuery, contentLanguage)
                        ?: previewLine(entry)
                } else {
                    previewLine(entry)
                }
                if (preview.isNotBlank()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun moodStripeColor(tag: MoodTag): androidx.compose.ui.graphics.Color =
    when (tag.polarity) {
        MoodTag.Polarity.POSITIVE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        MoodTag.Polarity.NEUTRAL -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        MoodTag.Polarity.NEGATIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }

@Composable
private fun TagsPreview(tags: List<MoodTag>) {
    if (tags.isEmpty()) return
    val visible = tags.take(3)
    val extra = tags.size - visible.size
    val labels = visible.map { it.displayLabel() }
    Text(
        text = buildString {
            labels.forEachIndexed { idx, label ->
                if (idx > 0) append(" · ")
                append(label)
            }
            if (extra > 0) append(" · +$extra")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

/** Берём первую непустую строку из пользовательских полей (без AI-отклика). */
private fun previewLine(entry: JournalEntry): String {
    // Приоритет: рефлексия → микро-победы → задачи на завтра.
    // AI-отклик НЕ используем — он часто начинается с однотипных фраз
    // и создаёт визуальное дублирование в ленте.
    val sources = listOf(entry.reflection, entry.microWins, entry.tomorrowTasks, entry.customFieldAnswer)
    return sources
        .asSequence()
        .map { it.lineSequence().firstOrNull { line -> line.isNotBlank() }?.trim().orEmpty() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

/**
 * Компактная карточка «Личные ориентиры» — только заголовок и статус.
 * Текст и переключатели контекста — в [ManifestoEditorDialog].
 */
@Composable
private fun PersonalManifestoCard(
    manifesto: String,
    mentorIncludeManifesto: Boolean,
    weeklyIncludeManifesto: Boolean,
    onSave: (String) -> Unit,
    onMentorIncludeChange: (Boolean) -> Unit,
    onWeeklyIncludeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.manifesto_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(R.string.manifesto_edit_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }

    if (showEditDialog) {
        ManifestoEditorDialog(
            initialText = manifesto,
            mentorIncludeManifesto = mentorIncludeManifesto,
            weeklyIncludeManifesto = weeklyIncludeManifesto,
            onMentorIncludeChange = onMentorIncludeChange,
            onWeeklyIncludeChange = onWeeklyIncludeChange,
            onDismiss = { showEditDialog = false },
            onSave = {
                onSave(it)
                showEditDialog = false
            }
        )
    }
}

/**
 * Полноэкранный диалог редактирования ориентиров.
 */
@Composable
private fun ManifestoEditorDialog(
    initialText: String,
    mentorIncludeManifesto: Boolean,
    weeklyIncludeManifesto: Boolean,
    onMentorIncludeChange: (Boolean) -> Unit,
    onWeeklyIncludeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val contextEnabled = text.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false // Позволяет диалогу корректно обрабатывать инсеты клавиатуры и статус-бара
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = stringResource(R.string.manifesto_edit_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = { onSave(text) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.action_save_general))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable container for Hint and SoftTextField only, so the Header remains static at the top
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SoftTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = stringResource(R.string.manifesto_placeholder),
                        minLines = 15,
                        minHeight = 280.dp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = stringResource(R.string.manifesto_context_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.manifesto_include_mentor),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (contextEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                        Switch(
                            checked = mentorIncludeManifesto,
                            onCheckedChange = onMentorIncludeChange,
                            enabled = contextEnabled,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.manifesto_include_weekly),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (contextEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                        Switch(
                            checked = weeklyIncludeManifesto,
                            onCheckedChange = onWeeklyIncludeChange,
                            enabled = contextEnabled,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
