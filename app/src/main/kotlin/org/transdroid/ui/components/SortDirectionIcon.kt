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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Three-bar sort-direction glyph, replacing a plain up/down arrow: the bars themselves shrink
 * top-to-bottom for descending order and grow top-to-bottom for ascending, so the shape encodes
 * direction rather than needing a separate arrow head.
 */
@Composable
fun SortDirectionIcon(descending: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val fractions = if (descending) listOf(1f, 0.68f, 0.36f) else listOf(0.36f, 0.68f, 1f)
        val strokeWidthPx = 2.dp.toPx()
        val left = 3.dp.toPx()
        val maxWidth = size.width - left * 2
        val rowGap = size.height / 4
        fractions.forEachIndexed { index, fraction ->
            val y = rowGap * (index + 1)
            drawLine(
                color = tint,
                start = Offset(left, y),
                end = Offset(left + maxWidth * fraction, y),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}
