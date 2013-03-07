package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;

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

		// Just load the notification-related preferences from XML
		addPreferencesFromResource(R.xml.pref_notifications);
		
	}
	
}
