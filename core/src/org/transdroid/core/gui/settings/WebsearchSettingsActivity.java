package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings_;

import android.os.Bundle;

/**
 * Activity that allows for a configuration of a web search site. The key can be supplied to update an existing web
 * search site setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName="activity_deleteableprefs")
public class WebsearchSettingsActivity extends KeyBoundPreferencesActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// Load the raw preferences to show in this screen
		init(R.xml.pref_websearch, ApplicationSettings_.getInstance_(this).getMaxWebsearch());
		initTextPreference("websearch_name");
		initTextPreference("websearch_baseurl");

	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		ApplicationSettings_.getInstance_(this).removeWebsearchSettings(key);
		finish();
	}
	
}
