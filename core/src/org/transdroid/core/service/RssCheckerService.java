package org.transdroid.core.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.rss.RssfeedsActivity_;
import org.transdroid.core.rssparser.Item;
import org.transdroid.core.rssparser.RssParser;
import org.transdroid.daemon.util.Collections2;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

/**
 * A background service that checks all user-configured RSS feeds for new items.
 * @author Eric Kok
 */
@EService
public class RssCheckerService extends IntentService {

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

	@Override
	protected void onHandleIntent(Intent intent) {

		if (!connectivityHelper.shouldPerformActions() || !notificationSettings.isEnabled()) {
			Log.d(this,
					"Skip the RSS checker service, as background data is disabled, the service is disabled or we are not connected.");
			return;
		}

		// Check every RSS feed for new items
		int unread = 0;
		Set<String> hasUnread = new LinkedHashSet<String>();
		for (RssfeedSetting feed : applicationSettings.getRssfeedSettings()) {
			try {

				Log.d(this, "Try to parse " + feed.getName() + " (" + feed.getUrl() + ")");
				RssParser parser = new RssParser(feed.getUrl());
				parser.parse();
				if (parser.getChannel() == null)
					continue;

				// Find the last item that is newer than the last viewed date
				for (Item item : parser.getChannel().getItems()) {
					if (item.getPubdate() != null && item.getPubdate().before(feed.getLastViewed())) {
						break;
					} else {
						unread++;
						if (!hasUnread.contains(feed.getName()))
							hasUnread.add(feed.getName());
					}
				}

				Log.d(this, feed.getName() + " has " + (hasUnread.contains(feed.getName()) ? "" : "no ")
						+ "unread items");

			} catch (Exception e) {
				// Ignore RSS feeds that could not be retrieved or parsed
			}
		}

		if (unread == 0) {
			// No new items; just exit
			return;
		}

		// Provide a notification, since there are new RSS items
		PendingIntent pi = PendingIntent.getActivity(this, 80000, new Intent(this, RssfeedsActivity_.class),
				Intent.FLAG_ACTIVITY_NEW_TASK);
		String title = getResources().getQuantityString(R.plurals.rss_service_new, unread, Integer.toString(unread));
		String forString = Collections2.joinString(hasUnread, ", ");
		Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_notification)
				.setTicker(title).setContentTitle(title)
				.setContentText(getString(R.string.rss_service_newfor, forString)).setNumber(unread)
				.setLights(notificationSettings.getDesiredLedColour(), 600, 1000)
				.setSound(notificationSettings.getSound()).setAutoCancel(true).setContentIntent(pi);
		if (notificationSettings.shouldVibrate())
			builder.setVibrate(notificationSettings.getDefaultVibratePattern());
		notificationManager.notify(80001, builder.build());

	}

}
