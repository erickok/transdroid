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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A horizontally scrolling row of [LocationChip]s for known/recently-used download locations -
 * shared by the add-torrent flow and the details screen's "Set download location" dialog (see
 * design/mockups/transdroid-m3-add-torrent(.html|-dark.html)'s `.chips`/`.locchip`) so a full path
 * rarely needs retyping in either place. Tapping the already-selected chip clears [selectedPath]
 * back to an empty string, letting the caller decide what that means (add-torrent: fall back to
 * the server's own default; set-location: just clears the field for a fresh manual entry).
 */
@Composable
fun LocationChipsRow(
    locations: List<String>,
    selectedPath: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        locations.forEach { path ->
            LocationChip(
                path = path,
                selected = selectedPath == path,
                onClick = { onSelect(if (selectedPath == path) "" else path) },
            )
        }
    }
}

/**
 * A wrapping (multi-row, non-scrolling) cloud of [LocationChip]s - matches the "Set download
 * location" sheet's `.chips` (design/mockups/transdroid-m3-set-location(.html|-dark.html)), which
 * uses `flex-wrap:wrap` rather than the add-torrent flow's single scrolling row ([LocationChipsRow]
 * above). Selecting a chip always fills [selectedPath] with its path - there is no toggle-to-clear
 * here, since the sheet's own field remains directly editable/clearable regardless.
 */
@Composable
fun LocationChipsFlow(
    locations: List<String>,
    selectedPath: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        locations.forEach { path ->
            LocationChip(path = path, selected = selectedPath == path, onClick = { onSelect(path) })
        }
    }
}

/**
 * Matches the mockup's `.locchip`: an outlined pill that fills with [tertiaryContainer] (the
 * app's shared "selected chip" tone, see [org.transdroid.ui.theme.Color]) when selected.
 */
@Composable
private fun LocationChip(path: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .then(
                if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
            )
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = null, tint = contentColor, modifier = Modifier.height(16.dp))
        Text(
            path,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
