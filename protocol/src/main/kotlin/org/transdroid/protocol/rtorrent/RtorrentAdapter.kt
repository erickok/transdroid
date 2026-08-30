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

import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker
import org.transdroid.protocol.TrackerStatus
import org.transdroid.protocol.internal.executeOnIo

/**
 * Adapter for rTorrent 0.9.7+ via XML-RPC over HTTP, the common seedbox setup where a web
 * server (or ruTorrent) exposes the SCGI socket at an endpoint like /RPC2. Uses the modern
 * dotted command names (d.multicall2, load.start); the pre-0.9.7 underscore API is not
 * supported.
 */
class RtorrentAdapter(
    override val config: DaemonConfig,
    private val httpClient: OkHttpClient,
) : DaemonAdapter {

    private val rpcUrl = config.baseUrl +
        (config.path?.takeIf { it.isNotBlank() } ?: "/RPC2").let { if (it.startsWith("/")) it else "/$it" }

    override suspend fun testConnection(): String {
        val version = call("system.client_version") as? String ?: "unknown"
        return "rTorrent $version"
    }

    override suspend fun listTorrents(): List<Torrent> {
        val rows = call(
            "d.multicall2",
            "", "main",
            "d.hash=", "d.name=", "d.state=", "d.complete=", "d.is_active=", "d.hashing=",
            "d.down.rate=", "d.up.rate=", "d.size_bytes=", "d.completed_bytes=", "d.up.total=",
            "d.ratio=", "d.peers_connected=", "d.timestamp.started=", "d.directory=", "d.message=",
            "d.custom1=", "d.peers_complete=",
        ) as? List<*> ?: throw DaemonException.UnexpectedResponse("Unexpected d.multicall2 reply")
        return rows.map { row ->
            val fields = row as? List<*> ?: throw DaemonException.UnexpectedResponse("Bad multicall row")
            parseTorrent(fields)
        }
    }

    private fun parseTorrent(fields: List<*>): Torrent {
        fun str(index: Int) = fields.getOrNull(index)?.toString().orEmpty()
        fun num(index: Int) = (fields.getOrNull(index) as? Long) ?: 0L

        val state = num(2)
        val complete = num(3) == 1L
        val isActive = num(4) == 1L
        val hashing = num(5) > 0L
        val downRate = num(6)
        val sizeBytes = num(8)
        val completedBytes = num(9)
        val message = str(15).takeIf { it.isNotBlank() }

        val status = when {
            message != null -> TorrentStatus.ERROR
            hashing -> TorrentStatus.CHECKING
            state == 0L || !isActive -> TorrentStatus.PAUSED
            complete -> TorrentStatus.SEEDING
            else -> TorrentStatus.DOWNLOADING
        }
        val eta = if (status == TorrentStatus.DOWNLOADING && downRate > 0) {
            (sizeBytes - completedBytes) / downRate
        } else {
            null
        }
        return Torrent(
            id = str(0),
            name = str(1),
            status = status,
            progress = if (sizeBytes <= 0) 0f else (completedBytes.toFloat() / sizeBytes).coerceIn(0f, 1f),
            downloadRate = downRate,
            uploadRate = num(7),
            etaSeconds = eta,
            sizeBytes = sizeBytes,
            downloadedBytes = completedBytes,
            uploadedBytes = num(10),
            // d.ratio is reported in per-mille
            ratio = num(11) / 1000f,
            peersConnected = num(12).toInt(),
            // d.peers_complete is the number of connected peers that are seeding (complete)
            seedersConnected = num(17).toInt(),
            leechersConnected = (num(12) - num(17)).toInt().coerceAtLeast(0),
            addedTimestamp = num(13).takeIf { it > 0 },
            downloadDir = str(14).takeIf { it.isNotBlank() },
            error = message,
            // ruTorrent stores its label encodeURIComponent-encoded in custom1; that
            // encoding leaves "+" literal, so protect it from URLDecoder's plus-to-space
            labels = str(16).takeIf { it.isNotBlank() }?.let { raw ->
                listOf(
                    try {
                        URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8")
                    } catch (e: IllegalArgumentException) {
                        raw
                    }
                )
            } ?: emptyList(),
        )
    }

    override suspend fun addByUrl(url: String, startPaused: Boolean) {
        // load.normal loads without starting; load.start loads and starts
        call(if (startPaused) "load.normal" else "load.start", "", url)
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean) {
        call(if (startPaused) "load.raw" else "load.raw_start", "", contents)
    }

    override suspend fun start(torrentId: String) {
        call("d.start", torrentId)
    }

    override suspend fun pause(torrentId: String) {
        call("d.stop", torrentId)
    }

    override suspend fun remove(torrentId: String, deleteData: Boolean) {
        if (deleteData) {
            // ruTorrent convention: an event hook on custom5 erases the data on removal.
            // Harmless when no such hook is configured; rTorrent itself never deletes data.
            call("d.custom5.set", torrentId, "1")
        }
        call("d.erase", torrentId)
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> {
        val rows = call(
            "f.multicall",
            torrentId, "",
            "f.path=", "f.size_bytes=", "f.completed_chunks=", "f.size_chunks=", "f.priority=",
        ) as? List<*> ?: throw DaemonException.UnexpectedResponse("Unexpected f.multicall reply")
        return rows.mapIndexed { index, row ->
            val fields = row as? List<*> ?: throw DaemonException.UnexpectedResponse("Bad multicall row")
            val size = (fields.getOrNull(1) as? Long) ?: 0L
            val completedChunks = (fields.getOrNull(2) as? Long) ?: 0L
            val sizeChunks = (fields.getOrNull(3) as? Long) ?: 0L
            TorrentFile(
                index = index,
                path = fields.getOrNull(0)?.toString().orEmpty(),
                sizeBytes = size,
                downloadedBytes = if (sizeChunks <= 0) 0L else size * completedChunks / sizeChunks,
                priority = when ((fields.getOrNull(4) as? Long) ?: 1L) {
                    0L -> FilePriority.OFF
                    2L -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                },
            )
        }
    }

    override suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority) {
        // rTorrent priorities: 0 = off, 1 = normal (LOW folds into it), 2 = high
        val value = when (priority) {
            FilePriority.OFF -> 0L
            FilePriority.HIGH -> 2L
            else -> 1L
        }
        call("f.priority.set", "$torrentId:f$fileIndex", value)
        call("d.update_priorities", torrentId)
    }

    override suspend fun listTrackers(torrentId: String): List<Tracker> {
        val rows = call(
            "t.multicall",
            torrentId, "",
            "t.url=", "t.is_enabled=", "t.success_counter=", "t.failed_counter=",
            "t.scrape_complete=", "t.scrape_incomplete=",
        ) as? List<*> ?: throw DaemonException.UnexpectedResponse("Unexpected t.multicall reply")
        return rows.map { row ->
            val fields = row as? List<*> ?: throw DaemonException.UnexpectedResponse("Bad multicall row")
            fun str(index: Int) = fields.getOrNull(index)?.toString().orEmpty()
            fun num(index: Int) = (fields.getOrNull(index) as? Long) ?: 0L
            val enabled = num(1) != 0L
            val status = when {
                !enabled -> TrackerStatus.IDLE
                num(2) > 0L -> TrackerStatus.WORKING
                num(3) > 0L -> TrackerStatus.ERROR
                else -> TrackerStatus.IDLE
            }
            Tracker(
                url = str(0),
                status = status,
                seeders = num(4).toInt().takeIf { it >= 0 },
                leechers = num(5).toInt().takeIf { it >= 0 },
            )
        }
    }

    private suspend fun call(method: String, vararg params: Any?): Any? {
        val builder = Request.Builder()
            .url(rpcUrl)
            .post(XmlRpc.buildRequest(method, params.toList()).toRequestBody("text/xml".toMediaType()))
        val username = config.username
        if (!username.isNullOrEmpty()) {
            builder.header("Authorization", Credentials.basic(username, config.password.orEmpty()))
        }
        httpClient.executeOnIo(builder.build()).use { response ->
            when {
                response.code == 401 || response.code == 403 ->
                    throw DaemonException.Authentication("rTorrent's web server rejected the username/password")
                !response.isSuccessful ->
                    throw DaemonException.UnexpectedResponse("rTorrent returned HTTP ${response.code}")
            }
            val body = response.body ?: throw DaemonException.UnexpectedResponse("Empty rTorrent response")
            // executeOnIo only wraps the round-trip up to receiving headers; the body still
            // streams off the same connection, so parsing it can still block on the socket
            // and must stay on IO too, or it risks a NetworkOnMainThreadException on whatever
            // dispatcher called the adapter.
            return withContext(Dispatchers.IO) { XmlRpc.parseResponse(body.byteStream()) }
        }
    }
}
