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
package org.transdroid

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.transdroid.background.FinishedTorrentsWorker
import org.transdroid.debug.DebugTools
import org.transdroid.errorlog.ErrorLog
import org.transdroid.data.ServerProfile
import org.transdroid.discovery.LanDiscovery
import org.transdroid.data.ServerProfilesRepository
import org.transdroid.data.SettingsRepository
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonAdapterFactory
import org.transdroid.protocol.rss.RssFetcher
import org.transdroid.widget.WidgetStateRepository

/** Lightweight manual dependency container; see the v3 plan's "keep DI light" decision. */
class AppContainer(context: Context) {

    val profilesRepository = ServerProfilesRepository(context)
    val settingsRepository = SettingsRepository(context)
    val widgetStateRepository = WidgetStateRepository(context)
    val lanDiscovery = LanDiscovery(context)

    val httpClient = DaemonAdapterFactory.defaultHttpClient()
    val rssFetcher = RssFetcher(httpClient)

    private var cachedAdapter: Pair<ServerProfile, DaemonAdapter>? = null

    /** The profile torrents are loaded from: the selected one, or the first configured. */
    val activeProfile: Flow<ServerProfile?> =
        combine(profilesRepository.profiles, settingsRepository.activeServerId) { profiles, activeId ->
            profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        }

    /**
     * Returns a (cached) adapter for [profile]; adapters keep session state like auth cookies.
     * [DebugTools.adapterFor] intercepts debug-only dummy test servers before this ever builds
     * a real network-backed adapter - a no-op in release builds, see that object's doc comment.
     */
    @Synchronized
    fun adapterFor(profile: ServerProfile): DaemonAdapter {
        cachedAdapter?.let { (cachedProfile, adapter) ->
            if (cachedProfile == profile) return adapter
        }
        val adapter = DebugTools.adapterFor(profile) ?: DaemonAdapterFactory.create(profile.toDaemonConfig(), httpClient)
        cachedAdapter = profile to adapter
        return adapter
    }

    /** An uncached adapter for testing yet-unsaved connection settings. */
    fun adapterForTest(profile: ServerProfile): DaemonAdapter =
        DebugTools.adapterFor(profile) ?: DaemonAdapterFactory.create(profile.toDaemonConfig(), httpClient)
}

class TransdroidApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ErrorLog.init(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            ErrorLog.logCrash(throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        container = AppContainer(this)
        // Re-arm after updates/reboots; KEEP is idempotent. Always scheduled, not just when
        // finished-torrent notifications are on: it's also what keeps a widget pointed at a
        // non-active server fresh in the background - see FinishedTorrentsWorker.doWork().
        FinishedTorrentsWorker.schedule(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TransdroidApplication).container
