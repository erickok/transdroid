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
package org.transdroid.ui.search

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.transdroid.R
import org.transdroid.protocol.search.SearchResult
import org.transdroid.ui.message
import org.transdroid.util.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmResult by remember { mutableStateOf<SearchResult?>(null) }
    val queryFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val addedMessage = ui.addedTitle?.let { stringResource(R.string.add_success) + ": " + it }
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
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (providers.isEmpty()) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        stringResource(R.string.search_no_providers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                }
                return@Column
            }

            OutlinedTextField(
                value = ui.query,
                onValueChange = { viewModel.setQuery(it) },
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.search()
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_title))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.search()
                    keyboardController?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(queryFocusRequester),
            )
            LaunchedEffect(Unit) {
                queryFocusRequester.requestFocus()
                keyboardController?.show()
            }

            if (providers.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                val selected = providers.firstOrNull { it.id == ui.selectedProviderId } ?: providers.first()
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    OutlinedTextField(
                        value = selected.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.search_provider)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    viewModel.selectProvider(provider.id)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.searching -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        ui.error!!.message(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )
                    ui.searched && ui.results.isEmpty() -> Text(
                        stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(ui.results) { result ->
                            ListItem(
                                headlineContent = {
                                    Text(result.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    val size = result.sizeBytes?.let { formatBytes(it) }
                                    val seeders = result.seeders?.let {
                                        stringResource(R.string.search_seeders, it, result.leechers ?: 0)
                                    }
                                    Text(listOfNotNull(size, seeders).joinToString(" · "))
                                },
                                modifier = Modifier.fillMaxWidth().clickable { confirmResult = result },
                            )
                        }
                    }
                }
            }
        }
    }

    confirmResult?.let { result ->
        AlertDialog(
            onDismissRequest = { confirmResult = null },
            title = { Text(stringResource(R.string.rss_add_item_title)) },
            text = { Text(result.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addResult(result)
                    confirmResult = null
                }) { Text(stringResource(R.string.add_title)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResult = null }) { Text(stringResource(R.string.details_cancel)) }
            },
        )
    }
}
