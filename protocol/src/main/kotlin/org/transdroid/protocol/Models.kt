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

/** The torrent client (daemon) types supported by the protocol layer. */
enum class DaemonType(val defaultPort: Int, val defaultSslPort: Int) {
    TRANSMISSION(9091, 443),
    QBITTORRENT(8080, 443),
    RTORRENT(80, 443),
    DELUGE(8112, 443),
}

/**
 * Connection settings for one daemon. This is a plain value object: the app layer owns
 * persistence (and encryption) of these fields and hands them to [DaemonAdapterFactory].
 */
data class DaemonConfig(
    val type: DaemonType,
    val host: String,
    val port: Int,
    val useSsl: Boolean = false,
    /**
     * Optional base or endpoint path. For Transmission this is the full RPC endpoint
     * (defaults to /transmission/rpc); for qBittorrent a base path prefix (defaults to none).
     */
    val path: String? = null,
    val username: String? = null,
    val password: String? = null,
    /**
     * Lowercase hex SHA-256 of a self-signed certificate the user explicitly trusts for
     * this server, or null to use normal CA validation only.
     */
    val pinnedCertSha256: String? = null,
    /**
     * Extra HTTP headers sent with every request to this server, e.g. Cloudflare Access
     * service-token headers for daemons behind an access portal.
     */
    val customHeaders: Map<String, String> = emptyMap(),
) {
    val baseUrl: String
        get() = (if (useSsl) "https" else "http") + "://" + host + ":" + port
}

enum class TorrentStatus {
    DOWNLOADING,
    SEEDING,
    PAUSED,
    CHECKING,
    QUEUED,
    ERROR,
    UNKNOWN;

    val isActive: Boolean
        get() = this == DOWNLOADING || this == SEEDING
}

/** One torrent as reported by a daemon, normalized across client types. */
data class Torrent(
    /** Daemon-specific identifier (numeric id for Transmission, info-hash for qBittorrent). */
    val id: String,
    val name: String,
    val status: TorrentStatus,
    /** Download completion in the range 0..1. */
    val progress: Float,
    /** Download rate in bytes per second. */
    val downloadRate: Long,
    /** Upload rate in bytes per second. */
    val uploadRate: Long,
    /** Estimated seconds until completion, or null when unknown/not applicable. */
    val etaSeconds: Long? = null,
    /** Total wanted size in bytes. */
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val ratio: Float,
    val peersConnected: Int = 0,
    /**
     * Connected peers currently acting as seeders (i.e. sending us data / already complete),
     * as best approximated by each daemon's API - see the adapter implementations for the
     * exact per-daemon meaning. Not a tracker-wide swarm count, just this client's connections.
     */
    val seedersConnected: Int = 0,
    /** Connected peers currently acting as leechers (still downloading), the complement of [seedersConnected]. */
    val leechersConnected: Int = 0,
    /** Unix timestamp (seconds) the torrent was added, or null when unknown. */
    val addedTimestamp: Long? = null,
    val downloadDir: String? = null,
    /** Human-readable error reported by the daemon, or null when none. */
    val error: String? = null,
    /** Labels/categories assigned on the daemon (qBittorrent's single category included). */
    val labels: List<String> = emptyList(),
    /**
     * When a magnet transfer is still fetching its metadata, the progress (0..1) of that
     * fetch; null once real content is known. In this phase size/progress read as zero.
     */
    val metadataProgress: Float? = null,
) {
    val isFinished: Boolean
        get() = progress >= 1f

    /** Content progress normally; metadata-fetch progress while a magnet resolves. */
    val displayProgress: Float
        get() = metadataProgress ?: progress
}

enum class FilePriority { OFF, LOW, NORMAL, HIGH }

/** One file inside a torrent. */
data class TorrentFile(
    /** Zero-based position in the daemon's file list; used to address priority changes. */
    val index: Int,
    val path: String,
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val priority: FilePriority,
) {
    val progress: Float
        get() = if (sizeBytes <= 0) 1f else (downloadedBytes.toFloat() / sizeBytes).coerceIn(0f, 1f)
}

enum class TrackerStatus { WORKING, IDLE, ERROR }

/** One tracker a torrent announces to, normalized across client types. */
data class Tracker(
    val url: String,
    val status: TrackerStatus,
    /** Seeders/leechers as last reported by this tracker's scrape, or null when not reported. */
    val seeders: Int? = null,
    val leechers: Int? = null,
    /** The daemon's status/error message for this tracker, or null when none. */
    val message: String? = null,
)

/** Errors thrown by daemon adapters, so the UI can give targeted feedback. */
sealed class DaemonException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The daemon could not be reached at all (network error, refused connection, timeout). */
    class Connection(message: String, cause: Throwable? = null) : DaemonException(message, cause)

    /** The daemon rejected the configured credentials. */
    class Authentication(message: String) : DaemonException(message)

    /** The daemon answered, but not in a way we understand. */
    class UnexpectedResponse(message: String, cause: Throwable? = null) : DaemonException(message, cause)

    /** TLS failed because the server's certificate is not trusted (e.g. self-signed). */
    class UntrustedServer(message: String, cause: Throwable? = null) : DaemonException(message, cause)
}
