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
package org.transdroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.transdroid.R

/**
 * The lighter-weight counterpart to [org.transdroid.ui.add.AddTorrentScreen] for adding a torrent
 * whose source is already known - a Search result or an RSS item's link - so there is nothing to
 * type or pick, only [AddTorrentOptionsSection]'s download-location and add-paused options. Same
 * bottom-sheet shell as [org.transdroid.ui.torrents.SetLabelSheet]/[org.transdroid.ui.torrents.SetLocationSheet]
 * (header/content/actions) for visual consistency with the rest of the app's per-item actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTorrentOptionsSheet(
    itemTitle: String,
    serverName: String,
    recentLocations: List<String>,
    onAdd: (startPaused: Boolean, downloadLocation: String?) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var location by remember { mutableStateOf("") }
    var startPaused by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Matches SetLabelSheet/SetLocationSheet's .sheet-head: its own horizontal inset (22dp),
        // distinct from the options section below (16dp).
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(stringResource(R.string.add_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.add_sheet_subtitle, itemTitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }
        AddTorrentOptionsSection(
            location = location,
            onLocationChange = { location = it },
            recentLocations = recentLocations,
            startPaused = startPaused,
            onStartPausedChange = { startPaused = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                onAdd(startPaused, location.trim().ifBlank { null })
                onDismiss()
            }) {
                Text(stringResource(R.string.add_button, serverName))
            }
        }
    }
}
