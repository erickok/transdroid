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
package org.transdroid.ui.torrents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentStatus

class TorrentFiltersTest {

    private fun torrent(id: String, labels: List<String> = emptyList(), trackers: List<String> = emptyList()) = Torrent(
        id = id,
        name = id,
        status = TorrentStatus.SEEDING,
        progress = 1f,
        downloadRate = 0,
        uploadRate = 0,
        sizeBytes = 1,
        downloadedBytes = 1,
        uploadedBytes = 0,
        ratio = 0f,
        labels = labels,
        trackers = trackers,
    )

    @Test
    fun `stored filter names fall back to all`() {
        assertEquals(TorrentFilter.COMPLETED, TorrentFilter.fromName("COMPLETED"))
        assertEquals(TorrentFilter.ALL, TorrentFilter.fromName("RENAMED"))
        assertEquals(TorrentFilter.ALL, TorrentFilter.fromName(null))
    }

    @Test
    fun `drawer lists match the torrents when nothing stale is selected`() {
        val ui = TorrentsUiState(
            torrents = listOf(torrent("a", labels = listOf("tv"), trackers = listOf("t1")), torrent("b")),
            labelFilters = setOf("tv"),
        )
        assertEquals(listOf("tv"), ui.drawerLabels)
        assertTrue(ui.drawerShowsNoLabel)
        assertEquals(listOf("t1"), ui.drawerTrackers)
    }

    @Test
    fun `restored filters no torrent matches anymore stay in the drawer so they can be cleared`() {
        val ui = TorrentsUiState(
            torrents = listOf(torrent("a", labels = listOf("tv"), trackers = listOf("t1"))),
            labelFilters = setOf("movies", NO_LABEL),
            trackerFilters = setOf("gone.example"),
        )
        assertTrue(ui.visibleTorrents.isEmpty())
        assertEquals(listOf("movies", "tv"), ui.drawerLabels)
        assertTrue(ui.drawerShowsNoLabel)
        assertEquals(listOf("gone.example", "t1"), ui.drawerTrackers)
        // The label picker still only offers labels that exist on the server
        assertEquals(listOf("tv"), ui.availableLabels)
        assertFalse(ui.hasUnlabeledTorrents)
    }
}
