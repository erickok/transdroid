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
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Peer
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
        val version = query("system.client_version", String.serializer()).ifBlank { "unknown" }
        return "rTorrent $version"
    }

    override suspend fun listTorrents(): List<Torrent> =
        multicall("d.multicall2", TorrentRow.serializer(), "", "main").map { it.toTorrent() }

    override suspend fun addByUrl(url: String, startPaused: Boolean, downloadLocation: String?) {
        // load.normal loads without starting; load.start loads and starts. Both accept extra
        // trailing command strings that run against the item right after it's loaded but before
        // it starts - d.directory.set here is the only way to steer where rTorrent places it,
        // since there is no dedicated directory argument on load.* itself.
        call(if (startPaused) "load.normal" else "load.start", "", url, *directoryCommand(downloadLocation))
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean, downloadLocation: String?) {
        // Stock rTorrent rejects XML-RPC requests over ~512 KiB, which a base64-encoded
        // .torrent for a large multi-file torrent easily exceeds; raise the limit first like
        // ruTorrent does. The limit is server-wide, so only ever raise it: never shrink a
        // larger one the user configured. Best-effort: locked-down hosts may refuse the
        // command, and small files work regardless.
        try {
            val needed = maxOf(2L * 1024 * 1024, contents.size * 2L + 1280L)
            val current = query("network.xmlrpc.size_limit", Long.serializer())
            if (current < needed) call("network.xmlrpc.size_limit.set", "", needed)
        } catch (e: DaemonException.UnexpectedResponse) {
            // Proceed; the load below fails with a clear fault if the file really is too big
        }
        call(if (startPaused) "load.raw" else "load.raw_start", "", contents, *directoryCommand(downloadLocation))
    }

    private fun directoryCommand(downloadLocation: String?): Array<String> =
        if (downloadLocation.isNullOrBlank()) emptyArray() else arrayOf("d.directory.set=$downloadLocation")

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
            // Also delete the tied .torrent file (e.g. in a watch directory), which would
            // otherwise re-add the torrent on the next rescan. Transdroid 2 did this too
            // (#655); best-effort, since the removal itself must not fail over it.
            try {
                call("d.delete_tied", torrentId)
            } catch (e: DaemonException.UnexpectedResponse) {
                // Not tied to a file, or an rTorrent build without the command
            }
        }
        call("d.erase", torrentId)
    }

    /** Started torrents are paused during the rehash and resumed automatically once it finishes. */
    override suspend fun checkData(torrentId: String) {
        call("d.check_hash", torrentId)
    }

    /**
     * Unlike the other three daemons, rTorrent has no RPC call that moves data on disk - this
     * only repoints where rTorrent looks for the torrent's files (see DaemonAdapter's doc on
     * this method). d.directory_base.set only takes effect once the item is closed, so this
     * stops it (harmless if already stopped), closes, repoints, then reopens - deliberately left
     * stopped afterward rather than guessing whether it's safe to resume.
     */
    override suspend fun setDownloadLocation(torrentId: String, location: String) {
        call("d.stop", torrentId)
        call("d.close", torrentId)
        call("d.directory_base.set", torrentId, location)
        call("d.open", torrentId)
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> =
        multicall("f.multicall", FileRow.serializer(), torrentId, "").mapIndexed { index, row ->
            TorrentFile(
                index = index,
                path = row.path,
                sizeBytes = row.sizeBytes,
                downloadedBytes = if (row.sizeChunks <= 0) 0L else row.sizeBytes * row.completedChunks / row.sizeChunks,
                priority = when (row.priority) {
                    0L -> FilePriority.OFF
                    2L -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                },
            )
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

    /** Mirrors the read side: ruTorrent stores its label encodeURIComponent-encoded in custom1. */
    override suspend fun setLabel(torrentId: String, label: String) {
        call("d.custom1.set", torrentId, encodeURIComponent(label))
    }

    /** Java's URLEncoder is form-encoding (space -> "+"); this matches JS's encodeURIComponent instead. */
    private fun encodeURIComponent(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    override suspend fun listTrackers(torrentId: String): List<Tracker> =
        multicall("t.multicall", TrackerRow.serializer(), torrentId, "").map { row ->
            val status = when {
                row.isEnabled == 0L -> TrackerStatus.IDLE
                row.successCounter > 0L -> TrackerStatus.WORKING
                row.failedCounter > 0L -> TrackerStatus.ERROR
                else -> TrackerStatus.IDLE
            }
            Tracker(
                url = row.url,
                status = status,
                seeders = row.scrapeComplete.toInt().takeIf { it >= 0 },
                leechers = row.scrapeIncomplete.toInt().takeIf { it >= 0 },
            )
        }

    override suspend fun listPeers(torrentId: String): List<Peer> =
        multicall("p.multicall", PeerRow.serializer(), torrentId, "").map { row ->
            Peer(
                ip = row.address,
                clientName = row.clientVersion.takeIf { it.isNotBlank() && it != "Unknown" },
                // p.completed_percent is 0-100, not 0..1
                progress = (row.completedPercent / 100f).coerceIn(0f, 1f),
                downloadRate = row.downRate,
                uploadRate = row.upRate,
                port = row.port.toInt().takeIf { it > 0 },
                // rTorrent's XML-RPC has no peer geolocation field at all
                countryCode = null,
                countryName = null,
                encrypted = row.isEncrypted != 0L,
            )
        }

    /**
     * Runs a multicall with [row]'s commands as the per-item fields, after the [leadingParams]
     * (the target and view/filter). The commands come from the row class itself, in the order
     * the values come back, so request and decoding can't drift apart.
     */
    private suspend fun <R> multicall(method: String, row: KSerializer<R>, vararg leadingParams: Any?): List<R> =
        query(method, ListSerializer(row), *leadingParams, *multicallCommands(row.descriptor).toTypedArray())

    /** Sends a call whose reply value isn't needed; faults still become exceptions. */
    private suspend fun call(method: String, vararg params: Any?) {
        query(method, XmlRpc.Ignored, *params)
    }

    /** Sends a call and decodes its reply value as [result], straight off the response stream. */
    private suspend fun <T> query(method: String, result: DeserializationStrategy<T>, vararg params: Any?): T {
        val builder = Request.Builder()
            .url(rpcUrl)
            .post(XmlRpc.buildRequest(method, params.toList()).toRequestBody("text/xml".toMediaType()))
        val username = config.username
        if (!username.isNullOrEmpty()) {
            builder.header("Authorization", Credentials.basic(username, config.password.orEmpty(), Charsets.UTF_8))
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
            return withContext(Dispatchers.IO) { XmlRpc.decodeResponse(body.byteStream(), result) }
        }
    }

    /** One d.multicall2 row; properties in request order, named after their commands. */
    @Serializable
    private data class TorrentRow(
        @SerialName("d.hash=") val hash: String = "",
        @SerialName("d.name=") val name: String = "",
        @SerialName("d.state=") val state: Long = 0,
        @SerialName("d.complete=") val complete: Long = 0,
        @SerialName("d.is_active=") val isActive: Long = 0,
        @SerialName("d.hashing=") val hashing: Long = 0,
        @SerialName("d.down.rate=") val downRate: Long = 0,
        @SerialName("d.up.rate=") val upRate: Long = 0,
        @SerialName("d.size_bytes=") val sizeBytes: Long = 0,
        @SerialName("d.completed_bytes=") val completedBytes: Long = 0,
        @SerialName("d.up.total=") val upTotal: Long = 0,
        /** In per-mille. */
        @SerialName("d.ratio=") val ratio: Long = 0,
        @SerialName("d.peers_connected=") val peersConnected: Long = 0,
        @SerialName("d.timestamp.started=") val startedAt: Long = 0,
        @SerialName("d.directory=") val directory: String = "",
        @SerialName("d.message=") val message: String = "",
        /** ruTorrent's label, encodeURIComponent-encoded. */
        @SerialName("d.custom1=") val custom1: String = "",
        /** The number of connected peers that are seeding (complete). */
        @SerialName("d.peers_complete=") val peersComplete: Long = 0,
    ) {
        fun toTorrent(): Torrent {
            // d.message carries tracker notices ("Tried all trackers", "Unregistered torrent")
            // that persist while the torrent keeps working via other trackers, DHT or PEX: shown
            // as the torrent's error text, but never a status of its own
            val status = when {
                hashing > 0L -> TorrentStatus.CHECKING
                state == 0L || isActive != 1L -> TorrentStatus.PAUSED
                complete == 1L -> TorrentStatus.SEEDING
                else -> TorrentStatus.DOWNLOADING
            }
            val eta = if (status == TorrentStatus.DOWNLOADING && downRate > 0) {
                (sizeBytes - completedBytes) / downRate
            } else {
                null
            }
            return Torrent(
                id = hash,
                name = name,
                status = status,
                progress = if (sizeBytes <= 0) 0f else (completedBytes.toFloat() / sizeBytes).coerceIn(0f, 1f),
                downloadRate = downRate,
                uploadRate = upRate,
                etaSeconds = eta,
                sizeBytes = sizeBytes,
                downloadedBytes = completedBytes,
                uploadedBytes = upTotal,
                ratio = ratio / 1000f,
                peersConnected = peersConnected.toInt(),
                seedersConnected = peersComplete.toInt(),
                leechersConnected = (peersConnected - peersComplete).toInt().coerceAtLeast(0),
                addedTimestamp = startedAt.takeIf { it > 0 },
                downloadDir = directory.takeIf { it.isNotBlank() },
                error = message.takeIf { it.isNotBlank() },
                // ruTorrent stores its label encodeURIComponent-encoded in custom1; that
                // encoding leaves "+" literal, so protect it from URLDecoder's plus-to-space
                labels = custom1.takeIf { it.isNotBlank() }?.let { raw ->
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
    }

    @Serializable
    private data class FileRow(
        @SerialName("f.path=") val path: String = "",
        @SerialName("f.size_bytes=") val sizeBytes: Long = 0,
        @SerialName("f.completed_chunks=") val completedChunks: Long = 0,
        @SerialName("f.size_chunks=") val sizeChunks: Long = 0,
        /** 0 = off, 1 = normal, 2 = high. */
        @SerialName("f.priority=") val priority: Long = 1,
    )

    @Serializable
    private data class TrackerRow(
        @SerialName("t.url=") val url: String = "",
        @SerialName("t.is_enabled=") val isEnabled: Long = 0,
        @SerialName("t.success_counter=") val successCounter: Long = 0,
        @SerialName("t.failed_counter=") val failedCounter: Long = 0,
        @SerialName("t.scrape_complete=") val scrapeComplete: Long = 0,
        @SerialName("t.scrape_incomplete=") val scrapeIncomplete: Long = 0,
    )

    @Serializable
    private data class PeerRow(
        @SerialName("p.address=") val address: String = "",
        @SerialName("p.port=") val port: Long = 0,
        @SerialName("p.client_version=") val clientVersion: String = "",
        /** 0-100. */
        @SerialName("p.completed_percent=") val completedPercent: Long = 0,
        @SerialName("p.down_rate=") val downRate: Long = 0,
        @SerialName("p.up_rate=") val upRate: Long = 0,
        @SerialName("p.is_encrypted=") val isEncrypted: Long = 0,
    )
}
