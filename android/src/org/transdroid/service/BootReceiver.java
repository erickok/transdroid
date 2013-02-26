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

import org.transdroid.preferences.Preferences;
import org.transdroid.util.TLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;

/**
 * Receives a broadcast message when the device has started and is used to manually start/stop the alarm
 * service.
 * 
 * @author erickok
 * 
 */
public class BootReceiver extends BroadcastReceiver {

	private static final String LOG_NAME = "Boot receiver";

	private static AlarmManager mgr = null;
	private static PendingIntent pi = null;
	private static PendingIntent pui = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		TLog.d(LOG_NAME, "Received fresh boot broadcast; start alarm service");
		startAlarm(context);
		startUpdateCheck(context);
	}

	public static void cancelAlarm() {
		if (mgr != null) {
			mgr.cancel(pi);
		}
	}

	public static void startAlarm(Context context) {
		AlarmSettings settings = Preferences.readAlarmSettings(PreferenceManager.getDefaultSharedPreferences(context));

		if (settings.isAlarmEnabled()) {
			// Set up PendingIntent for the alarm service
			if (mgr == null)
				mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, AlarmReceiver.class);
			pi = PendingIntent.getBroadcast(context, 0, i, 0);
			// First intent after a small (2 second) delay and repeat at the user-set intervals
			mgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, settings
				.getAlarmIntervalInMilliseconds(), pi);
		}
	}

	public static void cancelUpdateCheck() {
		if (mgr != null) {
			mgr.cancel(pui);
		}
	}

	public static void startUpdateCheck(Context context) {
		// Set up PendingIntent for the alarm service
		if (mgr == null)
			mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, UpdateReceiver.class);
		pui = PendingIntent.getBroadcast(context, 1, i, 0);
		// First intent after a small (2 second) delay and repeat every day
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000,
			AlarmManager.INTERVAL_DAY, pui);
	}

}
