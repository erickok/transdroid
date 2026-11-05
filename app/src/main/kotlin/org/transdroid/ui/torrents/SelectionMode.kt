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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.transdroid.R
import org.transdroid.protocol.Torrent
import org.transdroid.ui.components.LabelPill
import org.transdroid.util.formatBytes
import org.transdroid.util.formatSpeed

/**
 * Replaces the top bar while torrents are selected. Same height as the normal phone and tablet
 * bars (status bar inset plus 64dp, and the tablet bar's divider), so the list never shifts.
 */
@Composable
internal fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, showDivider: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.selection_close))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.selection_count, count),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = stringResource(R.string.selection_select_all))
                }
            }
            if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/** The actions on the selected torrents, with the details screen's icons. */
internal class SelectionActions(
    val canStart: Boolean,
    val canPause: Boolean,
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onCheck: () -> Unit,
    val onLabel: () -> Unit,
    val onMove: () -> Unit,
    val onRemove: () -> Unit,
)

/** The phone's floating action toolbar, in place of the speed toolbar while selecting. */
@Composable
internal fun SelectionActionBar(actions: SelectionActions, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth().height(68.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            BarAction(Icons.Rounded.PlayArrow, R.string.selection_start, actions.canStart, actions.onStart)
            BarAction(Icons.Rounded.Pause, R.string.selection_pause, actions.canPause, actions.onPause)
            BarAction(Icons.Rounded.Checklist, R.string.selection_check, true, actions.onCheck)
            BarAction(Icons.Rounded.Sell, R.string.selection_label, true, actions.onLabel)
            BarAction(Icons.AutoMirrored.Rounded.DriveFileMove, R.string.selection_move, true, actions.onMove)
            BarAction(Icons.Rounded.Delete, R.string.selection_remove, true, actions.onRemove, danger = true)
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, labelRes: Int, enabled: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/** The tablet's details pane while selecting: what is selected, and large action buttons. */
@Composable
internal fun SelectionSummaryPane(torrents: List<Torrent>, actions: SelectionActions, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryAddCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(R.string.selection_summary_title, torrents.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val labels = torrents.flatMap { it.labels }.distinct()
                if (labels.isNotEmpty()) {
                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { LabelPill(it) }
                    }
                }
            }
        }
        Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(R.string.selection_total_size, formatBytes(torrents.sumOf { it.sizeBytes }), Modifier.weight(1f))
            SummaryCard(R.string.selection_download, formatSpeed(torrents.sumOf { it.downloadRate }), Modifier.weight(1f))
            SummaryCard(R.string.selection_upload, formatSpeed(torrents.sumOf { it.uploadRate }), Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = actions.onStart, enabled = actions.canStart) {
                ButtonContent(Icons.Rounded.PlayArrow, R.string.selection_start)
            }
            FilledTonalButton(onClick = actions.onPause, enabled = actions.canPause) {
                ButtonContent(Icons.Rounded.Pause, R.string.selection_pause)
            }
            OutlinedButton(onClick = actions.onCheck) { ButtonContent(Icons.Rounded.Checklist, R.string.details_check_data) }
            OutlinedButton(onClick = actions.onLabel) { ButtonContent(Icons.Rounded.Sell, R.string.details_set_label) }
            OutlinedButton(onClick = actions.onMove) {
                ButtonContent(Icons.AutoMirrored.Rounded.DriveFileMove, R.string.selection_move)
            }
            OutlinedButton(
                onClick = actions.onRemove,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { ButtonContent(Icons.Rounded.Delete, R.string.selection_remove) }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                torrents.forEach { torrent ->
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            torrent.status.leadingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(torrent.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatBytes(torrent.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, labelRes: Int) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(stringResource(labelRes))
}

@Composable
private fun SummaryCard(labelRes: Int, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Confirms removing one or more torrents, optionally with their downloaded data. */
@Composable
internal fun RemoveTorrentsDialog(torrents: List<Torrent>, onDismiss: () -> Unit, onConfirm: (alsoDeleteData: Boolean) -> Unit) {
    var alsoDeleteData by rememberSaveable { mutableStateOf(false) }
    val single = torrents.singleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (single != null) stringResource(R.string.details_remove_title)
                else stringResource(R.string.selection_remove_title, torrents.size)
            )
        },
        text = {
            Column {
                Text(
                    if (single != null) stringResource(R.string.details_remove_message, single.name)
                    else stringResource(R.string.selection_remove_message, torrents.size)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = alsoDeleteData, onCheckedChange = { alsoDeleteData = it })
                    Text(stringResource(R.string.details_remove_also_data))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoDeleteData) }) { Text(stringResource(R.string.details_remove_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
        },
    )
}

/** The check circle that replaces a row's status icon while selecting. */
@Composable
internal fun SelectionCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) ring else MaterialTheme.colorScheme.surface)
            .then(
                if (checked) Modifier
                else Modifier.background(MaterialTheme.colorScheme.outline, CircleShape).padding(2.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The message after a bulk action, or null when there's nothing to say. */
@Composable
internal fun BulkResult.message(): String {
    val done = total - failed
    return if (failed > 0) {
        stringResource(R.string.selection_failed, failed, total)
    } else {
        stringResource(
            when (action) {
                BulkAction.START -> R.string.selection_done_start
                BulkAction.PAUSE -> R.string.selection_done_pause
                BulkAction.CHECK -> R.string.selection_done_check
                BulkAction.REMOVE -> R.string.selection_done_remove
                BulkAction.LABEL -> R.string.selection_done_label
                BulkAction.MOVE -> R.string.selection_done_move
            },
            done,
        )
    }
}

/** The one-time tip above the list that explains long-press selection. */
@Composable
internal fun SelectionHint(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TouchApp, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.selection_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.selection_hint_dismiss), modifier = Modifier.size(20.dp))
            }
        }
    }
}
