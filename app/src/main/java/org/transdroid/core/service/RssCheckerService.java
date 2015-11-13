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

import android.app.IntentService;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.rss.*;
import org.transdroid.core.rssparser.Item;
import org.transdroid.core.rssparser.RssParser;
import org.transdroid.daemon.util.Collections2;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A background service that checks all user-configured RSS feeds for new items.
 * @author Eric Kok
 */
@EService
public class RssCheckerService extends IntentService {

	@Bean
	protected Log log;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected NotificationSettings notificationSettings;
	@Bean
	protected ApplicationSettings applicationSettings;
	@SystemService
	protected NotificationManager notificationManager;

	public RssCheckerService() {
		super("RssCheckerService");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onHandleIntent(Intent intent) {

		if (!connectivityHelper.shouldPerformBackgroundActions() || !notificationSettings.isEnabledForRss()) {
			log.d(this,
					"Skip the RSS checker service, as background data is disabled, the service is disabled or we are not connected.");
			return;
		}

		// Check every RSS feed for new items
		int unread = 0;
		Set<String> hasUnread = new LinkedHashSet<String>();
		for (RssfeedSetting feed : applicationSettings.getRssfeedSettings()) {
			try {

				if (!feed.shouldAlarmOnNewItems()) {
					log.d(this, "Skip checker for " + feed.getName() + " as alarms are disabled");
					continue;
				}

				log.d(this, "Try to parse " + feed.getName() + " (" + feed.getUrl() + ")");
				RssParser parser = new RssParser(feed.getUrl(), feed.getExcludeFilter(), feed.getIncludeFilter());
				parser.parse();
				if (parser.getChannel() == null) {
					continue;
				}

				// Find the last item that is newer than the last viewed date
				for (Item item : parser.getChannel().getItems()) {
					if (item.getPubdate() != null && item.getPubdate().before(feed.getLastViewed())) {
						break;
					} else {
						unread++;
						if (!hasUnread.contains(feed.getName())) {
							hasUnread.add(feed.getName());
						}
					}
				}

				log.d(this,
						feed.getName() + " has " + (hasUnread.contains(feed.getName()) ? "" : "no ") + "unread items");

			} catch (Exception e) {
				// Ignore RSS feeds that could not be retrieved or parsed
			}
		}

		if (unread == 0) {
			// No new items; just exit
			return;
		}

		// Provide a notification, since there are new RSS items
		PendingIntent pi = PendingIntent
				.getActivity(this, 80000, new Intent(this, RssfeedsActivity_.class), PendingIntent.FLAG_UPDATE_CURRENT);
		String title = getResources().getQuantityString(R.plurals.rss_service_new, unread, Integer.toString(unread));
		String forString = Collections2.joinString(hasUnread, ", ");
		Builder builder = new Notification.Builder(this).setSmallIcon(R.drawable.ic_stat_notification).setTicker(title)
				.setContentTitle(title).setContentText(getString(R.string.rss_service_newfor, forString))
				.setNumber(unread).setLights(notificationSettings.getDesiredLedColour(), 600, 1000)
				.setSound(notificationSettings.getSound()).setAutoCancel(true).setContentIntent(pi);
		if (notificationSettings.shouldVibrate()) {
			builder.setVibrate(notificationSettings.getDefaultVibratePattern());
		}
		notificationManager.notify(80001, builder.getNotification());

	}

}
