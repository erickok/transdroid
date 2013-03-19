package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * Activity that allows for a configuration of some RSS feed. The key can be supplied to update an
 * existing RSS feed setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(R.menu.activity_deleteableprefs)
public class RssfeedSettingsActivity extends SherlockPreferenceActivity {

	@Extra
	protected int key = -1;

	@Bean
	protected ApplicationSettings applicationSettings;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the raw preferences to show in this screen
		addPreferencesFromResource(R.xml.pref_rssfeed);

		// Bind the preferences to the correct storage key, e.g. the first RSS feed setting stores its URL in the
		// 'rssfeed_url_0' shared preferences field
		if (key < 0) {
			key = applicationSettings.getMaxRssfeed() + 1;
		}
		findPreference("rssfeed_name").setKey("rssfeed_name_" + key);
		findPreference("rssfeed_url").setKey("rssfeed_url_" + key);
		findPreference("rssfeed_reqauth").setKey("rssfeed_reqauth_" + key);

	}

}
