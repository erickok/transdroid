package org.transdroid.lite.app.settings;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;

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
	 * Whether the background service is enabled, i.e. whether the user want to receive notifications
	 * @return True if the server should be checked for torrent status updates
	 */
	public boolean isEnabled() {
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
