/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.ui.rss

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.transdroid.R
import org.transdroid.data.RssFeed
import org.transdroid.ui.components.AddTorrentOptionsSheet
import org.transdroid.ui.components.DropdownPill
import org.transdroid.ui.components.EmptyState
import org.transdroid.ui.components.IconTooltip
import org.transdroid.ui.components.SelectionCheck
import org.transdroid.ui.components.SelectionCheckSize
import org.transdroid.ui.components.SelectionTopBar
import org.transdroid.ui.message
import org.transdroid.ui.theme.LocalStatusColors
import org.transdroid.util.formatBytes
import org.transdroid.util.formatRelativeAge
import org.transdroid.ui.components.TransdroidTextField

/** Room below the last row while selecting, so the Download button (56dp, 16dp margin) doesn't cover it. */
private val DownloadButtonClearance = 88.dp

/** A row's download button, and the room it keeps while selecting. */
private val DownloadButtonSize = 46.dp

/**
 * All configured feeds merged into one timeline, filterable to a single feed and/or "new only".
 * Matches design/mockups/transdroid-m3-rss(.html|-dark.html).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssScreen(
    viewModel: RssViewModel,
    initialFeedId: String?,
    onManageFeeds: () -> Unit,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmEntry by remember { mutableStateOf<RssEntry?>(null) }
    var showAddFeedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialFeedId) {
        if (initialFeedId != null) viewModel.setSelectedFeed(initialFeedId)
        viewModel.refresh()
    }

    val addedMessage = ui.addedItemTitle?.let { stringResource(R.string.add_success) + ": " + it }
    val addErrorMessage = ui.addError?.message()
    LaunchedEffect(addedMessage, addErrorMessage) {
        val message = addedMessage ?: addErrorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearAddResult()
        }
    }

    // Selection mode: Back ends it, and a bulk add reports back in a snackbar
    BackHandler(enabled = ui.selecting) { viewModel.clearSelection() }
    var showBulkAddSheet by remember { mutableStateOf(false) }
    val bulkTargets = ui.bulkTargets
    // A refresh or filter change can leave nothing to add while the sheet is open: close it, and
    // don't let it pop up again with the next selection
    LaunchedEffect(bulkTargets.isEmpty()) { if (bulkTargets.isEmpty()) showBulkAddSheet = false }
    val bulkAddMessage = ui.bulkAddResult?.let { result ->
        if (result.failed > 0) stringResource(R.string.rss_bulk_failed, result.failed, result.total)
        else stringResource(R.string.rss_bulk_added, result.total)
    }
    LaunchedEffect(bulkAddMessage) {
        if (bulkAddMessage != null) {
            snackbarHostState.showSnackbar(bulkAddMessage)
            viewModel.consumeBulkAddResult()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (ui.selecting && bulkTargets.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBulkAddSheet = true },
                    icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    text = { Text(stringResource(R.string.rss_download_selected, bulkTargets.size)) },
                )
            }
        },
        topBar = {
            Column {
                if (ui.selecting) {
                    SelectionTopBar(
                        count = ui.selection.size,
                        onClose = { viewModel.clearSelection() },
                        onSelectAll = { viewModel.selectAllVisible() },
                        showDivider = false,
                    )
                } else TopAppBar(
                    title = { Text(stringResource(R.string.rss_title)) },
                    navigationIcon = {
                        IconTooltip(stringResource(R.string.details_back)) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.details_back))
                            }
                        }
                    },
                    actions = {
                        IconTooltip(stringResource(R.string.torrents_refresh)) {
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.torrents_refresh))
                            }
                        }
                        IconTooltip(stringResource(R.string.rss_manage_feeds)) {
                            IconButton(onClick = onManageFeeds) {
                                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.rss_manage_feeds))
                            }
                        }
                    },
                )
                if (feeds.isNotEmpty()) {
                    val selectedFeed = feeds.firstOrNull { it.id == ui.selectedFeedId }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DropdownPill(
                            icon = Icons.Rounded.RssFeed,
                            label = selectedFeed?.displayName ?: stringResource(R.string.rss_all_feeds),
                            options = listOf<RssFeed?>(null) + feeds,
                            optionLabel = { it?.displayName ?: stringResource(R.string.rss_all_feeds) },
                            selected = selectedFeed,
                            onSelect = { viewModel.setSelectedFeed(it?.id) },
                        )
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = ui.newOnly,
                            onClick = { viewModel.setNewOnly(!ui.newOnly) },
                            label = { Text(stringResource(R.string.rss_new_only)) },
                            leadingIcon = {
                                if (ui.newOnly) Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                    if (!ui.loading) {
                        Text(
                            stringResource(
                                R.string.rss_summary,
                                ui.visibleEntries.size,
                                ui.newCount,
                                ui.lastUpdatedTimestamp?.let { formatRelativeAge(it, DateUtils.MINUTE_IN_MILLIS) }.orEmpty(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }
                }
                // A bulk add in progress; its rows turn "Added" one by one
                if (ui.bulkAdding) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                feeds.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.RssFeed,
                    title = stringResource(R.string.rss_no_feeds_title),
                    message = stringResource(R.string.rss_no_feeds_message),
                    modifier = Modifier.fillMaxSize().wrapContentSize(),
                    actions = {
                        Button(onClick = { showAddFeedDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.rss_add_feed))
                        }
                    },
                )
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    ui.error!!.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                ui.visibleEntries.isEmpty() -> Text(
                    stringResource(R.string.rss_no_items),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = if (ui.selecting) DownloadButtonClearance else 0.dp)) {
                    items(ui.visibleEntries, key = { it.key }) { entry ->
                        RssRow(
                            entry = entry,
                            added = entry.key in ui.addedKeys,
                            onAddClick = { confirmEntry = entry },
                            selecting = ui.selecting,
                            checked = entry.key in ui.selection,
                            selectable = canSelect(entry, ui.addedKeys),
                            onClick = { if (ui.selecting) viewModel.toggleSelected(entry) },
                            onLongClick = { viewModel.startSelection(entry) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    confirmEntry?.let { entry ->
        AddTorrentOptionsSheet(
            itemTitle = entry.item.title,
            serverName = ui.activeProfile?.displayName ?: "",
            recentLocations = ui.recentDownloadLocations,
            onAdd = { startPaused, downloadLocation -> viewModel.addItem(entry, startPaused, downloadLocation) },
            onDismiss = { confirmEntry = null },
        )
    }

    if (showBulkAddSheet && bulkTargets.isNotEmpty()) {
        AddTorrentOptionsSheet(
            itemTitle = bulkTargets.first().item.title,
            serverName = ui.activeProfile?.displayName ?: "",
            recentLocations = ui.recentDownloadLocations,
            onAdd = { startPaused, downloadLocation -> viewModel.addSelected(startPaused, downloadLocation) },
            onDismiss = { showBulkAddSheet = false },
            subtitle = stringResource(R.string.selection_subtitle, bulkTargets.size),
        )
    }

    if (showAddFeedDialog) {
        EditFeedDialog(
            onDismiss = { showAddFeedDialog = false },
            onSave = { name, url ->
                viewModel.saveFeed(RssFeed(id = viewModel.newFeedId(), name = name, url = url))
                showAddFeedDialog = false
            },
        )
    }
}

@Composable
private fun RssRow(
    entry: RssEntry,
    added: Boolean,
    onAddClick: () -> Unit,
    selecting: Boolean = false,
    checked: Boolean = false,
    selectable: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    // Long-press's gesture-free alternative for TalkBack users
    val selectLabel = stringResource(R.string.selection_select)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    .compositeOver(MaterialTheme.colorScheme.background)
                else Color.Transparent,
            )
            .semantics {
                if (!selecting && selectable) {
                    customActions = listOf(CustomAccessibilityAction(selectLabel) { onLongClick(); true })
                }
            }
            .then(
                when {
                    // While selecting, a tap toggles the row
                    selecting -> Modifier.clickable(enabled = selectable || checked, onClick = onClick)
                    // Otherwise only long-press does something (the download button adds), so the
                    // row isn't clickable: no ripple or TalkBack click action that does nothing
                    selectable -> Modifier.pointerInput(entry.key) {
                        detectTapGestures(
                            onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnLongClick()
                            },
                        )
                    }
                    else -> Modifier
                },
            )
            // The leading slot is wider than the dot, so less start padding keeps the dot in place
            .padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The new-item dot and the check circle share one slot, so the title doesn't shift when
        // selection mode starts or ends (rows that can't be selected keep the slot empty)
        Box(Modifier.size(SelectionCheckSize), contentAlignment = Alignment.Center) {
            if (selecting) {
                if (selectable) SelectionCheck(checked)
            } else {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (entry.isNew) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.isNew) FontWeight.Bold else FontWeight.SemiBold,
                color = if (entry.isNew) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                entry.feed.displayName,
                formatRelativeAge(entry.item.timestamp),
                entry.item.sizeBytes?.let { formatBytes(it) },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (added) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = LocalStatusColors.current.seeding,
                    modifier = Modifier.size(19.dp),
                )
                Text(stringResource(R.string.rss_added), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (selecting) {
            // Keeps the download button's room, so rows keep their width and height
            Spacer(Modifier.size(DownloadButtonSize))
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(DownloadButtonSize)
                    .clickable(enabled = entry.item.torrentUrl != null, onClick = onAddClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = stringResource(R.string.rss_download),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/** Also reused by [org.transdroid.ui.settings.SettingsScreen]'s RSS feeds section. */
@Composable
internal fun EditFeedDialog(onDismiss: () -> Unit, onSave: (name: String, url: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rss_add_feed)) },
        text = {
            Column {
                TransdroidTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                TransdroidTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.rss_feed_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), url.trim()) },
                enabled = url.trim().startsWith("http"),
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
        },
    )
}
