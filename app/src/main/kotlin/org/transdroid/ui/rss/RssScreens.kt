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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import org.transdroid.R
import org.transdroid.data.RssFeed
import org.transdroid.protocol.rss.RssItem
import org.transdroid.ui.message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssFeedsScreen(
    viewModel: RssViewModel,
    onOpenFeed: (String) -> Unit,
    onBack: () -> Unit,
) {
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    var editingFeed by remember { mutableStateOf<RssFeed?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rss_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.rss_add_feed))
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (feeds.isEmpty()) {
                Text(
                    stringResource(R.string.rss_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(feeds, key = { it.id }) { feed ->
                        ListItem(
                            headlineContent = { Text(feed.displayName) },
                            supportingContent = {
                                Text(feed.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { editingFeed = feed }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.rss_delete_feed),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { onOpenFeed(feed.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EditFeedDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, url ->
                viewModel.saveFeed(RssFeed(id = viewModel.newFeedId(), name = name, url = url))
                showAddDialog = false
            },
        )
    }

    editingFeed?.let { feed ->
        AlertDialog(
            onDismissRequest = { editingFeed = null },
            title = { Text(stringResource(R.string.rss_delete_feed)) },
            text = { Text(feed.displayName) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFeed(feed.id)
                    editingFeed = null
                }) { Text(stringResource(R.string.details_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { editingFeed = null }) { Text(stringResource(R.string.details_cancel)) }
            },
        )
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssItemsScreen(
    viewModel: RssViewModel,
    feedId: String,
    onBack: () -> Unit,
) {
    val state by viewModel.items.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmItem by remember { mutableStateOf<RssItem?>(null) }

    LaunchedEffect(feedId) { viewModel.openFeed(feedId) }

    val addedTitle = state.addedItemTitle
    val addedMessage = if (addedTitle != null) stringResource(R.string.add_success) + ": " + addedTitle else null
    val addErrorMessage = state.addError?.message()
    LaunchedEffect(addedMessage, addErrorMessage) {
        val message = addedMessage ?: addErrorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearAddResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.feed?.displayName ?: stringResource(R.string.rss_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                state.items.isEmpty() -> Text(
                    stringResource(R.string.rss_no_items),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        val isNew = state.newSinceTimestamp != null && item.timestamp != null &&
                            item.timestamp!! > state.newSinceTimestamp!!
                        ListItem(
                            headlineContent = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                item.timestamp?.let {
                                    Text(
                                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                            .format(Date(it * 1000))
                                    )
                                }
                            },
                            trailingContent = {
                                if (isNew) Badge { Text(stringResource(R.string.rss_new)) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.torrentUrl != null) { confirmItem = item },
                        )
                    }
                }
            }
        }
    }

    confirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmItem = null },
            title = { Text(stringResource(R.string.rss_add_item_title)) },
            text = { Text(item.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addItem(item)
                    confirmItem = null
                }) { Text(stringResource(R.string.add_title)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmItem = null }) { Text(stringResource(R.string.details_cancel)) }
            },
        )
    }
}
