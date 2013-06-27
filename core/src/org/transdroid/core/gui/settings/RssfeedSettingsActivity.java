package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.*;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
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

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Load the raw preferences to show in this screen
		init(R.xml.pref_rssfeed, ApplicationSettings_.getInstance_(this).getMaxRssfeed());
		initTextPreference("rssfeed_name");
		initTextPreference("rssfeed_url");
		initBooleanPreference("rssfeed_reqauth");
		// TODO: Replace this for cookies support like web searches

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		ApplicationSettings_.getInstance_(this).removeRssfeedSettings(key);
		finish();
	}

}
