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
package org.transdroid.core.gui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings_;

/**
 * Activity that allows for a configuration of some RSS feed. The key can be supplied to update an existing RSS feed setting instead of creating a new
 * one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(R.menu.activity_deleteableprefs)
public class RssfeedSettingsActivity extends KeyBoundPreferencesActivity {

	private static final int DIALOG_CONFIRMREMOVE = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Load the raw preferences to show in this screen
		init(R.xml.pref_rssfeed, ApplicationSettings_.getInstance_(this).getMaxRssfeed());
		initTextPreference("rssfeed_name");
		initTextPreference("rssfeed_url");
		initBooleanPreference("rssfeed_alarmnew");
		initTextPreference("rssfeed_exclude");
		initTextPreference("rssfeed_include");
		// TODO: Replace this for cookies support like web searches
		initBooleanPreference("rssfeed_reqauth");

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@SuppressWarnings("deprecation")
	@OptionsItem(R.id.action_removesettings)
	protected void removeSettings() {
		showDialog(DIALOG_CONFIRMREMOVE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_CONFIRMREMOVE:
				return new AlertDialog.Builder(this).setMessage(R.string.pref_confirmremove)
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ApplicationSettings_.getInstance_(RssfeedSettingsActivity.this).removeRssfeedSettings(key);
								finish();
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
		}
		return null;
	}

}
