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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.transdroid.appContainer
import org.transdroid.ui.torrents.TorrentSort

/** Per-widget-instance preference key: which server (profile id) this widget instance shows. */
val WIDGET_SERVER_ID_KEY = stringPreferencesKey("server_id")

/** Per-widget-instance preference keys: the order this widget instance lists its torrents in. */
val WIDGET_SORT_KEY = stringPreferencesKey("sort")
val WIDGET_SORT_DESCENDING_KEY = booleanPreferencesKey("sort_descending")

/** Per-widget-instance preference key: whether paused, queued, errored etc. torrents are listed too. */
val WIDGET_SHOW_INACTIVE_KEY = booleanPreferencesKey("show_inactive")

/**
 * Set by [WidgetConfigureActivity] when a widget instance is (re)configured: the stored snapshot
 * only holds the top torrents for the list options that were in use when it was written, so the
 * widget fetches afresh once (and clears this) to fill in its newly chosen ones.
 */
val WIDGET_NEEDS_REFRESH_KEY = booleanPreferencesKey("needs_refresh")

/**
 * Which torrents a widget instance lists and in what order; the sort fields are the same as the
 * app's torrent list. Without [showInactive] only downloading and seeding torrents are listed.
 */
data class WidgetListOptions(val sort: TorrentSort, val descending: Boolean, val showInactive: Boolean) {

    /** Orders rows like [TorrentSort.comparator]; the exhaustive `when` keeps the two in step. */
    private val comparator: Comparator<WidgetTorrent>
        get() {
            val ascending: Comparator<WidgetTorrent> = when (sort) {
                TorrentSort.DATE_ADDED -> compareBy { it.addedTimestamp ?: Long.MIN_VALUE }
                TorrentSort.NAME -> compareBy { it.name.lowercase() }
                TorrentSort.DOWNLOAD_SPEED -> compareBy { it.downloadRate }
                TorrentSort.RATIO -> compareBy { it.ratio }
            }
            return (if (descending) ascending.reversed() else ascending).thenBy { it.name.lowercase() }
        }

    /** The rows a widget with these options shows, at most [MAX_WIDGET_TORRENTS]. */
    fun select(torrents: List<WidgetTorrent>): List<WidgetTorrent> = torrents
        .filter { showInactive || it.status.isActive }
        .sortedWith(comparator)
        .take(MAX_WIDGET_TORRENTS)

    companion object {
        /** Also what widgets placed before these options existed show: active torrents, newest first. */
        val DEFAULT = WidgetListOptions(TorrentSort.DEFAULT, TorrentSort.DEFAULT.defaultDescending, showInactive = false)

        fun from(prefs: Preferences): WidgetListOptions {
            val sort = prefs[WIDGET_SORT_KEY]?.let { TorrentSort.fromName(it) } ?: DEFAULT.sort
            return WidgetListOptions(
                sort = sort,
                descending = prefs[WIDGET_SORT_DESCENDING_KEY] ?: sort.defaultDescending,
                showInactive = prefs[WIDGET_SHOW_INACTIVE_KEY] ?: DEFAULT.showInactive,
            )
        }
    }
}

/**
 * The server this widget instance is configured to show, set via [WidgetConfigureActivity] when
 * the widget was placed. Falls back to the app's current active server for a widget placed
 * before per-widget configuration existed, or one added while no server was yet configured -
 * both cases match the widget's original (pre-configuration) behavior.
 */
suspend fun resolveWidgetServerId(context: Context, glanceId: GlanceId): String? {
    val configured = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[WIDGET_SERVER_ID_KEY]
    if (configured != null) return configured
    return context.appContainer.activeProfile.first()?.id
}

/** The list options of every currently-placed widget instance, always including [WidgetListOptions.DEFAULT]. */
suspend fun widgetConfiguredListOptions(context: Context): Set<WidgetListOptions> =
    GlanceAppWidgetManager(context).getGlanceIds(TransdroidWidget::class.java)
        .map { id -> WidgetListOptions.from(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)) }
        .toSet() + WidgetListOptions.DEFAULT

/** The distinct set of servers any currently-placed widget instance is explicitly configured for. */
suspend fun widgetConfiguredServerIds(context: Context): Set<String> =
    GlanceAppWidgetManager(context).getGlanceIds(TransdroidWidget::class.java)
        .mapNotNull { id -> getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[WIDGET_SERVER_ID_KEY] }
        .toSet()

/**
 * Fetches [serverId]'s torrents once and stores them as its widget snapshot, which also redraws
 * every widget showing that server. Returns false (leaving the last-known snapshot untouched)
 * when the server is unknown or unreachable.
 */
suspend fun refreshWidgetServer(context: Context, serverId: String): Boolean {
    val container = context.appContainer
    val profile = container.profilesRepository.profiles.first().firstOrNull { it.id == serverId } ?: return false
    val torrents = try {
        container.adapterFor(profile).listTorrents()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return false
    }
    container.widgetStateRepository.update(profile.id, profile.displayName, torrents)
    return true
}
