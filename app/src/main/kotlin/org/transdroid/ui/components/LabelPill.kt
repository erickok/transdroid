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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.transdroid.ui.theme.labelHue

enum class LabelPillSize { Compact, Regular }

/**
 * A torrent's label as a pill in the label's own hue, with a matching dot: [LabelPillSize.Compact]
 * on the torrent list rows, [LabelPillSize.Regular] next to the status pill on the details screen.
 */
@Composable
fun LabelPill(label: String, modifier: Modifier = Modifier, size: LabelPillSize = LabelPillSize.Compact) {
    val hue = labelHue(label)
    val (padding, style) = when (size) {
        LabelPillSize.Compact -> PaddingValues(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp) to
            TextStyle(fontSize = 11.5.sp, lineHeight = 14.sp, letterSpacing = 0.1.sp)
        LabelPillSize.Regular -> PaddingValues(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp) to
            MaterialTheme.typography.labelMedium
    }
    Row(
        modifier = modifier
            .background(hue.container, CircleShape)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(hue.content, CircleShape))
        Text(
            label,
            style = style,
            fontWeight = FontWeight.SemiBold,
            color = hue.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // A long label shouldn't push the row's size/progress text out of view
            modifier = Modifier.widthIn(max = 160.dp),
        )
    }
}
