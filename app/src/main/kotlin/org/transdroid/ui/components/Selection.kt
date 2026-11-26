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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.transdroid.R

/** The width of [SelectionCheck], for rows that keep its space without showing one. */
val SelectionCheckSize = 22.dp

/**
 * Replaces the top bar while torrents or RSS items are selected. Same height as the normal phone
 * and tablet bars (status bar inset plus 64dp, and the tablet bar's divider), so the list never
 * shifts.
 */
@Composable
fun SelectionTopBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, showDivider: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTooltip(stringResource(R.string.selection_close)) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.selection_close))
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.selection_count, count),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconTooltip(stringResource(R.string.selection_select_all)) {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = stringResource(R.string.selection_select_all))
                    }
                }
            }
            if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/** The check circle that replaces a row's status icon while selecting. */
@Composable
fun SelectionCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val ring = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .size(SelectionCheckSize)
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
