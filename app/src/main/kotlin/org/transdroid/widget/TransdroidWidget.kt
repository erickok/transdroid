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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.color.ColorProvider as DayNightColor
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.transdroid.EXTRA_OPEN_ADD_TORRENT
import org.transdroid.MainActivity
import org.transdroid.R
import kotlinx.coroutines.flow.first
import org.transdroid.appContainer
import org.transdroid.protocol.TorrentStatus
import org.transdroid.util.formatSpeed

/** Fixed colors matching design/mockups/transdroid-m3-widget.html's --dl/--seed/--muted/etc tokens. */
private object WidgetColors {
    val on: ColorProvider = DayNightColor(day = Color(0xFF1A1C15), night = Color(0xFFE6E7DC))
    val muted: ColorProvider = DayNightColor(day = Color(0xFF5C6053), night = Color(0xFFA4A897))
    val down: ColorProvider = DayNightColor(day = Color(0xFF3F68B8), night = Color(0xFFADC7FF))
    val seed: ColorProvider = DayNightColor(day = Color(0xFF4D7D2A), night = Color(0xFFA0CF72))
    val track: ColorProvider = DayNightColor(day = Color(0x1F000000), night = Color(0x26FFFFFF))
    val chip: ColorProvider = DayNightColor(day = Color(0x0D000000), night = Color(0x14FFFFFF))
    val iconChip: ColorProvider = DayNightColor(day = Color(0xFF303331), night = Color(0xFF3A3D3B))
}

/** Below this height a single-row "stats strip" is shown instead of the header+list+footer layout. */
private val STRIP_MAX_HEIGHT = 100.dp

/** Home screen widget: the active server's totals, and (when tall enough) its running torrents. */
class TransdroidWidget : GlanceAppWidget() {

