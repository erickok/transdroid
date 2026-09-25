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
        version = "3.0.0-alpha5",
        date = "September 2026",
        changes = listOf(
            ChangelogEntry(ChangeKind.NEW, "Sort the Peers tab by speed, IP address or client"),
            ChangelogEntry(ChangeKind.NEW, "Optional on-device country database shows a flag for every peer, even on clients that don't report it themselves"),
            ChangelogEntry(ChangeKind.NEW, "Connect to qBittorrent 5.2+ using its Web UI API key instead of a username/password"),
            ChangelogEntry(ChangeKind.IMPROVED, "Swipe between the details screen's Overview/Files/Trackers/Peers tabs"),
            ChangelogEntry(ChangeKind.IMPROVED, "Torrent list sort order is remembered across app restarts, and changing it scrolls back to the top"),
        ),
    ),
    Release(
        version = "3.0.0-alpha4",
        date = "September 2026",
        changes = listOf(
            ChangelogEntry(ChangeKind.NEW, "Peers tab on the details screen: client, country, progress and speed for connected peers"),
            ChangelogEntry(ChangeKind.FIXED, "Adding a torrent from search could silently fail when the indexer needs its own login (e.g. a seedbox member area), separate from its API key"),
            ChangelogEntry(ChangeKind.FIXED, "A torrent shown as \"Error\" but actually stopped now offers Start instead of only Pause"),
        ),
    ),
    Release(
        version = "3.0.0-alpha3",
        date = "September 2026",
        changes = listOf(
            ChangelogEntry(ChangeKind.NEW, "Filter the torrent list by tracker"),
            ChangelogEntry(ChangeKind.NEW, "Set a download location when adding a torrent, including from Search and RSS"),
            ChangelogEntry(ChangeKind.NEW, "Redesigned Add torrent screen"),
            ChangelogEntry(ChangeKind.NEW, "Home screen widget shows a scrollable list of running torrents; each widget instance can pick its own server"),
            ChangelogEntry(ChangeKind.NEW, "Connect using different settings while on a chosen Wi-Fi network, e.g. a seedbox also reachable directly over the LAN"),
            ChangelogEntry(ChangeKind.IMPROVED, "Details screen action icons use a consistent accent color"),
            ChangelogEntry(ChangeKind.IMPROVED, "Location and label text wraps instead of truncating on the details Overview tab"),
            ChangelogEntry(ChangeKind.IMPROVED, "Clearer Edit Server screen: leading icons, a password-reveal toggle, and better connection help wording"),
            ChangelogEntry(ChangeKind.IMPROVED, "Widget uses the real app icon instead of a generic badge"),
            ChangelogEntry(ChangeKind.FIXED, "Widget icon rendering too small"),
            ChangelogEntry(ChangeKind.FIXED, "Tablet details placeholder styling and top bar status bar inset"),
        ),
    ),
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
