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
package org.transdroid.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RssFeedTest {

    private val feed = RssFeed(id = "f", name = "Shows", url = "https://example.com/rss", lastViewedTimestamp = 42)

    @Test
    fun feedsStoredBeforeFiltersExistedStillLoad() {
        // As ProfilesData was stored (and backed up) before the filter fields were added
        val stored = """{"feeds":[{"id":"f","name":"Shows","url":"https://example.com/rss","lastViewedTimestamp":42}]}"""
        val data = Json { ignoreUnknownKeys = true }.decodeFromString(ProfilesData.serializer(), stored)
        assertEquals(feed, data.feeds.single())
        assertFalse(data.feeds.single().hasFilters)
    }

    @Test
    fun editingKeepsTheIdAndLastViewedMarker() {
        val edited = feed.edited(name = "TV", url = feed.url, includeFilter = "1080p", excludeFilter = "cam")
        assertEquals(feed.copy(name = "TV", includeFilter = "1080p", excludeFilter = "cam"), edited)
        assertTrue(edited.hasFilters)
    }

    @Test
    fun aNewUrlDropsTheLastViewedMarker() {
        val edited = feed.edited(name = feed.name, url = "https://example.com/other", includeFilter = "", excludeFilter = "")
        assertEquals("f", edited.id)
        assertNull(edited.lastViewedTimestamp)
    }

    @Test
    fun onlyFiltersWithWordsCountAsFilters() {
        assertFalse(feed.copy(includeFilter = "|", excludeFilter = "").hasFilters)
        // A space is a word, as the filter applies it
        assertTrue(feed.copy(includeFilter = "", excludeFilter = " ").hasFilters)
    }
}
