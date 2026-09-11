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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.transdroid.R
import org.transdroid.data.RssFeed
import org.transdroid.ui.components.DropdownPill
import org.transdroid.ui.components.EmptyState
import org.transdroid.ui.message
import org.transdroid.ui.theme.LocalStatusColors
import org.transdroid.util.formatBytes
import org.transdroid.util.formatRelativeAge
import org.transdroid.ui.components.TransdroidTextField

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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.rss_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.details_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.torrents_refresh))
                        }
                        IconButton(onClick = onManageFeeds) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.rss_manage_feeds))
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
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(ui.visibleEntries, key = { it.key }) { entry ->
                        RssRow(
                            entry = entry,
                            added = entry.key in ui.addedKeys,
                            onAddClick = { confirmEntry = entry },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    confirmEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmEntry = null },
            title = { Text(stringResource(R.string.rss_add_item_title)) },
            text = { Text(entry.item.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addItem(entry)
                    confirmEntry = null
                }) { Text(stringResource(R.string.add_title)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEntry = null }) { Text(stringResource(R.string.details_cancel)) }
            },
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
private fun RssRow(entry: RssEntry, added: Boolean, onAddClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (entry.isNew) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
        )
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
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(46.dp)
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
