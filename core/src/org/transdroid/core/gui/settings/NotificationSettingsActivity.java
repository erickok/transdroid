package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.service.BootReceiver;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

@EActivity
public class NotificationSettingsActivity extends SherlockPreferenceActivity implements
		OnSharedPreferenceChangeListener {

	@Bean
	protected NotificationSettings notificationSettings;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Just load the notification-related preferences from XML
		addPreferencesFromResource(R.xml.pref_notifications);

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

		if (!notificationSettings.isEnabled()) {
			// Disabled background notifications; disable the alarms that start the service
			BootReceiver.cancelBackgroundServices(getApplicationContext());
		}

		// (Re-)enable the alarms for the background services
		BootReceiver.startBackgroundServices(getApplicationContext(), true);
	}

}