    // Lets the composable read the widget's actual current size via LocalSize, so it can switch
    // between the 4x1 stats-only strip and the full header+list+footer layout at any height,
    // rather than being limited to a small fixed set of Responsive size buckets.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = context.appContainer.widgetStateRepository
        val activeProfile = context.appContainer.activeProfile
        val initialServerId = resolveWidgetServerId(context, id)
        val initialStates = repository.states.first()
        val strings = widgetStrings(context)
        provideContent {
            // Glance keeps this session alive for a while after rendering, and update()/updateAll()
            // within that window only recompose, never re-run provideGlance. So the server choice
            // is read from this instance's Glance state here, not captured once above: the host
            // first renders a newly placed widget before WidgetConfigureActivity stores the pick.
            // Likewise the stored snapshots are observed so every write shows up immediately.
            val configuredServerId = currentState(WIDGET_SERVER_ID_KEY)
            val active by activeProfile.collectAsState(initial = null)
            val serverId = configuredServerId ?: active?.id ?: initialServerId
            val states by repository.states.collectAsState(initial = initialStates)
            val listOptions = WidgetListOptions.from(currentState())
            val state = (serverId?.let { states[it] } ?: WidgetState()).let { stored ->
                stored.copy(torrents = listOptions.select(stored.torrents))
            }
            // Fetch right away when just (re)configured, as the stored snapshot may lack the top
            // torrents for the newly chosen list options, or when there is no snapshot at all (a server
            // that isn't the app's active one), rather than wait for the next background worker run
            val needsRefresh = currentState(WIDGET_NEEDS_REFRESH_KEY) == true
            if (serverId != null && (needsRefresh || state.updatedAtMillis == null)) {
                LaunchedEffect(serverId, listOptions) {
                    updateAppWidgetState(context, id) { it.remove(WIDGET_NEEDS_REFRESH_KEY) }
                    refreshWidgetServer(context, serverId)
                }
            }
            GlanceTheme {
                WidgetContent(state, listOptions, strings)
            }
        }
    }

    // The widget picker preview (Android 15+): sample data in the strip and the full layout, so
    // the launcher can show whichever fits the preview's size. See publishWidgetPreviews().
    override val previewSizeMode = SizeMode.Responsive(setOf(PREVIEW_STRIP_SIZE, PREVIEW_FULL_SIZE))

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val strings = widgetStrings(context)
        val state = widgetPreviewState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state, WidgetListOptions.DEFAULT, strings)
            }
        }
    }

    private fun widgetStrings(context: Context) = WidgetStrings(
        appName = context.getString(R.string.app_name),
        noData = context.getString(R.string.widget_no_data),
        connected = context.getString(R.string.widget_connected),
        noActiveTorrents = context.getString(R.string.widget_no_active_torrents),
        noTorrents = context.getString(R.string.widget_no_torrents),
        seeding = context.getString(R.string.widget_seeding_label),
        active = context.getString(R.string.toolbar_download_active),
        sharing = context.getString(R.string.toolbar_upload_sharing),
        footerSummary = context.getString(R.string.widget_footer_summary),
        refresh = context.getString(R.string.widget_refresh),
        addTorrent = context.getString(R.string.add_title),
        start = context.getString(R.string.widget_start),
        pause = context.getString(R.string.widget_pause),
    )

    private data class WidgetStrings(
        val appName: String,
        val noData: String,
        val connected: String,
        val noActiveTorrents: String,
        val noTorrents: String,
        val seeding: String,
        val active: String,
        val sharing: String,
        val footerSummary: String,
        val refresh: String,
        val addTorrent: String,
        val start: String,
        val pause: String,
    )

    @Composable
    private fun WidgetContent(state: WidgetState, listOptions: WidgetListOptions, strings: WidgetStrings) {
        val size = LocalSize.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            when {
                state.updatedAtMillis == null -> EmptyState(strings)
                size.height < STRIP_MAX_HEIGHT -> StripContent(state, strings)
                else -> FullContent(state, listOptions, strings)
            }
        }
    }

    @Composable
    private fun EmptyState(strings: WidgetStrings) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetIcon()
            Spacer(GlanceModifier.width(10.dp))
            Text(
                strings.noData,
                style = TextStyle(color = WidgetColors.on, fontSize = 13.sp),
                maxLines = 2,
            )
        }
    }

    @Composable
    private fun StripContent(state: WidgetState, strings: WidgetStrings) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetIcon()
            Spacer(GlanceModifier.width(13.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    state.serverName ?: strings.appName,
                    style = TextStyle(color = WidgetColors.on, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                    maxLines = 1,
                )
                Text(
                    strings.connected,
                    style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(10.dp))
            StatBlock(
                headline = "↓ ${formatSpeed(state.downloadRate)}",
                caption = strings.active.format(state.downloadingCount),
                color = WidgetColors.down,
            )
            Spacer(GlanceModifier.width(16.dp))
            StatBlock(
                headline = "↑ ${formatSpeed(state.uploadRate)}",
                caption = strings.sharing.format(state.seedingCount),
                color = WidgetColors.seed,
            )
            Spacer(GlanceModifier.width(10.dp))
            RefreshButton(strings)
        }
    }

    @Composable
    private fun StatBlock(headline: String, caption: String, color: ColorProvider) {
        Column(horizontalAlignment = Alignment.End) {
            Text(headline, style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 13.5.sp), maxLines = 1)
            Text(caption, style = TextStyle(color = WidgetColors.muted, fontSize = 9.5.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }

    @Composable
    private fun FullContent(state: WidgetState, listOptions: WidgetListOptions, strings: WidgetStrings) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(14.dp)) {
            HeaderRow(state, strings)
            Spacer(GlanceModifier.height(8.dp))
            if (state.torrents.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (listOptions.showInactive) strings.noTorrents else strings.noActiveTorrents,
                        style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp),
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(state.torrents, itemId = { it.id.hashCode().toLong() }) { torrent ->
                        TorrentRow(torrent, strings)
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            FooterRow(state, strings)
        }
    }

    @Composable
    private fun HeaderRow(state: WidgetState, strings: WidgetStrings) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WidgetIcon()
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    strings.appName,
                    style = TextStyle(color = WidgetColors.on, fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                    maxLines = 1,
                )
                Text(
                    state.serverName ?: "",
                    style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
            // A RemoteViews Text can only have one uniform color, unlike the mockup's inline
            // dn/up spans within a single "↓ 4.4 ↑ 8.2 MB/s" line - shown here as two adjacent
            // fully-colored texts (each with its own trailing unit) instead.
            Text(
                "↓ ${formatSpeed(state.downloadRate)}",
                style = TextStyle(color = WidgetColors.down, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "↑ ${formatSpeed(state.uploadRate)}",
                style = TextStyle(color = WidgetColors.seed, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(8.dp))
            RefreshButton(strings)
        }
    }

    @Composable
    private fun TorrentRow(torrent: WidgetTorrent, strings: WidgetStrings) {
        // Paused, queued, errored etc. rows (only listed when the widget shows inactive torrents)
        // are muted and drop their always-zero speed
        val color = when {
            torrent.status == TorrentStatus.SEEDING -> WidgetColors.seed
            torrent.status.isActive -> WidgetColors.down
            else -> WidgetColors.muted
        }
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(color)) {}
                Spacer(GlanceModifier.width(11.dp))
                Text(
                    torrent.name,
                    style = TextStyle(color = WidgetColors.on, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (torrent.status.isActive) {
                    Spacer(GlanceModifier.width(8.dp))
                    val speed = if (torrent.status == TorrentStatus.SEEDING) torrent.uploadRate else torrent.downloadRate
                    val arrow = if (torrent.status == TorrentStatus.SEEDING) "↑" else "↓"
                    Text(
                        "$arrow ${formatSpeed(speed)}",
                        style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.width(8.dp))
                ToggleButton(torrent, strings)
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(start = 19.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = torrent.progress.coerceIn(0f, 1f),
                    modifier = GlanceModifier.defaultWeight().height(5.dp).cornerRadius(3.dp),
                    color = color,
                    backgroundColor = WidgetColors.track,
                )
                Spacer(GlanceModifier.width(9.dp))
                Text(
                    if (torrent.status == TorrentStatus.SEEDING) strings.seeding else "${(torrent.progress * 100).toInt()}%",
                    style = TextStyle(color = WidgetColors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    private fun FooterRow(state: WidgetState, strings: WidgetStrings) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.footerSummary.format(state.downloadingCount, state.totalCount),
                style = TextStyle(color = WidgetColors.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Box(
                modifier = GlanceModifier
                    .size(30.dp)
                    .cornerRadius(10.dp)
                    .background(WidgetColors.chip)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            parameters = actionParametersOf(openAddTorrentKey to true),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = strings.addTorrent,
                    colorFilter = ColorFilter.tint(WidgetColors.on),
                    modifier = GlanceModifier.size(19.dp),
                )
            }
        }
    }

    @Composable
    private fun ToggleButton(torrent: WidgetTorrent, strings: WidgetStrings) {
        val canStart = torrent.status.canStart
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .cornerRadius(14.dp)
                .background(WidgetColors.chip)
                .clickable(
                    actionRunCallback<ToggleTorrentWidgetAction>(
                        parameters = actionParametersOf(
                            WidgetTorrentIdKey to torrent.id,
                            WidgetTorrentCanStartKey to canStart,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(if (canStart) R.drawable.ic_widget_play else R.drawable.ic_widget_pause),
                contentDescription = if (canStart) strings.start else strings.pause,
                colorFilter = ColorFilter.tint(WidgetColors.muted),
                modifier = GlanceModifier.size(16.dp),
            )
        }
    }

    @Composable
    private fun RefreshButton(strings: WidgetStrings) {
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(16.dp)
                .background(WidgetColors.chip)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = strings.refresh,
                colorFilter = ColorFilter.tint(WidgetColors.muted),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    }

    /**
     * The app's own launcher icon on a dark grey rounded chip. Uses ic_widget_app_icon (a tight
     * center-crop of the adaptive icon's foreground layer, one per density bucket) rather than
     * that foreground drawable directly: the adaptive-icon format reserves a large transparent
     * safe zone around the visible badge for the launcher's own masking/parallax, which at this
     * widget's small size just reads as the icon being too small.
     */
    @Composable
    private fun WidgetIcon() {
        Box(
            modifier = GlanceModifier.size(30.dp).cornerRadius(10.dp).background(WidgetColors.iconChip),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_app_icon),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
            )
        }
    }

    private companion object {
        val PREVIEW_STRIP_SIZE = DpSize(250.dp, 60.dp)
        val PREVIEW_FULL_SIZE = DpSize(250.dp, 200.dp)
        val openAddTorrentKey = ActionParameters.Key<Boolean>(EXTRA_OPEN_ADD_TORRENT)
    }
}

class TransdroidWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TransdroidWidget()
}
