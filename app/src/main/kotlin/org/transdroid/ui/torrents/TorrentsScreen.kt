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
package org.transdroid.ui.torrents

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.transdroid.R
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentStatus
import org.transdroid.ui.message
import org.transdroid.ui.statusLabel
import org.transdroid.ui.theme.accentColor
import org.transdroid.ui.theme.trackColor
import org.transdroid.ui.components.EmptyState
import org.transdroid.ui.components.SortDirectionIcon
import org.transdroid.ui.components.TorrentProgressIndicator
import org.transdroid.util.formatBytes
import org.transdroid.util.formatEta
import org.transdroid.util.formatRatio
import org.transdroid.util.formatSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentsScreen(
    viewModel: TorrentsViewModel,
    useTwoPane: Boolean,
    onOpenDetails: (String) -> Unit,
    onAddTorrent: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddServer: () -> Unit,
    onOpenRss: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val rssAvailable = booleanResource(R.bool.rss_available)
    val searchAvailable = booleanResource(R.bool.search_available)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
                FilterDrawerContent(
                    ui = ui,
                    onSwitchProfile = { viewModel.switchProfile(it) },
                    onSetFilter = {
                        viewModel.setFilter(it)
                        scope.launch { drawerState.close() }
                    },
                    onToggleLabel = { viewModel.toggleLabelFilter(it) },
                    onSetNameQuery = { viewModel.setNameQuery(it) },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (useTwoPane && ui.activeProfile != null) {
                    TabletTopBar(
                        ui = ui,
                        searchAvailable = searchAvailable,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenSearch = onOpenSearch,
                        onRefresh = { viewModel.refresh() },
                        onSortSelect = { viewModel.setSort(it) },
                        onTitleClick = scrollToTop,
                        onSwitchProfile = { viewModel.switchProfile(it) },
                    )
                } else {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.drawer_open))
                            }
                        },
                        title = {
                            Column(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = scrollToTop,
                                ),
                            ) {
                                Text(stringResource(R.string.torrents_title))
                                ui.activeProfile?.let {
                                    Spacer(Modifier.height(7.dp))
                                    ServerChip(
                                        activeProfile = it,
                                        allProfiles = ui.allProfiles,
                                        onSwitchProfile = { id -> viewModel.switchProfile(id) },
                                    )
                                }
                            }
                        },
                        actions = {
                            if (searchAvailable) {
                                IconButton(onClick = onOpenSearch) {
                                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
                                }
                            }
                            if (rssAvailable) {
                                IconButton(onClick = onOpenRss) {
                                    Icon(Icons.Rounded.RssFeed, contentDescription = stringResource(R.string.rss_title))
                                }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    !ui.profilesLoaded -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    ui.activeProfile == null -> {
                        WelcomeContent(onAddServer = onAddServer, modifier = Modifier.align(Alignment.Center))
                    }
                    useTwoPane -> {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.width(392.dp)) {
                                TorrentListContent(
                                    ui = ui,
                                    viewModel = viewModel,
                                    onOpenDetails = onOpenDetails,
                                    onAddTorrent = onAddTorrent,
                                    searchAvailable = searchAvailable,
                                    rssAvailable = rssAvailable,
                                    onOpenSearch = onOpenSearch,
                                    onOpenRss = onOpenRss,
                                    listState = listState,
                                    showToolbar = false,
                                    showFilterChips = true,
                                )
                            }
                            VerticalDivider()
                            Box(Modifier.weight(1f)) {
                                val selected = ui.selectedTorrent
                                if (selected != null) {
                                    TorrentDetailsContent(viewModel = viewModel, torrent = selected)
                                } else {
                                    Text(
                                        stringResource(R.string.torrents_select_prompt),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                }
                            }
                        }
                    }
                    else -> TorrentListContent(
                        ui = ui,
                        viewModel = viewModel,
                        onOpenDetails = onOpenDetails,
                        onAddTorrent = onAddTorrent,
                        searchAvailable = searchAvailable,
                        rssAvailable = rssAvailable,
                        onOpenSearch = onOpenSearch,
                        onOpenRss = onOpenRss,
                        listState = listState,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletTopBar(
    ui: TorrentsUiState,
    searchAvailable: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSortSelect: (TorrentSort) -> Unit,
    onTitleClick: () -> Unit,
    onSwitchProfile: (String) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.drawer_open))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.torrents_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleClick,
                    ),
                )
                ui.activeProfile?.let {
                    Spacer(Modifier.width(10.dp))
                    ServerChip(
                        activeProfile = it,
                        allProfiles = ui.allProfiles,
                        onSwitchProfile = onSwitchProfile,
                    )
                }
                Spacer(Modifier.weight(1f))
                TabletStat(
                    icon = Icons.Rounded.ArrowDownward,
                    value = formatSpeed(ui.totalDownloadRate),
                    caption = stringResource(R.string.toolbar_download_active, ui.activeCount),
                )
                Spacer(Modifier.width(18.dp))
                TabletStat(
                    icon = Icons.Rounded.ArrowUpward,
                    value = formatSpeed(ui.totalUploadRate),
                    caption = stringResource(R.string.toolbar_upload_sharing, ui.sharingCount),
                )
                VerticalDivider(Modifier.height(26.dp).padding(horizontal = 10.dp))
                if (searchAvailable) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.torrents_refresh))
                }
                SortMenuButton(current = ui.sort, descending = ui.sortDescending, onSelect = onSortSelect)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/**
 * The active server as a small pill in the torrents list's header. Tapping the header text still
 * scrolls to top (its own clickable, further up the tree); tapping specifically this pill instead
 * opens a switcher, same idea as the filter drawer's [ServerSelector] - just compact enough for
 * the app bar.
 */
