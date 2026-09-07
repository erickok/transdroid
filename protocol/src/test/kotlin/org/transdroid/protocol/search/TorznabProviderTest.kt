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
package org.transdroid.protocol.search

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.transdroid.protocol.DaemonException

class TorznabProviderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun provider() = TorznabProvider(
        endpointUrl = server.url("/api/v2.0/indexers/all/results/torznab").toString(),
        apiKey = "key123",
        httpClient = OkHttpClient(),
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/search/$name")) { "Missing fixture $name" }
            .bufferedReader().readText()

    @Test
    fun `search builds query and parses results`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("torznab-results.xml")))

        val results = provider().search("linux iso")

        val request = server.takeRequest()
        assertEquals("t=search", request.requestUrl!!.encodedQuery!!.split("&")[0])
        assertEquals("linux iso", request.requestUrl!!.queryParameter("q"))
        assertEquals("key123", request.requestUrl!!.queryParameter("apikey"))

        // The item without any download link is dropped
        assertEquals(2, results.size)
        val first = results[0]
        assertEquals("ubuntu-24.04.2-desktop-amd64.iso", first.title)
        assertTrue("magnet attr wins over enclosure", first.torrentUrl.startsWith("magnet:"))
        assertEquals(6114656256L, first.sizeBytes)
        assertEquals(1200, first.seeders)
        assertEquals("leechers derived from peers minus seeders", 50, first.leechers)
        assertEquals(1785060001L, first.timestamp)

        val second = results[1]
        assertEquals("https://jackett.example.com/dl/1002.torrent", second.torrentUrl)
    }

    @Test
    fun `torznab error document maps to unexpected response`() = runTest {
        server.enqueue(MockResponse().setBody("""<?xml version="1.0"?><error code="100" description="Invalid API Key" />"""))

        try {
            provider().search("query")
            fail("Expected DaemonException.UnexpectedResponse")
        } catch (expected: DaemonException.UnexpectedResponse) {
            assertTrue(expected.message!!.contains("Invalid API Key"))
        }
    }

    @Test
    fun `401 maps to authentication error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        try {
            provider().search("query")
            fail("Expected DaemonException.Authentication")
        } catch (expected: DaemonException.Authentication) {
        }
    }

    @Test
    fun `sends basic auth header when username and password are configured`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("torznab-results.xml")))

        TorznabProvider(
            endpointUrl = server.url("/api/v2.0/indexers/all/results/torznab").toString(),
            apiKey = "key123",
            httpClient = OkHttpClient(),
            username = "member",
            password = "secret",
        ).search("linux iso")

        val request = server.takeRequest()
        assertEquals("Basic bWVtYmVyOnNlY3JldA==", request.getHeader("Authorization"))
    }

    @Test
    fun `omits basic auth header when no username or password is configured`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("torznab-results.xml")))

        provider().search("linux iso")

        val request = server.takeRequest()
        assertEquals(null, request.getHeader("Authorization"))
    }
}
