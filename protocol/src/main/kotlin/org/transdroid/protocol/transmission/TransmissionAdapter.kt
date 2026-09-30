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
package org.transdroid.protocol.transmission

import java.io.InputStream
import java.io.PushbackInputStream
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.internal.executeOnIo
import org.transdroid.protocol.internal.trackerHost
import org.transdroid.protocol.Peer
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker
import org.transdroid.protocol.TrackerStatus

/**
 * Adapter for the Transmission RPC protocol (JSON over HTTP POST), as documented in
 * https://github.com/transmission/transmission/blob/main/docs/rpc-spec.md. Handles the
 * CSRF handshake where the daemon answers 409 with a fresh X-Transmission-Session-Id.
 */
class TransmissionAdapter(
    override val config: DaemonConfig,
    private val httpClient: OkHttpClient,
) : DaemonAdapter {

    private val json = Json { ignoreUnknownKeys = true }
    private val rpcUrl = config.baseUrl + (config.path?.takeIf { it.isNotBlank() } ?: "/transmission/rpc")

    @Volatile
    private var sessionId: String? = null

    override suspend fun testConnection(): String {
        val session = request("session-get", SessionInfo.serializer())
        val version = session?.version ?: "unknown"
        return "Transmission $version" + (session?.rpcVersion?.let { " (RPC v$it)" } ?: "")
    }

    override suspend fun listTorrents(): List<Torrent> =
        torrentGet(TorrentInfo.serializer(), TORRENT_FIELDS, torrentId = null).map { it.toTorrent() }

    override suspend fun addByUrl(url: String, startPaused: Boolean, downloadLocation: String?) {
        request("torrent-add") {
            put("filename", url)
            if (startPaused) put("paused", true)
            if (!downloadLocation.isNullOrBlank()) put("download-dir", downloadLocation)
        }
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean, downloadLocation: String?) {
        request("torrent-add") {
            put("metainfo", Base64.getEncoder().encodeToString(contents))
            if (startPaused) put("paused", true)
            if (!downloadLocation.isNullOrBlank()) put("download-dir", downloadLocation)
        }
    }

    override suspend fun start(torrentId: String) {
        request("torrent-start") { putIds(torrentId) }
    }

    override suspend fun pause(torrentId: String) {
        request("torrent-stop") { putIds(torrentId) }
    }

    override suspend fun remove(torrentId: String, deleteData: Boolean) {
        request("torrent-remove") {
            putIds(torrentId)
            put("delete-local-data", deleteData)
        }
    }

    override suspend fun checkData(torrentId: String) {
        request("torrent-verify") { putIds(torrentId) }
    }

    override suspend fun setDownloadLocation(torrentId: String, location: String) {
        // move:true physically relocates the existing files; false would just repoint
        // Transmission at location and expect the files to already be there.
        request("torrent-set-location") {
            putIds(torrentId)
            put("location", location)
            put("move", true)
        }
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> {
        val torrent = singleTorrent(torrentId, TorrentFiles.serializer(), listOf("files", "fileStats"))
        return torrent.files.mapIndexed { index, file ->
            val stat = torrent.fileStats.getOrNull(index)
            val priority = when {
                stat?.wanted == false -> FilePriority.OFF
                else -> when (stat?.priority) {
                    -1 -> FilePriority.LOW
                    1 -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                }
            }
            TorrentFile(
                index = index,
                path = file.name,
                sizeBytes = file.length,
                downloadedBytes = file.bytesCompleted,
                priority = priority,
            )
        }
    }

    override suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority) {
        request("torrent-set") {
            putIds(torrentId)
            val indexes = buildJsonArray { add(fileIndex) }
            if (priority == FilePriority.OFF) {
                put("files-unwanted", indexes)
            } else {
                put("files-wanted", indexes)
                val key = when (priority) {
                    FilePriority.LOW -> "priority-low"
                    FilePriority.HIGH -> "priority-high"
                    else -> "priority-normal"
                }
                put(key, indexes)
            }
        }
    }

    override suspend fun listTrackers(torrentId: String): List<Tracker> =
        singleTorrent(torrentId, TorrentTrackerStats.serializer(), listOf("trackerStats"))
            .trackerStats.map { stat ->
                val status = when {
                    !stat.hasAnnounced -> TrackerStatus.IDLE
                    stat.lastAnnounceSucceeded -> TrackerStatus.WORKING
                    else -> TrackerStatus.ERROR
                }
                Tracker(
                    url = stat.announce ?: stat.host ?: "",
                    status = status,
                    seeders = stat.seederCount?.takeIf { it >= 0 },
                    leechers = stat.leecherCount?.takeIf { it >= 0 },
                    message = stat.lastAnnounceResult
                        ?.takeIf { stat.hasAnnounced && !stat.lastAnnounceSucceeded && it.isNotBlank() },
                )
            }

    override suspend fun listPeers(torrentId: String): List<Peer> =
        singleTorrent(torrentId, TorrentPeers.serializer(), listOf("peers")).peers.map { peer ->
            Peer(
                ip = peer.address,
                clientName = peer.clientName?.takeIf { it.isNotBlank() },
                progress = peer.progress.coerceIn(0f, 1f),
                downloadRate = peer.rateToClient,
                uploadRate = peer.rateToPeer,
                port = peer.port,
                // Transmission's RPC has no peer geolocation field at all
                countryCode = null,
                countryName = null,
                encrypted = peer.isEncrypted,
            )
        }

    override suspend fun setLabel(torrentId: String, label: String) {
        request("torrent-set") {
            putIds(torrentId)
            put("labels", buildJsonArray { if (label.isNotBlank()) add(label) })
        }
    }

    override val supportsAltSpeedLimits: Boolean get() = true

    override suspend fun isAltSpeedLimitsEnabled(): Boolean =
        request("session-get", SessionInfo.serializer())?.altSpeedEnabled ?: false

    override suspend fun setAltSpeedLimitsEnabled(enabled: Boolean) {
        request("session-set") { put("alt-speed-enabled", enabled) }
    }

    /** torrent-get for all torrents, or just [torrentId]; each reply entry decoded as [fields]' shape. */
    private suspend fun <T> torrentGet(entry: KSerializer<T>, fields: List<String>, torrentId: String?): List<T> {
        val arguments = request("torrent-get", TorrentGetArguments.serializer(entry)) {
            if (torrentId != null) putIds(torrentId)
            put("fields", buildJsonArray { fields.forEach { add(it) } })
        }
        return arguments?.torrents
            ?: throw DaemonException.UnexpectedResponse("Missing 'torrents' in torrent-get response")
    }

    private suspend fun <T> singleTorrent(torrentId: String, entry: KSerializer<T>, fields: List<String>): T =
        torrentGet(entry, fields, torrentId).firstOrNull()
            ?: throw DaemonException.UnexpectedResponse("Torrent $torrentId not found")

    private fun JsonObjectBuilder.putIds(torrentId: String) {
        val id = torrentId.toIntOrNull()
            ?: throw DaemonException.UnexpectedResponse("Not a Transmission torrent id: $torrentId")
        put("ids", buildJsonArray { add(id) })
    }

    /** Sends one RPC request whose reply arguments aren't needed. */
    private suspend fun request(method: String, argumentsBuilder: (JsonObjectBuilder.() -> Unit)? = null) {
        request(method, NoArguments.serializer(), argumentsBuilder)
    }

    /**
     * Sends one RPC request, retrying once after a 409 session-id challenge, and decodes the
     * reply's arguments as [arguments]; null when the reply carries none.
     */
    private suspend fun <A> request(
        method: String,
        arguments: KSerializer<A>,
        argumentsBuilder: (JsonObjectBuilder.() -> Unit)? = null,
    ): A? {
        val payload = buildJsonObject {
            put("method", method)
            if (argumentsBuilder != null) putJsonObject("arguments", argumentsBuilder)
        }

        var response = send(payload)
        if (response.code == 409) {
            sessionId = response.header(SESSION_ID_HEADER)
            response.close()
            response = send(payload)
        }
        response.use {
            when {
                it.code == 401 ->
                    throw DaemonException.Authentication("Transmission rejected the username/password")
                // Transmission answers 403 when the client address is outside rpc-whitelist, or
                // once rpc-anti-brute-force has locked out logins; never for a single bad password
                it.code == 403 -> {
                    val page = it.body?.string().orEmpty()
                    throw DaemonException.Authentication(
                        if (page.contains("unsuccessful login attempts", ignoreCase = true)) {
                            "Transmission locked out logins after too many failed attempts - restart " +
                                "transmission-daemon to lift it"
                        } else {
                            "Transmission refuses this device's IP address - add it to rpc-whitelist " +
                                "(or disable rpc-whitelist-enabled) in the daemon's settings.json"
                        }
                    )
                }
                !it.isSuccessful ->
                    throw DaemonException.UnexpectedResponse("Transmission returned HTTP ${it.code}")
            }
            val body = it.body ?: throw DaemonException.UnexpectedResponse("Empty Transmission response")
            val reply = decodeResponse(body.byteStream(), RpcReply.serializer(arguments))
            if (reply.result != "success") {
                throw DaemonException.UnexpectedResponse("Transmission error: ${reply.result ?: "no result"}")
            }
            return reply.arguments
        }
    }

    /**
     * Decodes the response directly off the stream into [reply]'s typed shape - no
     * intermediate String or JSON tree covering the whole body, and fields the app doesn't
     * read are skipped rather than kept; matters for a `torrent-get` reply listing thousands
     * of torrents - while still peeking a few bytes for the HTML-login-portal diagnostic below.
     *
     * [executeOnIo] only wraps the network round-trip up to receiving headers; the body
     * still streams off the same connection, and reading it (whichever way) can still block
     * on the socket, so that must stay on IO too or it risks a NetworkOnMainThreadException
     * on whatever dispatcher called the adapter.
     */
    private suspend fun <R> decodeResponse(stream: InputStream, reply: KSerializer<R>): R = withContext(Dispatchers.IO) {
        val pushback = PushbackInputStream(stream, PEEK_BYTES)
        val peek = ByteArray(PEEK_BYTES)
        val peeked = pushback.read(peek)
        if (peeked > 0) pushback.unread(peek, 0, peeked)
        try {
            json.decodeFromStream(reply, pushback)
        } catch (e: Exception) {
            // A reverse proxy or access portal (e.g. Cloudflare Access) commonly answers
            // with an HTML login page; make that diagnosable instead of a generic error
            val looksLikeHtml = peeked > 0 && String(peek, 0, peeked, Charsets.UTF_8).trimStart().startsWith("<")
            val hint = if (looksLikeHtml) {
                "Transmission's address answered with a web page instead of RPC — " +
                    "a login portal (such as Cloudflare Access) may be in front of it"
            } else {
                "Not a Transmission RPC response"
            }
            throw DaemonException.UnexpectedResponse(hint, e)
        }
    }

    private suspend fun send(payload: JsonObject): okhttp3.Response {
        val builder = Request.Builder()
            .url(rpcUrl)
            .post(jsonRequestBody(payload))
        sessionId?.let { builder.header(SESSION_ID_HEADER, it) }
        val username = config.username
        if (!username.isNullOrEmpty()) {
            builder.header("Authorization", Credentials.basic(username, config.password.orEmpty(), Charsets.UTF_8))
        }
        return httpClient.executeOnIo(builder.build())
    }

    /**
     * Encodes straight into an Okio buffer (no intermediate Kotlin String) while still
     * declaring a real Content-Length: request payloads here are tiny regardless of torrent
     * count, and some servers (embedded/minimal HTTP stacks) don't handle a chunked request
     * body with no Content-Length at all.
     */
    private fun jsonRequestBody(value: JsonObject): RequestBody {
        val buffer = Buffer()
        json.encodeToStream(value, buffer.outputStream())
        return object : RequestBody() {
            override fun contentType() = JSON_MEDIA_TYPE
            override fun contentLength() = buffer.size
            override fun writeTo(sink: BufferedSink) {
                sink.write(buffer.clone(), buffer.size)
            }
        }
    }

    /** The reply envelope: a "success" [result] or an error text, plus method-specific arguments. */
    @Serializable
    private data class RpcReply<A>(val result: String? = null, val arguments: A? = null)

    /** For methods whose reply arguments aren't needed; unknown keys are skipped, not kept. */
    @Serializable
    private class NoArguments

    @Serializable
    private data class TorrentGetArguments<T>(val torrents: List<T>? = null)

    @Serializable
    private data class SessionInfo(
        val version: String? = null,
        @SerialName("rpc-version") val rpcVersion: Int? = null,
        @SerialName("alt-speed-enabled") val altSpeedEnabled: Boolean = false,
    )

    @Serializable
    private data class TorrentInfo(
        val id: Long? = null,
        val name: String = "",
        val status: Int = -1,
        val percentDone: Float = 0f,
        val rateDownload: Long = 0,
        val rateUpload: Long = 0,
        val eta: Long = -1,
        val sizeWhenDone: Long = 0,
        val haveValid: Long = 0,
        val haveUnchecked: Long = 0,
        val uploadedEver: Long = 0,
        val uploadRatio: Float = 0f,
        val peersConnected: Int = 0,
        val peersSendingToUs: Int = 0,
        val peersGettingFromUs: Int = 0,
        val addedDate: Long = 0,
        val downloadDir: String? = null,
        val error: Int = 0,
        val errorString: String? = null,
        val labels: List<String> = emptyList(),
        val trackers: List<TrackerAnnounce> = emptyList(),
        val metadataPercentComplete: Float? = null,
    ) {
        fun toTorrent(): Torrent {
            // Transmission fills errorString for routine tracker warnings too (error codes 1/2)
            // while the torrent keeps working; only code 3 is a real local error
            val torrentStatus = when {
                error == 3 -> TorrentStatus.ERROR
                else -> when (status) {
                    0 -> TorrentStatus.PAUSED
                    1, 2 -> TorrentStatus.CHECKING
                    3, 5 -> TorrentStatus.QUEUED
                    4 -> TorrentStatus.DOWNLOADING
                    6 -> TorrentStatus.SEEDING
                    else -> TorrentStatus.UNKNOWN
                }
            }
            return Torrent(
                id = id?.toString() ?: throw DaemonException.UnexpectedResponse("Torrent without id"),
                name = name,
                status = torrentStatus,
                progress = percentDone.coerceIn(0f, 1f),
                downloadRate = rateDownload,
                uploadRate = rateUpload,
                etaSeconds = eta.takeIf { it >= 0 },
                // sizeWhenDone and haveValid+haveUnchecked cover only the wanted files; totalSize
                // and downloadedEver would count deselected files and discarded corrupt data
                sizeBytes = sizeWhenDone,
                downloadedBytes = haveValid + haveUnchecked,
                uploadedBytes = uploadedEver,
                ratio = uploadRatio.coerceAtLeast(0f),
                peersConnected = peersConnected,
                // Transmission has no direct seeders/leechers split; a peer sending us data has
                // pieces we lack (seed-like role for us), one we're sending to is missing pieces
                // (leech-like role) - the closest approximation its RPC exposes.
                seedersConnected = peersSendingToUs,
                leechersConnected = peersGettingFromUs,
                addedTimestamp = addedDate.takeIf { it > 0 },
                downloadDir = downloadDir,
                error = errorString?.takeIf { it.isNotBlank() && error != 0 },
                labels = labels.filter { it.isNotBlank() },
                trackers = trackers.mapNotNull { it.announce?.let(::trackerHost) }.distinct(),
                metadataProgress = metadataPercentComplete?.takeIf { it < 1f }?.coerceAtLeast(0f),
            )
        }
    }

    @Serializable
    private data class TrackerAnnounce(val announce: String? = null)

    @Serializable
    private data class TorrentFiles(
        val files: List<FileInfo> = emptyList(),
        val fileStats: List<FileStat> = emptyList(),
    )

    @Serializable
    private data class FileInfo(val name: String = "", val length: Long = 0, val bytesCompleted: Long = 0)

    @Serializable
    private data class FileStat(val wanted: Boolean = true, val priority: Int = 0)

    @Serializable
    private data class TorrentTrackerStats(val trackerStats: List<TrackerStat> = emptyList())

    @Serializable
    private data class TrackerStat(
        val announce: String? = null,
        val host: String? = null,
        val hasAnnounced: Boolean = false,
        val lastAnnounceSucceeded: Boolean = false,
        val seederCount: Int? = null,
        val leecherCount: Int? = null,
        val lastAnnounceResult: String? = null,
    )

    @Serializable
    private data class TorrentPeers(val peers: List<PeerInfo> = emptyList())

    @Serializable
    private data class PeerInfo(
        val address: String = "",
        val clientName: String? = null,
        val progress: Float = 0f,
        val rateToClient: Long = 0,
        val rateToPeer: Long = 0,
        val port: Int? = null,
        val isEncrypted: Boolean? = null,
    )

    private companion object {
        const val SESSION_ID_HEADER = "X-Transmission-Session-Id"

        /** Bytes peeked (without consuming) to sniff an HTML error page before decoding JSON. */
        const val PEEK_BYTES = 256

        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val TORRENT_FIELDS = listOf(
            "id", "name", "status", "percentDone", "rateDownload", "rateUpload", "eta",
            "sizeWhenDone", "haveValid", "haveUnchecked", "uploadedEver", "uploadRatio", "peersConnected",
            "peersSendingToUs", "peersGettingFromUs",
            "addedDate", "downloadDir", "error", "errorString", "labels", "metadataPercentComplete",
            // Just the static tracker list (announce URLs) - not "trackerStats", which adds a lot
            // of live per-tracker announce/scrape history this app has no use for in the list view.
            "trackers",
        )
    }
}
