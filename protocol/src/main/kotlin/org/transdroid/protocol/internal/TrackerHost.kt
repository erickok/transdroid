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
package org.transdroid.protocol.internal

import java.net.URI

/**
 * Extracts the host from an announce URL, e.g. "https://tracker.example.org:443/announce" ->
 * "tracker.example.org" - used to collapse a torrent's full/scrape-varying announce URLs down to
 * the stable identifier this app filters and groups by. Falls back to the raw string for anything
 * that isn't a parseable URL (some trackers use non-http schemes like udp:// with quirky syntax).
 */
internal fun trackerHost(url: String): String = try {
    URI(url).host?.takeIf { it.isNotBlank() } ?: url
} catch (e: Exception) {
    url
}
