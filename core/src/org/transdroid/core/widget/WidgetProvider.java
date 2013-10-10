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

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EReceiver;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.*;
import org.transdroid.core.gui.log.Log;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@EReceiver
public class WidgetProvider extends AppWidgetProvider {

	public static final String INTENT_STARTSERVER = "org.transdroid.START_SERVER";
	public static final String EXTRA_TORRENT = "extra_torrent";
	public static final String EXTRA_SERVER = "extra_server";
	public static final String EXTRA_REFRESH = "extra_refresh";

	@Bean
	protected ApplicationSettings applicationSettings;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null && intent.hasExtra(EXTRA_REFRESH)) {
			// Manually requested a refresh for the app widget of which the ID was supplied
			int appWidgetId = intent.getIntExtra(EXTRA_REFRESH, -1);
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId,
					buildRemoteViews(context, appWidgetId, applicationSettings.getWidgetConfig(appWidgetId)));
			AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.torrents_list);
			return;
		}
		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			appWidgetManager.updateAppWidget(appWidgetId,
					buildRemoteViews(context, appWidgetId, applicationSettings.getWidgetConfig(appWidgetId)));
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.torrents_list);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			applicationSettings.removeWidgetConfig(appWidgetId);
		}
	}

	/**
	 * Loads and sets up the layout for some specific app widget given the user's widget settings. Note that the views
	 * for the list view rows are loaded separately in the {@link WidgetViewsFactory}.
	 * @param context The app widget context, with access to resources
	 * @param appWidgetId The specific ID of the app widget to load
	 * @param config The user widget configuration, with filter and theme preferences
	 * @return A fully initialised set of remote views to update the widget with the AppWidgetManager
	 */
	@SuppressWarnings("deprecation")
	public static RemoteViews buildRemoteViews(Context context, int appWidgetId, WidgetConfig config) {

		// Does the server to show and its widget settings actually still exist?
		if (context == null || config == null)
			return null;
		ApplicationSettings appSettings = ApplicationSettings_.getInstance_(context);
		if (config.getServerId() < 0 || config.getServerId() > appSettings.getMaxServer()) {
			Log.e(context, "Tried to set up widget " + appWidgetId + " but the bound server ID " + config.getServerId()
					+ " no longer exists.");
			return null;
		}

		// Load the dark or light widget layout xml
		RemoteViews rv = new RemoteViews(context.getPackageName(),
				config.shouldUseDarkTheme() ? R.layout.widget_torrents_dark : R.layout.widget_torrents_light);

		// Set up the widget's list view loading service which refers to the WidgetViewsFactory
		Intent data = new Intent(context, WidgetService_.class);
		data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		data.setData(Uri.parse(data.toUri(Intent.URI_INTENT_SCHEME)));
		rv.setRemoteAdapter(appWidgetId, R.id.torrents_list, data);
		Intent open = new Intent(context, TorrentsActivity_.class);
		open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		rv.setPendingIntentTemplate(R.id.torrents_list,
				PendingIntent.getActivity(context, appWidgetId, open, PendingIntent.FLAG_UPDATE_CURRENT));
		rv.setEmptyView(R.id.torrents_list, R.id.error_text);
		rv.setTextViewText(R.id.error_text, context.getString(R.string.widget_loading));

		// Show the server and status type filter from the widget configuration in the 'action bar'
		ServerSetting server = appSettings.getServerSetting(config.getServerId());
		rv.setTextViewText(R.id.server_text, server.getName());
		rv.setTextViewText(R.id.filter_text, config.getStatusType().getFilterItem(context).getName());

		// Set up the START_SERVER intent for 'action bar' clicks to start Transdroid normally
		Intent start = new Intent(context, TorrentsActivity_.class);
		//start.setData(Uri.parse("intent://widget/" + appWidgetId + "/start/" + config.getServerId()));
		start.setAction(INTENT_STARTSERVER);
		start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		start.putExtra(EXTRA_SERVER, config.getServerId());
		rv.setOnClickPendingIntent(R.id.icon_image,
				PendingIntent.getActivity(context, appWidgetId, start, PendingIntent.FLAG_UPDATE_CURRENT));
		rv.setOnClickPendingIntent(R.id.navigation_view,
				PendingIntent.getActivity(context, appWidgetId, start, PendingIntent.FLAG_UPDATE_CURRENT));

		// Set up the widgets refresh button pending intent (calling this WidgetProvider itself)
		// Make sure that the intent is unique using a custom data path (rather than just the extras)
		Intent refresh = new Intent(context, WidgetProvider_.class);
		refresh.setData(Uri.parse("intent://widget/" + appWidgetId + "/refresh"));
		refresh.putExtra(EXTRA_REFRESH, appWidgetId);
		refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		rv.setOnClickPendingIntent(R.id.refresh_button,
				PendingIntent.getBroadcast(context, appWidgetId, refresh, PendingIntent.FLAG_UPDATE_CURRENT));

		return rv;

	}

}
