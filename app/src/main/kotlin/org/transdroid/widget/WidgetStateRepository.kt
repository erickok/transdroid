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

@Serializable
data class WidgetState(
    val serverName: String? = null,
    val downloadingCount: Int = 0,
    val seedingCount: Int = 0,
    val pausedCount: Int = 0,
    val totalCount: Int = 0,
    val downloadRate: Long = 0,
    val uploadRate: Long = 0,
    /** Only currently-active (downloading/seeding) torrents, most recently added first. */
    val torrents: List<WidgetTorrent> = emptyList(),
    val updatedAtMillis: Long? = null,
)

/** Caps how many active torrents are persisted and shown in the widget's scrollable list. */
private const val MAX_WIDGET_TORRENTS = 30

/**
 * A small snapshot of the last successful torrent list per server, written on every refresh
 * (foreground poll, the widget's own refresh button, or the background worker) and read by the
 * home screen widget - each widget instance picks which server's snapshot it shows via
 * [WidgetConfigureActivity]. No credentials are stored here - it is unencrypted preference data -
 * and each server's torrent list is capped and limited to active torrents so a large library
 * doesn't bloat every write.
 */
class WidgetStateRepository(private val context: Context) {

    private val statesKey = stringPreferencesKey("widget_states_json")

    private val json = Json { ignoreUnknownKeys = true }

    /** All servers' snapshots, keyed by server (profile) id. */
    val states: Flow<Map<String, WidgetState>> = context.widgetDataStore.data.map { prefs ->
        prefs[statesKey]?.let { raw ->
            try {
                json.decodeFromString<Map<String, WidgetState>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }

    /** The given server's snapshot, or an empty (no-data) state when [serverId] is null or unknown. */
    suspend fun stateFor(serverId: String?): WidgetState =
        serverId?.let { states.first()[it] } ?: WidgetState()

    @Volatile
    private var lastWritten: Map<String, WidgetState> = emptyMap()

    suspend fun update(serverId: String, serverName: String, torrents: List<Torrent>) {
        val widgetTorrents = torrents
            .filter { it.status.isActive }
            .sortedByDescending { it.addedTimestamp ?: 0L }
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
            updatedAtMillis = System.currentTimeMillis(),
        )
        val current = lastWritten.ifEmpty { states.first() }
        // Each server's list refreshes every few seconds while its app screen is open; skip the
        // write when nothing changed (comparing without the timestamp, which always differs).
        if (current[serverId]?.copy(updatedAtMillis = null) == snapshot.copy(updatedAtMillis = null)) return
        val updated = current + (serverId to snapshot)
        lastWritten = updated
        context.widgetDataStore.edit { prefs -> prefs[statesKey] = json.encodeToString(updated) }
        TransdroidWidget().updateAll(context)
    }
}
