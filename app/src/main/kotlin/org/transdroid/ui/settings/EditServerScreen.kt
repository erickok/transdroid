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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.net.URI
import org.transdroid.R
import org.transdroid.data.ServerProfile
import org.transdroid.errorlog.ErrorLog
import org.transdroid.protocol.DaemonType
import org.transdroid.protocol.discovery.DiscoveredDaemon
import org.transdroid.ui.message
import org.transdroid.ui.torrents.UiError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerScreen(
    viewModel: SettingsViewModel,
    serverId: String?,
    onBack: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val certificateState by viewModel.certificateState.collectAsStateWithLifecycle()
    val discovery by viewModel.discovery.collectAsStateWithLifecycle()
    val xirvikState by viewModel.xirvikState.collectAsStateWithLifecycle()
    val existing = profiles.firstOrNull { it.id == serverId }

    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.type ?: DaemonType.TRANSMISSION) }
    var host by rememberSaveable(existing?.id) { mutableStateOf(existing?.host.orEmpty()) }
    var port by rememberSaveable(existing?.id) {
        mutableStateOf((existing?.port ?: DaemonType.TRANSMISSION.defaultPort).toString())
    }
    var useSsl by rememberSaveable(existing?.id) { mutableStateOf(existing?.useSsl ?: false) }
    var path by rememberSaveable(existing?.id) { mutableStateOf(existing?.path.orEmpty()) }
    var username by rememberSaveable(existing?.id) { mutableStateOf(existing?.username.orEmpty()) }
    var password by rememberSaveable(existing?.id) { mutableStateOf(existing?.password.orEmpty()) }
    var pinnedCert by rememberSaveable(existing?.id) { mutableStateOf(existing?.pinnedCertSha256.orEmpty()) }
    var customHeaders by rememberSaveable(existing?.id) { mutableStateOf(existing?.customHeaders.orEmpty()) }
    var hostError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // Single-URL-first connection entry (Transmission/qBittorrent/Deluge web UIs, and rTorrent's
    // ruTorrent-URL sub-mode): the field below is the primary input, and host/port/ssl/path are
    // derived from it. "Override individual parts" reveals those raw fields for direct editing.
    var url by rememberSaveable(existing?.id) {
        mutableStateOf(composeServerUrl(existing?.useSsl ?: false, existing?.host.orEmpty(), existing?.port ?: type.defaultPort, existing?.path.orEmpty()))
    }
    var overridden by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var rtorrentUseUrl by rememberSaveable(existing?.id) { mutableStateOf(true) }

    // Xirvik seedbox quick setup (new servers only): a hostname + credentials probe against
    // Xirvik's auto-config endpoint to discover the account's per-user SCGI mount path, mirroring
    // Transdroid 2's XirvikSettingsActivity/RetrieveXirvikAutoConfTask. A hit or a miss both fill in
    // the rest of the form below, exactly like picking a LAN-discovered daemon does.
    var xirvikExpanded by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var xirvikServer by rememberSaveable(existing?.id) { mutableStateOf("") }
    var xirvikUser by rememberSaveable(existing?.id) { mutableStateOf("") }
    var xirvikPass by rememberSaveable(existing?.id) { mutableStateOf("") }

    LaunchedEffect(xirvikState) {
        val detectedPath = when (val state = xirvikState) {
            is XirvikState.Success -> state.path
            XirvikState.Failed -> ""
            else -> return@LaunchedEffect
        }
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
        xirvikExpanded = xirvikState is XirvikState.Failed
    }

    fun applyParsedUrl(raw: String) {
        url = raw
        val parsed = parseServerUrl(raw)
        if (parsed != null) {
            useSsl = parsed.secure
            host = parsed.host
            port = parsed.port.toString()
            path = parsed.path
            hostError = false
        }
    }

    fun toggleOverride(enabled: Boolean) {
        if (!enabled) url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: type.defaultPort, path)
        overridden = enabled
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetTestState() }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.settings_connection_help)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HelpEntry(R.string.settings_help_lan_title, R.string.settings_help_lan)
                    HelpEntry(R.string.settings_help_domain_title, R.string.settings_help_domain)
                    HelpEntry(R.string.settings_help_portal_title, R.string.settings_help_portal)
                    HelpEntry(R.string.settings_help_selfsigned_title, R.string.settings_help_selfsigned)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.settings_help_close)) }
            },
        )
    }

    // Adding a new server: look around the local network for daemons to offer.
    // Keyed on the nav argument, not `existing` — the profiles flow hasn't emitted on
    // first composition, so `existing` is briefly null even when editing.
    if (serverId == null) {
        LaunchedEffect(Unit) { viewModel.startLanScan() }
    }

    fun buildProfile() = ServerProfile(
        id = serverId ?: viewModel.newProfileId(),
        name = name.trim(),
        type = type,
        host = host.trim(),
        port = port.toIntOrNull() ?: type.defaultPort,
        useSsl = useSsl,
        path = path.trim(),
        username = username.trim(),
        password = password,
        pinnedCertSha256 = pinnedCert,
        customHeaders = customHeaders.trim(),
    )

    fun validate(): Boolean {
        hostError = host.isBlank()
        portError = port.toIntOrNull() !in 1..65535
        return !hostError && !portError
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (existing == null) R.string.settings_new_server else R.string.settings_edit_server))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = stringResource(R.string.settings_connection_help),
                        )
                    }
                    if (existing != null) {
                        IconButton(onClick = {
                            viewModel.delete(existing.id)
                            onBack()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.settings_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (serverId == null && (discovery.scanning || discovery.found.isNotEmpty())) {
                DiscoverySection(
                    state = discovery,
                    onPick = { daemon ->
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
                    },
                )
            }

            if (serverId == null) {
                XirvikSetupSection(
                    expanded = xirvikExpanded,
                    onExpandedChange = { xirvikExpanded = it },
                    server = xirvikServer,
                    onServerChange = { xirvikServer = it },
                    username = xirvikUser,
                    onUsernameChange = { xirvikUser = it },
                    password = xirvikPass,
                    onPasswordChange = { xirvikPass = it },
                    state = xirvikState,
                    onDetect = { viewModel.detectXirvik(xirvikServer.trim(), xirvikUser.trim(), xirvikPass) },
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            var typeMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = type.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false },
                ) {
                    DaemonType.entries.forEach { daemonType ->
                        DropdownMenuItem(
                            text = { Text(daemonType.displayName()) },
                            onClick = {
                                // Only nudge the port to the new type's default while nothing real has
                                // been entered yet - once a host/URL is set, switching type shouldn't
                                // silently rewrite a port the user (or a pasted URL) explicitly gave.
                                if (host.isBlank() && port == type.defaultPort.toString()) {
                                    port = daemonType.defaultPort.toString()
                                }
                                type = daemonType
                                if (!overridden) {
                                    url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: daemonType.defaultPort, path)
                                }
                                typeMenuExpanded = false
                            },
                        )
                    }
                }
            }

            if (type == DaemonType.RTORRENT) {
                Text(
                    stringResource(R.string.settings_rtorrent_how),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = rtorrentUseUrl,
                        onClick = { rtorrentUseUrl = true },
                        label = { Text(stringResource(R.string.settings_rtorrent_mode_url)) },
                    )
                    FilterChip(
                        selected = !rtorrentUseUrl,
                        onClick = {
                            rtorrentUseUrl = false
                            useSsl = false
                        },
                        label = { Text(stringResource(R.string.settings_rtorrent_mode_scgi)) },
                    )
                }
            }

            val showUrlSection = type != DaemonType.RTORRENT || rtorrentUseUrl
            if (showUrlSection) {
                OutlinedTextField(
                    value = url,
                    onValueChange = ::applyParsedUrl,
                    label = {
                        Text(
                            stringResource(
                                if (type == DaemonType.RTORRENT) R.string.settings_rturl_label else R.string.settings_url_label
                            )
                        )
                    },
                    placeholder = { Text(stringResource(R.string.settings_url_placeholder)) },
                    singleLine = true,
                    isError = hostError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        if (type == DaemonType.RTORRENT) R.string.settings_rturl_hint
                        else R.string.settings_url_hint,
                        type.displayName(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!overridden) {
                    DetectedSettingsPanel(secure = useSsl, host = host, port = port, path = path)
                    TextButton(onClick = { toggleOverride(true) }) {
                        Text(stringResource(R.string.settings_override_parts))
                    }
                }
            }

            val showManualFields = overridden || (type == DaemonType.RTORRENT && !rtorrentUseUrl)
            if (showManualFields) {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        hostError = false
                        if (!overridden) url = composeServerUrl(useSsl, host, port.toIntOrNull() ?: type.defaultPort, path)
                    },
                    label = {
                        Text(
                            stringResource(
                                if (type == DaemonType.RTORRENT && !rtorrentUseUrl) R.string.settings_scgi_host
                                else R.string.settings_host
                            )
                        )
                    },
                    isError = hostError,
                    supportingText = if (hostError) {
                        { Text(stringResource(R.string.settings_invalid_host)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        portError = false
                    },
                    label = {
                        Text(
                            stringResource(
                                if (type == DaemonType.RTORRENT && !rtorrentUseUrl) R.string.settings_scgi_port
                                else R.string.settings_port
                            )
                        )
                    },
                    isError = portError,
                    supportingText = {
                        Text(
                            stringResource(
                                if (portError) R.string.settings_invalid_port else R.string.settings_port_hint
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type != DaemonType.RTORRENT || rtorrentUseUrl) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = useSsl,
                            onCheckedChange = { checked ->
                                val oldDefault = if (useSsl) 443 else 80
                                val newDefault = if (checked) 443 else 80
                                if (port == oldDefault.toString()) port = newDefault.toString()
                                useSsl = checked
                            },
                        )
                        Spacer(Modifier.fillMaxWidth(0.05f))
                        Text(stringResource(R.string.settings_use_ssl))
                    }
                }
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = {
                        Text(
                            stringResource(
                                if (type == DaemonType.RTORRENT && !rtorrentUseUrl) R.string.settings_scgi_path
                                else R.string.settings_path
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(
                                when (type) {
                                    DaemonType.TRANSMISSION -> R.string.settings_path_hint_transmission
                                    DaemonType.QBITTORRENT -> R.string.settings_path_hint_qbittorrent
                                    DaemonType.RTORRENT -> R.string.settings_path_hint_rtorrent
                                    DaemonType.DELUGE -> R.string.settings_path_hint_deluge
                                }
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == DaemonType.RTORRENT && !rtorrentUseUrl) {
                    Text(
                        stringResource(R.string.settings_scgi_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.settings_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.settings_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = customHeaders,
                onValueChange = { customHeaders = it },
                label = { Text(stringResource(R.string.settings_custom_headers)) },
                placeholder = { Text(stringResource(R.string.settings_custom_headers_hint)) },
                supportingText = { Text(stringResource(R.string.settings_custom_headers_summary)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (pinnedCert.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            R.string.settings_cert_pinned,
                            pinnedCert.uppercase().chunked(2).take(8).joinToString(":"),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { pinnedCert = "" }) {
                        Text(stringResource(R.string.settings_cert_forget))
                    }
                }
            }

            when (val state = testState) {
                TestState.Idle -> {}
                TestState.Testing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(20.dp).fillMaxWidth(0.06f))
                    Text(
                        "  " + stringResource(R.string.settings_testing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is TestState.Success -> Text(
                    stringResource(R.string.settings_test_success, state.versionInfo),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                is TestState.Failure -> Column {
                    Text(
                        state.error.message(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.error == UiError.Ssl && useSsl) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                if (validate()) {
                                    viewModel.fetchCertificate(host.trim(), port.toIntOrNull() ?: type.defaultSslPort)
                                }
                            },
                            enabled = certificateState != CertificateState.Fetching,
                        ) {
                            Text(stringResource(R.string.settings_trust_cert))
                        }
                    }
                }
            }

            when (val certState = certificateState) {
                CertificateState.Idle -> {}
                CertificateState.Fetching -> Text(
                    stringResource(R.string.settings_cert_fetching),
                    style = MaterialTheme.typography.bodyMedium,
                )
                is CertificateState.Failed -> Text(
                    certState.error.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                is CertificateState.Fetched -> AlertDialog(
                    onDismissRequest = { viewModel.dismissCertificate() },
                    title = { Text(stringResource(R.string.settings_trust_cert_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.settings_trust_cert_message))
                            Spacer(Modifier.height(8.dp))
                            Text(certState.fingerprint.subject, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                certState.fingerprint.displayFingerprint,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pinnedCert = certState.fingerprint.sha256
                            viewModel.dismissCertificate()
                            viewModel.testConnection(buildProfile())
                        }) { Text(stringResource(R.string.settings_trust_cert_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissCertificate() }) {
                            Text(stringResource(R.string.details_cancel))
                        }
                    },
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (validate()) viewModel.testConnection(buildProfile()) },
                    enabled = testState != TestState.Testing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_test))
                }
                Button(
                    onClick = {
                        if (validate()) {
                            viewModel.save(buildProfile())
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
        }
    }
}

internal fun DaemonType.displayName(): String = when (this) {
    DaemonType.TRANSMISSION -> "Transmission"
    DaemonType.QBITTORRENT -> "qBittorrent"
    DaemonType.RTORRENT -> "rTorrent"
    DaemonType.DELUGE -> "Deluge"
}

/** Host/port/ssl/path as inferred from a pasted server URL. */
private data class ParsedServerUrl(val secure: Boolean, val host: String, val port: Int, val path: String)

private fun parseServerUrl(raw: String): ParsedServerUrl? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val hadHttps = trimmed.startsWith("https://", ignoreCase = true)
    val withScheme = if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) trimmed else "http://$trimmed"
    val uri = try {
        URI(withScheme)
    } catch (e: Exception) {
        // Never log the raw URL itself - a pasted server URL can embed credentials.
        ErrorLog.log("EditServer", "Failed to parse server URL", e)
        return null
    }
    val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
    val port = if (uri.port != -1) uri.port else if (hadHttps) 443 else 80
    val path = uri.rawPath.orEmpty().let { if (it.isBlank() || it == "/") "" else it }
    return ParsedServerUrl(secure = hadHttps, host = host, port = port, path = path)
}

private fun composeServerUrl(secure: Boolean, host: String, port: Int, path: String): String {
    if (host.isBlank()) return ""
    val scheme = if (secure) "https" else "http"
    val standardPort = if (secure) 443 else 80
    val portPart = if (port == standardPort) "" else ":$port"
    val pathPart = path.trim().let { if (it.isBlank()) "" else if (it.startsWith("/")) it else "/$it" }
    return "$scheme://$host$portPart$pathPart"
}

@Composable
private fun DetectedSettingsPanel(secure: Boolean, host: String, port: String, path: String) {
    val known = host.isNotBlank()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                stringResource(R.string.settings_detected_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
            DetectedRow(
                icon = if (secure) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                label = stringResource(R.string.settings_detected_ssl),
                value = if (!known) "—" else stringResource(if (secure) R.string.settings_yes else R.string.settings_no),
                emphasize = known && secure,
            )
            DetectedRow(
                icon = Icons.Rounded.Dns,
                label = stringResource(R.string.settings_detected_host),
                value = host.ifBlank { "—" },
            )
            DetectedRow(
                icon = Icons.Rounded.Numbers,
                label = stringResource(R.string.settings_port),
                value = if (!known) "—" else port,
            )
            DetectedRow(
                icon = Icons.Rounded.Folder,
                label = stringResource(R.string.settings_detected_folder),
                value = if (!known) "—" else path.ifBlank { "—" },
            )
        }
    }
}

@Composable
private fun DetectedRow(
    icon: ImageVector,
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HelpEntry(titleRes: Int, bodyRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun DiscoverySection(state: DiscoveryState, onPick: (DiscoveredDaemon) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.settings_discovery_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            if (state.scanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Text(
                        "  " + stringResource(R.string.settings_discovery_scanning),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            state.found.forEach { daemon ->
                ListItem(
                    headlineContent = { Text(daemon.type.displayName()) },
                    supportingContent = { Text("${daemon.host}:${daemon.port}") },
                    leadingContent = {
                        Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().clickable { onPick(daemon) },
                )
            }
        }
    }
}

/**
 * Collapsed: a prompt offering to fill in the form below from just a hostname and credentials.
 * Expanded: those three fields plus a "Detect settings" action that probes Xirvik's auto-config
 * endpoint for the account's SCGI mount path (see the [XirvikState] flow in [EditServerScreen]).
 */
@Composable
private fun XirvikSetupSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    server: String,
    onServerChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    state: XirvikState,
    onDetect: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.settings_xirvik_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            if (!expanded) {
                Text(stringResource(R.string.settings_xirvik_summary), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onExpandedChange(true) }) {
                    Text(stringResource(R.string.settings_xirvik_action))
                }
            } else {
                OutlinedTextField(
                    value = server,
                    onValueChange = onServerChange,
                    label = { Text(stringResource(R.string.settings_xirvik_server)) },
                    placeholder = { Text(stringResource(R.string.settings_xirvik_server_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.settings_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.settings_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onDetect,
                        enabled = state != XirvikState.Detecting &&
                            server.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.settings_xirvik_detect))
                    }
                    if (state == XirvikState.Detecting) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_xirvik_detecting), style = MaterialTheme.typography.bodySmall)
                    }
                }
                when (state) {
                    is XirvikState.Success -> Text(
                        stringResource(R.string.settings_xirvik_success),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    XirvikState.Failed -> Text(
                        stringResource(R.string.settings_xirvik_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    else -> {}
                }
            }
        }
    }
}
