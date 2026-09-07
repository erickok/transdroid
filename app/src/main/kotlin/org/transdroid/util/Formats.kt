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
package org.transdroid.util

import android.text.format.DateUtils
import java.util.Locale

private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

/** Formats a byte count like "1.4 GB" (binary-based, single decimal above KB). */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < UNITS.size - 1) {
        value /= 1024
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, UNITS[unit])
}

fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/s"

/** Formats seconds like "2d 4h", "3h 12m", "45m" or "30s"; null for unknown. */
fun formatEta(seconds: Long?): String? {
    if (seconds == null || seconds < 0) return null
    val days = seconds / 86400
    val hours = seconds % 86400 / 3600
    val minutes = seconds % 3600 / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/** Formats a ratio like "2.04". */
fun formatRatio(ratio: Float): String = String.format(Locale.getDefault(), "%.2f", ratio)

/**
 * Formats a unix-seconds timestamp like "2 days ago" or "3 weeks ago"; null for unknown.
 * [minResolutionMillis] is the smallest unit shown - day resolution for an item's publish age,
 * but minute resolution reads better for a "last updated" marker from moments ago.
 */
fun formatRelativeAge(epochSeconds: Long?, minResolutionMillis: Long = DateUtils.DAY_IN_MILLIS): String? {
    if (epochSeconds == null) return null
    return DateUtils.getRelativeTimeSpanString(
        epochSeconds * 1000,
        System.currentTimeMillis(),
        minResolutionMillis,
    ).toString()
}
