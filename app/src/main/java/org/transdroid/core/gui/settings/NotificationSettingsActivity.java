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
package org.transdroid.core.gui.settings;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.R;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.service.BootReceiver;

@EActivity
public class NotificationSettingsActivity extends PreferenceCompatActivity implements OnSharedPreferenceChangeListener {

	@Bean
	protected NotificationSettings notificationSettings;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Load the notification-related preferences from XML and update availability thereof
		addPreferencesFromResource(R.xml.pref_notifications);
		boolean disabled = !notificationSettings.isEnabledForRss() && !notificationSettings.isEnabledForTorrents();
		updatePrefsEnabled(disabled);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		// Start/stop the background service appropriately
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		boolean disabled = !notificationSettings.isEnabledForRss() && !notificationSettings.isEnabledForTorrents();
		updatePrefsEnabled(disabled);

		if (disabled) {
			// Disabled all background notifications; disable the alarms that start the service
			BootReceiver.cancelBackgroundServices(getApplicationContext());
		}

		// (Re-)enable the alarms for the background services
		// Note that this still respects the user preference
		BootReceiver.startBackgroundServices(getApplicationContext(), true);
	}

	@SuppressWarnings("deprecation")
	private void updatePrefsEnabled(boolean disabled) {
		findPreference("notifications_interval").setEnabled(!disabled);
		findPreference("notifications_sound").setEnabled(!disabled);
		findPreference("notifications_vibrate").setEnabled(!disabled);
		findPreference("notifications_ledcolour").setEnabled(!disabled);
		findPreference("notifications_adwnotify").setEnabled(!disabled);
	}

}
