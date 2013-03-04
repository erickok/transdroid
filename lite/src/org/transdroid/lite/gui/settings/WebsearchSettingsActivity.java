package org.transdroid.lite.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.transdroid.core.R;
import org.transdroid.lite.app.settings.ApplicationSettings;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * Activity that allows for a configuration of a web search site. The key can be supplied to update an existing web
 * search site setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
public class WebsearchSettingsActivity extends SherlockPreferenceActivity {

	@Extra
	protected int key = -1;

	@Bean
	protected ApplicationSettings applicationSettings;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the raw preferences to show in this screen
		addPreferencesFromResource(R.xml.pref_websearch);

		// Bind the preferences to the correct storage key, e.g. the first site setting stores its URL in the
		// 'websearch_baseurl_0' shared preferences field
		if (key < 0) {
			key = applicationSettings.getMaxWebsearch() + 1;
		}
		findPreference("websearch_name").setKey("websearch_name_" + key);
		findPreference("websearch_baseurl").setKey("websearch_baseurl_" + key);

	}

}
