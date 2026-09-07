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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.transdroid.R
import org.transdroid.protocol.search.SearchResult
import org.transdroid.ui.message
import org.transdroid.ui.theme.LocalStatusColors
import org.transdroid.util.formatBytes
import org.transdroid.util.formatRelativeAge

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

    fun doSearch() {
        viewModel.search()
        keyboardController?.hide()
    }

    Scaffold(
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                if (providers.isEmpty()) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.details_back))
                    }
                } else {
                    SearchBarPill(
                        query = ui.query,
                        onQueryChange = viewModel::setQuery,
                        onBack = onBack,
                        onSearch = ::doSearch,
                        focusRequester = queryFocusRequester,
                    )
                    LaunchedEffect(Unit) {
                        queryFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    if (providers.size > 1) {
                        val selected = providers.firstOrNull { it.id == ui.selectedProviderId } ?: providers.first()
                        DropdownPill(
                            icon = Icons.Rounded.Dns,
                            label = selected.displayName,
                            options = providers,
                            optionLabel = { it.displayName },
                            selected = selected,
                            onSelect = { viewModel.selectProvider(it.id) },
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                        )
                    }
                    ControlsRow(
                        indexerFilter = ui.indexerFilter,
                        availableIndexers = ui.availableIndexers,
                        onIndexerFilterChange = viewModel::setIndexerFilter,
                        sort = ui.sort,
                        onSortChange = viewModel::setSort,
                    )
                    val indexerFilter = ui.indexerFilter
                    if (ui.searched && ui.error == null) {
                        Text(
                            if (indexerFilter == null) {
                                stringResource(R.string.search_result_count, ui.visibleResults.size)
                            } else {
                                stringResource(R.string.search_result_count_filtered, ui.visibleResults.size, indexerFilter)
                            },
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
                providers.isEmpty() -> Text(
                    stringResource(R.string.search_no_providers),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                ui.searching -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    ui.error!!.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                ui.searched && ui.visibleResults.isEmpty() -> Text(
                    stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(ui.visibleResults) { result ->
                        ResultRow(result = result, onAddClick = { confirmResult = result })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

/** Back arrow + query field + clear button in one rounded capsule, matching
 *  design/mockups/transdroid-m3-search.html. */
@Composable
private fun SearchBarPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.details_back))
        }
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear))
            }
        }
    }
}

@Composable
private fun ControlsRow(
    indexerFilter: String?,
    availableIndexers: List<String>,
    onIndexerFilterChange: (String?) -> Unit,
    sort: SearchSort,
    onSortChange: (SearchSort) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (availableIndexers.size > 1) {
            val options = listOf<String?>(null) + availableIndexers
            DropdownPill(
                icon = Icons.Rounded.Dns,
                label = indexerFilter ?: stringResource(R.string.search_all_indexers),
                options = options,
                optionLabel = { it ?: stringResource(R.string.search_all_indexers) },
                selected = indexerFilter,
                onSelect = onIndexerFilterChange,
            )
        }
        Spacer(Modifier.weight(1f))
        DropdownPill(
            icon = Icons.AutoMirrored.Rounded.Sort,
            label = sort.label(),
            options = SearchSort.entries,
            optionLabel = { it.label() },
            selected = sort,
            onSelect = onSortChange,
        )
    }
}

@Composable
private fun <T> DropdownPill(
    icon: ImageVector,
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                .clickable { expanded = true },
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 8.dp).height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = {
                        if (option == selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun SearchSort.label(): String = stringResource(
    when (this) {
        SearchSort.SEEDERS -> R.string.search_sort_seeders
        SearchSort.LEECHERS -> R.string.search_sort_leechers
        SearchSort.SIZE -> R.string.search_sort_size
        SearchSort.NEWEST -> R.string.search_sort_newest
    }
)

@Composable
private fun ResultRow(result: SearchResult, onAddClick: () -> Unit) {
    val statusColors = LocalStatusColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                result.sizeBytes?.let { formatBytes(it) },
                formatRelativeAge(result.timestamp),
                result.indexerName,
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
            val seeders = result.seeders
            val leechers = result.leechers
            if (seeders != null || leechers != null) {
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (seeders != null) {
                        StatChip(
                            icon = Icons.Rounded.ArrowUpward,
                            value = seeders,
                            color = if (seeders == 0) MaterialTheme.colorScheme.error else statusColors.seeding,
                        )
                    }
                    if (leechers != null) {
                        StatChip(icon = Icons.Rounded.ArrowDownward, value = leechers, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(46.dp).clickable(onClick = onAddClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.search_download),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(value.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
