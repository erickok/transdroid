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

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.transdroid.protocol.DaemonException

class RssFetcherTest {

    private val fetcher = RssFetcher(OkHttpClient())

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/rss/$name")) { "Missing fixture $name" }
            .bufferedReader().readText()

    @Test
    fun `parses rss2 feed preferring magnet then enclosure then link`() {
        val channel = fetcher.parse(fixture("feed-rss2.xml"))

        assertEquals("Linux ISO releases", channel.title)
        assertEquals(3, channel.items.size)

        val enclosureItem = channel.items[0]
        assertEquals("ubuntu-24.04.2-desktop-amd64.iso", enclosureItem.title)
        assertEquals("https://example.com/torrents/1.torrent", enclosureItem.torrentUrl)
        assertEquals(1785060001L, enclosureItem.timestamp)
        assertEquals(6114656256L, enclosureItem.sizeBytes)

        val magnetItem = channel.items[1]
        assertEquals("magnet:?xt=urn:btih:2aa4f5a7e209e54b32803d43670971c4c8caaa05", magnetItem.torrentUrl)

        val linkOnly = channel.items[2]
        assertEquals("https://example.com/torrents/3", linkOnly.torrentUrl)
        assertNull(linkOnly.timestamp)
    }

    @Test
    fun `parses atom feed`() {
        val channel = fetcher.parse(fixture("feed-atom.xml"))

        assertEquals("Nightly builds", channel.title)
        assertEquals(2, channel.items.size)
        assertEquals("https://example.com/builds/2026-07-26.torrent", channel.items[0].torrentUrl)
        assertEquals(1785060001L, channel.items[0].timestamp)
        assertEquals(734003200L, channel.items[0].sizeBytes)
        assertEquals("https://example.com/builds/2026-07-25", channel.items[1].torrentUrl)
    }

    @Test
    fun `fetches over http`() = runTest {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody(fixture("feed-rss2.xml")))

        val channel = fetcher.fetch(server.url("/rss?passkey=secret").toString())

        assertEquals(3, channel.items.size)
        server.shutdown()
    }

    @Test
    fun `html error page maps to unexpected response`() {
        try {
            fetcher.parse("<html><body>login required</body></html>")
            fail("Expected DaemonException.UnexpectedResponse")
        } catch (expected: DaemonException.UnexpectedResponse) {
        }
    }
}
