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
package org.transdroid.data.legacy

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.Credentials
import org.transdroid.data.ProfilesData
import org.transdroid.data.RssFeed
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.DaemonType

/** What a Transdroid 2 settings set turns into, before it's merged into the v3 store. */
data class LegacyImport(
    val profiles: List<ServerProfile>,
    val feeds: List<RssFeed>,
    /** The server v2 would have opened with, if it was imported. */
    val activeProfileId: String?,
    val skipped: List<LegacySkippedServer>,
    /** Names of imported servers whose self-signed certificate must be trusted again. */
    val needsCertificate: List<String>,
)

@Serializable
data class LegacySkippedServer(val name: String, val reason: LegacySkipReason)

@Serializable
enum class LegacySkipReason {
    /** A client Transdroid 3 has no adapter for, like uTorrent or Synology. */
    UNSUPPORTED_CLIENT,

    /** Deluge's daemon RPC protocol; Transdroid 3 only talks to the Deluge Web UI. */
    DELUGE_DAEMON,

    /** A Dediseedbox preset, which is no longer supported. */
    UNSUPPORTED_SEEDBOX,
}

/** Shown to the user once after an import; persisted until dismissed. */
@Serializable
data class LegacyImportSummary(
    val serverCount: Int,
    val feedCount: Int,
    val skipped: List<LegacySkippedServer> = emptyList(),
    val needsCertificate: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = serverCount == 0 && feedCount == 0 && skipped.isEmpty()
}

data class LegacyMergeResult(val data: ProfilesData, val summary: LegacyImportSummary, val activeProfileId: String?)

/**
 * Converts Transdroid 2 settings to v3 profiles and feeds. Transdroid 2 kept everything in its
 * default SharedPreferences as numbered keys (`server_type_0`, `server_address_0`, ...), and could
 * export the same as a `settings.json` file; [fromExportJson] maps that file onto the same keys,
 * so both routes share [convert]. Plain JVM code so it can be unit tested without a device.
 *
 * The rules mirror how Transdroid 2 itself read these settings (ApplicationSettings, the
 * seedbox settings classes and each adapter's URL building at the v2.5.29 tag), so an imported
 * server connects to exactly the URL v2 did.
 */
object LegacySettingsConverter {

    fun convert(prefs: Map<String, Any?>, newId: () -> String = { UUID.randomUUID().toString() }): LegacyImport {
        val profiles = mutableListOf<ServerProfile>()
        val skipped = mutableListOf<LegacySkippedServer>()
        val needsCertificate = mutableListOf<String>()
        // v2 numbers all servers in one sequence: normal servers first, then each seedbox
        // provider in turn. Skipped servers still take up a number, so track the mapping.
        val idByV2Order = mutableMapOf<Int, String>()
        var order = 0

        fun add(result: Converted) {
            when (result) {
                is Converted.Profile -> {
                    profiles += result.profile
                    idByV2Order[order] = result.profile.id
                    if (result.needsCertificate) needsCertificate += result.profile.displayName
                }
                is Converted.Skipped -> skipped += result.server
            }
            order++
        }

        // Like v2's getMaxNormalServer: stop at the first number without a type or address
        var i = 0
        while (prefs.string("server_type_$i") != null && prefs.string("server_address_$i") != null) {
            add(normalServer(prefs, i, newId))
            i++
        }
        for (provider in SeedboxProvider.entries) {
            var n = 0
            while (prefs.string("seedbox_${provider.key}_server_$n") != null) {
                add(seedboxServer(prefs, provider, n, newId))
                n++
            }
        }

        return LegacyImport(
            profiles = profiles,
            feeds = feeds(prefs, newId),
            activeProfileId = activeProfileId(prefs, idByV2Order, profiles),
            skipped = skipped,
            needsCertificate = needsCertificate,
        )
    }

