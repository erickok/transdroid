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
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import org.transdroid.BuildConfig
import org.transdroid.R
import org.transdroid.data.RssFeed
import org.transdroid.data.SearchProviderConfig
import org.transdroid.data.SettingsRepository
import org.transdroid.ui.rss.EditFeedDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEditServer: (String?) -> Unit,
    onOpenFeed: (String) -> Unit,
    onBack: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeId by viewModel.activeServerId.collectAsStateWithLifecycle()
    val providers by viewModel.searchProviders.collectAsStateWithLifecycle()
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val notifyFinished by viewModel.notifyFinished.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val searchAvailable = booleanResource(R.bool.search_available)
    val rssAvailable = booleanResource(R.bool.rss_available)

    var editingProvider by remember { mutableStateOf<SearchProviderConfig?>(null) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var deletingFeed by remember { mutableStateOf<RssFeed?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val exportWrittenMessage = stringResource(R.string.backup_export_done)
    val exportFailedMessage = stringResource(R.string.backup_export_failed)
    val restoreWrongPassphrase = stringResource(R.string.backup_wrong_passphrase)
    val restoreInvalid = stringResource(R.string.backup_invalid_file)
    val restoredTemplate = stringResource(R.string.backup_restored)

    val exportCreator = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val bytes = pendingBackup
        pendingBackup = null
        if (uri != null && bytes != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
                    } catch (e: Exception) {
                        false
                    }
                }
                snackbarHostState.showSnackbar(if (ok) exportWrittenMessage else exportFailedMessage)
            }
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri = uri
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Enable regardless; without permission the worker simply cannot post, and the
        // system settings remain the source of truth the user controls.
        viewModel.setNotifyFinished(context, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditServer(null) }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_add_server))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                SettingsSection(stringResource(R.string.settings_servers)) {
                    val effectiveActiveId = activeId ?: profiles.firstOrNull()?.id
                    profiles.forEach { profile ->
                        SettingsRow(
                            icon = Icons.Rounded.Dns,
                            title = profile.displayName,
                            subtitle = "${profile.type.displayName()} · ${profile.host}:${profile.port}",
                            onClick = { onEditServer(profile.id) },
                            trailing = {
                                if (profile.id == effectiveActiveId) {
                                    SettingsChip(stringResource(R.string.settings_active_server))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                        )
                    }
                    SettingsAddRow(
                        title = stringResource(R.string.settings_add_server),
                        onClick = { onEditServer(null) },
                    )
                }
            }

            if (rssAvailable) {
                item {
                    SettingsSection(stringResource(R.string.rss_title)) {
                        feeds.forEach { feed ->
                            SettingsRow(
                                icon = Icons.Rounded.RssFeed,
                                title = feed.displayName,
                                subtitle = feed.url,
                                onClick = { onOpenFeed(feed.id) },
                                trailing = {
                                    IconButton(onClick = { deletingFeed = feed }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = stringResource(R.string.rss_delete_feed),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                            )
                        }
                        SettingsAddRow(
                            title = stringResource(R.string.rss_add_feed),
                            onClick = { showAddFeedDialog = true },
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_preferences)) {
                    val pollInterval by viewModel.pollIntervalSeconds.collectAsStateWithLifecycle()
                    var intervalMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        SettingsRow(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(R.string.settings_poll_interval),
                            subtitle = stringResource(R.string.settings_poll_interval_value, pollInterval),
                            onClick = { intervalMenuOpen = true },
                        )
                        DropdownMenu(expanded = intervalMenuOpen, onDismissRequest = { intervalMenuOpen = false }) {
                            SettingsRepository.POLL_INTERVAL_OPTIONS.forEach { seconds ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_poll_interval_value, seconds)) },
                                    leadingIcon = { RadioButton(selected = seconds == pollInterval, onClick = null) },
                                    onClick = {
                                        viewModel.setPollInterval(seconds)
                                        intervalMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.settings_notify_finished),
                        subtitle = stringResource(R.string.settings_notify_finished_summary),
                        onClick = null,
                        trailing = {
                            Switch(
                                checked = notifyFinished,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= 33) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotifyFinished(context, enabled)
                                    }
                                },
                            )
                        },
                    )
                }
            }

            if (searchAvailable) {
                item {
                    SettingsSection(stringResource(R.string.settings_search_providers)) {
                        providers.forEach { provider ->
                            SettingsRow(
                                icon = Icons.Rounded.Search,
                                title = provider.displayName,
                                subtitle = provider.url,
                                onClick = {
                                    editingProvider = provider
                                    showProviderDialog = true
                                },
                            )
                        }
                        SettingsAddRow(
                            title = stringResource(R.string.settings_add_search_provider),
                            onClick = {
                                editingProvider = null
                                showProviderDialog = true
                            },
                        )
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_backup)) {
                    SettingsRow(
                        icon = Icons.Rounded.Save,
                        title = stringResource(R.string.backup_export),
                        subtitle = stringResource(R.string.backup_export_summary),
                        onClick = { showExportDialog = true },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.SettingsBackupRestore,
                        title = stringResource(R.string.backup_import),
                        subtitle = stringResource(R.string.backup_import_summary),
                        onClick = { importPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_about, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showExportDialog) {
        PassphraseDialog(
            title = stringResource(R.string.backup_export),
            message = stringResource(R.string.backup_export_message),
            confirmLabel = stringResource(R.string.backup_export_confirm),
            onDismiss = { showExportDialog = false },
            onConfirm = { passphrase ->
                showExportDialog = false
                viewModel.createBackup(passphrase) { bytes ->
                    pendingBackup = bytes
                    exportCreator.launch("transdroid-backup.tdbk")
                }
            },
        )
    }

    importUri?.let { uri ->
        PassphraseDialog(
            title = stringResource(R.string.backup_import),
            message = stringResource(R.string.backup_import_message),
            confirmLabel = stringResource(R.string.backup_import_confirm),
            onDismiss = { importUri = null },
            onConfirm = { passphrase ->
                importUri = null
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bytes == null || bytes.size > MAX_BACKUP_BYTES) {
                        snackbarHostState.showSnackbar(restoreInvalid)
                        return@launch
                    }
                    viewModel.restoreBackup(bytes, passphrase) { result ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                when (result) {
                                    is SettingsViewModel.RestoreResult.Success ->
                                        String.format(restoredTemplate, result.serverCount)
                                    SettingsViewModel.RestoreResult.WrongPassphrase -> restoreWrongPassphrase
                                    SettingsViewModel.RestoreResult.InvalidFile -> restoreInvalid
                                }
                            )
                        }
                    }
                }
            },
        )
    }

    if (showProviderDialog) {
        SearchProviderDialog(
            existing = editingProvider,
            onDismiss = { showProviderDialog = false },
            onSave = { provider ->
                viewModel.saveSearchProvider(provider)
                showProviderDialog = false
            },
            onDelete = editingProvider?.let { provider ->
                {
                    viewModel.deleteSearchProvider(provider.id)
                    showProviderDialog = false
                }
            },
        )
    }

    if (showAddFeedDialog) {
        EditFeedDialog(
            onDismiss = { showAddFeedDialog = false },
            onSave = { name, url ->
                viewModel.saveFeed(RssFeed(id = viewModel.newFeedId(), name = name, url = url))
                showAddFeedDialog = false
            },
        )
    }

    deletingFeed?.let { feed ->
        AlertDialog(
            onDismissRequest = { deletingFeed = null },
            title = { Text(stringResource(R.string.rss_delete_feed)) },
            text = { Text(feed.displayName) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFeed(feed.id)
                    deletingFeed = null
                }) { Text(stringResource(R.string.details_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingFeed = null }) { Text(stringResource(R.string.details_cancel)) }
            },
        )
    }
}

private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

@Composable
private fun PassphraseDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(passphrase) }, enabled = passphrase.length >= 4) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
        },
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    trailing: @Composable RowScope.() -> Unit = {
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsAddRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun SearchProviderDialog(
    existing: SearchProviderConfig?,
    onDismiss: () -> Unit,
    onSave: (SearchProviderConfig) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var url by remember { mutableStateOf(existing?.url.orEmpty()) }
    var apiKey by remember { mutableStateOf(existing?.apiKey.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (existing == null) R.string.settings_add_search_provider
                    else R.string.settings_edit_search_provider
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.settings_torznab_url)) },
                    placeholder = { Text(stringResource(R.string.settings_torznab_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.settings_api_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        SearchProviderConfig(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            url = url.trim(),
                            apiKey = apiKey.trim(),
                        )
                    )
                },
                enabled = url.trim().startsWith("http"),
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.details_remove_confirm)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.details_cancel)) }
            }
        },
    )
}
