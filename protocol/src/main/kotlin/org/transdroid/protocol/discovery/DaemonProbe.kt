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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.transdroid.protocol.DaemonType

/** A torrent daemon found on the local network. */
data class DiscoveredDaemon(
    val type: DaemonType,
    val host: String,
    val port: Int,
)

/**
 * Identifies which torrent daemon (if any) answers on a host/port by probing each
 * client's characteristic HTTP endpoint. Only plain HTTP is probed — discovery targets
 * LAN default setups; HTTPS/self-signed servers are added manually.
 */
object DaemonProbe {

    /** Internal marker: the version endpoint wants a session, so confirm via the login endpoint. */
    private val NEEDS_LOGIN_CHECK = DiscoveredDaemon(DaemonType.QBITTORRENT, "", 0)

    /** Ports worth scanning: Transmission, qBittorrent and Deluge Web UI defaults. */
    val DEFAULT_PORTS: List<Int> = listOf(9091, 8080, 8112)

    suspend fun probe(httpClient: OkHttpClient, host: String, port: Int): DiscoveredDaemon? {
        probeTransmission(httpClient, host, port)?.let { return it }
        probeQbittorrent(httpClient, host, port)?.let { return it }
        probeDeluge(httpClient, host, port)?.let { return it }
        return null
    }

    private suspend fun probeTransmission(client: OkHttpClient, host: String, port: Int): DiscoveredDaemon? =
        tryRequest(client, Request.Builder().url("http://$host:$port/transmission/rpc").get().build()) { response ->
            val challenged = response.code == 409 && response.header("X-Transmission-Session-Id") != null
            // Any proxy or NAS can answer 401; Transmission names itself in the challenge realm
            val authWalled = response.code == 401 &&
                response.header("WWW-Authenticate")?.contains("Transmission", ignoreCase = true) == true
            if (challenged || authWalled) DiscoveredDaemon(DaemonType.TRANSMISSION, host, port) else null
        }

    private suspend fun probeQbittorrent(client: OkHttpClient, host: String, port: Int): DiscoveredDaemon? {
        val versionResult = tryRequest(
            client,
            Request.Builder().url("http://$host:$port/api/v2/app/webapiVersion").get().build(),
        ) { response ->
            val body = if (response.code == 200) response.body?.string().orEmpty() else ""
            val versionLike = response.code == 200 && body.length in 1..16 &&
                body.trim().firstOrNull()?.isDigit() == true
            when {
                versionLike -> DiscoveredDaemon(DaemonType.QBITTORRENT, host, port)
                response.code == 403 -> NEEDS_LOGIN_CHECK
                else -> null
            }
        }
        if (versionResult !== NEEDS_LOGIN_CHECK) return versionResult
        // A bare 403 is also what reverse proxies, NAS admin pages and IP allow-lists answer
        // on port 8080; qBittorrent's login endpoint, though, replies "Fails." to empty
        // credentials (or "Ok." with auth bypass), which nothing else does
        val form = FormBody.Builder().add("username", "").add("password", "").build()
        val login = Request.Builder().url("http://$host:$port/api/v2/auth/login").post(form).build()
        return tryRequest(client, login) { response ->
            val body = if (response.code == 200) response.body?.string().orEmpty().trim() else ""
            if (body == "Fails." || body == "Ok.") DiscoveredDaemon(DaemonType.QBITTORRENT, host, port) else null
        }
    }

    private suspend fun probeDeluge(client: OkHttpClient, host: String, port: Int): DiscoveredDaemon? {
        val body = """{"method":"web.connected","params":[],"id":1}"""
        val request = Request.Builder()
            .url("http://$host:$port/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        return tryRequest(client, request) { response ->
            val text = if (response.code == 200) response.body?.string().orEmpty() else ""
            if (text.contains("\"result\"") && text.contains("\"error\"")) {
                DiscoveredDaemon(DaemonType.DELUGE, host, port)
            } else {
                null
            }
        }
    }

    /**
     * One misbehaving LAN device must never abort the whole scan, so any failure —
     * transport or a response too strange to parse — just means "not this daemon".
     */
    private suspend fun tryRequest(
        client: OkHttpClient,
        request: Request,
        handle: (okhttp3.Response) -> DiscoveredDaemon?,
    ): DiscoveredDaemon? = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use(handle)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
