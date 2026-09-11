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
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.transdroid.appContainer

/**
 * Runs when the widget's own refresh button is tapped: a one-off poll of this widget instance's
 * configured server, independent of the app's foreground poll loop or the 15-minute background
 * worker. Mirrors [org.transdroid.background.FinishedTorrentsWorker]'s fetch-and-store pattern,
 * minus the finished-torrent notification (that stays the worker's job).
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = context.appContainer
        val serverId = resolveWidgetServerId(context, glanceId) ?: return
        val profile = container.profilesRepository.profiles.first().firstOrNull { it.id == serverId } ?: return
        val torrents = try {
            container.adapterFor(profile).listTorrents()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Server unreachable right now; the widget just keeps showing its last-known state
            return
        }
        // update() also triggers TransdroidWidget().updateAll(context)
        container.widgetStateRepository.update(profile.id, profile.displayName, torrents)
    }
}
