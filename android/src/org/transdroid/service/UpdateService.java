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
package org.transdroid.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.transdroid.R;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.preferences.Preferences;
import org.transdroid.util.TLog;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * A service that checks if a new version of the app or the search module is available.
 * 
 * @author erickok
 */
public class UpdateService extends IntentService {

	private static final String LATEST_URL_APP = "http://www.transdroid.org/update/latest-app.php";
	private static final String LATEST_URL_SEARCH = "http://www.transdroid.org/update/latest-search.php";
	private static final String PACKAGE_APP = "org.transdroid";
	private static final String PACKAGE_SEARCH = "org.transdroid.search";
	private static final String DOWNLOAD_URL_APP = "http://www.transdroid.org/latest";
	private static final String DOWNLOAD_URL_SEARCH = "http://www.transdroid.org/latest-search";
	private static final String LOG_NAME = "Update service";
	private static NotificationManager notificationManager;

	public UpdateService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Check if the user has background data disabled
		ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (!conn.getBackgroundDataSetting()) {
			TLog.d(LOG_NAME,
				"Skip checking for new app versions, since background data is disabled on a system-wide level");
			return;
		}

		DefaultHttpClient httpclient = new DefaultHttpClient();

		try {

			// Retrieve what is the latest released app and search module versions
			String[] app = retrieveLatestVersion(httpclient, LATEST_URL_APP);
			String[] search = retrieveLatestVersion(httpclient, LATEST_URL_SEARCH);
			int appVersion = Integer.parseInt(app[0].trim());
			int searchVersion = Integer.parseInt(search[0].trim());

			// New version of the app?
			try {
				PackageInfo appPackage = getPackageManager().getPackageInfo(PACKAGE_APP, 0);
				if (appPackage.versionCode < appVersion) {
					// New version available! Notify the user.
					newNotification(getString(R.string.update_app_newversion),
						getString(R.string.update_app_newversion), getString(R.string.update_updateto,
							app[1].trim()), DOWNLOAD_URL_APP, 0);
				}
			} catch (NameNotFoundException e) {
				// Not installed... this can never happen since this Service is part of the app itself.
			}

			// New version of the search module?
			try {
				PackageInfo searchPackage = getPackageManager().getPackageInfo(PACKAGE_SEARCH, 0);
				if (searchPackage.versionCode < searchVersion) {
					// New version available! Notify the user.
					newNotification(getString(R.string.update_search_newversion),
						getString(R.string.update_search_newversion), getString(R.string.update_updateto,
							search[1].trim()), DOWNLOAD_URL_SEARCH, 0);
				}
			} catch (NameNotFoundException e) {
				// The search module isn't installed yet at all; ignore and wait for the user to manually
				// install it (when the first search is initiated)
			}

		} catch (Exception e) {
			// Cannot check right now for some reason; log and `ignore
			TLog.d(LOG_NAME, "Cannot retrieve latest app or search module version code from the site: " + e.toString());
		}

	}

	private String[] retrieveLatestVersion(AbstractHttpClient httpclient, String url) throws ClientProtocolException, IOException {

		// Retrieve what is the latest released app version
		HttpResponse request = httpclient.execute(new HttpGet(url));
		InputStream stream = request.getEntity().getContent();
		String appVersion[] = HttpHelper.ConvertStreamToString(stream).split("\\|");
		stream.close();
		return appVersion;

	}

	private void newNotification(String ticker, String title, String text, String downloadUrl, int notifyID) {

		// Use the alarm service settings for the notification sound/vibrate/colour
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		AlarmSettings settings = Preferences.readAlarmSettings(prefs);

		// Set up an intent that will initiate a download of the new version
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));

		// Create a new notification
		Notification newNotification = new Notification(R.drawable.icon_notification, ticker, System
			.currentTimeMillis());
		newNotification.flags = Notification.FLAG_AUTO_CANCEL;
		newNotification.setLatestEventInfo(getApplicationContext(), title, text, PendingIntent.getActivity(
			getApplicationContext(), notifyID, i, 0));

		// Get the system notification manager, if not done so previously
		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		// If sound enabled add to notification
		if (settings.getAlarmPlaySound() && settings.getAlarmSoundURI() != null) {
			newNotification.sound = Uri.parse(settings.getAlarmSoundURI());
		}

		// If vibration enabled add to notification
		if (settings.getAlarmVibrate()) {
			newNotification.defaults = Notification.DEFAULT_VIBRATE;
		}

		// Add coloured light; defaults to 0xff7dbb21
		newNotification.ledARGB = settings.getAlarmColour();
		newNotification.ledOnMS = 600;
		newNotification.ledOffMS = 1000;
		newNotification.flags |= Notification.FLAG_SHOW_LIGHTS;

		// Send notification
		notificationManager.notify(notifyID, newNotification);

	}

}
