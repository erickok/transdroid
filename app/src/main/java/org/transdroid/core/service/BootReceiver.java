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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EReceiver;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.log.*;
import org.transdroid.core.gui.navigation.*;

/**
 * Receives the intent that the device has been started in order to set up proper alarms for all background services.
 * @author Eric Kok
 */
@EReceiver
public class BootReceiver extends BroadcastReceiver {

	public static final int ALARM_SERVERCHECKER = 0;
	public static final int ALARM_RSSCHECKER = 1;
	public static final int ALARM_APPUPDATES = 2;

	public static PendingIntent piServerChecker = null, piRssChecker = null, piAppUpdates = null;

	@Bean
	protected Log log;

	public static void startBackgroundServices(Context context, boolean forceReload) {
		NotificationSettings notificationSettings = NotificationSettings_.getInstance_(context);
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		// Start the alarms if one of the notifications are enabled and we do not yet have the alarms running
		// (or should reload it forcefully)
		if ((notificationSettings.isEnabledForRss() || notificationSettings.isEnabledForTorrents()) &&
				(forceReload || (piServerChecker == null && piRssChecker == null))) {

			Log_.getInstance_(context)
					.d("BootReceiver", "Boot signal received, starting server and rss checker background services");
			// Schedule repeating alarms, with the first being (somewhat) in 1 second from now
			piServerChecker = PendingIntent.getBroadcast(context, ALARM_SERVERCHECKER,
					new Intent(context, AlarmReceiver_.class).putExtra("service", ALARM_SERVERCHECKER), 0);
			piRssChecker = PendingIntent.getBroadcast(context, ALARM_RSSCHECKER,
					new Intent(context, AlarmReceiver_.class).putExtra("service", ALARM_RSSCHECKER), 0);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
					notificationSettings.getInvervalInMilliseconds(), piServerChecker);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
					notificationSettings.getInvervalInMilliseconds(), piRssChecker);

		}
	}

	public static void startAppUpdatesService(Context context) {
		SystemSettings systemSettings = SystemSettings_.getInstance_(context);
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (NavigationHelper_.getInstance_(context).enableUpdateChecker() && systemSettings.checkForUpdates() &&
				piAppUpdates == null) {

			Log_.getInstance_(context).d("BootReceiver", "Boot signal received, starting app update checker service");
			// Schedule a daily, with the first being (somewhat) in 1 second from now
			piAppUpdates = PendingIntent.getBroadcast(context, ALARM_APPUPDATES,
					new Intent(context, AlarmReceiver_.class).putExtra("service", ALARM_APPUPDATES), 0);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
					AlarmManager.INTERVAL_DAY, piAppUpdates);

		}
	}

	public static void cancelBackgroundServices(Context context) {
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (piServerChecker != null) {
			alarms.cancel(piServerChecker);
			piServerChecker = null;
		}
		if (piRssChecker != null) {
			alarms.cancel(piRssChecker);
			piRssChecker = null;
		}
	}

	public static void cancelAppUpdates(Context context) {
		if (piAppUpdates != null) {
			AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarms.cancel(piAppUpdates);
			piAppUpdates = null;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		startBackgroundServices(context, false);
		startAppUpdatesService(context);
	}

}
