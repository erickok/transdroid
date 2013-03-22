package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings_;

import android.os.Bundle;

/**
 * Activity that allows for a configuration of some RSS feed. The key can be supplied to update an
 * existing RSS feed setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName="activity_deleteableprefs")
public class RssfeedSettingsActivity extends KeyBoundPreferencesActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		// Load the raw preferences to show in this screen
		init(R.xml.pref_rssfeed, ApplicationSettings_.getInstance_(this).getMaxRssfeed());
		initTextPreference("rssfeed_name");
		initTextPreference("rssfeed_url");
		initBooleanPreference("rssfeed_reqauth");

	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		ApplicationSettings_.getInstance_(this).removeRssfeedSettings(key);
		finish();
	}

}
