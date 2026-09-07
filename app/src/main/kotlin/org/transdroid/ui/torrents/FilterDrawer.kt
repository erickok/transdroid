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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import org.transdroid.R
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.ui.theme.accentColor
import org.transdroid.ui.theme.labelColor

/**
 * The list screen's primary navigation and filtering surface, replacing the pre-redesign inline
 * filter-chip rows: brand + server switcher, name filter, single-select status filter, multi-select
 * label filter, and the entry point into Settings. Mirrors
 * design/mockups/transdroid-m3-filter-drawer.html: a fixed header (brand, server pill, filter field)
 * above one continuously-scrolling region that holds the status list, the label list and, at its very
 * end, the Settings row - nothing here is pinned to the screen bottom, matching the reference markup.
 */
@Composable
fun FilterDrawerContent(
    ui: TorrentsUiState,
    onSwitchProfile: (String) -> Unit,
    onSetFilter: (TorrentFilter) -> Unit,
    onToggleLabel: (String) -> Unit,
    onSetNameQuery: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
            Text(
                stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
            Spacer(Modifier.height(9.dp))
            ui.activeProfile?.let { activeProfile ->
                ServerSelector(
                    activeProfile = activeProfile,
                    allProfiles = ui.allProfiles,
                    hasError = ui.error != null,
                    hasLoaded = ui.hasLoaded,
                    torrentCount = ui.torrents.size,
                    onSwitchProfile = onSwitchProfile,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        DrawerFilterField(
            value = ui.nameQuery,
            onValueChange = onSetNameQuery,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(top = 8.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            SectionTitle(stringResource(R.string.drawer_status_header))
            TorrentFilter.entries.forEach { filter ->
                DrawerItemRow(
                    icon = filter.icon(),
                    label = filter.label(),
                    count = ui.countFor(filter),
                    selected = ui.filter == filter,
                    onClick = { onSetFilter(filter) },
                )
            }

            if (ui.availableLabels.isNotEmpty()) {
                DrawerDivider()
                SectionTitle(stringResource(R.string.drawer_labels_header))
                ui.availableLabels.forEach { label ->
                    val (dotColor, _) = labelColor(label)
                    DrawerItemRow(
                        dotColor = dotColor,
                        label = label,
                        count = ui.countForLabel(label),
                        selected = label in ui.labelFilters,
                        onClick = { onToggleLabel(label) },
                    )
                }
                if (ui.hasUnlabeledTorrents) {
                    DrawerItemRow(
                        hollowDot = true,
                        label = stringResource(R.string.drawer_no_label),
                        count = ui.countForLabel(NO_LABEL),
                        selected = NO_LABEL in ui.labelFilters,
                        onClick = { onToggleLabel(NO_LABEL) },
                    )
                }
            }

            DrawerDivider()
            DrawerItemRow(
                icon = Icons.Rounded.Settings,
                label = stringResource(R.string.torrents_settings),
                count = null,
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/**
 * The active server as a pill, matching design/mockups/transdroid-m3-filter-drawer.html. With
 * more than one configured server it's a dropdown: tapping it opens a menu to switch, and it
 * carries the chevron affordance; with only one server there's nothing to select, so the chevron
 * is dropped and the pill just displays the connection state.
 */
@Composable
private fun ServerSelector(
    activeProfile: ServerProfile,
    allProfiles: List<ServerProfile>,
    hasError: Boolean,
    hasLoaded: Boolean,
    torrentCount: Int,
    onSwitchProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val canSwitch = allProfiles.size > 1
    Box(modifier) {
        ServerPill(
            profile = activeProfile,
            hasError = hasError,
            hasLoaded = hasLoaded,
            torrentCount = torrentCount,
            showChevron = canSwitch,
            onClick = { if (canSwitch) expanded = true },
        )
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
private fun ServerPill(
    profile: ServerProfile,
    hasError: Boolean,
    hasLoaded: Boolean,
    torrentCount: Int,
    showChevron: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = showChevron, onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (text, dotColor) = when {
                    hasError -> stringResource(R.string.drawer_connection_error) to MaterialTheme.colorScheme.error
                    hasLoaded -> stringResource(R.string.drawer_connected_count, torrentCount) to
                        TorrentStatus.SEEDING.accentColor
                    else -> stringResource(R.string.drawer_connecting) to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(dotColor))
                Spacer(Modifier.width(6.dp))
                Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showChevron) {
            Icon(
                Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DrawerFilterField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.drawer_filter_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.drawer_clear_filter),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** A single pill-shaped row shared by the status list, the label list and the Settings entry. */
@Composable
private fun DrawerItemRow(
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    dotColor: Color? = null,
    hollowDot: Boolean = false,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
                hollowDot -> Box(
                    Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(5.dp)),
                )
                dotColor != null -> Box(Modifier.size(13.dp).clip(RoundedCornerShape(5.dp)).background(dotColor))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

private fun TorrentFilter.icon(): ImageVector = when (this) {
    TorrentFilter.ALL -> Icons.Rounded.ListAlt
    TorrentFilter.DOWNLOADING -> Icons.Rounded.Download
    TorrentFilter.SEEDING -> Icons.Rounded.Upload
    TorrentFilter.COMPLETED -> Icons.Rounded.CheckCircle
    TorrentFilter.PAUSED -> Icons.Rounded.Pause
}
