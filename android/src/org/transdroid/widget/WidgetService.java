/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.widget;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.DLog;
import org.transdroid.gui.LocalTorrent;
import org.transdroid.preferences.Preferences;
import org.transdroid.rss.RssFeedSettings;
import org.transdroid.util.TLog;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

/**
 * A service that updates any running widget by making (synchronous) calls to its assigned 
 * server daemons. It can start these updates by calling the scheduleUpdate method for every 
 * newly added widget.
 * 
 * @author erickok
 */
public class WidgetService extends IntentService {

	private static final String LOG_NAME = "Widget service";

	public static final String INTENT_EXTRAS_WIDGET_ID = "org.transdroid.widget.WIDGET_ID";

	private static final Map<Integer, PendingIntent> intents = new HashMap<Integer, PendingIntent>();

	public WidgetService() {
		super(LOG_NAME);
	}

	public static void cancelUpdates(Context context, int widgetId) {
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		if (intents.containsKey(widgetId)) {
			mgr.cancel(intents.get(widgetId));
		}
	}
	
	/**
	 * Repeatedly ask the service to update a service widget, starting directly with the first call
	 * @param context The application context
	 * @param int The widget for which to schedule the updates
	 */
	public static void scheduleUpdates(Context context, int widgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		// Ensure a PendingIntent for this widget
		if (!intents.containsKey(widgetId)) {
        	Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        	intent.setData(Uri.parse("widget:" + widgetId)); // This is used to make the intent unique (see http://stackoverflow.com/questions/2844274)
        	intent.putExtra(WidgetService.INTENT_EXTRAS_WIDGET_ID, widgetId);
        	intents.put(widgetId, PendingIntent.getBroadcast(context, widgetId, intent, 0));
		}
		
		// Schedule a first (directly) and any subsequent updates
		int interval = Preferences.readWidgetIntervalSettin(prefs, widgetId) * 1000;
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), interval, intents.get(widgetId));

	}
	
	@Override
	protected void onHandleIntent(Intent intent) {

		// Attach the Android TLog to the daemon logger
		DLog.setLogger(TLog.getInstance());
		
		// Retrieve the widget settings
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	List<DaemonSettings> allDaemons = Preferences.readAllDaemonSettings(prefs);
    	boolean onlyShowTransferring = prefs.getBoolean(Preferences.KEY_PREF_LASTSORTGTZERO, false);
    	WidgetSettings widget = Preferences.readWidgetSettings(prefs, intent.getIntExtra(INTENT_EXTRAS_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), allDaemons);
    	
    	if (widget != null && widget.getDaemonSettings() != null && widget.getDaemonSettings().getType() != null) {

    		// Also refresh the RSS feed counter?
    		int newRssCount = -1;
    		if (widget.getLayoutResourceId() == R.layout.appwidget_small) {
    			TLog.d(LOG_NAME, "Looking for RSS feed updates");
    			newRssCount = refreshRssFeedNewItems(prefs);
    		}
    		
        	// Retrieve the torrents from the server
			TLog.d(LOG_NAME, widget.getDaemonSettings().getHumanReadableIdentifier() + ": Retrieving torrent listing");
			WidgetServiceHelper.showMessageTextOnWidget(getApplicationContext(), widget, getText(R.string.connecting));
    		IDaemonAdapter daemon = widget.getDaemonSettings().getType().createAdapter(widget.getDaemonSettings());
			DaemonTaskResult result = RetrieveTask.create(daemon).execute();

			if (result instanceof RetrieveTaskSuccessResult) {

				// Get the returned torrents
				RetrieveTaskSuccessResult success = (RetrieveTaskSuccessResult) result;
				List<Torrent> torrents = success.getTorrents();
				
				if (torrents.size() > 0) {
					// Show the overall status of the retrieved torrents
					WidgetServiceHelper.showTorrentStatisticsOnWidget(getApplicationContext(), widget, torrents, onlyShowTransferring, newRssCount);
				} else {
		        	// Set the view to show 'no torrents'
					WidgetServiceHelper.showMessageTextOnWidget(getApplicationContext(), widget, getText(R.string.no_torrents));
				}
				
			} else {

				// Set the error text on the widget
				DaemonTaskFailureResult failure = (DaemonTaskFailureResult) result;
				WidgetServiceHelper.showMessageTextOnWidget(getApplicationContext(), widget, getText(LocalTorrent.getResourceForDaemonException(failure.getException())));
				
			}

    	}
		
	}

    private static int refreshRssFeedNewItems(SharedPreferences prefs) {

		List<RssFeedSettings> feeds = Preferences.readAllRssFeedSettings(prefs);
		int unread = 0;
		for (RssFeedSettings settings : feeds) {

			try {
				// Load RSS items
				RssParser parser = new RssParser(settings.getUrl());
				parser.parse();
				
				if (parser.getChannel() != null) {
					// Count items until that last known read item is found again
					// Note that the item URL is used as unique identifier
					List<Item> items = parser.getChannel().getItems();
					Collections.sort(items, Collections.reverseOrder());
					for (Item item : items) {
						if (settings.getLastNew() == null || item == null || item.getTheLink() == null || item.getTheLink().equals(settings.getLastNew())) {
							break;
						}
						unread++;
					}
				}
				
			} catch (Exception e) {
				// Ignore RSS items that could not be retrieved or parsed
			}
			
		}
		
		return unread;
		
    }

}
