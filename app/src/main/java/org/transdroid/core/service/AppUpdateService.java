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
package org.transdroid.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.transdroid.R;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.SystemSettings;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.daemon.util.HttpHelper;

import android.app.IntentService;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

@EService
public class AppUpdateService extends IntentService {

	private static final String LATEST_URL_APP = "https://raw.githubusercontent.com/erickok/transdroid/master/latest-app.html";
	private static final String LATEST_URL_SEARCH = "https://raw.githubusercontent.com/erickok/transdroid/master/latest-search.html";
	private static final String DOWNLOAD_URL_APP = "http://www.transdroid.org/latest";
	private static final String DOWNLOAD_URL_SEARCH = "http://www.transdroid.org/latest-search";

	@Bean
	protected Log log;
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected SystemSettings systemSettings;
	@Bean
	protected NotificationSettings notificationSettings;
	@SystemService
	protected NotificationManager notificationManager;

	public AppUpdateService() {
		super("AppUpdateService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		// Only run this service if app updates are handled via transdroid.org at all
		if (!navigationHelper.enableUpdateChecker())
			return;
		
		if (!connectivityHelper.shouldPerformBackgroundActions() || !systemSettings.checkForUpdates()) {
			log.d(this, "Skip the app update service, as background data is disabled, the service is explicitly " +
					"disabled or we are not connected.");
			return;
		}

		Date lastChecked = systemSettings.getLastCheckedForAppUpdates();
		Calendar lastDay = Calendar.getInstance();
		lastDay.add(Calendar.DAY_OF_MONTH, -1);
		if (lastChecked != null && lastChecked.after(lastDay.getTime())) {
			log.d(this, "Skip the update service, as we already checked the last 24 hours (or to be exact at "
					+ lastChecked.toString() + ").");
			return;
		}

		DefaultHttpClient httpclient = new DefaultHttpClient();
		Random random = new Random();

		try {

			// Retrieve what is the latest released app and search module versions
			String[] app = retrieveLatestVersion(httpclient, LATEST_URL_APP);
			String[] search = retrieveLatestVersion(httpclient, LATEST_URL_SEARCH);
			int appVersion = Integer.parseInt(app[0].trim());
			int searchVersion = Integer.parseInt(search[0].trim());

			// New version of the app?
			try {
				PackageInfo appPackage = getPackageManager().getPackageInfo(getPackageName(), 0);
				log.d(this, "Local Transdroid is at " + appPackage.versionCode + " and the reported latest version is "
						+ appVersion);
				if (appPackage.versionCode < appVersion) {
					// New version available! Notify the user.
					newNotification(getString(R.string.update_app_newversion),
							getString(R.string.update_app_newversion),
							getString(R.string.update_updateto, app[1].trim()),
							DOWNLOAD_URL_APP + "?" + Integer.toString(random.nextInt()), 90000);
				}
			} catch (NameNotFoundException e) {
				// Not installed... this can never happen since this Service is part of the app itself
			}

			// New version of the search module?
			try {
				PackageInfo searchPackage = getPackageManager().getPackageInfo("org.transdroid.search", 0);
				log.d(this, "Local Transdroid Seach is at " + searchPackage.versionCode
						+ " and the reported latest version is " + searchVersion);
				if (searchPackage.versionCode < searchVersion) {
					// New version available! Notify the user.
					newNotification(getString(R.string.update_search_newversion),
							getString(R.string.update_search_newversion),
							getString(R.string.update_updateto, search[1].trim()),
							DOWNLOAD_URL_SEARCH + "?" + Integer.toString(random.nextInt()), 90001);
				}
			} catch (NameNotFoundException e) {
				// The search module isn't installed yet at all; ignore and wait for the user to manually
				// install it (when the first search is initiated)
			}

			// Save that we successfully checked for app updates (and notified the user)
			// This prevents checking again for 1 day
			systemSettings.setLastCheckedForAppUpdates(new Date());

		} catch (Exception e) {
			// Cannot check right now for some reason; log and ignore
			log.d(this, "Cannot retrieve latest app or search module version code from the site: " + e.toString());
		}

	}

	/**
	 * Retrieves the latest version number of the app or search module by checking an online text file that looks like
	 * '160|1.1.15' for version code 160 and version name 1.1.15.
	 * @param httpclient An already instantiated HTTP client
	 * @param url The URL of the the text file that contains the current latest version code and name
	 * @return A string array with two elements: the version code and the version number
	 * @throws ClientProtocolException Thrown when the provided URL is invalid
	 * @throws IOException Thrown when the last version information could not be retrieved
	 */
	private String[] retrieveLatestVersion(AbstractHttpClient httpclient, String url) throws IOException {
		HttpResponse request = httpclient.execute(new HttpGet(url));
		InputStream stream = request.getEntity().getContent();
		String appVersion[] = HttpHelper.convertStreamToString(stream).split("\\|");
		stream.close();
		return appVersion;
	}

	@SuppressWarnings("deprecation")
	private void newNotification(String ticker, String title, String text, String downloadUrl, int notifyID) {
		PendingIntent pi = PendingIntent.getActivity(this, notifyID,
				new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)), PendingIntent.FLAG_UPDATE_CURRENT);
		Builder builder = new Notification.Builder(this).setSmallIcon(R.drawable.ic_stat_notification)
				.setTicker(ticker).setContentTitle(title).setContentText(text)
				.setLights(notificationSettings.getDesiredLedColour(), 600, 1000)
				.setSound(notificationSettings.getSound()).setAutoCancel(true).setContentIntent(pi);
		notificationManager.notify(notifyID, builder.getNotification());
	}

}
