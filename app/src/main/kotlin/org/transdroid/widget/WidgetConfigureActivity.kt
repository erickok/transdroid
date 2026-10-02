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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.transdroid.R
import org.transdroid.appContainer
import org.transdroid.data.ServerProfile
import org.transdroid.ui.theme.TransdroidTheme
import org.transdroid.ui.torrents.TorrentSort
import org.transdroid.ui.torrents.label

/**
 * Shown when a widget instance is first placed, and when it is reconfigured from the launcher
 * (Android 12+), letting the user pick which server it tracks, how its torrents are sorted and
 * whether inactive (paused, queued, errored etc.) torrents are listed too. Laid out after
 * design/mockups/transdroid-m3-widget-config.html (and -dark.html). The server choice is left out with only one configured server; with none at all there is
 * nothing to show, so it finishes straight away. Standard AppWidget configuration activity: see
 * widget_info.xml's android:configure and this app's ACTION_APPWIDGET_CONFIGURE manifest entry.
 */
class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The host removes a newly placed widget unless this is overwritten with RESULT_OK below.
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            TransdroidTheme {
                ConfigureContent(
                    onDone = { profile, listOptions -> finishWithChoice(profile, listOptions) },
                    onNoChoiceNeeded = { finishWithChoice(null, null) },
                )
            }
        }
    }

    private fun finishWithChoice(profile: ServerProfile?, listOptions: WidgetListOptions?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            if (profile != null && listOptions != null) {
                updateAppWidgetState(this@WidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WIDGET_SERVER_ID_KEY] = profile.id
                        this[WIDGET_SORT_KEY] = listOptions.sort.name
                        this[WIDGET_SORT_DESCENDING_KEY] = listOptions.descending
                        this[WIDGET_SHOW_INACTIVE_KEY] = listOptions.showInactive
                        this[WIDGET_NEEDS_REFRESH_KEY] = true
                    }
                }
            }
            TransdroidWidget().update(this@WidgetConfigureActivity, glanceId)
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }

    /** What the picker starts from: this instance's current settings when it is reconfigured. */
    private data class Initial(val profiles: List<ServerProfile>, val serverId: String?, val listOptions: WidgetListOptions)

    @Composable
    private fun ConfigureContent(
        onDone: (ServerProfile, WidgetListOptions) -> Unit,
        onNoChoiceNeeded: () -> Unit,
    ) {
        var initial by remember { mutableStateOf<Initial?>(null) }
        LaunchedEffect(Unit) {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(this@WidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId)
            initial = Initial(
                profiles = applicationContext.appContainer.profilesRepository.profiles.first(),
                serverId = prefs[WIDGET_SERVER_ID_KEY] ?: applicationContext.appContainer.activeProfile.first()?.id,
                listOptions = WidgetListOptions.from(prefs),
            )
        }
        val loaded = initial ?: return
        if (loaded.profiles.isEmpty()) {
            LaunchedEffect(Unit) { onNoChoiceNeeded() }
            return
        }

        var selectedServerId by remember { mutableStateOf(loaded.serverId) }
        var sort by remember { mutableStateOf(loaded.listOptions.sort) }
        var descending by remember { mutableStateOf(loaded.listOptions.descending) }
        var showInactive by remember { mutableStateOf(loaded.listOptions.showInactive) }
        val selectedProfile = loaded.profiles.firstOrNull { it.id == selectedServerId } ?: loaded.profiles.first()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = { ConfigureAppBar() },
            bottomBar = {
                DoneBar(onClick = { onDone(selectedProfile, WidgetListOptions(sort, descending, showInactive)) })
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 20.dp),
            ) {
                if (loaded.profiles.size > 1) {
                    section(R.string.widget_configure_server, first = true) {
                        loaded.profiles.forEachIndexed { index, profile ->
                            SelectRow(
                                title = profile.displayName,
                                subtitle = profile.host,
                                selected = profile == selectedProfile,
                                showDivider = index > 0,
                                onClick = { selectedServerId = profile.id },
                            )
                        }
                    }
                }
                section(R.string.widget_configure_sort, first = loaded.profiles.size <= 1) {
                    TorrentSort.entries.forEachIndexed { index, option ->
                        SelectRow(
                            title = option.label(),
                            selected = option == sort,
                            showDivider = index > 0,
                            onClick = {
                                // Like the app's sort menu, a newly picked field starts in its natural direction
                                if (option != sort) descending = option.defaultDescending
                                sort = option
                            },
                        )
                    }
                }
                section(R.string.widget_configure_direction) {
                    DirectionToggle(descending = descending, onChange = { descending = it })
                }
                section(R.string.widget_configure_filter) {
                    ShowInactiveRow(checked = showInactive, onChange = { showInactive = it })
                }
            }
        }
    }

    @Composable
    private fun ConfigureAppBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.widget_configure_title),
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    @Composable
    private fun DoneBar(onClick: () -> Unit) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onClick,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
                    .height(54.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.widget_configure_done), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    /** A titled, rounded group of rows, like the mockup's `.section` + `.group`. */
    private fun LazyListScope.section(title: Int, first: Boolean = false, content: @Composable () -> Unit) = item {
        Column(Modifier.padding(top = if (first) 6.dp else 18.dp)) {
            Text(
                stringResource(title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(groupColor())
                    .selectableGroup(),
            ) {
                content()
            }
        }
    }

    /** White on the light theme, but a raised container (not the near-black lowest one) on the dark one. */
    @Composable
    private fun groupColor(): Color = with(MaterialTheme.colorScheme) {
        if (surface.luminance() > 0.5f) surfaceContainerLowest else surfaceContainer
    }

    /**
     * A radio row; with a [subtitle] it is a server row, without one a compact sort row. The selected row is tinted with the selection container also used by the filter drawer.
     */
    @Composable
    private fun SelectRow(
        title: String,
        selected: Boolean,
        showDivider: Boolean,
        onClick: () -> Unit,
        subtitle: String? = null,
    ) {
        val compact = subtitle == null
        val colors = MaterialTheme.colorScheme
        if (showDivider) {
            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = if (compact) 56.dp else 60.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 52.dp else 60.dp)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .background(if (selected) colors.tertiaryContainer else Color.Transparent)
                .padding(horizontal = 18.dp, vertical = if (compact) 9.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.outline),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = if (compact) FontWeight.Medium else FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = 12.5.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }

    /** The mockup's pill-shaped ascending/descending segmented control. */
    @Composable
    private fun DirectionToggle(descending: Boolean, onChange: (Boolean) -> Unit) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colors.surfaceContainer)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                Triple(false, Icons.Rounded.ArrowUpward, R.string.sort_ascending),
                Triple(true, Icons.Rounded.ArrowDownward, R.string.sort_descending),
            ).forEach { (isDescending, icon, label) ->
                val selected = descending == isDescending
                val contentColor = if (selected) colors.onTertiaryContainer else colors.onSurfaceVariant
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.tertiaryContainer else Color.Transparent)
                        .selectable(selected = selected, onClick = { onChange(isDescending) }, role = Role.RadioButton),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                }
            }
        }
    }

    @Composable
    private fun ShowInactiveRow(checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.widget_configure_show_inactive),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.widget_configure_show_inactive_summary),
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
