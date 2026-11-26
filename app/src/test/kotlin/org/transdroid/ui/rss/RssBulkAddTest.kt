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
package org.transdroid.ui.rss

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.transdroid.data.RssFeed
import org.transdroid.protocol.rss.RssItem

class RssBulkAddTest {

    private val feedA = RssFeed(id = "a", name = "Feed A", url = "https://example.com/rss?q=a")
    private val feedB = RssFeed(id = "b", name = "Feed B", url = "https://example.com/rss?q=b")

    private fun entry(feed: RssFeed, torrentUrl: String?, title: String = torrentUrl ?: "no link", isNew: Boolean = false) =
        RssEntry(
            feed = feed,
            item = RssItem(title = title, link = null, torrentUrl = torrentUrl, timestamp = null, sizeBytes = null),
            isNew = isNew,
        )

    @Test
    fun onlyItemsWithALinkThatAreNotAddedYetCanBeSelected() {
        val withLink = entry(feedA, "https://example.com/1.torrent")
        val noLink = entry(feedA, null)

        assertTrue(canSelect(withLink, addedKeys = emptySet()))
        assertFalse(canSelect(noLink, addedKeys = emptySet()))
        assertFalse(canSelect(withLink, addedKeys = setOf(withLink.key)))
    }

    @Test
    fun theSameTorrentSelectedUnderTwoFeedsIsAddedOnce() {
        val url = "https://example.com/1.torrent"
        val targets = bulkAddTargets(
            listOf(entry(feedA, url), entry(feedB, url), entry(feedB, "https://example.com/2.torrent")),
            addedKeys = emptySet(),
        )

        assertEquals(listOf(url, "https://example.com/2.torrent"), targets.map { it.item.torrentUrl })
    }

    @Test
    fun bulkTargetsSkipItemsWithoutALinkAndAlreadyAddedOnes() {
        val added = entry(feedA, "https://example.com/1.torrent")
        val targets = bulkAddTargets(
            listOf(added, entry(feedA, null), entry(feedA, "https://example.com/2.torrent")),
            addedKeys = setOf(added.key),
        )

        assertEquals(listOf("https://example.com/2.torrent"), targets.map { it.item.torrentUrl })
    }

    @Test
    fun addEachKeepsGoingAfterAFailureAndReturnsWhatWasAdded() = runTest {
        val targets = listOf("1", "2", "3").map { entry(feedA, "https://example.com/$it.torrent") }
        val attempted = mutableListOf<String>()

        val added = addEach(targets) { url ->
            attempted += url
            if (url.endsWith("2.torrent")) throw IOException("server said no")
        }

        assertEquals(targets.map { it.item.torrentUrl }, attempted)
        assertEquals(setOf("https://example.com/1.torrent", "https://example.com/3.torrent"), added)
    }

    @Test
    fun addEachStopsWhenCancelled() = runTest {
        val targets = listOf("1", "2").map { entry(feedA, "https://example.com/$it.torrent") }
        val attempted = mutableListOf<String>()

        try {
            addEach(targets) { url ->
                attempted += url
                throw CancellationException("screen closed")
            }
            fail("cancellation should propagate")
        } catch (e: CancellationException) {
            assertEquals(1, attempted.size)
        }
    }

    @Test
    fun anAddedTorrentShowsAsAddedUnderEveryFeed() {
        val url = "https://example.com/1.torrent"
        val entries = listOf(entry(feedA, url), entry(feedB, url), entry(feedB, "https://example.com/2.torrent"))

        assertEquals(setOf(entries[0].key, entries[1].key), keysForUrls(entries, setOf(url)))
    }

    @Test
    fun aFilterChangeOrRefreshDropsSelectedItemsThatAreNoLongerVisible() {
        val a = entry(feedA, "https://example.com/1.torrent", isNew = true)
        val b = entry(feedB, "https://example.com/2.torrent", isNew = false)
        val state = RssUiState(entries = listOf(a, b), selection = setOf(a.key, b.key))

        assertEquals(setOf(a.key), state.copy(newOnly = true).withPrunedSelection().selection)
        assertEquals(setOf(b.key), state.copy(selectedFeedId = "b").withPrunedSelection().selection)
        assertEquals(setOf(a.key), state.copy(entries = listOf(a)).withPrunedSelection().selection)
    }

    @Test
    fun bulkTargetsOnlyCountSelectedItemsThatAreVisible() {
        val a = entry(feedA, "https://example.com/1.torrent")
        val b = entry(feedB, "https://example.com/2.torrent")
        val state = RssUiState(entries = listOf(a, b), selection = setOf(a.key), selectedFeedId = null)

        assertEquals(listOf(a), state.bulkTargets)
        assertEquals(emptyList<RssEntry>(), state.copy(selectedFeedId = "b").bulkTargets)
    }
}
