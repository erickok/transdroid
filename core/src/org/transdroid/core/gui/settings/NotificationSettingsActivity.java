package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

@EActivity
public class NotificationSettingsActivity extends SherlockPreferenceActivity {

	@Bean
	protected ApplicationSettings applicationSettings;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Just load the notification-related preferences from XML
		addPreferencesFromResource(R.xml.pref_notifications);
		
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

}
