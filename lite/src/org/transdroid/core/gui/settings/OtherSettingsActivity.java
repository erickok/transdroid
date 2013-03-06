package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.transdroid.core.app.settings.ApplicationSettings;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

@EActivity
public class OtherSettingsActivity extends SherlockPreferenceActivity {

	@Extra
	protected int preferencesResourceID;
	
	@Bean
	protected ApplicationSettings applicationSettings;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Just load the preferences from XML, of which the ID is supplied as extra
		addPreferencesFromResource(preferencesResourceID);
		
	}
	
}
