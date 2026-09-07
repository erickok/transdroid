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
package org.transdroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.transdroid.R
import org.transdroid.protocol.TorrentStatus
import org.transdroid.ui.torrents.UiError

@Composable
fun UiError.message(): String = when (this) {
    is UiError.Connection -> stringResource(R.string.error_connection, host)
    is UiError.Authentication -> detail ?: stringResource(R.string.error_authentication)
    UiError.Ssl -> stringResource(R.string.error_ssl)
    is UiError.Unexpected ->
        detail ?: stringResource(R.string.error_unexpected)
}

@Composable
fun TorrentStatus.label(): String = stringResource(
    when (this) {
        TorrentStatus.DOWNLOADING -> R.string.status_downloading
        TorrentStatus.SEEDING -> R.string.status_seeding
        TorrentStatus.PAUSED -> R.string.status_paused
        TorrentStatus.CHECKING -> R.string.status_checking
        TorrentStatus.QUEUED -> R.string.status_queued
        TorrentStatus.ERROR -> R.string.status_error
        TorrentStatus.UNKNOWN -> R.string.status_unknown
    }
)

/** The status label, with the magnet metadata-fetch phase surfaced explicitly. */
@Composable
fun org.transdroid.protocol.Torrent.statusLabel(): String =
    if (metadataProgress != null) stringResource(R.string.status_metadata) else status.label()
