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
package org.transdroid.protocol.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RssFilterTest {

    @Test
    fun noFiltersKeepEverything() {
        val filter = RssFilter("", "")
        assertTrue(filter.isEmpty)
        assertTrue(filter.matches("Anything at all"))
    }

    @Test
    fun includeKeepsTitlesContainingAnyWordIgnoringCase() {
        val filter = RssFilter(include = "1080p|x265", exclude = "")
        assertTrue(filter.matches("Some.Show.S01E01.1080P.WEB"))
        assertTrue(filter.matches("Other show [X265]"))
        assertFalse(filter.matches("Some.Show.S01E01.720p.WEB"))
    }

    @Test
    fun excludeDropsTitlesContainingAnyWord() {
        val filter = RssFilter(include = "", exclude = "cam|hdts")
        assertFalse(filter.matches("Movie.2026.CAM"))
        assertFalse(filter.matches("Movie 2026 HDTS"))
        assertTrue(filter.matches("Movie.2026.1080p"))
    }

    @Test
    fun excludeWinsOverInclude() {
        val filter = RssFilter(include = "1080p", exclude = "cam")
        assertTrue(filter.matches("Movie.1080p.WEB"))
        assertFalse(filter.matches("Movie.1080p.CAM"))
        assertFalse(filter.matches("Movie.720p.WEB"))
    }

    @Test
    fun emptyWordsAreIgnored() {
        // A trailing or doubled separator doesn't turn into a word that matches every title
        assertFalse(RssFilter(include = "1080p|", exclude = "").matches("Movie.720p"))
        assertTrue(RssFilter(include = "", exclude = "cam||").matches("Movie.720p"))
        assertTrue(RssFilter(include = "|", exclude = "|").isEmpty)
    }

    @Test
    fun wordsAreNotTrimmedLikeInTransdroid2() {
        val filter = RssFilter(include = "", exclude = " CAM ")
        assertFalse(filter.matches("Movie CAM 2026"))
        assertTrue(filter.matches("Movie.CAMERA.2026"))
    }

    @Test
    fun filteredChannelKeepsOnlyMatchingItems() {
        val channel = RssChannel(
            title = "Feed",
            items = listOf("A 1080p", "B 720p", "C 1080p CAM").map { RssItem(it, link = null, torrentUrl = null, timestamp = null, sizeBytes = null) },
        )
        assertEquals(listOf("A 1080p"), channel.filtered(RssFilter("1080p", "cam")).items.map { it.title })
        assertSame(channel, channel.filtered(RssFilter("", "")))
    }
}
