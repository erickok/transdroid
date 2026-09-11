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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first
import org.transdroid.appContainer

/** Per-widget-instance preference key: which server (profile id) this widget instance shows. */
val WIDGET_SERVER_ID_KEY = stringPreferencesKey("server_id")

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

/** The distinct set of servers any currently-placed widget instance is explicitly configured for. */
suspend fun widgetConfiguredServerIds(context: Context): Set<String> =
    GlanceAppWidgetManager(context).getGlanceIds(TransdroidWidget::class.java)
        .mapNotNull { id -> getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[WIDGET_SERVER_ID_KEY] }
        .toSet()
