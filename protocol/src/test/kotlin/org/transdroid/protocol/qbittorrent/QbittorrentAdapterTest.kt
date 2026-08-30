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
package org.transdroid.protocol.qbittorrent

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.DaemonType
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.TrackerStatus

class QbittorrentAdapterTest {

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

    private fun adapter(username: String? = "admin", password: String? = "adminadmin") = QbittorrentAdapter(
        DaemonConfig(
            type = DaemonType.QBITTORRENT,
            host = server.hostName,
            port = server.port,
            username = username,
            password = password,
        ),
        OkHttpClient(),
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/qbittorrent/$name")) { "Missing fixture $name" }
            .bufferedReader().readText()

    private fun loginOk() = MockResponse()
        .setBody("Ok.")
        .setHeader("Set-Cookie", "SID=sessionIdHere; HttpOnly; path=/")

    @Test
    fun `logs in and passes session cookie to api calls`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-info.json")))

        adapter().listTorrents()

        val login = server.takeRequest()
        assertEquals("/api/v2/auth/login", login.path)
        assertEquals("username=admin&password=adminadmin", login.body.readUtf8())
        val info = server.takeRequest()
        assertEquals("/api/v2/torrents/info", info.path)
        assertEquals("SID=sessionIdHere", info.getHeader("Cookie"))
    }

    @Test
    fun `rejected login maps to authentication error`() = runTest {
        server.enqueue(MockResponse().setBody("Fails."))

        try {
            adapter().listTorrents()
            fail("Expected DaemonException.Authentication")
        } catch (expected: DaemonException.Authentication) {
        }
    }

    @Test
    fun `list torrents parses and normalizes fixture`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-info.json")))

        val torrents = adapter().listTorrents()

        assertEquals(4, torrents.size)
        val downloading = torrents[0]
        assertEquals("8c212779b4abde7c6bc608063a0d008b7e40ce32", downloading.id)
        assertEquals(TorrentStatus.DOWNLOADING, downloading.status)
        assertEquals(1220L, downloading.etaSeconds)
        assertEquals("seeds and leeches sum to connected peers", 34, downloading.peersConnected)
        assertEquals(22, downloading.seedersConnected)
        assertEquals(12, downloading.leechersConnected)
        assertEquals("category maps to a label", listOf("linux"), downloading.labels)

        val seeding = torrents[1]
        assertEquals(TorrentStatus.SEEDING, seeding.status)
        assertNull("eta 8640000 must normalize to null", seeding.etaSeconds)

        assertEquals("qBittorrent 5 stoppedDL state", TorrentStatus.PAUSED, torrents[2].status)
        assertEquals(TorrentStatus.ERROR, torrents[3].status)
    }

    @Test
    fun `anonymous config skips login`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("torrents-info.json")))

        val torrents = adapter(username = null, password = null).listTorrents()

        assertEquals(4, torrents.size)
        assertEquals("/api/v2/torrents/info", server.takeRequest().path)
    }

    @Test
    fun `list trackers filters pseudo-trackers and maps status`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("trackers.json")))

        val trackers = adapter().listTrackers("abcdef")

        assertEquals("DHT pseudo-tracker excluded", 3, trackers.size)
        val working = trackers[0]
        assertEquals(TrackerStatus.WORKING, working.status)
        assertEquals(6, working.seeders)
        assertEquals(40, working.leechers)

        val notWorking = trackers[1]
        assertEquals(TrackerStatus.ERROR, notWorking.status)
        assertEquals("Unregistered torrent", notWorking.message)

        assertEquals("not-contacted status maps to idle", TrackerStatus.IDLE, trackers[2].status)
    }

    @Test
    fun `pause falls back to legacy endpoint on 404`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setBody(""))

        adapter().pause("abcdef")

        server.takeRequest() // login
        assertEquals("/api/v2/torrents/stop", server.takeRequest().path)
        val fallback = server.takeRequest()
        assertEquals("/api/v2/torrents/pause", fallback.path)
        assertEquals("hashes=abcdef", fallback.body.readUtf8())
    }

    @Test
    fun `expired session re-authenticates once`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-info.json")))

        val torrents = adapter().listTorrents()

        assertEquals(4, torrents.size)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `add paused sends both pause field spellings`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody("Ok."))

        adapter().addByUrl("magnet:?xt=urn:btih:abcdef", startPaused = true)

        server.takeRequest() // login
        val add = server.takeRequest().body.readUtf8()
        assertTrue("4.x field", add.contains("paused=true"))
        assertTrue("5.x field", add.contains("stopped=true"))
    }

    @Test
    fun `failed add is surfaced as an error`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody("Fails."))

        try {
            adapter().addByUrl("http://example.com/broken.torrent")
            fail("Expected DaemonException.UnexpectedResponse")
        } catch (expected: DaemonException.UnexpectedResponse) {
        }
    }

    @Test
    fun `remove sends hashes and deleteFiles`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(""))

        adapter().remove("abcdef", deleteData = true)

        server.takeRequest() // login
        val delete = server.takeRequest()
        assertEquals("/api/v2/torrents/delete", delete.path)
        assertEquals("hashes=abcdef&deleteFiles=true", delete.body.readUtf8())
    }
}
