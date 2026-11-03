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
package org.transdroid.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.DaemonType
import org.transdroid.protocol.discovery.DiscoveredDaemon

/**
 * Everything typed into the add/edit server screen. Kept in one state holder rather than as
 * dozens of local `var x by remember` in the screen composable: besides keeping the form logic
 * in one place, the Kotlin compiler's data-flow analysis of that many reassigned locals in a
 * single function made the app module take minutes to compile.
 */
class EditServerFormState(existing: ServerProfile?) {

    var name by mutableStateOf(existing?.name.orEmpty())
    var type by mutableStateOf(existing?.type ?: DaemonType.TRANSMISSION)
    var host by mutableStateOf(existing?.host.orEmpty())
    var port by mutableStateOf((existing?.port ?: DaemonType.TRANSMISSION.defaultPort).toString())
    var useSsl by mutableStateOf(existing?.useSsl ?: false)
    var path by mutableStateOf(existing?.path.orEmpty())
    var username by mutableStateOf(existing?.username.orEmpty())
    var password by mutableStateOf(existing?.password.orEmpty())
    var apiKey by mutableStateOf(existing?.apiKey.orEmpty())
    var pinnedCert by mutableStateOf(existing?.pinnedCertSha256.orEmpty())
    var customHeaders by mutableStateOf(existing?.customHeaders.orEmpty())
    var hostError by mutableStateOf(false)
    var portError by mutableStateOf(false)

    // Single-URL-first connection entry (Transmission/qBittorrent/Deluge web UIs, and rTorrent's
    // ruTorrent-URL sub-mode): the URL is the primary input, and host/port/ssl/path are derived
    // from it. "Override individual parts" reveals those raw fields for direct editing.
    var url by mutableStateOf(
        composeServerUrl(
            existing?.useSsl ?: false,
            existing?.host.orEmpty(),
            existing?.port ?: DaemonType.TRANSMISSION.defaultPort,
            existing?.path.orEmpty(),
        )
    )
    var overridden by mutableStateOf(false)
    var rtorrentUseUrl by mutableStateOf(true)

    // Local-network connection override: same URL-first entry as the main connection, just
    // without the "override individual parts" escape hatch - see ServerProfile.toDaemonConfig.
    var localNetworkEnabled by mutableStateOf(existing?.localNetworkEnabled ?: false)
    var localSsids by mutableStateOf(
        (existing?.localNetworkSsids.orEmpty() + existing?.localNetworkSsid.orEmpty())
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    )

    /** A network typed but not yet added as a chip; still saved so it isn't silently dropped. */
    var localSsidDraft by mutableStateOf("")
    var localHost by mutableStateOf(existing?.localHost.orEmpty())
    var localPort by mutableStateOf((existing?.localPort ?: 0).takeIf { it != 0 }?.toString().orEmpty())
    var localUseSsl by mutableStateOf(existing?.localUseSsl ?: false)
    var localPath by mutableStateOf(existing?.localPath.orEmpty())
    var localUsername by mutableStateOf(existing?.localUsername.orEmpty())
    var localPassword by mutableStateOf(existing?.localPassword.orEmpty())
    var localUrl by mutableStateOf(
        if (existing?.localHost.isNullOrBlank()) {
            ""
        } else {
            composeServerUrl(existing.localUseSsl, existing.localHost, existing.localPort, existing.localPath)
        }
    )

    // Xirvik seedbox quick setup (new servers only), see XirvikSetupSection
    var xirvikExpanded by mutableStateOf(false)
    var xirvikServer by mutableStateOf("")
    var xirvikUser by mutableStateOf("")
    var xirvikPass by mutableStateOf("")

    /** True while the raw host/port/path fields are shown instead of only the URL. */
    val showManualFields: Boolean
        get() = overridden || (type == DaemonType.RTORRENT && !rtorrentUseUrl)

    val showUrlSection: Boolean
        get() = type != DaemonType.RTORRENT || rtorrentUseUrl

    val usesScgi: Boolean
        get() = type == DaemonType.RTORRENT && !rtorrentUseUrl

    fun applyParsedUrl(raw: String) {
        url = raw
        val parsed = parseServerUrl(raw) ?: return
        useSsl = parsed.secure
        host = parsed.host
        port = parsed.port.toString()
        path = parsed.path
        hostError = false
    }

    fun applyParsedLocalUrl(raw: String) {
        localUrl = raw
        val parsed = parseServerUrl(raw) ?: return
        localUseSsl = parsed.secure
        localHost = parsed.host
        localPort = parsed.port.toString()
        localPath = parsed.path
    }

