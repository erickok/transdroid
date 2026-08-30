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
package org.transdroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.transdroid.protocol.TorrentStatus

// M3 Expressive palette: warm off-white/green-tinted surfaces with a deep olive-green primary,
// a direct evolution of the classic Transdroid grey-green identity. Values match
// design/mockups/transdroid-m3-expressive.html and -dark.html, the source-of-truth visual reference.

private val LightPrimary = Color(0xFF3F6A1E)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFC0F39A)
private val LightOnPrimaryContainer = Color(0xFF0B2100)
private val LightSurface = Color(0xFFF8FAF0)
private val LightSurfaceContainerLow = Color(0xFFF2F5EA)
private val LightSurfaceContainer = Color(0xFFECEEE2)
private val LightSurfaceContainerHigh = Color(0xFFE6E9DD)
private val LightSurfaceContainerHighest = Color(0xFFE0E3D7)
private val LightOnSurface = Color(0xFF1A1C15)
private val LightOnSurfaceVariant = Color(0xFF44483B)
private val LightOutline = Color(0xFF75796A)
private val LightOutlineVariant = Color(0xFFC5C8B8)

private val DarkPrimary = Color(0xFFA6D67E)
private val DarkOnPrimary = Color(0xFF133800)
private val DarkPrimaryContainer = Color(0xFF284F10)
private val DarkOnPrimaryContainer = Color(0xFFC1F39B)
private val DarkSurface = Color(0xFF12140E)
private val DarkSurfaceContainerLow = Color(0xFF191B14)
private val DarkSurfaceContainer = Color(0xFF1E201A)
private val DarkSurfaceContainerHigh = Color(0xFF282B23)
private val DarkSurfaceContainerHighest = Color(0xFF33362D)
private val DarkOnSurface = Color(0xFFE3E4D9)
private val DarkOnSurfaceVariant = Color(0xFFC6C9B9)
private val DarkOutline = Color(0xFF8E9284)
private val DarkOutlineVariant = Color(0xFF43473B)

// Not specified by the mockups (no secondary/tertiary/error swatches shown) - kept close to the
// previous grey-green theme's values rather than invented from scratch.
private val LegacyTertiary = Color(0xFF7DBB21)
private val LegacyError = Color(0xFFC62828)
private val LegacyErrorDark = Color(0xFFEF9A9A)

// Selection pill in the filter drawer (design/mockups/transdroid-m3-filter-drawer.html: --sel-bg /
// --sel-fg) - a softer, more desaturated green than primaryContainer, mapped onto the otherwise
// unused tertiary-container role. No dark mockup exists for the drawer; the dark pair is derived
// following the same lighten/desaturate relationship the other dark tokens use.
private val LightSelectedContainer = Color(0xFFD8E8C9)
private val LightOnSelectedContainer = Color(0xFF2B3B1C)
private val DarkSelectedContainer = Color(0xFF3A4A2C)
private val DarkOnSelectedContainer = Color(0xFFD7E4C4)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightOnSurfaceVariant,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceContainerHigh,
    onSecondaryContainer = LightOnSurface,
    tertiary = LegacyTertiary,
    tertiaryContainer = LightSelectedContainer,
    onTertiaryContainer = LightOnSelectedContainer,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceContainerHighest,
    surfaceBright = LightSurface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LegacyError,
    onError = Color.White,
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkOnSurfaceVariant,
    onSecondary = Color(0xFF243425),
    secondaryContainer = DarkSurfaceContainerHigh,
    onSecondaryContainer = DarkOnSurface,
    tertiary = LegacyTertiary,
    tertiaryContainer = DarkSelectedContainer,
    onTertiaryContainer = DarkOnSelectedContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkSurface,
    surfaceBright = DarkSurfaceContainerHighest,
    surfaceContainerLowest = Color(0xFF0C0D09),
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = LegacyErrorDark,
    onError = Color(0xFF400C0C),
)

/**
 * Torrent status accents plus a paired lighter "track" tone for progress-bar backgrounds, and the
 * error color (kept in sync with [ColorScheme.error] since the mockups don't define a status color
 * for the error state). Theme-aware: dark-mode values are brightened, not an inverted light palette.
 */
data class StatusColors(
    val downloading: Color,
    val downloadingTrack: Color,
    val seeding: Color,
    val seedingTrack: Color,
    val idle: Color,
    val idleTrack: Color,
    val waiting: Color,
    val waitingTrack: Color,
    val error: Color,
)

val LightStatusColors = StatusColors(
    downloading = Color(0xFF3F68B8),
    downloadingTrack = Color(0xFFD3DDF2),
    seeding = Color(0xFF4D7D2A),
    seedingTrack = Color(0xFFD3E6BE),
    idle = Color(0xFF8A8D80),
    idleTrack = Color(0xFFD6D9CB),
    waiting = Color(0xFF9A7B33),
    waitingTrack = Color(0xFFE7DDC6),
    error = LegacyError,
)

val DarkStatusColors = StatusColors(
    downloading = Color(0xFFADC7FF),
    downloadingTrack = Color(0xFF324365),
    seeding = Color(0xFFA0CF72),
    seedingTrack = Color(0xFF2F4A1E),
    idle = Color(0xFFB1B5A5),
    idleTrack = Color(0xFF3A3E33),
    waiting = Color(0xFFF0C250),
    waitingTrack = Color(0xFF473D1F),
    error = LegacyErrorDark,
)

/**
 * Progress/state accent for a torrent's current status. Queued/checking (not yet actively
 * transferring) read as "waiting"; paused/unknown read as "idle"; error is its own accent since
 * the mockups don't define a status color for it.
 */
fun StatusColors.accent(status: TorrentStatus): Color = when (status) {
    TorrentStatus.DOWNLOADING -> downloading
    TorrentStatus.SEEDING -> seeding
    TorrentStatus.QUEUED, TorrentStatus.CHECKING -> waiting
    TorrentStatus.ERROR -> error
    TorrentStatus.PAUSED, TorrentStatus.UNKNOWN -> idle
}

/** The lighter progress-track tone paired with [accent]. */
fun StatusColors.track(status: TorrentStatus): Color = when (status) {
    TorrentStatus.DOWNLOADING -> downloadingTrack
    TorrentStatus.SEEDING -> seedingTrack
    TorrentStatus.QUEUED, TorrentStatus.CHECKING -> waitingTrack
    TorrentStatus.PAUSED, TorrentStatus.ERROR, TorrentStatus.UNKNOWN -> idleTrack
}

// Label/category chip hues - labels are free-form user text, so a fixed palette is cycled by a
// stable hash of the label name rather than matched to specific category keywords.
private val LabelHues: List<Pair<Color, Color>> = listOf(
    Color(0xFFF2A93C) to Color(0xFFFFF2D9), // amber
    Color(0xFF9C6ADE) to Color(0xFFF1E7FB), // purple
    Color(0xFF3F68B8) to Color(0xFFDDE5F7), // blue
    Color(0xFF2E9E8F) to Color(0xFFDCF1EE), // teal
)

/** Returns a stable (color, containerColor) pair for a label/category name. */
fun labelColor(label: String): Pair<Color, Color> = LabelHues[label.hashCode().mod(LabelHues.size)]
