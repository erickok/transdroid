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
package org.transdroid.protocol

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.transdroid.protocol.deluge.DelugeAdapter
import org.transdroid.protocol.qbittorrent.QbittorrentAdapter
import org.transdroid.protocol.rtorrent.RtorrentAdapter
import org.transdroid.protocol.transmission.TransmissionAdapter

/**
 * A connection to one torrent daemon. Implementations are stateless beyond connection/session
 * bookkeeping and safe to call from any dispatcher; all calls block on network I/O internally
 * on the IO dispatcher. All methods throw [DaemonException] on failure.
 */
interface DaemonAdapter {
    val config: DaemonConfig

    /** Verifies connectivity and credentials, returning a daemon version description. */
    suspend fun testConnection(): String

    suspend fun listTorrents(): List<Torrent>

    /**
     * Adds a torrent by magnet link or a URL to a .torrent file. With [startPaused] the
     * torrent is added stopped, so files can be deselected before starting it.
     */
    suspend fun addByUrl(url: String, startPaused: Boolean = false)

    /** Adds a torrent from the raw bytes of a .torrent file. */
    suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean = false)

    suspend fun start(torrentId: String)

    suspend fun pause(torrentId: String)

    suspend fun remove(torrentId: String, deleteData: Boolean)

    suspend fun listFiles(torrentId: String): List<TorrentFile>

    /**
     * Changes one file's download priority. Clients without a LOW level treat LOW as
     * NORMAL; OFF always means "do not download".
     */
    suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority)

    suspend fun listTrackers(torrentId: String): List<Tracker>

    /**
     * Sets (replaces) the torrent's label; a blank string clears it. Every client supports some
     * notion of a per-torrent label, but the exact model differs - qBittorrent and Deluge keep a
     * server-side registry that a label must be created in before it can be assigned (handled
     * internally here), while Transmission and rTorrent just accept an arbitrary string.
     */
    suspend fun setLabel(torrentId: String, label: String)

    /** Whether this client exposes an alternative ("turtle") speed limits toggle at all. */
    val supportsAltSpeedLimits: Boolean get() = false

    /** Whether alternative speed limits are currently active. Only meaningful when [supportsAltSpeedLimits]. */
    suspend fun isAltSpeedLimitsEnabled(): Boolean = false

    /** Enables or disables alternative ("turtle") speed limits. No-op where unsupported. */
    suspend fun setAltSpeedLimitsEnabled(enabled: Boolean) {}
}

object DaemonAdapterFactory {

    /** A default client with timeouts suited for home servers and seedboxes. */
    fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun create(config: DaemonConfig, httpClient: OkHttpClient = defaultHttpClient()): DaemonAdapter {
        var client = config.pinnedCertSha256
            ?.takeIf { it.isNotBlank() }
            ?.let { Tls.clientWithPinnedCertificate(httpClient, it) }
            ?: httpClient
        if (config.customHeaders.isNotEmpty()) {
            client = client.newBuilder().addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    config.customHeaders.forEach { (name, value) -> header(name, value) }
                }.build()
                chain.proceed(request)
            }.build()
        }
        return when (config.type) {
            DaemonType.TRANSMISSION -> TransmissionAdapter(config, client)
            DaemonType.QBITTORRENT -> QbittorrentAdapter(config, client)
            DaemonType.RTORRENT -> RtorrentAdapter(config, client)
            DaemonType.DELUGE -> DelugeAdapter(config, client)
        }
    }
}
