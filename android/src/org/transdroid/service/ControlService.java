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

import java.util.List;

import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.PauseAllTask;
import org.transdroid.daemon.task.ResumeAllTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.task.StartAllTask;
import org.transdroid.daemon.task.StopAllTask;
import org.transdroid.daemon.util.DLog;
import org.transdroid.preferences.Preferences;
import org.transdroid.util.TLog;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * A service that can be asked to perform some control action on a
 * remote torrent server via an Intent.
 * 
 * @author erickok
 */
public class ControlService extends IntentService {

	private static final String LOG_NAME = "Control service";
	
	public static final String INTENT_SET_TRANSFER_RATES = "org.transdroid.control.SET_TRANSFER_RATES";
	public static final String INTENT_PAUSE_ALL = "org.transdroid.control.PAUSE_ALL";
	public static final String INTENT_RESUME_ALL = "org.transdroid.control.RESUME_ALL";
	public static final String INTENT_START_ALL = "org.transdroid.control.START_ALL";
	public static final String INTENT_STOP_ALL = "org.transdroid.control.STOP_ALL";
	public static final String INTENT_EXTRA_DAEMON = "DAEMON";
	public static final String INTENT_EXTRA_UPLOAD_RATE = "UPLOAD_RATE";
	public static final String INTENT_EXTRA_DOWNLOAD_RATE = "DOWNLOAD_RATE";
	
	public ControlService() {
		super(LOG_NAME);
		// Attach the Android TLog to the daemon logger
		DLog.setLogger(TLog.getInstance());
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {

		// Settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		List<DaemonSettings> allSettings = Preferences.readAllDaemonSettings(prefs);

		// Get the daemon to execute the action against
		DaemonSettings daemonSetting = parseDaemonFromIntent(intent.getExtras(), allSettings, prefs);
		if (daemonSetting == null) {
			TLog.e(LOG_NAME, "No default daemon can be found.");
			return;
		}
		IDaemonAdapter daemon = daemonSetting.getType().createAdapter(daemonSetting);
		
		// Execute the requested task
		DaemonTaskResult result = null;
		if (intent.getAction().equals(INTENT_SET_TRANSFER_RATES)) {
			result = setTransferRates(daemon, intent.getExtras());
		} else if (intent.getAction().equals(INTENT_PAUSE_ALL)) {
			result = PauseAllTask.create(daemon).execute();
		} else if (intent.getAction().equals(INTENT_RESUME_ALL)) {
			result = ResumeAllTask.create(daemon).execute();
		} else if (intent.getAction().equals(INTENT_STOP_ALL)) {
			result = StopAllTask.create(daemon).execute();
		} else if (intent.getAction().equals(INTENT_START_ALL)) {
			result = StartAllTask.create(daemon, false).execute();
		}
		if (result == null) {
			return;
		}
		
		// Log task result
		TLog.d(LOG_NAME, result.toString());
		
	}

	private static DaemonSettings parseDaemonFromIntent(Bundle extras, List<DaemonSettings> allSettings, SharedPreferences prefs) {
		if (allSettings.size() == 0) {
			return null;
		}
		if (extras != null && extras.containsKey(INTENT_EXTRA_DAEMON)) {
			// Explicitly supplied a daemon number; try to parse this
			int daemonNumber;
			try {
				daemonNumber = Integer.parseInt(extras.getString(INTENT_EXTRA_DAEMON));
				return allSettings.get(daemonNumber);
			} catch (Exception e) {
				TLog.e(LOG_NAME, "Invalid daemon number specified: \"" + extras.getString(INTENT_EXTRA_DAEMON) + "\" does not exist.");
				return null;
			}
		}
	
		// No daemon defined explicitly; use the last used daemon
		return Preferences.readLastUsedDaemonSettings(prefs, allSettings);
		
	}
		
	private DaemonTaskResult setTransferRates(IDaemonAdapter daemon, Bundle extras) {
		
		int downloadRate;
		int uploadRate;
		
		// Parse the extras for the new rates
		if (extras == null || !extras.containsKey(INTENT_EXTRA_DOWNLOAD_RATE)) {
			TLog.e(LOG_NAME, "Tried to set transfer rates, but no " + INTENT_EXTRA_DOWNLOAD_RATE + " was provided.");
			return null;
		} else {
			downloadRate = extras.getInt(INTENT_EXTRA_DOWNLOAD_RATE);
		}
		if (extras == null || !extras.containsKey(INTENT_EXTRA_UPLOAD_RATE)) {
			TLog.e(LOG_NAME, "Tried to set transfer rates, but no " + INTENT_EXTRA_UPLOAD_RATE + " was provided.");
			return null;
		} else {
			uploadRate = extras.getInt(INTENT_EXTRA_UPLOAD_RATE);
		}
		
		return SetTransferRatesTask.create(daemon, uploadRate, downloadRate).execute();
		
	}

}