    fun toggleOverride(enabled: Boolean) {
        if (!enabled) url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: type.defaultPort, path)
        overridden = enabled
    }

    fun selectType(newType: DaemonType) {
        // Only nudge the port to the new type's default while nothing real has been entered
        // yet - once a host/URL is set, switching type shouldn't silently rewrite a port the
        // user (or a pasted URL) explicitly gave.
        if (host.isBlank() && port == type.defaultPort.toString()) {
            port = newType.defaultPort.toString()
        }
        type = newType
        if (!overridden) {
            url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: newType.defaultPort, path)
        }
    }

    fun changeHost(value: String) {
        host = value
        hostError = false
        if (!overridden) url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: type.defaultPort, path)
    }

    fun changePort(value: String) {
        port = value
        portError = false
    }

    /** Also moves a standard port along, e.g. 80 to 443, when HTTPS is switched. */
    fun changeSsl(enabled: Boolean) {
        val oldDefault = if (useSsl) 443 else 80
        val newDefault = if (enabled) 443 else 80
        if (port == oldDefault.toString()) port = newDefault.toString()
        useSsl = enabled
    }

    fun useScgi() {
        rtorrentUseUrl = false
        useSsl = false
    }

    fun addSsid(ssid: String) {
        localSsids = appendSsid(localSsids, ssid)
    }

    fun addSsidDraft() {
        addSsid(localSsidDraft)
        localSsidDraft = ""
    }

    fun removeSsid(ssid: String) {
        localSsids = localSsids - ssid
    }

    /** Fills the form from a daemon found on the local network. */
    fun applyDiscovered(daemon: DiscoveredDaemon) {
        type = daemon.type
        host = daemon.host
        port = daemon.port.toString()
        useSsl = false
        path = ""
        overridden = false
        rtorrentUseUrl = true
        url = composeServerUrl(false, daemon.host, daemon.port, "")
        if (name.isBlank()) name = daemon.type.displayName()
        hostError = false
        portError = false
    }

    /** Fills the form from a Xirvik auto-configuration attempt; [detectedPath] is empty on a miss. */
    fun applyXirvik(detectedPath: String, found: Boolean) {
        type = DaemonType.RTORRENT
        host = xirvikServer.trim()
        port = "443"
        useSsl = true
        path = detectedPath
        username = xirvikUser.trim()
        password = xirvikPass
        overridden = false
        rtorrentUseUrl = true
        url = composeServerUrl(true, xirvikServer.trim(), 443, detectedPath)
        if (name.isBlank()) name = "Xirvik"
        hostError = false
        xirvikExpanded = !found
    }

    fun validate(): Boolean {
        hostError = host.isBlank()
        portError = port.toIntOrNull() !in 1..65535
        return !hostError && !portError
    }

    fun toProfile(id: String) = ServerProfile(
        id = id,
        name = name.trim(),
        type = type,
        host = host.trim(),
        port = port.toIntOrNull() ?: type.defaultPort,
        useSsl = useSsl,
        path = path.trim(),
        username = username.trim(),
        password = password,
        apiKey = apiKey.trim(),
        pinnedCertSha256 = pinnedCert,
        customHeaders = customHeaders.trim(),
        localNetworkEnabled = localNetworkEnabled,
        localNetworkSsids = appendSsid(localSsids, localSsidDraft),
        localHost = localHost.trim(),
        localPort = localPort.toIntOrNull() ?: 0,
        localUseSsl = localUseSsl,
        localPath = localPath.trim(),
        localUsername = localUsername.trim(),
        localPassword = localPassword,
    )

    companion object {
        /** Keeps what was typed across configuration changes and process death. */
        val Saver: Saver<EditServerFormState, Any> = Saver(
            save = { form ->
                arrayListOf<Any?>(
                    form.name, form.type.name, form.host, form.port, form.useSsl, form.path,
                    form.username, form.password, form.apiKey, form.pinnedCert, form.customHeaders,
                    form.url, form.overridden, form.rtorrentUseUrl,
                    form.localNetworkEnabled, ArrayList(form.localSsids), form.localSsidDraft, form.localHost,
                    form.localPort, form.localUseSsl, form.localPath, form.localUsername, form.localPassword, form.localUrl,
                    form.xirvikExpanded, form.xirvikServer, form.xirvikUser, form.xirvikPass,
                )
            },
            restore = { saved ->
                val v = saved as List<*>
                var i = 0
                fun string() = v[i++] as String
                fun bool() = v[i++] as Boolean
                EditServerFormState(null).apply {
                    name = string(); type = DaemonType.valueOf(string()); host = string(); port = string()
                    useSsl = bool(); path = string(); username = string(); password = string(); apiKey = string()
                    pinnedCert = string(); customHeaders = string(); url = string(); overridden = bool()
                    rtorrentUseUrl = bool(); localNetworkEnabled = bool()
                    @Suppress("UNCHECKED_CAST")
                    localSsids = v[i++] as List<String>
                    localSsidDraft = string(); localHost = string(); localPort = string(); localUseSsl = bool()
                    localPath = string(); localUsername = string(); localPassword = string(); localUrl = string()
                    xirvikExpanded = bool(); xirvikServer = string(); xirvikUser = string(); xirvikPass = string()
                }
            },
        )
    }
}
