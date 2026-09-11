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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LabelOff
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.transdroid.R
import org.transdroid.ui.theme.labelColor

/**
 * Bottom sheet to pick, clear, or create a torrent's label - matches
 * design/mockups/transdroid-m3-set-label(.html|-dark.html). Built on the standard Material 3
 * ModalBottomSheet rather than a custom overlay/dialog.
 *
 * [availableLabels]/[countForLabel] are the labels already seen on the server - in practice
 * [TorrentsUiState.availableLabels]/[TorrentsUiState.countForLabel], derived from the currently
 * loaded torrent list rather than a separate per-protocol "list all labels" call, since only
 * qBittorrent and Deluge even have a real server-side label registry to query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLabelSheet(
    torrentName: String,
    currentLabel: String?,
    availableLabels: List<String>,
    countForLabel: (String) -> Int,
    onSetLabel: (String?) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var filterText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(currentLabel) }
    val trimmedFilter = filterText.trim()
    val matches = availableLabels.filter { it.contains(trimmedFilter, ignoreCase = true) }
    val exactMatch = availableLabels.any { it.equals(trimmedFilter, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Matches the mockup's .sheet-head: its own horizontal inset (22dp), distinct from the
        // field below (16dp) - the field is deliberately a touch wider than the title/subtitle.
        Column(Modifier.padding(horizontal = 22.dp)) {
            Text(stringResource(R.string.details_set_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.details_set_label_subtitle, torrentName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
        }
        LabelFilterField(
            value = filterText,
            onValueChange = { filterText = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        LazyColumn(Modifier.heightIn(max = 340.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (trimmedFilter.isEmpty()) {
                item {
                    LabelRow(
                        icon = Icons.Rounded.LabelOff,
                        name = stringResource(R.string.drawer_no_label),
                        count = null,
                        selected = selected == null,
                        onClick = { selected = null; filterText = "" },
                    )
                }
            }
            items(matches) { label ->
                LabelRow(
                    dotColor = labelColor(label).first,
                    name = label,
                    count = countForLabel(label),
                    selected = selected == label,
                    onClick = { selected = label; filterText = "" },
                )
            }
            if (trimmedFilter.isNotEmpty() && !exactMatch) {
                item {
                    LabelRow(
                        icon = Icons.Rounded.Add,
                        iconTint = MaterialTheme.colorScheme.primary,
                        nameColor = MaterialTheme.colorScheme.primary,
                        name = stringResource(R.string.details_label_create, trimmedFilter),
                        count = null,
                        selected = false,
                        onClick = {
                            selected = trimmedFilter
                            filterText = ""
                        },
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
            Button(onClick = {
                // Whatever is still typed wins over a previous row tap - so typing a brand-new
                // name and tapping this button directly creates it, without first requiring a
                // separate tap on the "Create …" row below.
                onSetLabel(if (trimmedFilter.isNotEmpty()) trimmedFilter else selected)
                onDismiss()
            }) { Text(stringResource(R.string.details_set_label)) }
        }
    }
}

/**
 * Matches the mockup's `.sheet-field`: a fixed-height pill (grey fill, no border/label) with a
 * leading icon and a plain placeholder - the same shape as [org.transdroid.ui.search.SearchScreen]'s
 * search bar, not the boxed/labeled [org.transdroid.ui.components.TransdroidTextField] used for
 * actual form fields elsewhere.
 */
@Composable
private fun LabelFilterField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.details_label_filter_hint),
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

@Composable
private fun LabelRow(
    name: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    nameColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else nameColor
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            icon != null -> Icon(
                icon,
                contentDescription = null,
                tint = if (selected) contentColor else iconTint,
                modifier = Modifier.size(20.dp),
            )
            dotColor != null -> Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(dotColor))
        }
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}
