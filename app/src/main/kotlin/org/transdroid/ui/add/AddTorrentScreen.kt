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
package org.transdroid.ui.add

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.transdroid.R
import org.transdroid.errorlog.ErrorLog
import org.transdroid.ui.message
import org.transdroid.ui.torrents.TorrentsViewModel
import org.transdroid.ui.torrents.UiError

/** .torrent files are tiny; anything larger than this is not one. */
private const val MAX_TORRENT_FILE_BYTES = 10L * 1024 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTorrentScreen(
    viewModel: TorrentsViewModel,
    initialUrl: String,
    onDone: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val initialIsFile = initialUrl.startsWith("content:") || initialUrl.startsWith("file:")
    var url by rememberSaveable { mutableStateOf(if (initialIsFile) "" else initialUrl) }
    var fileUri by rememberSaveable { mutableStateOf(if (initialIsFile) initialUrl else null) }
    var invalidInput by rememberSaveable { mutableStateOf(false) }
    var startPaused by rememberSaveable { mutableStateOf(false) }
    // Deliberately not saveable: the completion callback writes to this composition's state,
    // so restoring `true` across recreation would leave the button disabled forever
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiError?>(null) }
    var fileReadFailed by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri.toString()
            invalidInput = false
        }
    }

    fun submit() {
        val pickedFile = fileUri
        error = null
        fileReadFailed = false
        if (pickedFile != null) {
            submitting = true
            scope.launch {
                val contents = withContext(Dispatchers.IO) { readTorrentFile(context, Uri.parse(pickedFile)) }
                if (contents == null) {
                    submitting = false
                    fileReadFailed = true
                } else {
                    viewModel.addFile(contents.first, contents.second, startPaused) { result ->
                        submitting = false
                        if (result == null) onDone() else error = result
                    }
                }
            }
            return
        }
        val trimmed = url.trim()
        val valid = trimmed.startsWith("magnet:") ||
            trimmed.startsWith("http://") || trimmed.startsWith("https://")
        if (!valid) {
            invalidInput = true
        } else {
            submitting = true
            viewModel.add(trimmed, startPaused) { result ->
                submitting = false
                if (result == null) onDone() else error = result
            }
        }
    }

    // With a single configured server there is nothing to choose: a picked or opened
    // .torrent file is added right away instead of asking for another confirming tap.
    // (The paused checkbox sits above the picker, so that choice still comes first.)
    LaunchedEffect(fileUri, ui.profileCount) {
        if (fileUri != null && ui.profileCount == 1 && !submitting && error == null && !fileReadFailed) {
            submit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { submit() },
                        enabled = !submitting && ui.activeProfile != null && (url.isNotBlank() || fileUri != null),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Text(stringResource(R.string.add_button, ui.activeProfile?.displayName ?: ""))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            val pickedFile = fileUri
            if (pickedFile == null) {
                MagnetUrlField(
                    value = url,
                    onValueChange = {
                        url = it
                        invalidInput = false
                    },
                    isError = invalidInput,
                    onPaste = {
                        clipboard.getText()?.text?.let {
                            url = it
                            invalidInput = false
                        }
                    },
                )
                if (invalidInput) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.add_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(16.dp))
                OrDivider()
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        filePicker.launch(arrayOf("application/x-bittorrent", "application/octet-stream"))
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.add_pick_file))
                }
            } else {
                PickedFileChip(
                    name = displayName(context, Uri.parse(pickedFile)),
                    onRemove = { fileUri = null },
                )
            }
            Spacer(Modifier.height(18.dp))
            AddPausedRow(startPaused) { startPaused = it }
            if (fileReadFailed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.add_file_read_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it.message(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Matches the mockup's `.field`: a filled pill with no visible border at rest that gains a full
 * accent-colored outline while focused, a static label row (with an inline Paste action, unlike
 * a standard Material label) above a leading-icon input row - too bespoke a layout for the
 * generic [org.transdroid.ui.components.TransdroidTextField].
 */
@Composable
private fun MagnetUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    onPaste: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val containerColor = if (focused) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
            .border(1.5.dp, borderColor, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.add_url_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onPaste, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.height(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.add_paste), style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 7.dp).height(20.dp),
            )
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.add_url_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = 2,
                    maxLines = 6,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.add_or).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Matches the mockup's `.filechip`: a primary-container pill replacing the choose-file button. */
@Composable
private fun PickedFileChip(name: String, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.height(38.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.details_cancel),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Matches the mockup's `.optrow`: a surface-container card with a title/subtitle and a switch. */
@Composable
private fun AddPausedRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.add_paused),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.add_paused_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Reads the picked .torrent file, returning (fileName, contents) or null on failure. Reads
 * at most [MAX_TORRENT_FILE_BYTES] so a mistakenly picked huge file cannot exhaust memory.
 */
private fun readTorrentFile(context: Context, uri: Uri): Pair<String, ByteArray>? = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_TORRENT_FILE_BYTES) return null
            output.write(buffer, 0, read)
        }
        val contents = output.toByteArray()
        if (contents.isEmpty()) null else displayName(context, uri) to contents
    }
} catch (e: Exception) {
    ErrorLog.log("AddTorrent", "Failed to read torrent file", e)
    null
}

private fun displayName(context: Context, uri: Uri): String {
    try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.let { return it }
            }
        }
    } catch (e: Exception) {
        // Fall through to the path-based name
        ErrorLog.log("AddTorrent", "Failed to resolve display name", e)
    }
    return uri.lastPathSegment ?: "file.torrent"
}
