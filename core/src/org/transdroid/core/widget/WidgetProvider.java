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
import org.transdroid.core.app.settings.ServerSetting;
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

	@Bean
	protected ApplicationSettings applicationSettings;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			appWidgetManager.updateAppWidget(appWidgetId,
					buildRemoteViews(context, appWidgetId, applicationSettings.getWidgetConfig(appWidgetId)));
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			applicationSettings.removeWidgetConfig(appWidgetId);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent == null || intent.getAction() == null || intent.getExtras() == null
				|| !intent.hasExtra(EXTRA_SERVER))
			return;

		// Launch an Intent to start Transdroid on some specific server; and possibly a specific torrent too
		Intent start = new Intent(INTENT_STARTSERVER);
		start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		start.putExtra(EXTRA_SERVER, intent.getIntExtra(EXTRA_SERVER, -1));
		if (intent.getAction().equals(EXTRA_TORRENT)) {
			start.putExtra(EXTRA_TORRENT, intent.getParcelableExtra(EXTRA_TORRENT));
		}
		context.startActivity(start);

		super.onReceive(context, intent);

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
				config.shouldUseDarkTheme() ? R.layout.list_item_widget_dark : R.layout.list_item_widget_light);

		// Set up the widget's list view loading service which refers to the WidgetViewsFactory
		// Use a unique data URI next to the extra to make sure the intents are unique form each widget
		Intent intent = new Intent(context, WidgetService.class);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME) + "//widget/" + appWidgetId + "/server/"
				+ config.getServerId()));
		intent.putExtra(EXTRA_SERVER, config.getServerId());
		rv.setRemoteAdapter(appWidgetId, R.id.torrents_list, intent);
		rv.setPendingIntentTemplate(R.id.torrents_list,
				PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		rv.setEmptyView(R.id.torrents_list, R.id.error_text);
		rv.setTextViewText(R.id.error_text, context.getString(R.string.navigation_emptytorrents));

		// Show the server and status type filter from the widget configuration
		ServerSetting server = appSettings.getServerSetting(config.getServerId());
		rv.setTextViewText(R.id.server_text, server.getName());
		rv.setTextViewText(R.id.filter_text, config.getStatusType().getFilterItem(context).getName());
		rv.setOnClickPendingIntent(R.id.icon_image,
				PendingIntent.getActivity(context, 0, intent.cloneFilter(), PendingIntent.FLAG_UPDATE_CURRENT));

		return rv;

	}

}
