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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.transdroid.R
import org.transdroid.ui.components.LocationChipsFlow

/**
 * Bottom sheet to move a torrent's download location - matches
 * design/mockups/transdroid-m3-set-location(.html|-dark.html), the same shell as [SetLabelSheet]
 * (header/field/body/actions) but its own field and body content.
 *
 * Unlike [SetLabelSheet]'s field, [location] IS the value that gets saved rather than a separate
 * filter over a fixed vocabulary - a download path is free text, not one of a small set of
 * existing labels - so [recentLocations] below it is always shown in full as tap-to-fill chips
 * rather than filtered as you type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLocationSheet(
    torrentName: String,
    currentLocation: String,
    isRtorrent: Boolean,
    recentLocations: List<String>,
    onSetLocation: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var location by remember { mutableStateOf(currentLocation) }
    val trimmedLocation = location.trim()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Matches the mockup's .sheet-head: its own horizontal inset (22dp), distinct from the
        // field below (16dp) - the field is deliberately a touch wider than the title/subtitle.
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(stringResource(R.string.details_set_location), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.details_set_location_subtitle, torrentName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }
        LocationField(
            value = location,
            onValueChange = { location = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            if (recentLocations.isNotEmpty()) {
                Text(
                    stringResource(R.string.details_set_location_known),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LocationChipsFlow(
                    locations = recentLocations,
                    selectedPath = trimmedLocation,
                    onSelect = { location = it },
                )
            }
            // rTorrent can only repoint where it looks for a torrent's files, not physically move
            // them (see DaemonAdapter.setDownloadLocation's doc) - shown as its own error-toned
            // warning instead of the mockup's generic "files will be moved" note, since that would
            // be actively wrong for this one client; the button stays enabled regardless, since
            // repointing is still a real, useful action there when the files already exist (or will
            // exist) at the new path.
            if (isRtorrent) {
                Text(
                    stringResource(R.string.details_set_location_rtorrent_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                )
            } else {
                Row(
                    Modifier.padding(top = 16.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp).height(17.dp),
                    )
                    Text(
                        stringResource(R.string.details_set_location_move_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSetLocation(trimmedLocation); onDismiss() }, enabled = trimmedLocation.isNotEmpty()) {
                Text(stringResource(R.string.details_set_location_save))
            }
        }
    }
}

/** Matches the mockup's `.sheet-field`: a 52dp pill with a folder icon, directly editable (this field's own value is what gets saved). */
@Composable
private fun LocationField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.details_set_location_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
