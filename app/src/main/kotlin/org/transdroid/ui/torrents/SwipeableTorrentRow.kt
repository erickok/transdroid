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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.transdroid.R
import org.transdroid.protocol.Torrent

/**
 * Wraps a torrent list row with swipe gestures: swipe right to start/pause, swipe left to
 * remove (always behind the usual confirmation dialog). The row snaps back either way;
 * the result shows up through the next refresh.
 */
@Composable
fun SwipeableTorrentRow(
    torrent: Torrent,
    onToggleStartPause: (Torrent) -> Unit,
    onRequestRemove: (Torrent) -> Unit,
    content: @Composable () -> Unit,
) {
    val currentTorrent by rememberUpdatedState(torrent)
    val currentToggle by rememberUpdatedState(onToggleStartPause)
    val currentRemove by rememberUpdatedState(onRequestRemove)
    // material3 consults confirmValueChange both when the drag crosses the threshold and
    // again when the release settles, so one swipe would otherwise act twice
    val lastFiredAt = remember { longArrayOf(0L) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val now = System.currentTimeMillis()
            if (value != SwipeToDismissBoxValue.Settled && now - lastFiredAt[0] > SWIPE_DEBOUNCE_MILLIS) {
                lastFiredAt[0] = now
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> currentToggle(currentTorrent)
                    SwipeToDismissBoxValue.EndToStart -> currentRemove(currentTorrent)
                    SwipeToDismissBoxValue.Settled -> {}
                }
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val toStart = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toEnd = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (toStart || toEnd) {
                val tint = if (toEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val icon = when {
                    toEnd -> Icons.Rounded.Delete
                    torrent.status.canStart -> Icons.Rounded.PlayArrow
                    else -> Icons.Rounded.Pause
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = if (toStart) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        },
    ) {
        content()
    }
}

private const val SWIPE_DEBOUNCE_MILLIS = 600L

/** The same removal confirmation as the details screen, for the list's swipe gesture. */
@Composable
fun SwipeRemoveDialog(
    torrent: Torrent,
    onDismiss: () -> Unit,
    onConfirm: (alsoDeleteData: Boolean) -> Unit,
) {
    var alsoDeleteData by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = { onConfirm(alsoDeleteData) }) {
                Text(stringResource(R.string.details_remove_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.details_cancel))
            }
        },
    )
}
