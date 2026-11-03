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

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.net.URI
import org.transdroid.R
import org.transdroid.data.ServerProfile
import org.transdroid.discovery.CurrentSsid
import org.transdroid.errorlog.ErrorLog
import org.transdroid.protocol.DaemonType
import org.transdroid.protocol.discovery.DiscoveredDaemon
import org.transdroid.ui.message
import org.transdroid.ui.torrents.UiError
import org.transdroid.ui.components.TransdroidTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerScreen(
    viewModel: SettingsViewModel,
    serverId: String?,
    onBack: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val discovery by viewModel.discovery.collectAsStateWithLifecycle()
    val xirvikState by viewModel.xirvikState.collectAsStateWithLifecycle()
    val existing = profiles.firstOrNull { it.id == serverId }
    // Keyed on the loaded profile: the profiles flow hasn't emitted on first composition
    val form = rememberSaveable(existing?.id, saver = EditServerFormState.Saver) { EditServerFormState(existing) }
    var showHelp by remember { mutableStateOf(false) }

    // Xirvik seedbox quick setup (new servers only): a hostname + credentials probe against
    // Xirvik's auto-config endpoint to discover the account's per-user SCGI mount path, mirroring
    // Transdroid 2's XirvikSettingsActivity/RetrieveXirvikAutoConfTask. A hit or a miss both fill in
    // the rest of the form below, exactly like picking a LAN-discovered daemon does.
    LaunchedEffect(xirvikState) {
        when (val state = xirvikState) {
            is XirvikState.Success -> form.applyXirvik(state.path, found = true)
            XirvikState.Failed -> form.applyXirvik("", found = false)
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetTestState() }
    }

    // Adding a new server: look around the local network for daemons to offer.
    // Keyed on the nav argument, not `existing` — the profiles flow hasn't emitted on
    // first composition, so `existing` is briefly null even when editing.
    if (serverId == null) {
        LaunchedEffect(Unit) { viewModel.startLanScan() }
    }

    if (showHelp) {
        ConnectionHelpDialog(onDismiss = { showHelp = false })
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
                DiscoverySection(state = discovery, onPick = form::applyDiscovered)
            }
            if (serverId == null) {
                XirvikSetupSection(
                    expanded = form.xirvikExpanded,
                    onExpandedChange = { form.xirvikExpanded = it },
                    server = form.xirvikServer,
                    onServerChange = { form.xirvikServer = it },
                    username = form.xirvikUser,
                    onUsernameChange = { form.xirvikUser = it },
                    password = form.xirvikPass,
                    onPasswordChange = { form.xirvikPass = it },
                    state = xirvikState,
                    onDetect = { viewModel.detectXirvik(form.xirvikServer.trim(), form.xirvikUser.trim(), form.xirvikPass) },
                )
            }
            ConnectionSection(form)
            CredentialsSection(form)
            LocalNetworkFormSection(form)
            TestAndSaveSection(
                form = form,
                viewModel = viewModel,
                profileId = { serverId ?: viewModel.newProfileId() },
                onSaved = onBack,
            )
        }
    }
}

@Composable
private fun ConnectionHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_help_close)) }
        },
    )
}

/** Name, client type and where to reach it: a single URL, or the raw parts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionSection(form: EditServerFormState) {
    Text(
        stringResource(R.string.settings_server_section_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TransdroidTextField(
        value = form.name,
        onValueChange = { form.name = it },
        label = { Text(stringResource(R.string.settings_name)) },
        leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    ClientTypeMenu(selected = form.type, onSelect = form::selectType)

    if (form.type == DaemonType.RTORRENT) {
        Text(
            stringResource(R.string.settings_rtorrent_how),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.rtorrentUseUrl,
                onClick = { form.rtorrentUseUrl = true },
                label = { Text(stringResource(R.string.settings_rtorrent_mode_url)) },
            )
            FilterChip(
                selected = !form.rtorrentUseUrl,
                onClick = form::useScgi,
                label = { Text(stringResource(R.string.settings_rtorrent_mode_scgi)) },
            )
        }
    }

    if (form.showUrlSection) {
        TransdroidTextField(
            value = form.url,
            onValueChange = form::applyParsedUrl,
            label = {
                Text(
                    stringResource(
                        if (form.type == DaemonType.RTORRENT) R.string.settings_rturl_label else R.string.settings_url_label
                    )
                )
            },
            placeholder = { Text(stringResource(R.string.settings_url_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            singleLine = true,
            isError = form.hostError,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(
                if (form.type == DaemonType.RTORRENT) R.string.settings_rturl_hint else R.string.settings_url_hint,
                form.type.displayName(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!form.overridden) {
            DetectedSettingsPanel(secure = form.useSsl, host = form.host, port = form.port, path = form.path)
            TextButton(onClick = { form.toggleOverride(true) }) {
                Text(stringResource(R.string.settings_override_parts))
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (form.showManualFields) {
        ManualConnectionFields(form)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientTypeMenu(selected: DaemonType, onSelect: (DaemonType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TransdroidTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_type)) },
            leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DaemonType.entries.forEach { daemonType ->
                DropdownMenuItem(
                    text = { Text(daemonType.displayName()) },
                    onClick = {
                        onSelect(daemonType)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Host, port, HTTPS and path as separate fields: "override individual parts", or rTorrent SCGI. */
