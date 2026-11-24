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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.transdroid.data.RssFeed
import org.transdroid.protocol.rss.RssItem

class MergeFeedEntriesTest {

    private val feedA = RssFeed(id = "a", name = "Feed A", url = "https://example.com/rss?q=a")
    private val feedB = RssFeed(id = "b", name = "Feed B", url = "https://example.com/rss?q=b")

    private fun entry(feed: RssFeed, torrentUrl: String?, timestamp: Long?, title: String = "item", link: String? = null) =
        RssEntry(
            feed = feed,
            item = RssItem(title = title, link = link, torrentUrl = torrentUrl, timestamp = timestamp, sizeBytes = null),
            isNew = false,
        )

    @Test
    fun sameTorrentInTwoFeedsKeepsBothWithDistinctKeys() {
        val url = "https://example.com/download/1234.torrent"
        val merged = mergeFeedEntries(listOf(listOf(entry(feedA, url, 100)), listOf(entry(feedB, url, 100))))

        assertEquals(2, merged.size)
        assertEquals(2, merged.map { it.key }.toSet().size)
    }

    @Test
    fun repeatedItemWithinOneFeedIsDroppedKeepingTheFirst() {
        val url = "https://example.com/download/1234.torrent"
        val merged = mergeFeedEntries(listOf(listOf(entry(feedA, url, 100, title = "first"), entry(feedA, url, 50, title = "second"))))

        assertEquals(listOf("first"), merged.map { it.item.title })
    }

    @Test
    fun itemsWithoutLinksFallBackToTitleForTheKey() {
        val merged = mergeFeedEntries(
            listOf(listOf(entry(feedA, null, 1, title = "x"), entry(feedA, null, 2, title = "y"), entry(feedA, null, 3, title = "x"))),
        )

        assertEquals(listOf("y", "x"), merged.map { it.item.title })
    }

    @Test
    fun failedFeedsAreSkippedAndResultIsNewestFirst() {
        val merged = mergeFeedEntries(
            listOf(
                listOf(entry(feedA, "u1", 10), entry(feedA, "u2", null)),
                null,
                listOf(entry(feedB, "u3", 30)),
            ),
        )

        assertEquals(listOf("u3", "u1", "u2"), merged.map { it.item.torrentUrl })
    }
}
