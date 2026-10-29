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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.transdroid.R
import org.transdroid.data.legacy.LegacyImportSummary
import org.transdroid.data.legacy.LegacySkipReason

/** Tells the user, once, what came over from Transdroid 2 and what needs their attention. */
@Composable
fun LegacyImportDialog(summary: LegacyImportSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.legacy_import_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.legacy_import_counts, summary.serverCount, summary.feedCount))
                if (summary.needsCertificate.isNotEmpty()) {
                    BulletedList(stringResource(R.string.legacy_import_certificates), summary.needsCertificate)
                }
                if (summary.skipped.isNotEmpty()) {
                    BulletedList(
                        stringResource(R.string.legacy_import_skipped),
                        summary.skipped.map { server ->
                            stringResource(
                                when (server.reason) {
                                    LegacySkipReason.UNSUPPORTED_CLIENT -> R.string.legacy_skip_unsupported_client
                                    LegacySkipReason.DELUGE_DAEMON -> R.string.legacy_skip_deluge_daemon
                                    LegacySkipReason.UNSUPPORTED_SEEDBOX -> R.string.legacy_skip_seedbox
                                },
                                server.name,
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.legacy_import_dismiss)) }
        },
    )
}

@Composable
private fun BulletedList(heading: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(heading)
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}
