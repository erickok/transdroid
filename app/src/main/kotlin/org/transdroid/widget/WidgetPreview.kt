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
package org.transdroid.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import org.transdroid.BuildConfig
import org.transdroid.R
import org.transdroid.appContainer
import org.transdroid.protocol.TorrentStatus

/** Made-up torrents for the widget picker's preview; see [TransdroidWidget.providePreview]. */
fun widgetPreviewState(context: Context): WidgetState = WidgetState(
    serverName = context.getString(R.string.widget_preview_server_name),
    downloadingCount = 2,
    seedingCount = 6,
    pausedCount = 6,
    totalCount = 14,
    downloadRate = 4_400_000,
    uploadRate = 8_200_000,
    torrents = listOf(
        WidgetTorrent("1", "ubuntu-26.04-desktop-amd64.iso", TorrentStatus.DOWNLOADING, 0.38f, 4_100_000, 120_000, addedTimestamp = 3),
        WidgetTorrent("2", "debian-13.1.0-amd64-DVD-1.iso", TorrentStatus.DOWNLOADING, 0.72f, 310_000, 45_000, addedTimestamp = 2),
        WidgetTorrent("3", "Big Buck Bunny (2008) 4K", TorrentStatus.SEEDING, 1f, 0, 820_000, addedTimestamp = 1),
    ),
    updatedAtMillis = 0,
)

/**
 * Hands the launcher the widget's generated picker preview (Android 15+; older versions show the
 * static @drawable/widget_preview image from widget_info.xml). Previews persist and the call is
 * rate limited, so it is only made once per app version, and retried on a later start if it fails.
 */
suspend fun publishWidgetPreviews(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    val repository = context.appContainer.widgetStateRepository
    if (repository.previewsPublishedForVersion() == BuildConfig.VERSION_CODE) return
    val result = GlanceAppWidgetManager(context).setWidgetPreviews(TransdroidWidgetReceiver::class)
    if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
        repository.setPreviewsPublishedForVersion(BuildConfig.VERSION_CODE)
    }
}
