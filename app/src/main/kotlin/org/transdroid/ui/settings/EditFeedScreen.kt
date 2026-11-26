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
package org.transdroid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.transdroid.R
import org.transdroid.data.RssFeed
import org.transdroid.ui.components.IconTooltip
import org.transdroid.ui.components.TransdroidTextField

/**
 * Adds an RSS feed, or edits the one with [feedId]: its name, URL and Transdroid 2-style title
 * filters. Laid out like [EditSearchIndexerScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFeedScreen(
    viewModel: SettingsViewModel,
    feedId: String?,
    onBack: () -> Unit,
) {
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val existing = feeds.firstOrNull { it.id == feedId }

    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var url by rememberSaveable(existing?.id) { mutableStateOf(existing?.url.orEmpty()) }
    var includeFilter by rememberSaveable(existing?.id) { mutableStateOf(existing?.includeFilter.orEmpty()) }
    var excludeFilter by rememberSaveable(existing?.id) { mutableStateOf(existing?.excludeFilter.orEmpty()) }

    fun save() {
        // Filters are kept as typed: spaces can be part of a word, as in Transdroid 2
        val feed = existing?.edited(name.trim(), url.trim(), includeFilter, excludeFilter)
            ?: RssFeed(
                id = viewModel.newFeedId(),
                name = name.trim(),
                url = url.trim(),
                includeFilter = includeFilter,
                excludeFilter = excludeFilter,
            )
        viewModel.saveFeed(feed)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.rss_add_feed else R.string.rss_edit_feed)) },
                navigationIcon = {
                    IconTooltip(stringResource(R.string.details_back)) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.details_back),
                            )
                        }
                    }
                },
                actions = {
                    if (existing != null) {
                        IconTooltip(stringResource(R.string.rss_delete_feed)) {
                            IconButton(onClick = {
                                viewModel.deleteFeed(existing.id)
                                onBack()
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.rss_delete_feed))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TransdroidTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TransdroidTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.rss_feed_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                stringResource(R.string.rss_filters_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TransdroidTextField(
                value = includeFilter,
                onValueChange = { includeFilter = it },
                label = { Text(stringResource(R.string.rss_include_filter)) },
                supportingText = { Text(stringResource(R.string.rss_include_filter_summary)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TransdroidTextField(
                value = excludeFilter,
                onValueChange = { excludeFilter = it },
                label = { Text(stringResource(R.string.rss_exclude_filter)) },
                supportingText = { Text(stringResource(R.string.rss_exclude_filter_summary)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = ::save,
                enabled = url.trim().startsWith("http"),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save))
            }
        }
    }
}
