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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private val WaveLength = 13.dp
private val WaveAmplitude = 2.1.dp
private val Gap = 7.dp
private val DotRadius = 2.2.dp

/**
 * Torrent list/details progress bar matching the design mockups: a flat two-tone line for
 * idle/seeding/queued rows, and an animated "wave" leading edge for actively-downloading rows.
 */
@Composable
fun TorrentProgressIndicator(
    progress: Float,
    active: Boolean,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animateWave = active && clamped in 0f..1f && clamped < 1f
    val phase: Dp = if (animateWave) {
        val transition = rememberInfiniteTransition(label = "torrent-wave")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = WaveLength.value,
            animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart),
            label = "torrent-wave-phase",
        )
        animated.dp
    } else {
        remember { mutableFloatStateOf(0f) }.floatValue.dp
    }

    Canvas(modifier = modifier.height(14.dp)) {
        val w = size.width
        val strokeWidthPx = (if (active) 4.dp else 3.dp).toPx()
        val dotRadiusPx = DotRadius.toPx()
        val yMid = size.height / 2f
        val activeW = (w * clamped).coerceIn(0f, w)
        val gapPx = Gap.toPx()

        fun line(x1: Float, x2: Float, c: Color, strokeWidth: Float = strokeWidthPx) {
            if (x2 <= x1) return
            drawLine(c, Offset(x1, yMid), Offset(x2, yMid), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }

        fun dot(x: Float, c: Color) = drawCircle(c, radius = dotRadiusPx, center = Offset(x, yMid))

        when {
            clamped <= 0f -> line(strokeWidthPx, w - strokeWidthPx, trackColor)
            clamped >= 1f -> {
                line(strokeWidthPx, w - strokeWidthPx, color)
                dot(w - strokeWidthPx, color)
            }
            animateWave -> {
                line(activeW + gapPx, w - strokeWidthPx, trackColor)
                val waveLengthPx = WaveLength.toPx()
                val amplitudePx = WaveAmplitude.toPx()
                val phasePx = phase.toPx()
                val path = Path()
                var x = -waveLengthPx
                var first = true
                while (x <= activeW + waveLengthPx) {
                    val y = yMid + amplitudePx * sin(((x + phasePx) / waveLengthPx) * 2 * PI).toFloat()
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    x += 1.5f
                }
                clipRect(left = 0f, top = 0f, right = activeW, bottom = size.height) {
                    drawPath(path, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
                }
                dot(w - strokeWidthPx, trackColor)
            }
            else -> {
                line(strokeWidthPx, activeW, color)
                line(activeW + gapPx, w - strokeWidthPx, trackColor)
            }
        }
    }
}
