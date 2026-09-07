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
package org.transdroid.data

import kotlinx.serialization.Serializable
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonType

/**
 * One configured server connection. Persisted encrypted (see [ServerProfilesRepository]);
 * [extras] leaves room for adapter-specific settings without schema migrations.
 */
@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val type: DaemonType,
    val host: String,
    val port: Int,
    val useSsl: Boolean = false,
    val path: String = "",
    val username: String = "",
    val password: String = "",
    /** SHA-256 (lowercase hex) of an explicitly trusted self-signed certificate, or empty. */
    val pinnedCertSha256: String = "",
    /** Extra HTTP headers, one "Name: Value" per line (e.g. Cloudflare Access tokens). */
    val customHeaders: String = "",
    val extras: Map<String, String> = emptyMap(),
) {
    val displayName: String
        get() = name.ifBlank { host }

    fun toDaemonConfig() = DaemonConfig(
        type = type,
        host = host,
        port = port,
        useSsl = useSsl,
        path = path.takeIf { it.isNotBlank() },
        username = username.takeIf { it.isNotBlank() },
        password = password.takeIf { it.isNotBlank() },
        pinnedCertSha256 = pinnedCertSha256.takeIf { it.isNotBlank() },
        customHeaders = parseHeaders(customHeaders),
    )

    private fun parseHeaders(raw: String): Map<String, String> = raw.lines()
        .mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            val name = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (name.isEmpty() || value.isEmpty()) null else name to value
        }
        .toMap()
}

/**
 * One RSS feed subscription. Stored encrypted because private feed URLs commonly embed
 * per-user passkeys.
 */
@Serializable
data class RssFeed(
    val id: String,
    val name: String,
    val url: String,
    /** Publication time (unix seconds) of the newest item the user has seen. */
    val lastViewedTimestamp: Long? = null,
) {
    val displayName: String
        get() = name.ifBlank { url }
}

/**
 * One configured Torznab search endpoint (Jackett/Prowlarr). Stored encrypted (API key).
 * [username]/[password] are optional HTTP Basic Auth credentials for endpoints that sit behind
 * their own reverse-proxy login (e.g. a seedbox's member area) in front of the indexer itself.
 */
@Serializable
data class SearchProviderConfig(
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
) {
    val displayName: String
        get() = name.ifBlank { url }
}

/** Root object stored in the encrypted profiles DataStore. */
@Serializable
data class ProfilesData(
    val profiles: List<ServerProfile> = emptyList(),
    val feeds: List<RssFeed> = emptyList(),
    val searchProviders: List<SearchProviderConfig> = emptyList(),
)
