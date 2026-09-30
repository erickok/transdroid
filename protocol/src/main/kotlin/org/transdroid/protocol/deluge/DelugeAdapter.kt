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

import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
import org.transdroid.protocol.Peer
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker
import org.transdroid.protocol.TrackerStatus
import org.transdroid.protocol.internal.executeOnIo
import org.transdroid.protocol.internal.joinPath
import org.transdroid.protocol.internal.trackerHost

/**
 * Adapter for the Deluge Web UI JSON-RPC API (POST /json with an _session_id cookie),
 * compatible with Deluge 1.3 and 2.x. Authentication uses the Web UI password only; the
 * username field is ignored. Connects the Web UI to its daemon host itself (see
 * [ensureConnectedToDaemon]) rather than assuming that's already done.
 */
class DelugeAdapter(
    override val config: DaemonConfig,
    private val httpClient: OkHttpClient,
) : DaemonAdapter {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonUrl = config.baseUrl + joinPath(config.path, "json")

    @Volatile
    private var sessionCookie: String? = null

    @Volatile
    private var requestId: Long = 0

    override suspend fun testConnection(): String {
        ensureAuthenticated()
        // daemon.info exists on Deluge 1.3, daemon.get_version on 2.x; either may be absent
        val version = tryVersionCall("daemon.info") ?: tryVersionCall("daemon.get_version")
        return if (version == null) "Deluge" else "Deluge $version"
    }

    private suspend fun tryVersionCall(method: String): String? = try {
        call(method, String.serializer())
    } catch (e: DaemonException.UnexpectedResponse) {
        null
    }

    override suspend fun listTorrents(): List<Torrent> {
        ensureAuthenticated()
        val torrents = call(
            "core.get_torrents_status",
            MapSerializer(String.serializer(), TorrentStatusFields.serializer()),
            buildJsonObject {},
            buildJsonArray { TORRENT_KEYS.forEach { add(it) } },
        ) ?: throw DaemonException.UnexpectedResponse("Unexpected core.get_torrents_status reply")
        return torrents.map { (hash, fields) -> fields.toTorrent(hash) }
    }

    override suspend fun addByUrl(url: String, startPaused: Boolean, downloadLocation: String?) {
        ensureAuthenticated()
        val options = buildJsonObject {
            if (startPaused) put("add_paused", true)
            if (!downloadLocation.isNullOrBlank()) put("download_location", downloadLocation)
        }
        if (url.startsWith("magnet:")) {
            callIgnoringResult("core.add_torrent_magnet", url, options)
        } else {
            callIgnoringResult("core.add_torrent_url", url, options)
        }
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean, downloadLocation: String?) {
        ensureAuthenticated()
        val options = buildJsonObject {
            if (startPaused) put("add_paused", true)
            if (!downloadLocation.isNullOrBlank()) put("download_location", downloadLocation)
        }
        callIgnoringResult("core.add_torrent_file", fileName, Base64.getEncoder().encodeToString(contents), options)
    }

    override suspend fun start(torrentId: String) {
        ensureAuthenticated()
        callIgnoringResult("core.resume_torrent", buildJsonArray { add(torrentId) })
    }

    override suspend fun pause(torrentId: String) {
        ensureAuthenticated()
        callIgnoringResult("core.pause_torrent", buildJsonArray { add(torrentId) })
    }

    override suspend fun remove(torrentId: String, deleteData: Boolean) {
        ensureAuthenticated()
        callIgnoringResult("core.remove_torrent", torrentId, deleteData)
    }

    override suspend fun checkData(torrentId: String) {
        ensureAuthenticated()
        callIgnoringResult("core.force_recheck", buildJsonArray { add(torrentId) })
    }

    override suspend fun setDownloadLocation(torrentId: String, location: String) {
        ensureAuthenticated()
        callIgnoringResult("core.move_storage", buildJsonArray { add(torrentId) }, location)
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> {
        ensureAuthenticated()
        val status = torrentStatus(torrentId, TorrentFilesFields.serializer(), "files", "file_progress", "file_priorities")
        return status.files.mapIndexed { listIndex, file ->
            val size = file.size.toLong()
            val index = file.index?.toInt() ?: listIndex
            val fileProgress = status.file_progress.getOrNull(listIndex) ?: 0.0
            TorrentFile(
                index = index,
                path = file.path,
                sizeBytes = size,
                downloadedBytes = (size * fileProgress).toLong(),
                priority = when (status.file_priorities.getOrNull(index)?.toInt() ?: 4) {
                    0 -> FilePriority.OFF
                    1 -> FilePriority.LOW
                    7 -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                },
            )
        }
    }

    override suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority) {
        ensureAuthenticated()
        // Deluge wants the complete priorities array; read-modify-write it
        val current = torrentStatus(torrentId, FilePrioritiesField.serializer(), "file_priorities")
            .file_priorities?.map { it.toInt() }
            ?: throw DaemonException.UnexpectedResponse("Deluge did not report file priorities")
        if (fileIndex !in current.indices) {
            throw DaemonException.UnexpectedResponse("File index $fileIndex out of range")
        }
        val value = when (priority) {
            FilePriority.OFF -> 0
            FilePriority.LOW -> 1
            FilePriority.HIGH -> 7
            else -> 4
        }
        val updated = current.toMutableList().also { it[fileIndex] = value }
        callIgnoringResult(
            "core.set_torrent_options",
            buildJsonArray { add(torrentId) },
            buildJsonObject { put("file_priorities", buildJsonArray { updated.forEach { add(it) } }) },
        )
    }

    override suspend fun listTrackers(torrentId: String): List<Tracker> {
        ensureAuthenticated()
        val fields = torrentStatus(torrentId, TorrentTrackersFields.serializer(), "trackers", "tracker_status", "tracker_host")
        // Deluge's core only reports live announce status/errors for the currently-active
        // tracker (via tracker_status/tracker_host); the rest just sit in the tracker list
        val activeHost = fields.tracker_host?.takeIf { it.isNotBlank() }
        val activeStatusText = fields.tracker_status
        return fields.trackers.map { tracker ->
            val url = tracker.url.orEmpty()
            val isActive = activeHost != null && url.contains(activeHost)
            val status = when {
                !isActive || activeStatusText == null -> TrackerStatus.IDLE
                activeStatusText.startsWith("Error", ignoreCase = true) -> TrackerStatus.ERROR
                else -> TrackerStatus.WORKING
            }
            Tracker(url = url, status = status, message = if (isActive) activeStatusText else null)
        }
    }

    override suspend fun listPeers(torrentId: String): List<Peer> {
        ensureAuthenticated()
        return torrentStatus(torrentId, TorrentPeersField.serializer(), "peers").peers.map { peer ->
            // Deluge reports the endpoint as one "%s:%s" % (ip, port) string, never bracketing
            // an IPv6 address, so the port is always whatever follows the last colon - for
            // "2001:db8::1:6881" as much as for "203.0.113.5:51413".
            val ipAndPort = peer.ip
            Peer(
                ip = ipAndPort.substringBeforeLast(':'),
                clientName = peer.client?.takeIf { it.isNotBlank() && it != "unknown" },
                progress = peer.progress.toFloat().coerceIn(0f, 1f),
                downloadRate = peer.down_speed.toLong(),
                uploadRate = peer.up_speed.toLong(),
                port = ipAndPort.substringAfterLast(':').toIntOrNull()?.takeIf { it > 0 },
                // Only populated if the server has a GeoIP database configured - often blank
                // even when present, since modern Deluge no longer bundles one. No separate
                // country-name field exists.
                countryCode = peer.country?.takeIf { it.isNotBlank() },
                countryName = null,
                // Deluge's peer records carry no encryption state
                encrypted = null,
            )
        }
    }

    /**
     * Requires the Label plugin; label.set_torrent requires the label to already exist in its
     * registry, so a new one is created first (ignoring failure - most likely it already exists).
     * An empty label clears it, mirroring how the read side treats a blank "label" as no label.
     */
    override suspend fun setLabel(torrentId: String, label: String) {
        ensureAuthenticated()
        if (label.isNotBlank()) {
            try {
                callIgnoringResult("label.add", label)
            } catch (e: DaemonException.UnexpectedResponse) {
                // Most likely already exists; label.set_torrent below still works
            }
        }
        callIgnoringResult("label.set_torrent", torrentId, label)
    }

    private suspend fun ensureAuthenticated() {
        if (sessionCookie == null) login()
    }

    private suspend fun login() {
        val response = send("auth.login", listOf(JsonPrimitive(config.password.orEmpty())))
        response.use {
            val cookie = it.headers("Set-Cookie").firstOrNull { header -> header.startsWith("_session_id=") }
            val reply = parseBody(it, Reply.serializer(Boolean.serializer()))
            val loggedIn = reply.result == true
            if (!loggedIn || cookie == null) {
                throw DaemonException.Authentication("Deluge rejected the Web UI password")
            }
            sessionCookie = cookie.substringBefore(';')
        }
        ensureConnectedToDaemon()
    }

    /**
     * The Web UI is a thin client to a separate daemon process; every `core.*` method - the
     * whole torrent list, add/remove, everything - only becomes callable once the Web UI
     * session has attached to a daemon host via web.connect. Without this, core.* calls fail
     * with "Unknown method" even though auth.login already succeeded, since the Web UI itself
     * only implements the auth and web namespaces until it's connected. See the handshake at
     * https://deluge.readthedocs.io/en/latest/devguide/how-to/curl-jsonrpc.html.
     */
    private suspend fun ensureConnectedToDaemon() {
        val connected = send("web.connected", emptyList())
            .use { parseBody(it, Reply.serializer(Boolean.serializer())) }.result ?: false
        if (connected) return

        // A tiny reply whose entries are mixed-type [id, host, port, status-or-user] tuples
        // (or bare ids on some versions), so it's decoded as plain JSON elements
        val hosts = send("web.get_hosts", emptyList())
            .use { parseBody(it, Reply.serializer(ListSerializer(JsonElement.serializer()))) }.result
        val firstHost = hosts?.firstOrNull()
        val hostId = when {
            firstHost is JsonArray -> firstHost.firstOrNull()?.jsonPrimitive?.contentOrNull
            firstHost != null -> firstHost.jsonPrimitive.contentOrNull
            else -> null
        } ?: throw DaemonException.UnexpectedResponse("Deluge Web UI has no daemon host configured")

        val connectError = send("web.connect", listOf(hostId)).use { parseBody(it, ErrorOnlyReply.serializer()) }.error
        if (connectError != null) {
            throw DaemonException.UnexpectedResponse(
                "Deluge could not connect the Web UI to its daemon: ${connectError.message ?: "unknown error"}"
            )
        }
    }

    /** core.get_torrent_status for one torrent's [keys], decoded as [fields]' shape. */
    private suspend fun <T> torrentStatus(torrentId: String, fields: KSerializer<T>, vararg keys: String): T =
        call("core.get_torrent_status", fields, torrentId, buildJsonArray { keys.forEach { add(it) } })
            ?: throw DaemonException.UnexpectedResponse("Unexpected core.get_torrent_status reply")

    /** Sends one JSON-RPC call and decodes its result as [result]; null when the reply has none. */
    private suspend fun <T> call(method: String, result: KSerializer<T>, vararg params: Any?): T? =
        exchange(method, params.toList(), Reply.serializer(result)).result

    /** Sends one JSON-RPC call whose result isn't needed; it is skipped while decoding, not kept. */
    private suspend fun callIgnoringResult(method: String, vararg params: Any?) {
        exchange(method, params.toList(), ErrorOnlyReply.serializer())
    }

    /** Sends one JSON-RPC call, re-authenticating once if the session expired. */
    private suspend fun <R : RpcReply> exchange(method: String, params: List<Any?>, reply: KSerializer<R>): R {
        var body = send(method, params).use { parseBody(it, reply) }
        if (isNotAuthenticated(body.error)) {
            login()
            body = send(method, params).use { parseBody(it, reply) }
        } else if (method.startsWith("core.") && isUnknownMethod(body.error)) {
            // The session outlived its daemon connection (routine after the daemon restarts):
            // the web UI only knows core.* while attached, so re-attach and retry once
            ensureConnectedToDaemon()
            body = send(method, params).use { parseBody(it, reply) }
        }
        val error = body.error
        if (error != null) {
            if (isNotAuthenticated(error)) {
                throw DaemonException.Authentication("Deluge rejected the Web UI password")
            }
            throw DaemonException.UnexpectedResponse("Deluge error: ${error.message ?: "unknown error"}")
        }
        return body
    }

    private fun isUnknownMethod(error: RpcError?): Boolean =
        error?.message?.contains("Unknown method", ignoreCase = true) == true

    private fun isNotAuthenticated(error: RpcError?): Boolean = error?.code == NOT_AUTHENTICATED_CODE

    private suspend fun send(method: String, params: List<Any?>): okhttp3.Response {
        val payload = buildJsonObject {
            put("method", method)
            put("params", buildJsonArray {
                params.forEach { param ->
                    when (param) {
                        null -> add(JsonNull)
                        is String -> add(param)
                        is Boolean -> add(param)
                        is Number -> add(param)
                        is JsonElement -> add(param)
                        else -> throw IllegalArgumentException("Unsupported param type: ${param::class}")
                    }
                }
            })
            put("id", ++requestId)
        }
        val builder = Request.Builder()
            .url(jsonUrl)
            .post(jsonRequestBody(payload))
        sessionCookie?.let { builder.header("Cookie", it) }
        val response = httpClient.executeOnIo(builder.build())
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw DaemonException.UnexpectedResponse("Deluge returned HTTP $code")
        }
        return response
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

    /**
     * Decodes the response directly off the body stream into [reply]'s typed shape, without
     * first buffering the whole reply into a String or JSON tree, and skipping fields the app
     * doesn't read - matters for a `core.get_torrents_status` call listing thousands of
     * torrents. [executeOnIo] only wraps the network round-trip up to receiving headers;
     * the body still streams off the same connection, and reading it can still block on the
     * socket, so that must stay on IO too or it risks a NetworkOnMainThreadException on
     * whatever dispatcher called the adapter.
     */
    private suspend fun <R> parseBody(response: okhttp3.Response, reply: KSerializer<R>): R {
        val body = response.body ?: throw DaemonException.UnexpectedResponse("Empty Deluge response")
        return withContext(Dispatchers.IO) {
            try {
                json.decodeFromStream(reply, body.byteStream())
            } catch (e: Exception) {
                throw DaemonException.UnexpectedResponse("Not a Deluge JSON response", e)
            }
        }
    }

    /** Every JSON-RPC reply carries an error (null on success) next to its result. */
    private interface RpcReply {
        val error: RpcError?
    }

    @Serializable
    private data class Reply<T>(val result: T? = null, override val error: RpcError? = null) : RpcReply

    /** For calls whose result isn't needed; the result is skipped as an unknown key, not kept. */
    @Serializable
    private data class ErrorOnlyReply(override val error: RpcError? = null) : RpcReply

    @Serializable
    private data class RpcError(val message: String? = null, val code: Int? = null)

    // Deluge freely mixes ints and floats between versions (2.x reports even eta as a float),
    // so every numeric field is read as a Double and converted afterwards

    @Serializable
    private data class TorrentStatusFields(
        val name: String = "",
        val state: String = "",
        val progress: Double = 0.0,
        val download_payload_rate: Double = 0.0,
        val upload_payload_rate: Double = 0.0,
        val eta: Double = 0.0,
        val total_wanted: Double = 0.0,
        val total_done: Double = 0.0,
        val total_uploaded: Double = 0.0,
        val ratio: Double = 0.0,
        val num_peers: Double = 0.0,
        val num_seeds: Double = 0.0,
        val time_added: Double = 0.0,
        val save_path: String? = null,
        val message: String? = null,
        /** Present only when Deluge's Label plugin is enabled. */
        val label: String? = null,
        val trackers: List<TrackerUrl> = emptyList(),
    ) {
        fun toTorrent(hash: String): Torrent {
            val status = when (state) {
                "Downloading" -> TorrentStatus.DOWNLOADING
                "Seeding" -> TorrentStatus.SEEDING
                "Paused" -> TorrentStatus.PAUSED
                "Checking", "Allocating", "Moving" -> TorrentStatus.CHECKING
                "Queued" -> TorrentStatus.QUEUED
                "Error" -> TorrentStatus.ERROR
                else -> TorrentStatus.UNKNOWN
            }
            return Torrent(
                id = hash,
                name = name,
                status = status,
                progress = (progress.toFloat() / 100f).coerceIn(0f, 1f),
                downloadRate = download_payload_rate.toLong(),
                uploadRate = upload_payload_rate.toLong(),
                etaSeconds = eta.toLong().takeIf { it > 0 },
                sizeBytes = total_wanted.toLong(),
                downloadedBytes = total_done.toLong(),
                uploadedBytes = total_uploaded.toLong(),
                ratio = ratio.toFloat().coerceAtLeast(0f),
                peersConnected = num_peers.toInt() + num_seeds.toInt(),
                seedersConnected = num_seeds.toInt(),
                // Deluge's own "num_peers" already excludes seeds (it's total connections minus num_seeds)
                leechersConnected = num_peers.toInt(),
                addedTimestamp = time_added.toLong().takeIf { it > 0 },
                downloadDir = save_path,
                error = if (status == TorrentStatus.ERROR) message ?: "Torrent in error state" else null,
                labels = label?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
                trackers = trackers.mapNotNull { it.url?.let(::trackerHost) }.distinct(),
            )
        }
    }

    @Serializable
    private data class TrackerUrl(val url: String? = null)

    @Serializable
    private data class TorrentFilesFields(
        val files: List<FileInfo> = emptyList(),
        val file_progress: List<Double> = emptyList(),
        val file_priorities: List<Double> = emptyList(),
    )

    @Serializable
    private data class FileInfo(val index: Double? = null, val path: String = "", val size: Double = 0.0)

    @Serializable
    private data class FilePrioritiesField(val file_priorities: List<Double>? = null)

    @Serializable
    private data class TorrentTrackersFields(
        val trackers: List<TrackerUrl> = emptyList(),
        val tracker_status: String? = null,
        val tracker_host: String? = null,
    )

    @Serializable
    private data class TorrentPeersField(val peers: List<PeerInfo> = emptyList())

    @Serializable
    private data class PeerInfo(
        val ip: String = "",
        val client: String? = null,
        val progress: Double = 0.0,
        val down_speed: Double = 0.0,
        val up_speed: Double = 0.0,
        val country: String? = null,
    )

    private companion object {
        const val NOT_AUTHENTICATED_CODE = 1

        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val TORRENT_KEYS = listOf(
            "name", "state", "progress", "download_payload_rate", "upload_payload_rate", "eta",
            "total_wanted", "total_done", "total_uploaded", "ratio", "num_peers", "num_seeds",
            "time_added", "save_path", "message", "label", "trackers",
        )
    }
}
