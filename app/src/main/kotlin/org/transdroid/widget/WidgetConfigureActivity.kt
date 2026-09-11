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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.transdroid.R
import org.transdroid.appContainer
import org.transdroid.data.ServerProfile
import org.transdroid.ui.theme.TransdroidTheme

/**
 * Shown when a widget instance is first placed, letting the user pick which server it should
 * track. Auto-picks and skips the UI when there are zero or one configured servers - nothing
 * to pick between. Standard AppWidget configuration activity: see widget_info.xml's
 * android:configure and this app's ACTION_APPWIDGET_CONFIGURE manifest entry.
 */
class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The host removes the pending widget unless this is overwritten with RESULT_OK below.
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TransdroidTheme {
                ConfigureContent(
                    onPick = { profile -> finishWithChoice(profile) },
                    onNoChoiceNeeded = { finishWithChoice(null) },
                )
            }
        }
    }

    private fun finishWithChoice(profile: ServerProfile?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            if (profile != null) {
                updateAppWidgetState(this@WidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply { this[WIDGET_SERVER_ID_KEY] = profile.id }
                }
            }
            TransdroidWidget().update(this@WidgetConfigureActivity, glanceId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConfigureContent(onPick: (ServerProfile) -> Unit, onNoChoiceNeeded: () -> Unit) {
        var profiles by remember { mutableStateOf<List<ServerProfile>?>(null) }
        LaunchedEffect(Unit) {
            profiles = applicationContext.appContainer.profilesRepository.profiles.first()
        }
        val loaded = profiles ?: return
        when {
            loaded.isEmpty() -> LaunchedEffect(Unit) { onNoChoiceNeeded() }
            loaded.size == 1 -> LaunchedEffect(Unit) { onPick(loaded.first()) }
            else -> Scaffold(
                topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_configure_title)) }) },
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    Text(
                        stringResource(R.string.widget_configure_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    LazyColumn {
                        items(loaded) { profile ->
                            ListItem(
                                headlineContent = { Text(profile.displayName) },
                                supportingContent = { Text(profile.host) },
                                leadingContent = {
                                    Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth().clickable { onPick(profile) },
                            )
                        }
                    }
                }
            }
        }
    }
}
