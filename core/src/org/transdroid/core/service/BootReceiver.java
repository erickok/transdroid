package org.transdroid.core.service;

import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.NotificationSettings_;
import org.transdroid.core.app.settings.SystemSettings;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.log.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Receives the intent that the device has been started in order to set up proper alarms for all background services.
 * @author Eric Kok
 */
public class BootReceiver extends BroadcastReceiver {

	public static final int ALARM_SERVERCHECKER = 0;
	public static final int ALARM_RSSCHECKER = 1;
	public static final int ALARM_APPUPDATES = 2;

	public static PendingIntent piServerChecker = null, piRssChecker = null, piAppUpdates = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		startBackgroundServices(context, false);
		startAppUpdatesService(context);
	}

	public static void startBackgroundServices(Context context, boolean forceReload) {
		NotificationSettings notificationSettings = NotificationSettings_.getInstance_(context);
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (notificationSettings.isEnabled() && (forceReload || (piServerChecker == null && piRssChecker == null))) {

			Log.d(context, "Boot signal received, starting server and rss checker background services");
			// Schedule repeating alarms, with the first being (somewhat) in 1 second from now
			piServerChecker = PendingIntent.getBroadcast(context, ALARM_SERVERCHECKER, new Intent(context,
					AlarmReceiver_.class).putExtra("service", ALARM_SERVERCHECKER), 0);
			piRssChecker = PendingIntent.getBroadcast(context, ALARM_RSSCHECKER, new Intent(context,
					AlarmReceiver_.class).putExtra("service", ALARM_RSSCHECKER), 0);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
					notificationSettings.getInvervalInMilliseconds(), piServerChecker);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
					notificationSettings.getInvervalInMilliseconds(), piRssChecker);

		}
	}

	public static void startAppUpdatesService(Context context) {
		SystemSettings systemSettings = SystemSettings_.getInstance_(context);
		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (systemSettings.checkForUpdates() && piAppUpdates == null) {

			Log.d(context, "Boot signal received, starting app update checker service");
			// Schedule a daily, with the first being (somewhat) in 1 second from now
			piAppUpdates = PendingIntent.getBroadcast(context, ALARM_APPUPDATES, new Intent(context,
					AlarmReceiver_.class).putExtra("service", ALARM_APPUPDATES), 0);
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

}
