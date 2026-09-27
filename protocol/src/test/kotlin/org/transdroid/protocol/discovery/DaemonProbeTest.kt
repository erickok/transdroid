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
package org.transdroid.protocol.discovery

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.transdroid.protocol.DaemonType

class DaemonProbeTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun dispatch(handler: (RecordedRequest) -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = handler(request)
        }
    }

    @Test
    fun `recognizes transmission by its 409 session challenge`() = runTest {
        dispatch { request ->
            if (request.path == "/transmission/rpc") {
                MockResponse().setResponseCode(409).setHeader("X-Transmission-Session-Id", "abc")
            } else {
                MockResponse().setResponseCode(404)
            }
        }

        val found = DaemonProbe.probe(client, server.hostName, server.port)

        assertEquals(DaemonType.TRANSMISSION, found?.type)
        assertEquals(server.port, found?.port)
    }

    @Test
    fun `recognizes qbittorrent by its version endpoint`() = runTest {
        dispatch { request ->
            when (request.path) {
                "/api/v2/app/webapiVersion" -> MockResponse().setBody("2.11.2")
                else -> MockResponse().setResponseCode(404)
            }
        }

        val found = DaemonProbe.probe(client, server.hostName, server.port)

        assertEquals(DaemonType.QBITTORRENT, found?.type)
    }

    @Test
    fun `recognizes deluge by its json-rpc envelope`() = runTest {
        dispatch { request ->
            when (request.path) {
                "/json" -> MockResponse().setBody("""{"result": false, "error": null, "id": 1}""")
                else -> MockResponse().setResponseCode(404)
            }
        }

        val found = DaemonProbe.probe(client, server.hostName, server.port)

        assertEquals(DaemonType.DELUGE, found?.type)
    }

    @Test
    fun `qbittorrent requiring login is confirmed through its login endpoint`() = runTest {
        dispatch { request ->
            when (request.path) {
                "/api/v2/app/webapiVersion" -> MockResponse().setResponseCode(403)
                "/api/v2/auth/login" -> MockResponse().setBody("Fails.")
                else -> MockResponse().setResponseCode(404)
            }
        }

        assertEquals(DaemonType.QBITTORRENT, DaemonProbe.probe(client, server.hostName, server.port)?.type)
    }

    @Test
    fun `a proxy answering 403 everywhere is not qbittorrent`() = runTest {
        dispatch { MockResponse().setResponseCode(403).setBody("Forbidden") }

        assertNull(DaemonProbe.probe(client, server.hostName, server.port))
    }

    @Test
    fun `a generic 401 is not transmission but its own realm is`() = runTest {
        dispatch { MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Basic realm=\"nginx\"") }
        assertNull(DaemonProbe.probe(client, server.hostName, server.port))

        dispatch { request ->
            if (request.path == "/transmission/rpc") {
                MockResponse().setResponseCode(401).setHeader("WWW-Authenticate", "Basic realm=\"Transmission\"")
            } else {
                MockResponse().setResponseCode(404)
            }
        }
        assertEquals(DaemonType.TRANSMISSION, DaemonProbe.probe(client, server.hostName, server.port)?.type)
    }

    @Test
    fun `a plain web server is not misidentified`() = runTest {
        dispatch { MockResponse().setBody("<html>welcome to my NAS</html>") }

        assertNull(DaemonProbe.probe(client, server.hostName, server.port))
    }
}
