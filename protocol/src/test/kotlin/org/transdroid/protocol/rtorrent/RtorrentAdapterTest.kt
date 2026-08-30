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
package org.transdroid.protocol.rtorrent

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
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.TrackerStatus

class RtorrentAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: RtorrentAdapter

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        adapter = RtorrentAdapter(
            DaemonConfig(
                type = DaemonType.RTORRENT,
                host = server.hostName,
                port = server.port,
                username = "user",
                password = "secret",
            ),
            OkHttpClient(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/rtorrent/$name")) { "Missing fixture $name" }
            .bufferedReader().readText()

    private fun xmlResponse(inner: String) = MockResponse().setBody(
        """<?xml version="1.0"?><methodResponse><params><param><value>$inner</value></param></params></methodResponse>"""
    )

    @Test
    fun `test connection reads client version and uses default RPC2 path`() = runTest {
        server.enqueue(xmlResponse("<string>0.9.8</string>"))

        val version = adapter.testConnection()

        assertEquals("rTorrent 0.9.8", version)
        val request = server.takeRequest()
        assertEquals("/RPC2", request.path)
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
        assertTrue(request.body.readUtf8().contains("<methodName>system.client_version</methodName>"))
    }

    @Test
    fun `list torrents parses and normalizes multicall fixture`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("d-multicall2.xml")))

        val torrents = adapter.listTorrents()

        assertEquals(4, torrents.size)
        val downloading = torrents[0]
        assertEquals("8C212779B4ABDE7C6BC608063A0D008B7E40CE32", downloading.id)
        assertEquals(TorrentStatus.DOWNLOADING, downloading.status)
        assertEquals(0.4266f, downloading.progress, 0.001f)
        assertEquals("eta computed from remaining bytes and rate", 2804L, downloading.etaSeconds)
        assertEquals("per-mille ratio normalized", 0.04f, downloading.ratio, 0.001f)
        assertEquals(34, downloading.peersConnected)
        assertEquals("d.peers_complete is the connected-seeder count", 6, downloading.seedersConnected)
        assertEquals(28, downloading.leechersConnected)
        assertEquals("urlencoded ruTorrent label decoded", listOf("Linux ISOs"), downloading.labels)

        val seeding = torrents[1]
        assertEquals(TorrentStatus.SEEDING, seeding.status)
        assertEquals(2.0f, seeding.ratio, 0.001f)
        assertNull(seeding.etaSeconds)

        assertEquals(TorrentStatus.PAUSED, torrents[2].status)

        val errored = torrents[3]
        assertEquals(TorrentStatus.ERROR, errored.status)
        assertTrue(errored.error!!.contains("Unregistered torrent"))
    }

    @Test
    fun `list files maps chunks and priorities`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("f-multicall.xml")))

        val files = adapter.listFiles("8C212779B4ABDE7C6BC608063A0D008B7E40CE32")

        assertEquals(3, files.size)
        assertEquals(FilePriority.HIGH, files[0].priority)
        assertEquals(1f, files[0].progress, 0.001f)
        assertEquals(FilePriority.NORMAL, files[1].priority)
        assertEquals(0.25f, files[1].progress, 0.001f)
        assertEquals(FilePriority.OFF, files[2].priority)
    }

    @Test
    fun `list trackers maps enabled state and scrape counts`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("t-multicall.xml")))

        val trackers = adapter.listTrackers("8C212779B4ABDE7C6BC608063A0D008B7E40CE32")

        assertEquals(3, trackers.size)
        val working = trackers[0]
        assertEquals("https://tracker.example.org/announce", working.url)
        assertEquals(TrackerStatus.WORKING, working.status)
        assertEquals(6, working.seeders)
        assertEquals(40, working.leechers)

        val errored = trackers[1]
        assertEquals(TrackerStatus.ERROR, errored.status)
        assertNull("scrape count -1 must normalize to null", errored.seeders)

        assertEquals("disabled tracker reads as idle", TrackerStatus.IDLE, trackers[2].status)
    }

    @Test
    fun `add by url sends load-start with escaped url`() = runTest {
        server.enqueue(xmlResponse("<i8>0</i8>"))

        adapter.addByUrl("magnet:?xt=urn:btih:abc&dn=name")

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("<methodName>load.start</methodName>"))
        assertTrue("ampersand must be XML-escaped", body.contains("magnet:?xt=urn:btih:abc&amp;dn=name"))
    }

    @Test
    fun `remove with data sets the rutorrent erase-data marker first`() = runTest {
        server.enqueue(xmlResponse("<i8>0</i8>"))
        server.enqueue(xmlResponse("<i8>0</i8>"))

        adapter.remove("ABCDEF", deleteData = true)

        val first = server.takeRequest().body.readUtf8()
        assertTrue(first.contains("<methodName>d.custom5.set</methodName>"))
        val second = server.takeRequest().body.readUtf8()
        assertTrue(second.contains("<methodName>d.erase</methodName>"))
        assertTrue(second.contains("ABCDEF"))
    }

    @Test
    fun `xml-rpc fault maps to unexpected response`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """<?xml version="1.0"?><methodResponse><fault><value><struct>
                   <member><name>faultCode</name><value><i4>-506</i4></value></member>
                   <member><name>faultString</name><value><string>Method 'nope' not defined</string></value></member>
                   </struct></value></fault></methodResponse>"""
            )
        )

        try {
            adapter.testConnection()
            fail("Expected DaemonException.UnexpectedResponse")
        } catch (expected: DaemonException.UnexpectedResponse) {
            assertTrue(expected.message!!.contains("Method 'nope' not defined"))
        }
    }

    @Test
    fun `401 maps to authentication error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        try {
            adapter.listTorrents()
            fail("Expected DaemonException.Authentication")
        } catch (expected: DaemonException.Authentication) {
        }
    }
}