@Composable
private fun ManualConnectionFields(form: EditServerFormState) {
    TransdroidTextField(
        value = form.host,
        onValueChange = form::changeHost,
        label = { Text(stringResource(if (form.usesScgi) R.string.settings_scgi_host else R.string.settings_host)) },
        leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
        isError = form.hostError,
        supportingText = if (form.hostError) {
            { Text(stringResource(R.string.settings_invalid_host)) }
        } else {
            null
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TransdroidTextField(
        value = form.port,
        onValueChange = form::changePort,
        label = { Text(stringResource(if (form.usesScgi) R.string.settings_scgi_port else R.string.settings_port)) },
        leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
        isError = form.portError,
        supportingText = {
            Text(stringResource(if (form.portError) R.string.settings_invalid_port else R.string.settings_port_hint))
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (!form.usesScgi) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = form.useSsl, onCheckedChange = form::changeSsl)
            Spacer(Modifier.fillMaxWidth(0.05f))
            Text(stringResource(R.string.settings_use_ssl))
        }
    }
    TransdroidTextField(
        value = form.path,
        onValueChange = { form.path = it },
        label = { Text(stringResource(if (form.usesScgi) R.string.settings_scgi_path else R.string.settings_path)) },
        placeholder = {
            Text(
                stringResource(
                    when (form.type) {
                        DaemonType.TRANSMISSION -> R.string.settings_path_hint_transmission
                        DaemonType.QBITTORRENT -> R.string.settings_path_hint_qbittorrent
                        DaemonType.RTORRENT -> R.string.settings_path_hint_rtorrent
                        DaemonType.DELUGE -> R.string.settings_path_hint_deluge
                    }
                )
            )
        },
        leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (form.usesScgi) {
        Text(
            stringResource(R.string.settings_scgi_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Username, password, qBittorrent's API key and custom headers. */
@Composable
private fun CredentialsSection(form: EditServerFormState) {
    var passwordVisible by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    TransdroidTextField(
        value = form.username,
        onValueChange = { form.username = it },
        label = { Text(stringResource(R.string.settings_username)) },
        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TransdroidTextField(
        value = form.password,
        onValueChange = { form.password = it },
        label = { Text(stringResource(R.string.settings_password)) },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        trailingIcon = { PasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (form.type == DaemonType.QBITTORRENT) {
        TransdroidTextField(
            value = form.apiKey,
            onValueChange = { form.apiKey = it },
            label = { Text(stringResource(R.string.settings_qbittorrent_api_key)) },
            supportingText = { Text(stringResource(R.string.settings_qbittorrent_api_key_summary)) },
            leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
            trailingIcon = { PasswordVisibilityToggle(visible = apiKeyVisible, onToggle = { apiKeyVisible = !apiKeyVisible }) },
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    TransdroidTextField(
        value = form.customHeaders,
        onValueChange = { form.customHeaders = it },
        label = { Text(stringResource(R.string.settings_custom_headers)) },
        placeholder = { Text(stringResource(R.string.settings_custom_headers_hint)) },
        supportingText = { Text(stringResource(R.string.settings_custom_headers_summary)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The local Wi-Fi override, including reading the current network's name. */
@Composable
private fun LocalNetworkFormSection(form: EditServerFormState) {
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) CurrentSsid.read(context)?.let(form::addSsid)
    }
    LocalNetworkSection(
        enabled = form.localNetworkEnabled,
        onEnabledChange = { form.localNetworkEnabled = it },
        ssids = form.localSsids,
        ssidDraft = form.localSsidDraft,
        onSsidDraftChange = { form.localSsidDraft = it },
        onAddSsid = form::addSsidDraft,
        onRemoveSsid = form::removeSsid,
        onUseCurrentSsid = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                CurrentSsid.read(context)?.let(form::addSsid)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
        url = form.localUrl,
        onUrlChange = form::applyParsedLocalUrl,
        secure = form.localUseSsl,
        host = form.localHost,
        port = form.localPort,
        path = form.localPath,
        username = form.localUsername,
        onUsernameChange = { form.localUsername = it },
        password = form.localPassword,
        onPasswordChange = { form.localPassword = it },
    )
}

/** The pinned certificate, connection test results and the Test/Save buttons. */
@Composable
private fun TestAndSaveSection(
    form: EditServerFormState,
    viewModel: SettingsViewModel,
    profileId: () -> String,
    onSaved: () -> Unit,
) {
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val certificateState by viewModel.certificateState.collectAsStateWithLifecycle()

    if (form.pinnedCert.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    R.string.settings_cert_pinned,
                    form.pinnedCert.uppercase().chunked(2).take(8).joinToString(":"),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { form.pinnedCert = "" }) {
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
            if (state.error == UiError.Ssl && form.useSsl) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        if (form.validate()) {
                            viewModel.fetchCertificate(form.host.trim(), form.port.toIntOrNull() ?: form.type.defaultSslPort)
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
                    form.pinnedCert = certState.fingerprint.sha256
                    viewModel.dismissCertificate()
                    viewModel.testConnection(form.toProfile(profileId()))
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
            onClick = { if (form.validate()) viewModel.testConnection(form.toProfile(profileId())) },
            enabled = testState != TestState.Testing,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.settings_test))
        }
        Button(
            onClick = {
                if (form.validate()) {
                    viewModel.save(form.toProfile(profileId()))
                    onSaved()
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.settings_save))
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
internal data class ParsedServerUrl(val secure: Boolean, val host: String, val port: Int, val path: String)

internal fun parseServerUrl(raw: String): ParsedServerUrl? {
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

internal fun composeServerUrl(secure: Boolean, host: String, port: Int, path: String): String {
    if (host.isBlank()) return ""
    val scheme = if (secure) "https" else "http"
    val standardPort = if (secure) 443 else 80
    val portPart = if (port == standardPort) "" else ":$port"
    val pathPart = path.trim().let { if (it.isBlank()) "" else if (it.startsWith("/")) it else "/$it" }
    // A bare IPv6 literal must be bracketed to be a valid URL authority, as in DaemonConfig.baseUrl
    val authorityHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
    return "$scheme://$authorityHost$portPart$pathPart"
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
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = stringResource(
                if (visible) R.string.settings_hide_password else R.string.settings_show_password
            ),
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
 * Connect using a different address (and optionally login) while the device is on one of the chosen
 * Wi-Fi networks - e.g. a seedbox that's also reachable directly over the LAN. See
 * [ServerProfile.toDaemonConfig].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalNetworkSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    ssids: List<String>,
    ssidDraft: String,
    onSsidDraftChange: (String) -> Unit,
    onAddSsid: () -> Unit,
    onRemoveSsid: (String) -> Unit,
    onUseCurrentSsid: () -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    secure: Boolean,
    host: String,
    port: String,
    path: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
) {
    Text(
        stringResource(R.string.settings_local_network_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_local_network_toggle), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.settings_local_network_toggle_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
    if (enabled) {
        var localPasswordVisible by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TransdroidTextField(
                value = ssidDraft,
                onValueChange = onSsidDraftChange,
                label = { Text(stringResource(R.string.settings_local_ssid)) },
                placeholder = { Text(stringResource(R.string.settings_local_ssid_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Wifi, contentDescription = null) },
                trailingIcon = if (ssidDraft.isNotBlank()) {
                    {
                        IconButton(onClick = onAddSsid) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_local_add_network))
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddSsid() }),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUseCurrentSsid) {
                Text(stringResource(R.string.settings_local_use_current))
            }
        }
        if (ssids.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ssids.forEach { ssid ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveSsid(ssid) },
                        label = { Text(ssid) },
                        leadingIcon = { Icon(Icons.Rounded.Wifi, contentDescription = null, Modifier.size(InputChipDefaults.IconSize)) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.settings_local_remove_ssid, ssid),
                                Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
        Text(
            stringResource(R.string.settings_local_ssid_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.settings_local_override_address),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TransdroidTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(stringResource(R.string.settings_local_url_label)) },
            placeholder = { Text(stringResource(R.string.settings_local_url_placeholder)) },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DetectedSettingsPanel(secure = secure, host = host, port = port, path = path)
        Text(
            stringResource(R.string.settings_local_override_login),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TransdroidTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.settings_local_username)) },
            placeholder = { Text(stringResource(R.string.settings_local_login_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TransdroidTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.settings_local_password)) },
            placeholder = { Text(stringResource(R.string.settings_local_login_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                PasswordVisibilityToggle(
                    visible = localPasswordVisible,
                    onToggle = { localPasswordVisible = !localPasswordVisible },
                )
            },
            visualTransformation = if (localPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Adds [ssid] (trimmed) to [existing] unless it is blank or already listed. */
internal fun appendSsid(existing: List<String>, ssid: String): List<String> {
    val normalizedSsid = ssid.trim()
    return if (normalizedSsid.isBlank() || normalizedSsid in existing) existing else existing + normalizedSsid
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
                var xirvikPasswordVisible by remember { mutableStateOf(false) }
                TransdroidTextField(
                    value = server,
                    onValueChange = onServerChange,
                    label = { Text(stringResource(R.string.settings_xirvik_server)) },
                    placeholder = { Text(stringResource(R.string.settings_xirvik_server_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TransdroidTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.settings_username)) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TransdroidTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.settings_password)) },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = xirvikPasswordVisible,
                            onToggle = { xirvikPasswordVisible = !xirvikPasswordVisible },
                        )
                    },
                    visualTransformation = if (xirvikPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