@Composable
private fun ServerChip(activeProfile: ServerProfile, allProfiles: List<ServerProfile>, onSwitchProfile: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val canSwitch = allProfiles.size > 1
    Box {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.clickable(enabled = canSwitch, onClick = { expanded = true }),
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(activeProfile.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (canSwitch) {
                    Icon(
                        Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        if (canSwitch) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allProfiles.forEach { profile ->
                    val isActive = profile.id == activeProfile.id
                    DropdownMenuItem(
                        text = { Text(profile.displayName) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Dns,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            if (isActive) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            expanded = false
                            if (!isActive) onSwitchProfile(profile.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletStat(icon: ImageVector, value: String, caption: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Column {
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TorrentListContent(
    ui: TorrentsUiState,
    viewModel: TorrentsViewModel,
    onOpenDetails: (String) -> Unit,
    onAddTorrent: () -> Unit,
    searchAvailable: Boolean = false,
    rssAvailable: Boolean = false,
    onOpenSearch: () -> Unit = {},
    onOpenRss: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    showToolbar: Boolean = true,
    showFilterChips: Boolean = false,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ui.error?.let { error ->
                ErrorBanner(message = error.message(), onRetry = { viewModel.refresh() })
            }
            if (showFilterChips) {
                FilterChipsRow(
                    ui = ui,
                    onSetFilter = { viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            PullToRefreshBox(
                isRefreshing = ui.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (ui.hasLoaded && ui.torrents.isEmpty()) {
                    val showShortcuts = searchAvailable || rssAvailable
                    EmptyState(
                        icon = Icons.Rounded.Inbox,
                        title = stringResource(R.string.torrents_no_torrents_title),
                        message = stringResource(R.string.torrents_no_torrents_message),
                        modifier = Modifier.fillMaxSize().wrapContentSize(),
                        actions = if (!showShortcuts) null else {
                            {
                                if (searchAvailable) {
                                    FilledTonalButton(onClick = onOpenSearch) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.search_title))
                                    }
                                }
                                if (rssAvailable) {
                                    FilledTonalButton(onClick = onOpenRss) {
                                        Icon(Icons.Rounded.RssFeed, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.rss_short_title))
                                    }
                                }
                            }
                        },
                    )
                } else if (ui.hasLoaded && ui.visibleTorrents.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.FilterAltOff,
                        title = stringResource(R.string.torrents_no_filter_title),
                        message = stringResource(R.string.torrents_no_filter_message, ui.filter.label()),
                        modifier = Modifier.fillMaxSize().wrapContentSize(),
                        actions = {
                            FilledTonalButton(onClick = { viewModel.setFilter(TorrentFilter.ALL) }) {
                                Icon(Icons.Rounded.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.torrents_show_all))
                            }
                        },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 96.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(ui.visibleTorrents, key = { it.id }) { torrent ->
                            TorrentRow(
                                torrent = torrent,
                                selected = torrent.id == ui.selectedTorrentId,
                                onClick = { onOpenDetails(torrent.id) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (ui.activeProfile != null) {
                AddTorrentFab(onClick = onAddTorrent)
                Spacer(Modifier.height(10.dp))
            }
            if (showToolbar) {
                TorrentsToolbar(
                    downloadRate = ui.totalDownloadRate,
                    activeCount = ui.activeCount,
                    uploadRate = ui.totalUploadRate,
                    sharingCount = ui.sharingCount,
                    sort = ui.sort,
                    sortDescending = ui.sortDescending,
                    onSortSelect = { viewModel.setSort(it) },
                    onRefresh = { viewModel.refresh() },
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(ui: TorrentsUiState, onSetFilter: (TorrentFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TorrentFilter.entries.forEach { filter ->
            FilterChip(
                selected = ui.filter == filter,
                onClick = { onSetFilter(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun TorrentsToolbar(
    downloadRate: Long,
    activeCount: Int,
    uploadRate: Long,
    sharingCount: Int,
    sort: TorrentSort,
    sortDescending: Boolean,
    onSortSelect: (TorrentSort) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().height(62.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarStat(
                icon = Icons.Rounded.ArrowDownward,
                value = formatSpeed(downloadRate),
                caption = stringResource(R.string.toolbar_download_active, activeCount),
            )
            VerticalDivider(Modifier.height(26.dp).padding(horizontal = 8.dp))
            ToolbarStat(
                icon = Icons.Rounded.ArrowUpward,
                value = formatSpeed(uploadRate),
                caption = stringResource(R.string.toolbar_upload_sharing, sharingCount),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.torrents_refresh))
            }
            SortMenuButton(current = sort, descending = sortDescending, onSelect = onSortSelect)
        }
    }
}

@Composable
private fun ToolbarStat(icon: ImageVector, value: String, caption: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddTorrentFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 30.dp else 22.dp, label = "fab-corner")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        interactionSource = interactionSource,
        modifier = Modifier.size(66.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.torrents_add))
        }
    }
}

@Composable
private fun SortMenuButton(current: TorrentSort, descending: Boolean, onSelect: (TorrentSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortTitle = stringResource(R.string.sort_title)
    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.semantics { contentDescription = sortTitle },
    ) {
        SortDirectionIcon(descending = descending, tint = LocalContentColor.current)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        TorrentSort.entries.forEach { sort ->
            val selected = sort == current
            DropdownMenuItem(
                text = { Text(sort.label()) },
                leadingIcon = {
                    RadioButton(selected = selected, onClick = null)
                },
                trailingIcon = {
                    if (selected) {
                        SortDirectionIcon(
                            descending = descending,
                            tint = LocalContentColor.current,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                onClick = {
                    onSelect(sort)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun TorrentSort.label(): String = stringResource(
    when (this) {
        TorrentSort.DATE_ADDED -> R.string.sort_date_added
        TorrentSort.NAME -> R.string.sort_name
        TorrentSort.DOWNLOAD_SPEED -> R.string.sort_download_speed
        TorrentSort.RATIO -> R.string.sort_ratio
    }
)

@Composable
internal fun TorrentFilter.label(): String = stringResource(
    when (this) {
        TorrentFilter.ALL -> R.string.filter_all
        TorrentFilter.DOWNLOADING -> R.string.filter_downloading
        TorrentFilter.SEEDING -> R.string.filter_seeding
        TorrentFilter.COMPLETED -> R.string.filter_completed
        TorrentFilter.PAUSED -> R.string.filter_paused
        TorrentFilter.ERROR -> R.string.filter_error
    }
)

/** Leading icon for a torrent row, matching the mockup's down/seed/idle/wait state icons. */
internal val TorrentStatus.leadingIcon: ImageVector
    get() = when (this) {
        TorrentStatus.DOWNLOADING -> Icons.Rounded.Download
        TorrentStatus.SEEDING -> Icons.Rounded.Upload
        TorrentStatus.QUEUED, TorrentStatus.CHECKING -> Icons.Rounded.Schedule
        TorrentStatus.ERROR -> Icons.Rounded.ErrorOutline
        TorrentStatus.PAUSED, TorrentStatus.UNKNOWN -> Icons.Rounded.Pause
    }

@Composable
private fun TorrentRow(torrent: Torrent, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .let {
                if (selected) it.background(MaterialTheme.colorScheme.surfaceContainerHigh) else it
            }
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Icon(
                torrent.status.leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).padding(top = 1.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        torrent.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // fill (the default) so the name always claims the row's full
                        // remaining width like the mockup's `.name{flex:1}` - otherwise a
                        // short title only takes its natural width and the tag ends up sitting
                        // right after it instead of pushed to the row's far right.
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        rowTag(torrent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    rowDetail(torrent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TorrentProgressIndicator(
                    progress = torrent.displayProgress,
                    active = torrent.status == TorrentStatus.DOWNLOADING,
                    color = torrent.status.accentColor,
                    trackColor = torrent.status.trackColor,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RowMetaLeft(torrent)
                    RowMetaRight(torrent)
                }
            }
        }
        // Colour-coded accent line centred under the leading icon (not a full-height gutter
        // to its left): starts right below the icon (row top padding 12dp + icon's own 1dp
        // inset + its 22dp size = 35dp) and ends flush with the bottom of the row's own
        // content (the meta row, e.g. "Ratio …") rather than running into the row's own
        // bottom padding — hence the matching bottom=12dp inset. matchParentSize() forces
        // THIS node's own reported size to the full row bounds via tight constraints, which —
        // if padding/width were chained right on it — get re-clamped back up past a smaller
        // requested width, silently pushing the line off toward the middle of the row instead
        // of shrinking it. Nesting a plain Box inside sidesteps that: the outer box's tight
        // sizing is already resolved, so its own children (this inner one) get measured with
        // normal loose (0..max) constraints, and width/padding behave as expected.
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .padding(start = 17.5.dp, top = 35.dp, bottom = 12.dp)
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(torrent.status.accentColor),
            )
        }
    }
}

@Composable
private fun rowTag(torrent: Torrent): String =
    if (torrent.metadataProgress == null && torrent.status == TorrentStatus.DOWNLOADING) {
        formatEta(torrent.etaSeconds)?.let {
            stringResource(R.string.torrent_row_eta_left, it)
        } ?: torrent.statusLabel()
    } else {
        torrent.statusLabel()
    }

@Composable
private fun rowDetail(torrent: Torrent): String = when {
    torrent.metadataProgress != null -> "${(torrent.displayProgress * 100).toInt()}%"
    torrent.isFinished -> stringResource(R.string.torrent_row_complete, formatBytes(torrent.sizeBytes))
    else -> stringResource(
        R.string.torrent_row_progress,
        formatBytes(torrent.downloadedBytes),
        formatBytes(torrent.sizeBytes),
        (torrent.displayProgress * 100).toInt(),
    )
}

@Composable
private fun RowMetaLeft(torrent: Torrent) {
    val (icon, text) = when (torrent.status) {
        TorrentStatus.DOWNLOADING -> Icons.Rounded.Group to stringResource(R.string.torrent_row_peers, torrent.peersConnected)
        TorrentStatus.QUEUED, TorrentStatus.CHECKING -> Icons.Rounded.Schedule to torrent.statusLabel()
        else -> Icons.Rounded.SyncAlt to (stringResource(R.string.details_ratio) + " " + formatRatio(torrent.ratio))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowMetaRight(torrent: Torrent) {
    when (torrent.status) {
        TorrentStatus.DOWNLOADING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpeedChip(Icons.Rounded.South, formatSpeed(torrent.downloadRate))
            SpeedChip(Icons.Rounded.North, formatSpeed(torrent.uploadRate))
        }
        TorrentStatus.SEEDING -> SpeedChip(Icons.Rounded.North, formatSpeed(torrent.uploadRate))
        else -> {}
    }
}

@Composable
private fun SpeedChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.torrents_retry))
            }
        }
    }
}

@Composable
private fun WelcomeContent(onAddServer: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Rounded.Dns,
        title = stringResource(R.string.torrents_no_server_title),
        message = stringResource(R.string.torrents_no_server_message),
        modifier = modifier,
        actions = {
            Button(onClick = onAddServer) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.torrents_no_server_button))
            }
        },
    )
}
