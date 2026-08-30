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
package org.transdroid.protocol.deluge

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

class DelugeAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: DelugeAdapter

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        adapter = DelugeAdapter(
            DaemonConfig(
                type = DaemonType.DELUGE,
                host = server.hostName,
                port = server.port,
                password = "deluge",
            ),
            OkHttpClient(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/deluge/$name")) { "Missing fixture $name" }
            .bufferedReader().readText()

    private fun loginOk() = MockResponse()
        .setBody("""{"result": true, "error": null, "id": 1}""")
        .setHeader("Set-Cookie", "_session_id=abc123; Path=/json")

    @Test
    fun `logs in with password and passes session cookie`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-status.json")))

        adapter.listTorrents()

        val login = server.takeRequest()
        assertEquals("/json", login.path)
        val loginBody = login.body.readUtf8()
        assertTrue(loginBody.contains("\"method\":\"auth.login\""))
        assertTrue(loginBody.contains("\"deluge\""))
        val status = server.takeRequest()
        assertEquals("_session_id=abc123", status.getHeader("Cookie"))
        assertTrue(status.body.readUtf8().contains("core.get_torrents_status"))
    }

    @Test
    fun `rejected login maps to authentication error`() = runTest {
        server.enqueue(MockResponse().setBody("""{"result": false, "error": null, "id": 1}"""))

        try {
            adapter.listTorrents()
            fail("Expected DaemonException.Authentication")
        } catch (expected: DaemonException.Authentication) {
        }
    }

    @Test
    fun `list torrents parses and normalizes fixture`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-status.json")))

        val torrents = adapter.listTorrents().sortedByDescending { it.addedTimestamp }

        assertEquals(4, torrents.size)
        val downloading = torrents[0]
        assertEquals("8c212779b4abde7c6bc608063a0d008b7e40ce32", downloading.id)
        assertEquals(TorrentStatus.DOWNLOADING, downloading.status)
        assertEquals("percent scale normalized to 0..1", 0.4266f, downloading.progress, 0.001f)
        assertEquals(1220L, downloading.etaSeconds)
        assertEquals(34, downloading.peersConnected)
        assertEquals(22, downloading.seedersConnected)
        assertEquals("num_peers already excludes seeds", 12, downloading.leechersConnected)
        assertEquals("Label plugin value maps to a label", listOf("linux-isos"), downloading.labels)

        val seeding = torrents[1]
        assertEquals(TorrentStatus.SEEDING, seeding.status)
        assertNull("eta 0 must normalize to null", seeding.etaSeconds)

        val paused = torrents[2]
        assertEquals(TorrentStatus.PAUSED, paused.status)
        assertEquals("negative ratio must clamp to 0", 0f, paused.ratio, 0.001f)

        val errored = torrents[3]
        assertEquals(TorrentStatus.ERROR, errored.status)
        assertEquals("Files missing", errored.error)
    }

    @Test
    fun `expired session re-authenticates once`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(
            MockResponse().setBody("""{"result": null, "error": {"message": "Not authenticated", "code": 1}, "id": 2}""")
        )
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody(fixture("torrents-status.json")))

        val torrents = adapter.listTorrents()

        assertEquals(4, torrents.size)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `list trackers marks only the active tracker with live status`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(
            MockResponse().setBody(
                """{"result": {
                    "trackers": [
                        {"url": "https://tracker.example.org/announce", "tier": 0},
                        {"url": "udp://backup.tracker.net:80/announce", "tier": 1}
                    ],
                    "tracker_status": "Announce OK",
                    "tracker_host": "tracker.example.org"
                }, "error": null, "id": 2}"""
            )
        )

        val trackers = adapter.listTrackers("abcdef")

        assertEquals(2, trackers.size)
        val active = trackers[0]
        assertEquals(TrackerStatus.WORKING, active.status)
        assertEquals("Announce OK", active.message)

        val inactive = trackers[1]
        assertEquals("Deluge only reports live status for the active tracker", TrackerStatus.IDLE, inactive.status)
        assertNull(inactive.message)
    }

    @Test
    fun `list trackers reads tracker errors as error status`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(
            MockResponse().setBody(
                """{"result": {
                    "trackers": [{"url": "https://tracker.example.org/announce", "tier": 0}],
                    "tracker_status": "Error: Unregistered torrent",
                    "tracker_host": "tracker.example.org"
                }, "error": null, "id": 2}"""
            )
        )

        val trackers = adapter.listTrackers("abcdef")

        assertEquals(TrackerStatus.ERROR, trackers[0].status)
    }

    @Test
    fun `pause and resume use list parameters`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody("""{"result": null, "error": null, "id": 2}"""))
        server.enqueue(MockResponse().setBody("""{"result": null, "error": null, "id": 3}"""))

        adapter.pause("abcdef")
        adapter.start("abcdef")

        server.takeRequest() // login
        val pause = server.takeRequest().body.readUtf8()
        assertTrue(pause.contains("\"method\":\"core.pause_torrent\""))
        assertTrue(pause.contains("[[\"abcdef\"]]"))
        val resume = server.takeRequest().body.readUtf8()
        assertTrue(resume.contains("\"method\":\"core.resume_torrent\""))
    }

    @Test
    fun `remove sends id and delete flag`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(MockResponse().setBody("""{"result": true, "error": null, "id": 2}"""))

        adapter.remove("abcdef", deleteData = true)

        server.takeRequest() // login
        val remove = server.takeRequest().body.readUtf8()
        assertTrue(remove.contains("\"method\":\"core.remove_torrent\""))
        assertTrue(remove.contains("[\"abcdef\",true]"))
    }

    @Test
    fun `daemon error maps to unexpected response`() = runTest {
        server.enqueue(loginOk())
        server.enqueue(
            MockResponse().setBody("""{"result": null, "error": {"message": "Unknown method", "code": 2}, "id": 2}""")
        )

        try {
            adapter.listTorrents()
            fail("Expected DaemonException.UnexpectedResponse")
        } catch (expected: DaemonException.UnexpectedResponse) {
            assertTrue(expected.message!!.contains("Unknown method"))
        }
    }
}
