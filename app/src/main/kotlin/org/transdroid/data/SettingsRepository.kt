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
package org.transdroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Non-sensitive app preferences. */
class SettingsRepository(private val context: Context) {

    private val activeServerKey = stringPreferencesKey("active_server_id")
    private val notifyFinishedKey = booleanPreferencesKey("notify_finished")
    private val pollIntervalKey = intPreferencesKey("poll_interval_seconds")

    val activeServerId: Flow<String?> = context.settingsDataStore.data.map { it[activeServerKey] }

    suspend fun setActiveServer(profileId: String?) {
        context.settingsDataStore.edit { settings ->
            if (profileId == null) settings.remove(activeServerKey) else settings[activeServerKey] = profileId
        }
    }

    /** How often the torrents list refreshes while on screen, in seconds. */
    val pollIntervalSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[pollIntervalKey] ?: DEFAULT_POLL_INTERVAL_SECONDS }

    suspend fun setPollIntervalSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[pollIntervalKey] = seconds.coerceIn(POLL_INTERVAL_OPTIONS.first(), POLL_INTERVAL_OPTIONS.last()) }
    }

    /** Whether the background finished-torrent check and its notifications are enabled. */
    val notifyFinished: Flow<Boolean> = context.settingsDataStore.data.map { it[notifyFinishedKey] ?: false }

    suspend fun setNotifyFinished(enabled: Boolean) {
        context.settingsDataStore.edit { it[notifyFinishedKey] = enabled }
    }

    /** Ids of torrents seen unfinished on the last background check, per server profile. */
    fun unfinishedTorrentIds(profileId: String): Flow<Set<String>> =
        context.settingsDataStore.data.map { it[unfinishedKey(profileId)] ?: emptySet() }

    suspend fun setUnfinishedTorrentIds(profileId: String, ids: Set<String>) {
        context.settingsDataStore.edit { it[unfinishedKey(profileId)] = ids }
    }

    private fun unfinishedKey(profileId: String) = stringSetPreferencesKey("unfinished_ids_$profileId")

    /**
     * Download locations the user has previously typed or picked when adding a torrent to this
     * server, most-recently-used first - shown on the add-torrent screen as quick-pick chips so a
     * full path rarely needs retyping. A plain delimited string (not a string set) because, unlike
     * [unfinishedTorrentIds], the order itself is the point.
     */
    fun recentDownloadLocations(profileId: String): Flow<List<String>> =
        context.settingsDataStore.data.map { prefs ->
            prefs[recentLocationsKey(profileId)]?.split(RECENT_LOCATIONS_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun addRecentDownloadLocation(profileId: String, location: String) {
        if (location.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val key = recentLocationsKey(profileId)
            val existing = prefs[key]?.split(RECENT_LOCATIONS_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(location) + existing.filterNot { it == location }).take(MAX_RECENT_LOCATIONS)
            prefs[key] = updated.joinToString(RECENT_LOCATIONS_SEPARATOR)
        }
    }

    private fun recentLocationsKey(profileId: String) = stringPreferencesKey("recent_locations_$profileId")

    companion object {
        const val DEFAULT_POLL_INTERVAL_SECONDS = 5
        val POLL_INTERVAL_OPTIONS = listOf(3, 5, 10, 30, 60)
        private const val MAX_RECENT_LOCATIONS = 8

        // A control character that cannot appear in a typed filesystem path.
        private const val RECENT_LOCATIONS_SEPARATOR = ""
    }
}
