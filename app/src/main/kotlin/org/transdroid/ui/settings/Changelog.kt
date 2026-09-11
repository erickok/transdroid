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
package org.transdroid.ui.settings

enum class ChangeKind { NEW, IMPROVED, FIXED }

data class ChangelogEntry(val kind: ChangeKind, val text: String)

data class Release(val version: String, val date: String, val changes: List<ChangelogEntry>)

/**
 * The in-app "What's new" history, newest release first - this is hand-maintained, there is no
 * generator. Add one [Release] here (with a handful of user-facing bullets, not every commit)
 * once a version is actually tagged and released, not while it's still in development; the
 * first item is shown with a "Latest" badge regardless of what BuildConfig.VERSION_NAME says.
 */
val CHANGELOG: List<Release> = listOf(
    Release(
        version = "3.0.0-alpha2",
        date = "September 2026",
        changes = listOf(
            ChangelogEntry(ChangeKind.NEW, "About screen with license, project links and a changelog, plus a donate link"),
            ChangelogEntry(ChangeKind.NEW, "Set a torrent's label, or create a new one, from its details screen"),
            ChangelogEntry(ChangeKind.NEW, "Verify data (force recheck) from the torrent details screen"),
            ChangelogEntry(ChangeKind.NEW, "Alternative (\"turtle\") speed limits toggle for qBittorrent and Transmission"),
            ChangelogEntry(ChangeKind.NEW, "Switch servers directly from the torrents list, and a redesigned app icon"),
            ChangelogEntry(ChangeKind.NEW, "\"Send error report\" to get support or report a bug, modernized from Transdroid 2"),
            ChangelogEntry(ChangeKind.IMPROVED, "Predictive back gesture now animates correctly"),
            ChangelogEntry(ChangeKind.IMPROVED, "Redesigned empty states across the torrents list, RSS and search"),
            ChangelogEntry(ChangeKind.IMPROVED, "Torrent details refresh immediately after pausing or starting"),
            ChangelogEntry(ChangeKind.FIXED, "Deluge servers failed to load torrents with an \"Unknown method\" error"),
            ChangelogEntry(ChangeKind.FIXED, "qBittorrent could get itself banned by retrying a failed login every poll"),
        ),
    ),
    Release(
        version = "3.0.0-alpha1",
        date = "September 2026",
        changes = listOf(
            ChangelogEntry(ChangeKind.NEW, "Complete rebuild in Kotlin and Jetpack Compose"),
            ChangelogEntry(ChangeKind.NEW, "RSS feeds, Torznab search, finished-torrent notifications and a home screen widget"),
            ChangelogEntry(ChangeKind.NEW, "Certificate pinning, LAN server discovery, labels and per-file priorities"),
            ChangelogEntry(ChangeKind.NEW, "Connect through login portals like Cloudflare Access, and add torrents paused"),
            ChangelogEntry(ChangeKind.NEW, "Encrypted settings backup and restore"),
            ChangelogEntry(ChangeKind.IMPROVED, "Clearer connection error messages and in-app connection help"),
        ),
    ),
)
