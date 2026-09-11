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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
        val arguments = request("session-get")
        val version = arguments["version"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val rpcVersion = arguments["rpc-version"]?.jsonPrimitive?.contentOrNull
        return "Transmission $version" + (rpcVersion?.let { " (RPC v$it)" } ?: "")
    }

    override suspend fun listTorrents(): List<Torrent> {
        val arguments = request("torrent-get") {
            put("fields", buildJsonArray { TORRENT_FIELDS.forEach { add(it) } })
        }
        val torrents = arguments["torrents"]?.jsonArray
            ?: throw DaemonException.UnexpectedResponse("Missing 'torrents' in torrent-get response")
        return torrents.map { parseTorrent(it.jsonObject) }
    }

    override suspend fun addByUrl(url: String, startPaused: Boolean) {
        request("torrent-add") {
            put("filename", url)
            if (startPaused) put("paused", true)
        }
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean) {
        request("torrent-add") {
            put("metainfo", Base64.getEncoder().encodeToString(contents))
            if (startPaused) put("paused", true)
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

    override suspend fun listFiles(torrentId: String): List<TorrentFile> {
        val arguments = request("torrent-get") {
            putIds(torrentId)
            put("fields", buildJsonArray { listOf("files", "fileStats").forEach { add(it) } })
        }
        val torrent = arguments["torrents"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw DaemonException.UnexpectedResponse("Torrent $torrentId not found")
        val files = torrent["files"]?.jsonArray ?: return emptyList()
        val stats = torrent["fileStats"]?.jsonArray
        return files.mapIndexed { index, element ->
            val file = element.jsonObject
            val stat = stats?.getOrNull(index)?.jsonObject
            val wanted = stat?.get("wanted")?.jsonPrimitive?.contentOrNull != "false"
            val priority = when {
                !wanted -> FilePriority.OFF
                else -> when (stat?.get("priority")?.jsonPrimitive?.contentOrNull) {
                    "-1" -> FilePriority.LOW
                    "1" -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                }
            }
            TorrentFile(
                index = index,
                path = file["name"]?.jsonPrimitive?.contentOrNull ?: "",
                sizeBytes = file["length"]?.jsonPrimitive?.long ?: 0L,
                downloadedBytes = file["bytesCompleted"]?.jsonPrimitive?.long ?: 0L,
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

    override suspend fun listTrackers(torrentId: String): List<Tracker> {
        val arguments = request("torrent-get") {
            putIds(torrentId)
            put("fields", buildJsonArray { add("trackerStats") })
        }
        val torrent = arguments["torrents"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw DaemonException.UnexpectedResponse("Torrent $torrentId not found")
        val stats = torrent["trackerStats"]?.jsonArray ?: return emptyList()
        return stats.map { element ->
            val obj = element.jsonObject
            val announced = obj["hasAnnounced"]?.jsonPrimitive?.booleanOrNull ?: false
            val succeeded = obj["lastAnnounceSucceeded"]?.jsonPrimitive?.booleanOrNull ?: false
            val status = when {
                !announced -> TrackerStatus.IDLE
                succeeded -> TrackerStatus.WORKING
                else -> TrackerStatus.ERROR
            }
            Tracker(
                url = obj["announce"]?.jsonPrimitive?.contentOrNull
                    ?: obj["host"]?.jsonPrimitive?.contentOrNull ?: "",
                status = status,
                seeders = obj["seederCount"]?.jsonPrimitive?.int?.takeIf { it >= 0 },
                leechers = obj["leecherCount"]?.jsonPrimitive?.int?.takeIf { it >= 0 },
                message = obj["lastAnnounceResult"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { announced && !succeeded && it.isNotBlank() },
            )
        }
    }

    override suspend fun setLabel(torrentId: String, label: String) {
        request("torrent-set") {
            putIds(torrentId)
            put("labels", buildJsonArray { if (label.isNotBlank()) add(label) })
        }
    }

    override val supportsAltSpeedLimits: Boolean get() = true

    override suspend fun isAltSpeedLimitsEnabled(): Boolean {
        val arguments = request("session-get")
        return arguments["alt-speed-enabled"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    override suspend fun setAltSpeedLimitsEnabled(enabled: Boolean) {
        request("session-set") { put("alt-speed-enabled", enabled) }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putIds(torrentId: String) {
        val id = torrentId.toIntOrNull()
            ?: throw DaemonException.UnexpectedResponse("Not a Transmission torrent id: $torrentId")
        put("ids", buildJsonArray { add(id) })
    }

    /** Sends one RPC request, retrying once after a 409 session-id challenge. */
    private suspend fun request(
        method: String,
        argumentsBuilder: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null,
    ): JsonObject {
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
                it.code == 401 || it.code == 403 ->
                    throw DaemonException.Authentication("Transmission rejected the username/password")
                !it.isSuccessful ->
                    throw DaemonException.UnexpectedResponse("Transmission returned HTTP ${it.code}")
            }
            val body = it.body ?: throw DaemonException.UnexpectedResponse("Empty Transmission response")
            val root = decodeResponse(body.byteStream())
            val result = root["result"]?.jsonPrimitive?.contentOrNull
            if (result != "success") {
                throw DaemonException.UnexpectedResponse("Transmission error: ${result ?: "no result"}")
            }
            return root["arguments"]?.jsonObject ?: JsonObject(emptyMap())
        }
    }

    /**
     * Decodes the response directly off the stream (no intermediate String covering the
     * whole body — matters for a `torrent-get` reply listing thousands of torrents), while
     * still peeking a few bytes for the HTML-login-portal diagnostic below.
     *
     * [executeOnIo] only wraps the network round-trip up to receiving headers; the body
     * still streams off the same connection, and reading it (whichever way) can still block
     * on the socket, so that must stay on IO too or it risks a NetworkOnMainThreadException
     * on whatever dispatcher called the adapter.
     */
    private suspend fun decodeResponse(stream: InputStream): JsonObject = withContext(Dispatchers.IO) {
        val pushback = PushbackInputStream(stream, PEEK_BYTES)
        val peek = ByteArray(PEEK_BYTES)
        val peeked = pushback.read(peek)
        if (peeked > 0) pushback.unread(peek, 0, peeked)
        try {
            json.decodeFromStream<JsonObject>(pushback)
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
            builder.header("Authorization", Credentials.basic(username, config.password.orEmpty()))
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

    private fun parseTorrent(obj: JsonObject): Torrent {
        val error = obj["errorString"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val statusCode = obj["status"]?.jsonPrimitive?.int ?: -1
        val status = when {
            error != null -> TorrentStatus.ERROR
            else -> when (statusCode) {
                0 -> TorrentStatus.PAUSED
                1, 2 -> TorrentStatus.CHECKING
                3, 5 -> TorrentStatus.QUEUED
                4 -> TorrentStatus.DOWNLOADING
                6 -> TorrentStatus.SEEDING
                else -> TorrentStatus.UNKNOWN
            }
        }
        val eta = obj["eta"]?.jsonPrimitive?.long?.takeIf { it >= 0 }
        return Torrent(
            id = obj["id"]?.jsonPrimitive?.contentOrNull
                ?: throw DaemonException.UnexpectedResponse("Torrent without id"),
            name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
            status = status,
            progress = obj["percentDone"]?.jsonPrimitive?.float?.coerceIn(0f, 1f) ?: 0f,
            downloadRate = obj["rateDownload"]?.jsonPrimitive?.long ?: 0L,
            uploadRate = obj["rateUpload"]?.jsonPrimitive?.long ?: 0L,
            etaSeconds = eta,
            sizeBytes = obj["totalSize"]?.jsonPrimitive?.long ?: 0L,
            downloadedBytes = obj["downloadedEver"]?.jsonPrimitive?.long ?: 0L,
            uploadedBytes = obj["uploadedEver"]?.jsonPrimitive?.long ?: 0L,
            ratio = (obj["uploadRatio"]?.jsonPrimitive?.float ?: 0f).coerceAtLeast(0f),
            peersConnected = obj["peersConnected"]?.jsonPrimitive?.int ?: 0,
            // Transmission has no direct seeders/leechers split; a peer sending us data has
            // pieces we lack (seed-like role for us), one we're sending to is missing pieces
            // (leech-like role) - the closest approximation its RPC exposes.
            seedersConnected = obj["peersSendingToUs"]?.jsonPrimitive?.int ?: 0,
            leechersConnected = obj["peersGettingFromUs"]?.jsonPrimitive?.int ?: 0,
            addedTimestamp = obj["addedDate"]?.jsonPrimitive?.long?.takeIf { it > 0 },
            downloadDir = obj["downloadDir"]?.jsonPrimitive?.contentOrNull,
            error = error,
            labels = obj["labels"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }
                ?: emptyList(),
            metadataProgress = obj["metadataPercentComplete"]?.jsonPrimitive?.floatOrNull
                ?.takeIf { it < 1f }
                ?.coerceAtLeast(0f),
        )
    }

    private companion object {
        const val SESSION_ID_HEADER = "X-Transmission-Session-Id"

        /** Bytes peeked (without consuming) to sniff an HTML error page before decoding JSON. */
        const val PEEK_BYTES = 256

        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val TORRENT_FIELDS = listOf(
            "id", "name", "status", "percentDone", "rateDownload", "rateUpload", "eta",
            "totalSize", "downloadedEver", "uploadedEver", "uploadRatio", "peersConnected",
            "peersSendingToUs", "peersGettingFromUs",
            "addedDate", "downloadDir", "errorString", "labels", "metadataPercentComplete",
        )
    }
}
