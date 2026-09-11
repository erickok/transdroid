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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentStatus

private val Context.widgetDataStore by preferencesDataStore(name = "widget_state")

/** Only the fields the widget's torrent list rows actually render, kept deliberately small. */
@Serializable
data class WidgetTorrent(
    val id: String,
    val name: String,
    val status: TorrentStatus,
    val progress: Float,
    val downloadRate: Long,
    val uploadRate: Long,
)

data class WidgetState(
    val serverName: String? = null,
    val downloadingCount: Int = 0,
    val seedingCount: Int = 0,
    val pausedCount: Int = 0,
    val totalCount: Int = 0,
    val downloadRate: Long = 0,
    val uploadRate: Long = 0,
    /** Only currently-active (downloading/seeding) torrents - see [MAX_WIDGET_TORRENTS]. */
    val torrents: List<WidgetTorrent> = emptyList(),
    val updatedAtMillis: Long? = null,
)

/** Caps how many active torrents are persisted and shown in the widget's scrollable list. */
private const val MAX_WIDGET_TORRENTS = 30

/**
 * A small snapshot of the last successful torrent list, written on every refresh (foreground
 * or background) and read by the home screen widget. No credentials are stored here - it is
 * unencrypted preference data - and the torrent list is capped and limited to active torrents
 * so a large library doesn't bloat every write.
 */
class WidgetStateRepository(private val context: Context) {

    private val serverKey = stringPreferencesKey("server_name")
    private val downloadingKey = intPreferencesKey("downloading_count")
    private val seedingKey = intPreferencesKey("seeding_count")
    private val pausedKey = intPreferencesKey("paused_count")
    private val totalKey = intPreferencesKey("total_count")
    private val downRateKey = longPreferencesKey("download_rate")
    private val upRateKey = longPreferencesKey("upload_rate")
    private val torrentsKey = stringPreferencesKey("torrents_json")
    private val updatedKey = longPreferencesKey("updated_at")

    private val json = Json { ignoreUnknownKeys = true }

    val state: Flow<WidgetState> = context.widgetDataStore.data.map { prefs ->
        WidgetState(
            serverName = prefs[serverKey],
            downloadingCount = prefs[downloadingKey] ?: 0,
            seedingCount = prefs[seedingKey] ?: 0,
            pausedCount = prefs[pausedKey] ?: 0,
            totalCount = prefs[totalKey] ?: 0,
            downloadRate = prefs[downRateKey] ?: 0,
            uploadRate = prefs[upRateKey] ?: 0,
            torrents = prefs[torrentsKey]?.let { raw ->
                try {
                    json.decodeFromString<List<WidgetTorrent>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList(),
            updatedAtMillis = prefs[updatedKey],
        )
    }

    suspend fun current(): WidgetState = state.first()

    @Volatile
    private var lastWritten: WidgetState? = null

    suspend fun update(serverName: String, torrents: List<Torrent>) {
        val widgetTorrents = torrents
            .filter { it.status.isActive }
            .sortedByDescending { it.downloadRate + it.uploadRate }
            .take(MAX_WIDGET_TORRENTS)
            .map { WidgetTorrent(it.id, it.name, it.status, it.progress, it.downloadRate, it.uploadRate) }
        val snapshot = WidgetState(
            serverName = serverName,
            downloadingCount = torrents.count { it.status == TorrentStatus.DOWNLOADING },
            seedingCount = torrents.count { it.status == TorrentStatus.SEEDING },
            pausedCount = torrents.count { it.status == TorrentStatus.PAUSED },
            totalCount = torrents.size,
            downloadRate = torrents.sumOf { it.downloadRate },
            uploadRate = torrents.sumOf { it.uploadRate },
            torrents = widgetTorrents,
        )
        // The list refreshes every few seconds; skip the write when nothing changed
        if (snapshot == lastWritten) return
        lastWritten = snapshot
        context.widgetDataStore.edit { prefs ->
            prefs[serverKey] = snapshot.serverName!!
            prefs[downloadingKey] = snapshot.downloadingCount
            prefs[seedingKey] = snapshot.seedingCount
            prefs[pausedKey] = snapshot.pausedCount
            prefs[totalKey] = snapshot.totalCount
            prefs[downRateKey] = snapshot.downloadRate
            prefs[upRateKey] = snapshot.uploadRate
            prefs[torrentsKey] = json.encodeToString(snapshot.torrents)
            prefs[updatedKey] = System.currentTimeMillis()
        }
        TransdroidWidget().updateAll(context)
    }
}
