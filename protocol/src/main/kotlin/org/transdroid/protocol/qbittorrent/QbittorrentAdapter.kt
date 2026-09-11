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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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
import org.transdroid.protocol.internal.joinPath

/**
 * Adapter for the qBittorrent Web API v2 (REST over HTTP with SID cookie auth), as documented
 * in https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-(qBittorrent-4.1). Also covers
 * qBittorrent 5.x, which renamed the pause/resume endpoints to stop/start; both are tried.
 */
class QbittorrentAdapter(
    override val config: DaemonConfig,
    private val httpClient: OkHttpClient,
) : DaemonAdapter {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var sessionCookie: String? = null

    // Backoff after a failed login, so a poll loop doesn't hammer qBittorrent's login endpoint
    // every cycle - which not only wastes requests but keeps re-triggering (and likely renewing)
    // qBittorrent's own "too many failed logins" IP ban, making it worse rather than letting it
    // expire.
    @Volatile
    private var authRetryAfterMillis: Long = 0L
    @Volatile
    private var lastAuthFailureMessage: String? = null

    override suspend fun testConnection(): String {
        val version = get("api/v2/app/version").use { it.readBodyOrThrow() }
        return "qBittorrent $version"
    }

    override suspend fun listTorrents(): List<Torrent> {
        val infos = get("api/v2/torrents/info").use { it.decodeJsonListOrThrow<TorrentInfo>("torrent list") }
        return infos.map { it.toTorrent() }
    }

    override suspend fun addByUrl(url: String, startPaused: Boolean) {
        val form = FormBody.Builder().add("urls", url).apply {
            if (startPaused) {
                // qBittorrent 4.x reads "paused", 5.x reads "stopped"; unknown fields are ignored
                add("paused", "true")
                add("stopped", "true")
            }
        }.build()
        post("api/v2/torrents/add", form).use { it.checkAddSucceeded() }
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "torrents", fileName,
                contents.toRequestBody("application/x-bittorrent".toMediaType()),
            )
            .apply {
                if (startPaused) {
                    addFormDataPart("paused", "true")
                    addFormDataPart("stopped", "true")
                }
            }
            .build()
        post("api/v2/torrents/add", body).use { it.checkAddSucceeded() }
    }

    /** torrents/add reports failure as HTTP 200 with the body "Fails." */
    private suspend fun Response.checkAddSucceeded() {
        if (readBodyOrThrow().trim() == "Fails.") {
            throw DaemonException.UnexpectedResponse("qBittorrent could not add that torrent")
        }
    }

    override suspend fun start(torrentId: String) {
        hashesActionWithFallback("start", "resume", torrentId)
    }

    override suspend fun pause(torrentId: String) {
        hashesActionWithFallback("stop", "pause", torrentId)
    }

    override suspend fun remove(torrentId: String, deleteData: Boolean) {
        val form = FormBody.Builder()
            .add("hashes", torrentId)
            .add("deleteFiles", deleteData.toString())
            .build()
        post("api/v2/torrents/delete", form).use { it.readBodyOrThrow() }
    }

    override suspend fun checkData(torrentId: String) {
        val form = FormBody.Builder().add("hashes", torrentId).build()
        post("api/v2/torrents/recheck", form).use { it.readBodyOrThrow() }
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> {
        val files = get("api/v2/torrents/files?hash=$torrentId").use { it.decodeJsonListOrThrow<FileInfo>("file list") }
        return files.mapIndexed { listIndex, file ->
            TorrentFile(
                // Newer qBittorrent reports the real file index; fall back to list position
                index = file.index ?: listIndex,
                path = file.name,
                sizeBytes = file.size,
                downloadedBytes = (file.progress * file.size).toLong(),
                priority = when (file.priority) {
                    0 -> FilePriority.OFF
                    6, 7 -> FilePriority.HIGH
                    else -> FilePriority.NORMAL
                },
            )
        }
    }

    override suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority) {
        // qBittorrent has no LOW level: 0 = skip, 1 = normal, 6 = high
        val value = when (priority) {
            FilePriority.OFF -> 0
            FilePriority.HIGH -> 6
            else -> 1
        }
        val form = FormBody.Builder()
            .add("hash", torrentId)
            .add("id", fileIndex.toString())
            .add("priority", value.toString())
            .build()
        post("api/v2/torrents/filePrio", form).use { it.readBodyOrThrow() }
    }

    override suspend fun listTrackers(torrentId: String): List<Tracker> {
        val trackers = get("api/v2/torrents/trackers?hash=$torrentId")
            .use { it.decodeJsonListOrThrow<TrackerInfo>("tracker list") }
        // qBittorrent lists DHT/PeX/LSD as pseudo-trackers with urls like "** [DHT] **"
        return trackers.filterNot { it.url.startsWith("**") }.map { it.toTracker() }
    }

    /** qBittorrent calls this a "category"; a category must exist before it can be assigned. */
    override suspend fun setLabel(torrentId: String, label: String) {
        if (label.isNotBlank()) {
            try {
                val createForm = FormBody.Builder().add("category", label).build()
                post("api/v2/torrents/createCategory", createForm).use { it.readBodyOrThrow() }
            } catch (e: DaemonException.UnexpectedResponse) {
                // Most likely already exists (qBittorrent answers 409); setCategory below still works
            }
        }
        val form = FormBody.Builder().add("hashes", torrentId).add("category", label).build()
        post("api/v2/torrents/setCategory", form).use { it.readBodyOrThrow() }
    }

    override val supportsAltSpeedLimits: Boolean get() = true

    override suspend fun isAltSpeedLimitsEnabled(): Boolean =
        get("api/v2/transfer/speedLimitsMode").use { it.readBodyOrThrow() }.trim() == "1"

    /** There's no "set" endpoint, only a toggle - so only flip it if it's not already at [enabled]. */
    override suspend fun setAltSpeedLimitsEnabled(enabled: Boolean) {
        if (isAltSpeedLimitsEnabled() != enabled) {
            post("api/v2/transfer/toggleSpeedLimitsMode", FormBody.Builder().build()).use { it.readBodyOrThrow() }
        }
    }

    /** qBittorrent 5 renamed pause/resume to stop/start; try new name first, fall back on 404. */
    private suspend fun hashesActionWithFallback(newEndpoint: String, legacyEndpoint: String, hash: String) {
        val form = { FormBody.Builder().add("hashes", hash).build() }
        val response = post("api/v2/torrents/$newEndpoint", form(), allowNotFound = true)
        if (response.code == 404) {
            response.close()
            post("api/v2/torrents/$legacyEndpoint", form()).use { it.readBodyOrThrow() }
        } else {
            response.use { it.readBodyOrThrow() }
        }
    }

    private suspend fun ensureAuthenticated() {
        if (sessionCookie != null || config.username.isNullOrEmpty()) return
        loginWithBackoff()
    }

    /**
     * Calls [login], but after a failure refuses to try again until [AUTH_RETRY_COOLDOWN_MILLIS]
     * has passed - re-throwing the last failure instead. Without this, a poll loop calls this on
     * every cycle (as often as every few seconds) whenever [sessionCookie] is null, which for a
     * "too many failed logins" ban means continuously hammering the login endpoint and likely
     * renewing the ban instead of letting it expire.
     */
    private suspend fun loginWithBackoff() {
        val now = System.currentTimeMillis()
        if (now < authRetryAfterMillis) {
            throw DaemonException.Authentication(lastAuthFailureMessage ?: "qBittorrent authentication is temporarily unavailable")
        }
        try {
            login()
            authRetryAfterMillis = 0L
            lastAuthFailureMessage = null
        } catch (e: DaemonException.Authentication) {
            authRetryAfterMillis = now + AUTH_RETRY_COOLDOWN_MILLIS
            lastAuthFailureMessage = e.message
            throw e
        }
    }

    private suspend fun login() {
        val form = FormBody.Builder()
            .add("username", config.username.orEmpty())
            .add("password", config.password.orEmpty())
            .build()
        val request = Request.Builder()
            .url(config.baseUrl + joinPath(config.path, "api/v2/auth/login"))
            .post(form)
            .build()
        httpClient.executeOnIo(request).use { response ->
            if (response.code == 403) {
                throw DaemonException.Authentication("qBittorrent blocked this address (too many failed logins)")
            }
            val body = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
            if (!response.isSuccessful || body.trim() != "Ok.") {
                throw DaemonException.Authentication("qBittorrent rejected the username/password")
            }
            val cookie = response.headers("Set-Cookie").firstOrNull { it.startsWith("SID=") }
                ?: throw DaemonException.UnexpectedResponse("qBittorrent login did not return a session cookie")
            sessionCookie = cookie.substringBefore(';')
        }
    }

    private suspend fun get(endpoint: String): Response =
        sendAuthenticated { Request.Builder().url(config.baseUrl + joinPath(config.path, endpoint)).get() }

    private suspend fun post(endpoint: String, body: okhttp3.RequestBody, allowNotFound: Boolean = false): Response =
        sendAuthenticated(allowNotFound) {
            Request.Builder().url(config.baseUrl + joinPath(config.path, endpoint)).post(body)
        }

    /** Sends a request with the SID cookie, re-authenticating once when the session expired. */
    private suspend fun sendAuthenticated(
        allowNotFound: Boolean = false,
        build: () -> Request.Builder,
    ): Response {
        ensureAuthenticated()
        var response = send(build())
        if (response.code == 403 && !config.username.isNullOrEmpty()) {
            response.close()
            loginWithBackoff()
            response = send(build())
        }
        when {
            response.code == 401 || response.code == 403 -> {
                response.close()
                throw DaemonException.Authentication("qBittorrent requires a valid username/password")
            }
            response.code == 404 && allowNotFound -> return response
            !response.isSuccessful -> {
                val code = response.code
                response.close()
                throw DaemonException.UnexpectedResponse("qBittorrent returned HTTP $code")
            }
        }
        return response
    }

    private suspend fun send(builder: Request.Builder): Response {
        sessionCookie?.let { builder.header("Cookie", it) }
        return httpClient.executeOnIo(builder.build())
    }

    /**
     * [executeOnIo] only wraps the network round-trip up to receiving headers; the body
     * still streams off the same connection, and reading it can still block on the socket,
     * so that must stay on IO too or it risks a NetworkOnMainThreadException on whatever
     * dispatcher called the adapter.
     */
    private suspend fun Response.readBodyOrThrow(): String =
        withContext(Dispatchers.IO) { body?.string().orEmpty() }

    /**
     * Decodes a JSON array response directly off the body stream, without first buffering
     * the whole reply into a String — matters for a server with thousands of torrents.
     */
    private suspend inline fun <reified T> Response.decodeJsonListOrThrow(what: String): List<T> =
        withContext(Dispatchers.IO) {
            val stream = body?.byteStream() ?: return@withContext emptyList()
            try {
                json.decodeFromStream<List<T>>(stream)
            } catch (e: Exception) {
                throw DaemonException.UnexpectedResponse("Cannot parse qBittorrent $what", e)
            }
        }

    @Serializable
    private data class TorrentInfo(
        val hash: String,
        val name: String = "",
        val state: String = "",
        val progress: Float = 0f,
        val dlspeed: Long = 0,
        val upspeed: Long = 0,
        val eta: Long = INFINITE_ETA,
        val size: Long = 0,
        val completed: Long = 0,
        val uploaded: Long = 0,
        val ratio: Float = 0f,
        val num_seeds: Int = 0,
        val num_leechs: Int = 0,
        val added_on: Long = 0,
        val save_path: String? = null,
        val category: String = "",
    ) {
        fun toTorrent() = Torrent(
            id = hash,
            name = name,
            status = when (state) {
                "downloading", "metaDL", "forcedDL", "stalledDL", "forcedMetaDL" -> TorrentStatus.DOWNLOADING
                "uploading", "stalledUP", "forcedUP" -> TorrentStatus.SEEDING
                "pausedDL", "pausedUP", "stoppedDL", "stoppedUP" -> TorrentStatus.PAUSED
                "checkingDL", "checkingUP", "checkingResumeData", "allocating" -> TorrentStatus.CHECKING
                "queuedDL", "queuedUP" -> TorrentStatus.QUEUED
                "error", "missingFiles" -> TorrentStatus.ERROR
                else -> TorrentStatus.UNKNOWN
            },
            progress = progress.coerceIn(0f, 1f),
            downloadRate = dlspeed,
            uploadRate = upspeed,
            etaSeconds = eta.takeIf { it in 0 until INFINITE_ETA },
            sizeBytes = size,
            downloadedBytes = completed,
            uploadedBytes = uploaded,
            ratio = ratio.coerceAtLeast(0f),
            peersConnected = num_seeds + num_leechs,
            seedersConnected = num_seeds,
            leechersConnected = num_leechs,
            addedTimestamp = added_on.takeIf { it > 0 },
            downloadDir = save_path,
            error = if (state == "error" || state == "missingFiles") "Torrent in error state ($state)" else null,
            labels = listOf(category).filter { it.isNotBlank() },
            // qBittorrent flags the metadata phase by state but reports no percentage
            metadataProgress = if (state == "metaDL" || state == "forcedMetaDL") 0f else null,
        )
    }

    @Serializable
    private data class TrackerInfo(
        val url: String,
        // 0=disabled, 1=not contacted, 2=working, 3=updating, 4=not working
        val status: Int = 0,
        val num_seeds: Int = -1,
        val num_leeches: Int = -1,
        val msg: String = "",
    ) {
        fun toTracker() = Tracker(
            url = url,
            status = when (status) {
                2 -> TrackerStatus.WORKING
                4 -> TrackerStatus.ERROR
                else -> TrackerStatus.IDLE
            },
            seeders = num_seeds.takeIf { it >= 0 },
            leechers = num_leeches.takeIf { it >= 0 },
            message = msg.takeIf { it.isNotBlank() },
        )
    }

    @Serializable
    private data class FileInfo(
        val name: String,
        val size: Long = 0,
        val progress: Float = 0f,
        val priority: Int = 1,
        val index: Int? = null,
    )

    private companion object {
        /** qBittorrent reports 8640000 seconds as "no ETA". */
        const val INFINITE_ETA = 8640000L

        /** Minimum time between login attempts after a failure; see [loginWithBackoff]. */
        const val AUTH_RETRY_COOLDOWN_MILLIS = 60_000L
    }
}
