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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LabelOff
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.net.URI
import java.text.DateFormat
import java.util.Date
import org.transdroid.R
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker
import org.transdroid.protocol.TrackerStatus
import org.transdroid.ui.components.TorrentProgressIndicator
import org.transdroid.ui.statusLabel
import org.transdroid.ui.theme.LocalStatusColors
import org.transdroid.ui.theme.accentColor
import org.transdroid.ui.theme.trackColor
import org.transdroid.util.formatBytes
import org.transdroid.util.formatEta
import org.transdroid.util.formatRatio
import org.transdroid.util.formatSpeed

/** Full-screen torrent details for compact widths; two-pane layouts embed the content directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsScreen(
    viewModel: TorrentsViewModel,
    torrentId: String,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val torrent = ui.torrents.firstOrNull { it.id == torrentId }

    Scaffold(
        topBar = {
            TopAppBar(
                // No title text here - the mockup's app bar is icon-only (back, share, more);
                // the name is already the prominent header inside DetailsHeader below, and
                // repeating it here just duplicated it.
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (torrent == null) {
                Text(
                    stringResource(R.string.details_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                TorrentDetailsContent(viewModel = viewModel, torrent = torrent, onRemoved = onBack)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsContent(
    viewModel: TorrentsViewModel,
    torrent: Torrent,
    onRemoved: (() -> Unit)? = null,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable(torrent.id) { mutableStateOf(0) }

    LaunchedEffect(torrent.id) {
        viewModel.loadFiles(torrent.id)
        viewModel.loadTrackers(torrent.id)
    }

    val files = ui.files[torrent.id].orEmpty()
    val trackers = ui.trackers[torrent.id].orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        DetailsHeader(torrent)

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val paused = torrent.status == TorrentStatus.PAUSED
            Button(
                onClick = { viewModel.toggleStartPause(torrent) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (paused) R.string.details_start else R.string.details_pause))
            }
            OutlinedButton(
                onClick = { showRemoveDialog = true },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.details_remove))
            }
            FilledIconButton(
                onClick = { viewModel.checkData(torrent) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.Rounded.Checklist, contentDescription = stringResource(R.string.details_check_data))
            }
            FilledIconToggleButton(
                checked = torrent.labels.isNotEmpty(),
                onCheckedChange = { showLabelSheet = true },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Icon(Icons.Rounded.Sell, contentDescription = stringResource(R.string.details_set_label))
            }
        }

        Spacer(Modifier.height(20.dp))
        TorrentProgressIndicator(
            progress = torrent.displayProgress,
            active = torrent.status == TorrentStatus.DOWNLOADING,
            color = torrent.status.accentColor,
            trackColor = torrent.status.trackColor,
            modifier = Modifier.fillMaxWidth().height(20.dp),
        )
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                detailsProgressLine(torrent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            formatEta(torrent.etaSeconds)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        torrent.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        PrimaryTabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.details_tab_overview)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.details_tab_files, files.size)) },
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(R.string.details_tab_trackers, trackers.size)) },
            )
        }
        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> OverviewTab(torrent)
            1 -> FilesTab(torrent, files, viewModel)
            else -> TrackersTab(trackers)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showRemoveDialog) {
        var alsoDeleteData by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.details_remove_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.details_remove_message, torrent.name))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = alsoDeleteData, onCheckedChange = { alsoDeleteData = it })
                        Text(stringResource(R.string.details_remove_also_data))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    viewModel.remove(torrent, alsoDeleteData)
                    onRemoved?.invoke()
                }) {
                    Text(stringResource(R.string.details_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.details_cancel))
                }
            },
        )
    }

    if (showLabelSheet) {
        SetLabelSheet(
            torrentName = torrent.name,
            currentLabel = torrent.labels.firstOrNull(),
            availableLabels = ui.availableLabels,
            countForLabel = { ui.countForLabel(it) },
            onSetLabel = { label -> viewModel.setLabel(torrent, label) },
            onDismiss = { showLabelSheet = false },
        )
    }
}

@Composable
private fun detailsProgressLine(torrent: Torrent): String = when {
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
private fun DetailsHeader(torrent: Torrent) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(48.dp)
                .background(torrent.status.trackColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                torrent.status.leadingIcon,
                contentDescription = null,
                tint = torrent.status.accentColor,
                modifier = Modifier.size(25.dp),
            )
        }
        Column {
            Text(torrent.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailPill(torrent.status.leadingIcon, torrent.statusLabel())
                val label = torrent.labels.firstOrNull()
                DetailPill(
                    if (label != null) Icons.Rounded.Label else Icons.Rounded.LabelOff,
                    label ?: stringResource(R.string.drawer_no_label),
                )
            }
        }
    }
}

@Composable
private fun DetailPill(icon: ImageVector, text: String) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverviewTab(torrent: Torrent) {
    val statusColors = LocalStatusColors.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OverviewCard(
                modifier = Modifier.weight(1f),
                container = statusColors.downloadingTrack,
                accent = statusColors.downloading,
                icon = Icons.Rounded.ArrowDownward,
                title = stringResource(R.string.details_section_download),
                hero = formatSpeed(torrent.downloadRate),
                rows = listOf(
                    stringResource(R.string.details_downloaded) to formatBytes(torrent.downloadedBytes),
                    stringResource(R.string.details_seeders) to torrent.seedersConnected.toString(),
                ),
            )
            OverviewCard(
                modifier = Modifier.weight(1f),
                container = statusColors.seedingTrack,
                accent = statusColors.seeding,
                icon = Icons.Rounded.ArrowUpward,
                title = stringResource(R.string.details_section_upload),
                hero = formatSpeed(torrent.uploadRate),
                rows = listOf(
                    stringResource(R.string.details_uploaded) to formatBytes(torrent.uploadedBytes),
                    stringResource(R.string.details_leechers) to torrent.leechersConnected.toString(),
                ),
            )
        }

        Spacer(Modifier.height(10.dp))
        // The first 4 tiles always show, with a placeholder for an unknown ETA/Added, matching
        // the mockup's fixed Ratio/Size/ETA/Added row; Location/Labels are extra, kept from the
        // pre-redesign detail list since the mockup doesn't otherwise surface them here.
        val unknown = "—"
        val tiles = buildList {
            add(stringResource(R.string.details_ratio) to formatRatio(torrent.ratio))
            add(stringResource(R.string.details_size) to formatBytes(torrent.sizeBytes))
            add(stringResource(R.string.details_eta) to (formatEta(torrent.etaSeconds) ?: unknown))
            add(
                stringResource(R.string.details_added) to (
                    torrent.addedTimestamp?.let {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it * 1000))
                    } ?: unknown
                ),
            )
            torrent.downloadDir?.let { add(stringResource(R.string.details_location) to it) }
            if (torrent.labels.isNotEmpty()) {
                add(stringResource(R.string.details_labels) to torrent.labels.joinToString())
            }
        }
        StatTileGrid(tiles)
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier,
    container: Color,
    accent: Color,
    icon: ImageVector,
    title: String,
    hero: String,
    rows: List<Pair<String, String>>,
) {
    Surface(shape = MaterialTheme.shapes.large, color = container, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    // Matches the mockup's `.ovhead{text-transform:uppercase}`
                    title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                hero,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            rows.forEachIndexed { index, (key, value) ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatTileGrid(tiles: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (key, value) -> StatTile(key, value, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FilesTab(torrent: Torrent, files: List<TorrentFile>, viewModel: TorrentsViewModel) {
    if (files.isEmpty()) {
        Text(
            stringResource(R.string.details_files_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        files.forEachIndexed { index, file ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            FileRow(file) { priority -> viewModel.setFilePriority(torrent.id, file, priority) }
        }
    }
}

@Composable
private fun FileRow(file: TorrentFile, onSetPriority: (FilePriority) -> Unit) {
    var priorityMenuOpen by remember(file.index) { mutableStateOf(false) }
    val statusColors = LocalStatusColors.current
    val complete = file.progress >= 1f
    val barColor = when {
        complete -> statusColors.seeding
        file.progress > 0f -> statusColors.downloading
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { priorityMenuOpen = true }
                .padding(vertical = 11.dp),
        ) {
            Icon(
                if (complete) Icons.Rounded.TaskAlt else Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatBytes(file.downloadedBytes)} / ${formatBytes(file.sizeBytes)} · ${(file.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(2.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(file.progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(barColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            PriorityChip(file.priority)
        }
        DropdownMenu(expanded = priorityMenuOpen, onDismissRequest = { priorityMenuOpen = false }) {
            FilePriority.entries.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority.label()) },
                    leadingIcon = { RadioButton(selected = priority == file.priority, onClick = null) },
                    onClick = {
                        priorityMenuOpen = false
                        if (priority != file.priority) onSetPriority(priority)
                    },
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(priority: FilePriority) {
    val statusColors = LocalStatusColors.current
    val (container, content) = when (priority) {
        FilePriority.OFF -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.outline
        FilePriority.HIGH -> statusColors.downloadingTrack to statusColors.downloading
        FilePriority.LOW, FilePriority.NORMAL ->
            MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = container) {
        Text(
            priority.label(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FilePriority.label(): String = stringResource(
    when (this) {
        FilePriority.OFF -> R.string.priority_off
        FilePriority.LOW -> R.string.priority_low
        FilePriority.NORMAL -> R.string.priority_normal
        FilePriority.HIGH -> R.string.priority_high
    }
)

@Composable
private fun TrackersTab(trackers: List<Tracker>) {
    if (trackers.isEmpty()) {
        Text(
            stringResource(R.string.details_trackers_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        trackers.forEachIndexed { index, tracker ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            TrackerRow(tracker)
        }
    }
}

@Composable
private fun TrackerRow(tracker: Tracker) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.Lan,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                trackerHost(tracker.url),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trackerDetail(tracker)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        TrackerStatusChip(tracker.status)
    }
}

private fun trackerHost(url: String): String = try {
    URI(url).host?.takeIf { it.isNotBlank() } ?: url
} catch (e: Exception) {
    url
}

@Composable
private fun trackerDetail(tracker: Tracker): String? = when {
    tracker.seeders != null || tracker.leechers != null ->
        stringResource(R.string.details_tracker_stats, tracker.seeders ?: 0, tracker.leechers ?: 0)
    else -> tracker.message
}

@Composable
private fun TrackerStatusChip(status: TrackerStatus) {
    val statusColors = LocalStatusColors.current
    val (container, content, textRes) = when (status) {
        TrackerStatus.WORKING ->
            Triple(statusColors.seedingTrack, statusColors.seeding, R.string.details_tracker_working)
        TrackerStatus.ERROR ->
            Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, R.string.details_tracker_error)
        TrackerStatus.IDLE ->
            Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, R.string.details_tracker_idle)
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = container) {
        Text(
            stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
