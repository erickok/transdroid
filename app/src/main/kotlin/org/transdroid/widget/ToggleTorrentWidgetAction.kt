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
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.transdroid.R
import org.transdroid.appContainer

val WidgetTorrentIdKey = ActionParameters.Key<String>("widget_torrent_id")
val WidgetTorrentCanStartKey = ActionParameters.Key<Boolean>("widget_torrent_can_start")

/**
 * A row's play/pause button: starts or pauses one torrent on this widget instance's server,
 * then refreshes the snapshot like [RefreshWidgetAction]. A widget has no room for an error
 * banner, so the tap is confirmed and any failure reported with a toast.
 */
class ToggleTorrentWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val torrentId = parameters[WidgetTorrentIdKey] ?: return
        val canStart = parameters[WidgetTorrentCanStartKey] ?: return
        val container = context.appContainer
        val serverId = resolveWidgetServerId(context, glanceId) ?: return
        val profile = container.profilesRepository.profiles.first().firstOrNull { it.id == serverId } ?: return
        toast(context, context.getString(if (canStart) R.string.widget_starting else R.string.widget_pausing))
        try {
            val adapter = container.adapterFor(profile)
            if (canStart) adapter.start(torrentId) else adapter.pause(torrentId)
            container.widgetStateRepository.update(profile.id, profile.displayName, adapter.listTorrents())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast(context, context.getString(R.string.widget_action_failed, e.message ?: e.javaClass.simpleName))
        }
    }

    private suspend fun toast(context: Context, text: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