    /**
     * Adds an import to existing v3 data without overwriting anything. A server already present
     * with the same client type, host and port is left alone (so a repeated import adds nothing),
     * as is a feed with the same URL.
     */
    fun merge(existing: ProfilesData, import: LegacyImport): LegacyMergeResult {
        val newProfiles = import.profiles.filter { candidate ->
            existing.profiles.none { it.type == candidate.type && it.host.equals(candidate.host, ignoreCase = true) && it.port == candidate.port }
        }
        val newFeeds = import.feeds.filter { candidate -> existing.feeds.none { it.url == candidate.url } }
        val newNames = newProfiles.map { it.displayName }.toSet()
        return LegacyMergeResult(
            data = existing.copy(profiles = existing.profiles + newProfiles, feeds = existing.feeds + newFeeds),
            summary = LegacyImportSummary(
                serverCount = newProfiles.size,
                feedCount = newFeeds.size,
                skipped = import.skipped,
                needsCertificate = import.needsCertificate.filter { it in newNames },
            ),
            activeProfileId = import.activeProfileId?.takeIf { id -> newProfiles.any { it.id == id } },
        )
    }

    /**
     * Maps a Transdroid 2 `settings.json` export onto the SharedPreferences keys v2 would have
     * restored it into, for [convert]. Throws [IllegalArgumentException] if it isn't such a file.
     */
    fun fromExportJson(text: String): Map<String, Any?> {
        val root = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a Transdroid 2 settings file", e)
        }
        require("servers" in root || "rssfeeds" in root) { "Not a Transdroid 2 settings file" }
        val prefs = mutableMapOf<String, Any?>()
        (root["servers"] as? JsonArray)?.forEachIndexed { i, element ->
            val server = element as? JsonObject ?: return@forEachIndexed
            fun copy(from: String, to: String) {
                server.primitive(from)?.let { prefs["${to}_$i"] = it }
            }
            copy("name", "server_name")
            copy("type", "server_type")
            copy("host", "server_address")
            copy("port", "server_port")
            copy("ssl", "server_sslenabled")
            copy("ssl_accept_all", "server_ssltrustall")
            copy("ssl_trust_key", "server_ssltrustkey")
            copy("folder", "server_folder")
            copy("username", "server_user")
            copy("password", "server_pass")
            copy("extra_password", "server_extrapass")
            copy("local_network", "server_localnetwork")
            copy("local_host", "server_localaddress")
            copy("local_port", "server_localport")
            copy("local_ssl", "server_localsslenabled")
            (server.primitive("use_auth") as? Boolean)?.let { prefs["server_disableauth_$i"] = !it }
        }
        (root["rssfeeds"] as? JsonArray)?.forEachIndexed { i, element ->
            val feed = element as? JsonObject ?: return@forEachIndexed
            feed.primitive("name")?.let { prefs["rssfeed_name_$i"] = it }
            feed.primitive("url")?.let { prefs["rssfeed_url_$i"] = it }
            feed.primitive("last_seen_time")?.let { prefs["rssfeed_lastviewed_$i"] = it }
        }
        return prefs
    }

    private sealed class Converted {
        data class Profile(val profile: ServerProfile, val needsCertificate: Boolean) : Converted()
        data class Skipped(val server: LegacySkippedServer) : Converted()
    }

    private fun normalServer(prefs: Map<String, Any?>, i: Int, newId: () -> String): Converted {
        val code = prefs.string("server_type_$i")
        val host = prefs.string("server_address_$i").orEmpty().trim()
        val name = prefs.string("server_name_$i")?.takeIf { it.isNotBlank() } ?: host
        val type = daemonType(code) ?: return Converted.Skipped(
            LegacySkippedServer(name, if (code in DELUGE_DAEMON_CODES) LegacySkipReason.DELUGE_DAEMON else LegacySkipReason.UNSUPPORTED_CLIENT)
        )

        val ssl = prefs.bool("server_sslenabled_$i") ?: false
        val port = prefs.port("server_port_$i") ?: v2DefaultPort(type, ssl)
        val path = v3Path(type, prefs.string("server_folder_$i"))
        val useAuth = !(prefs.bool("server_disableauth_$i") ?: false)
        val user = if (useAuth) prefs.string("server_user_$i").orEmpty() else ""
        val pass = if (useAuth) prefs.string("server_pass_$i").orEmpty() else ""
        val extraPass = prefs.string("server_extrapass_$i").orEmpty()

        // v2 matched any of several SSIDs separated by '|'
        val ssids = prefs.string("server_localnetwork_$i").orEmpty().split('|').map(String::trim).filter(String::isNotEmpty)
        val localHost = prefs.string("server_localaddress_$i").orEmpty().trim()
        val local = ssids.isNotEmpty() && localHost.isNotEmpty()

        val profile = credentials(
            ServerProfile(
                id = newId(),
                name = name,
                type = type,
                host = host,
                port = port,
                useSsl = ssl,
                path = path,
                localNetworkEnabled = local,
                localNetworkSsids = if (local) ssids else emptyList(),
                localHost = if (local) localHost else "",
                // v2 fell back to the main port, and to the main SSL setting
                localPort = if (local) prefs.port("server_localport_$i") ?: port else 0,
                localUseSsl = if (local) prefs.bool("server_localsslenabled_$i") ?: ssl else false,
                // v2 used the same folder and login on the local network
                localPath = if (local) path else "",
            ),
            user = user,
            pass = pass,
            extraPass = extraPass,
        )
        // v2 pinned a SHA-1 fingerprint (or trusted everything); v3 pins SHA-256, so the user has
        // to accept the certificate again on first connect.
        val trusted = prefs.bool("server_ssltrustall_$i") == true || !prefs.string("server_ssltrustkey_$i").isNullOrBlank()
        return Converted.Profile(profile, needsCertificate = trusted && (ssl || profile.localUseSsl))
    }

    private fun seedboxServer(prefs: Map<String, Any?>, provider: SeedboxProvider, n: Int, newId: () -> String): Converted {
        fun get(field: String) = prefs.string("seedbox_${provider.key}_${field}_$n")
        val server = get("server").orEmpty().trim()
        val name = get("name")?.takeIf { it.isNotBlank() } ?: server
        val user = get("user").orEmpty()
        val pass = get("pass").orEmpty()

        // Xirvik's dedicated servers can run several clients; the other presets are rTorrent only
        val type = when (provider) {
            SeedboxProvider.DEDISEEDBOX -> return Converted.Skipped(LegacySkippedServer(name, LegacySkipReason.UNSUPPORTED_SEEDBOX))
            SeedboxProvider.XIRVIK_DEDI -> daemonType(get("client") ?: RTORRENT_CODE)
                ?: return Converted.Skipped(LegacySkippedServer(name, LegacySkipReason.UNSUPPORTED_CLIENT))
            else -> DaemonType.RTORRENT
        }
        // The fixed settings v2's XirvikSharedSettings/XirvikSemiSettings/XirvikDediSettings used
        val folder = when (provider) {
            SeedboxProvider.XIRVIK_SHARED -> get("rpc")
            SeedboxProvider.XIRVIK_SEMI -> "/RPC2"
            else -> if (type == DaemonType.DELUGE) "/deluge" else null
        }
        val extraPass = if (type == DaemonType.DELUGE) "deluge" else ""
        val profile = credentials(
            ServerProfile(
                id = newId(),
                name = name,
                type = type,
                host = server,
                port = 443,
                useSsl = true,
                path = v3Path(type, folder),
            ),
            user = user,
            pass = pass,
            extraPass = extraPass,
        )
        return Converted.Profile(profile, needsCertificate = false)
    }

    /**
     * v2's Deluge adapter logged in with the separate "extra password" (the Web UI password)
     * and sent the username/password, if any, as HTTP authentication to a server in front of it.
     * v3's Deluge adapter only knows the Web UI password, so that HTTP login travels as a
     * custom header instead.
     */
    private fun credentials(profile: ServerProfile, user: String, pass: String, extraPass: String): ServerProfile =
        if (profile.type == DaemonType.DELUGE) {
            profile.copy(
                password = extraPass,
                customHeaders = if (user.isNotEmpty()) "Authorization: ${Credentials.basic(user, pass, Charsets.UTF_8)}" else "",
            )
        } else {
            profile.copy(username = user, password = pass)
        }

    /**
     * v2's folder setting to v3's path. Only Transmission differs: v2 always appended "/rpc" to
     * the folder (default "/transmission"), while v3 uses a custom path as the full endpoint.
     * The others mean the same in both: an rTorrent endpoint (default /RPC2), or a prefix for
     * qBittorrent's API and Deluge's /json. Trailing slashes are dropped, as v2 did.
     */
    internal fun v3Path(type: DaemonType, folder: String?): String {
        val trimmed = folder.orEmpty().trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        val withSlash = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return when (type) {
            DaemonType.TRANSMISSION -> "$withSlash/rpc"
            DaemonType.RTORRENT -> folder.orEmpty().trim()
            else -> withSlash
        }
    }

    /** v2's Daemon.getDefaultPortNumber: only rTorrent switched to 443 for SSL. */
    internal fun v2DefaultPort(type: DaemonType, ssl: Boolean): Int = when (type) {
        DaemonType.TRANSMISSION -> 9091
        DaemonType.QBITTORRENT -> 8080
        DaemonType.RTORRENT -> if (ssl) 443 else 80
        DaemonType.DELUGE -> 8112
    }

    private fun daemonType(code: String?): DaemonType? = when (code) {
        "daemon_transmission" -> DaemonType.TRANSMISSION
        "daemon_qbittorrent" -> DaemonType.QBITTORRENT
        RTORRENT_CODE -> DaemonType.RTORRENT
        "daemon_deluge" -> DaemonType.DELUGE
        else -> null
    }

    private fun feeds(prefs: Map<String, Any?>, newId: () -> String): List<RssFeed> {
        val feeds = mutableListOf<RssFeed>()
        var i = 0
        while (true) {
            val url = prefs.string("rssfeed_url_$i") ?: break
            // v2 stored milliseconds, v3 stores seconds
            val lastViewed = (prefs["rssfeed_lastviewed_$i"] as? Number)?.toLong()?.takeIf { it > 0 }?.div(1000)
            feeds += RssFeed(id = newId(), name = prefs.string("rssfeed_name_$i").orEmpty(), url = url.trim(), lastViewedTimestamp = lastViewed)
            i++
        }
        return feeds
    }

    /**
     * The server v2 would have shown first: an explicitly chosen default server, otherwise the
     * last used one, otherwise the first (matching ApplicationSettings.getDefaultServer).
     */
    private fun activeProfileId(prefs: Map<String, Any?>, idByV2Order: Map<Int, String>, profiles: List<ServerProfile>): String? {
        val default = prefs.string("header_defaultserver")?.toIntOrNull() ?: -2
        val order = if (default >= 0) default else (prefs["system_lastusedserver"] as? Number)?.toInt() ?: -1
        return idByV2Order[order] ?: profiles.firstOrNull()?.id
    }

    private enum class SeedboxProvider(val key: String) {
        // The order of v2's SeedboxProvider enum, which decides the server numbering
        DEDISEEDBOX("dediseedbox"),
        XIRVIK_SHARED("xirvikshared"),
        XIRVIK_SEMI("xirviksemi"),
        XIRVIK_DEDI("xirvikdedi"),
    }

    private const val RTORRENT_CODE = "daemon_rtorrent"
    private val DELUGE_DAEMON_CODES = setOf("daemon_deluge_rpc", "daemon_deluge2_rpc")

    private fun Map<String, Any?>.string(key: String): String? = this[key] as? String
    private fun Map<String, Any?>.bool(key: String): Boolean? = this[key] as? Boolean

    /** v2 kept ports as strings and fell back to the default when they didn't parse. */
    private fun Map<String, Any?>.port(key: String): Int? = when (val value = this[key]) {
        is String -> value.trim().toIntOrNull()
        is Number -> value.toInt()
        else -> null
    }?.takeIf { it in 1..65535 }

    private fun JsonObject.primitive(key: String): Any? {
        val value = this[key] as? JsonPrimitive ?: return null
        if (value.isString) return value.content
        return value.booleanOrNull ?: value.longOrNull ?: value.contentOrNull?.takeIf { it != "null" }
    }
}
