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
package org.transdroid.core.app.settings;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.transdroid.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;

/**
 * Allows instantiation of the settings specified in R.xml.pref_notifications.
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class NotificationSettings {

	@RootContext
	protected Context context;
	private SharedPreferences prefs;
	
	protected NotificationSettings(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Whether the background service is enabled and the user wants to receive RSS-related notifications
	 * @return True if the server should be checked for RSS feed updates
	 */
	public boolean isEnabledForRss() {
		return prefs.getBoolean("notifications_enabledrss", true);
	}

	/**
	 * Whether the background service is enabled and the user wants to receive torrent-related notifications
	 * @return True if the server should be checked for torrent status updates
	 */
	public boolean isEnabledForTorrents() {
		return prefs.getBoolean("notifications_enabled", true);
	}

	private String getRawInverval() {
		return prefs.getString("notifications_interval", "10800");
	}

	/**
	 * Returns the interval between two server checks
	 * @return The interval, in milliseconds
	 */
	public Long getInvervalInMilliseconds() {
		return Long.parseLong(getRawInverval()) * 1000L;
	}

	private String getRawSound() {
		return prefs.getString("notifications_sound", null);
	}

	/**
	 * Returns the sound (ring tone) to play on a new notification, or null if it should not play any 
	 * @return Either the user-specified sound, null if the user specified 'Silent' or the system default notification sound
	 */
	public Uri getSound() {
		String raw = getRawSound();
		if (raw == null)
			return null;
		if (raw.equals(""))
			return Settings.System.DEFAULT_NOTIFICATION_URI;
		return Uri.parse(raw);
	}

	/**
	 * Whether the device should vibrate on a new notification
	 * @return
	 */
	public boolean shouldVibrate() {
		return prefs.getBoolean("notifications_vibrate", false);
	}

	/**
	 * Returns the default vibrate pattern to use if the user enabled notification vibrations; check
	 * {@link #shouldVibrate()},
	 * @return A unique pattern for vibrations in Transdroid
	 */
	public long[] getDefaultVibratePattern() {
		return new long[]{100, 100, 200, 300, 400, 700}; // Unique pattern?
	}
	
	private int getRawLedColour() {
		return prefs.getInt("notifications_ledcolour", -1);
	}

	/**
	 * Returns the LED colour to use on a new notification
	 * @return The integer value of the user-specified or default colour
	 */
	public int getDesiredLedColour() {
		int raw = getRawLedColour();
		if (raw <= 0)
			return context.getResources().getColor(R.color.ledgreen);
		return raw;
	}

	/**
	 * Whether the background service should report to ADW Launcher
	 * @return True if the user want Transdroid to report to ADW Launcher
	 */
	public boolean shouldReportToAdwLauncher() {
		return prefs.getBoolean("notifications_adwnotify", false);
	}

}
