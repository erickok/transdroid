/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.androidannotations.annotations.EService;
import org.transdroid.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.log.*;
import org.transdroid.core.service.*;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentsComparator;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.FileSizeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A service for the list widget to update the remote views that a list widget shows, by getting the torrents from the
 * server (synchronously) and building {@link RemoteViews} objects for each torrent.
 * @author Eric Kok
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@EService
public class ListWidgetViewsService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetViewsFactory(this.getApplicationContext(), intent);
	}

}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private final Context context;
	private final int appWidgetId;
	private final Log log;
	private List<Torrent> torrents = null;
	private ListWidgetConfig config = null;

	public WidgetViewsFactory(Context applicationContext, Intent intent) {
		this.context = applicationContext;
		this.appWidgetId =
				intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		this.log = Log_.getInstance_(applicationContext);
	}

	@Override
	public void onCreate() {
		// Nothing to do here (wait for onDataSetChanged)
	}

	@Override
	public void onDataSetChanged() {

		// Load the widget settings
		ApplicationSettings settings = ApplicationSettings_.getInstance_(context);
		config = settings.getWidgetConfig(appWidgetId);
		if (config == null || config.getServerId() < 0) {
			log.e(context, "Looking for widget data while the widget wasn't yet configured");
			return;
		}
		ServerSetting server = settings.getServerSetting(config.getServerId());
		if (server == null) {
			// TODO: Show error text some how in the remote view, perhaps via the EmptyView's text?
			log.e(context, "The server for which this widget was created no longer exists");
			if (torrents != null) {
				torrents.clear();
			}
			return;
		}

		// Load the torrents; synchronously
		IDaemonAdapter connection =
				server.createServerAdapter(ConnectivityHelper_.getInstance_(context).getConnectedNetworkName(),
						context);
		DaemonTaskResult result = RetrieveTask.create(connection).execute(log);
		if (!(result instanceof RetrieveTaskSuccessResult)) {
			// TODO: Show error text somehow in the remote view, perhaps via the EmptyView's text?
			log.e(context, "The torrents could not be retrieved at this time; probably a connection issue");
			if (torrents != null) {
				torrents.clear();
			}
			return;
		}

		// We have data; filter, sort and store it to use later when getViewAt gets called
		SystemSettings systemSettings = SystemSettings_.getInstance_(context);
		ArrayList<Torrent> filteredTorrents = new ArrayList<>();
		List<Torrent> allTorrents = ((RetrieveTaskSuccessResult) result).getTorrents();
		for (Torrent torrent : allTorrents) {
			if (config.getStatusType().getFilterItem(context)
					.matches(torrent, systemSettings.treatDormantAsInactive())) {
				filteredTorrents.add(torrent);
			}
		}
		if (filteredTorrents.size() > 0) {
			// Only sort when there are actually torrents left after filtering
			Daemon serverType = filteredTorrents.get(0).getDaemon();
			Collections.sort(filteredTorrents,
					new TorrentsComparator(serverType, config.getSortBy(), config.shouldReserveSort()));
		}
		torrents = filteredTorrents;

		// If the user asked to show the server status statistics, we need to update the widget remote views again
		RemoteViews rv = ListWidgetProvider.buildRemoteViews(context, appWidgetId, config);
		if (config.shouldShowStatusView()) {

			// Update the server status count and speeds in the 'action bar'
			int downcount = 0, upcount = 0, downspeed = 0, upspeed = 0;
			for (Torrent torrent : torrents) {
				if (torrent.isDownloading(systemSettings.treatDormantAsInactive())) {
					downcount++;
					upcount++;
				} else if (torrent.isSeeding(systemSettings.treatDormantAsInactive())) {
					upcount++;
				}
				downspeed += torrent.getRateDownload();
				upspeed += torrent.getRateUpload();
			}
			rv.setViewVisibility(R.id.navigation_view, View.GONE);
			rv.setViewVisibility(R.id.serverstatus_view, View.VISIBLE);
			rv.setTextViewText(R.id.downcount_text, Integer.toString(downcount));
			rv.setTextViewText(R.id.upcount_text, Integer.toString(upcount));
			rv.setTextViewText(R.id.downspeed_text, FileSizeConverter.getSize(downspeed) + "/s");
			rv.setTextViewText(R.id.upspeed_text, FileSizeConverter.getSize(upspeed) + "/s");

			AppWidgetManager.getInstance(context.getApplicationContext()).updateAppWidget(appWidgetId, rv);

		}

	}

	@Override
	public RemoteViews getViewAt(int position) {

		// Load the dark or light widget list item layout xml
		RemoteViews rv = new RemoteViews(context.getPackageName(),
				config.shouldUseDarkTheme() ? R.layout.list_item_widget_dark : R.layout.list_item_widget_light);

		// Bind the torrent details texts and status colour
		Torrent torrent = torrents.get(position);
		LocalTorrent local = LocalTorrent.fromTorrent(torrent);
		rv.setTextViewText(R.id.name_text, torrent.getName());
		rv.setTextViewText(R.id.progress_text, local.getProgressSizeText(context.getResources(), false));
		rv.setTextViewText(R.id.ratio_text, local.getProgressEtaRatioText(context.getResources()));
		int statusColour;
		switch (torrent.getStatusCode()) {
			case Downloading:
				statusColour = R.color.torrent_downloading;
				break;
			case Paused:
				statusColour = R.color.torrent_paused;
				break;
			case Seeding:
				statusColour = R.color.torrent_seeding;
				break;
			case Error:
				statusColour = R.color.torrent_error;
				break;
			default: // Checking, Waiting, Queued, Unknown
				statusColour = R.color.torrent_other;
				break;
		}
		rv.setInt(R.id.status_view, "setBackgroundColor", context.getResources().getColor(statusColour));
		Intent startIntent = new Intent();
		startIntent.putExtra(ListWidgetProvider.EXTRA_SERVER, config.getServerId());
		startIntent.putExtra(ListWidgetProvider.EXTRA_TORRENT, torrent);
		rv.setOnClickFillInIntent(R.id.widget_line_layout, startIntent);

		return rv;

	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public void onDestroy() {
		if (torrents != null) {
			torrents.clear();
		}
		torrents = null;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (torrents == null) {
			return 0;
		}
		return torrents.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
