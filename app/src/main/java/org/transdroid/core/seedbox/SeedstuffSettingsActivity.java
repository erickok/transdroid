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
package org.transdroid.core.seedbox;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.R;
import org.transdroid.core.gui.settings.*;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;

/**
 * Activity that allows for the configuration of a Seedstuff seedbox. The key can be supplied to update an
 * existing server setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName = "activity_deleteableprefs")
public class SeedstuffSettingsActivity extends KeyBoundPreferencesActivity {

	private EditTextPreference excludeFilter, includeFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Load the raw preferences to show in this screen
		init(R.xml.pref_seedbox_seedstuff,
				SeedboxProvider.Seedstuff.getSettings().getMaxSeedboxOrder(
						PreferenceManager.getDefaultSharedPreferences(this)));
		initTextPreference("seedbox_seedstuff_name");
		initTextPreference("seedbox_seedstuff_server");
		initTextPreference("seedbox_seedstuff_user");
		initTextPreference("seedbox_seedstuff_pass");
		initBooleanPreference("seedbox_seedstuff_alarmfinished", true);
		initBooleanPreference("seedbox_seedstuff_alarmnew", true);
		excludeFilter = initTextPreference("seedbox_seedstuff_alarmexclude");
		includeFilter = initTextPreference("seedbox_seedstuff_alarminclude");

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SeedboxProvider.Seedstuff.getSettings().removeServerSetting(prefs, key);
		finish();
	}

	@Override
	protected void onPreferencesChanged() {

		// Show the exclude and the include filters if notifying
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean alarmFinished = prefs.getBoolean("seedbox_seedstuff_alarmfinished_" + key, true);
		boolean alarmNew = prefs.getBoolean("seedbox_seedstuff_alarmnew_" + key, true);
		excludeFilter.setEnabled(alarmNew || alarmFinished);
		includeFilter.setEnabled(alarmNew || alarmFinished);

	}

}
