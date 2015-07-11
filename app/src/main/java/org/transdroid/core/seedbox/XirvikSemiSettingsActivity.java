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
import android.preference.PreferenceManager;

/**
 * Activity that allows for the configuration of a Xirvik semi-dedicated seedbox. The key can be supplied to update an
 * existing server setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName = "activity_deleteableprefs")
public class XirvikSemiSettingsActivity extends KeyBoundPreferencesActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Load the raw preferences to show in this screen
		init(R.xml.pref_seedbox_xirviksemi,
				SeedboxProvider.XirvikSemi.getSettings().getMaxSeedboxOrder(
						PreferenceManager.getDefaultSharedPreferences(this)));
		initTextPreference("seedbox_xirviksemi_name");
		initTextPreference("seedbox_xirviksemi_server");
		initTextPreference("seedbox_xirviksemi_user");
		initTextPreference("seedbox_xirviksemi_pass");

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SeedboxProvider.XirvikSemi.getSettings().removeServerSetting(prefs, key);
		finish();
	}

}
