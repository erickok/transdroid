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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.json.JSONException;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.SettingsPersistence;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.search.SearchHistoryProvider;
import org.transdroid.core.service.BootReceiver;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.keyboardsurfer.android.widget.crouton.Crouton;

@EActivity
public class SystemSettingsActivity extends PreferenceActivity {

	protected static final int DIALOG_IMPORTSETTINGS = 0;
	protected static final int DIALOG_EXPORTSETTINGS = 1;

	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected ErrorLogSender errorLogSender;
	@Bean
	protected SettingsPersistence settingsPersistence;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Just load the system-related preferences from XML
		addPreferencesFromResource(R.xml.pref_system);

		// Handle outgoing links and preference changes
		if (navigationHelper.enableUpdateChecker()) {
			findPreference("system_checkupdates").setOnPreferenceClickListener(onCheckUpdatesClick);
		} else {
			getPreferenceScreen().removePreference(findPreference("system_checkupdates"));
		}
		findPreference("system_clearsearch").setOnPreferenceClickListener(onClearSearchClick);
		findPreference("system_importsettings").setOnPreferenceClickListener(onImportSettingsClick);
		findPreference("system_exportsettings").setOnPreferenceClickListener(onExportSettingsClick);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	private OnPreferenceClickListener onCheckUpdatesClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (((CheckBoxPreference) preference).isChecked())
				BootReceiver.startAppUpdatesService(getApplicationContext());
			else
				BootReceiver.cancelAppUpdates(getApplicationContext());
			return true;
		}
	};

	private OnPreferenceClickListener onImportSettingsClick = new OnPreferenceClickListener() {
		@SuppressWarnings("deprecation")
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_IMPORTSETTINGS);
			return true;
		}
	};

	private OnPreferenceClickListener onExportSettingsClick = new OnPreferenceClickListener() {
		@SuppressWarnings("deprecation")
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_EXPORTSETTINGS);
			return true;
		}
	};

	private OnPreferenceClickListener onClearSearchClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			SearchHistoryProvider.clearHistory(getApplicationContext());
			Crouton.showText(SystemSettingsActivity.this, R.string.pref_clearsearch_success,
					NavigationHelper.CROUTON_INFO_STYLE);
			return true;
		}
	};

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_IMPORTSETTINGS:
			// @formatter:off
			return new AlertDialog.Builder(this)
					.setMessage(
							getString(
									R.string.pref_import_dialog, 
									getString(R.string.app_name), 
									SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
					.setPositiveButton(android.R.string.ok, importSettings)
					.setNegativeButton(android.R.string.cancel, null).create();
			// @formatter:on
		case DIALOG_EXPORTSETTINGS:
			// @formatter:off
			return new AlertDialog.Builder(this)
					.setMessage(
							getString(
									R.string.pref_export_dialog, 
									getString(R.string.app_name), 
									SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
					.setPositiveButton(android.R.string.ok, exportSettings)
					.setNegativeButton(android.R.string.cancel, null).create();
			// @formatter:on
		}
		return null;
	}

	private OnClickListener importSettings = new OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
			try {
				settingsPersistence.importSettings(prefs, SettingsPersistence.DEFAULT_SETTINGS_FILE);
				Crouton.showText(SystemSettingsActivity.this, R.string.pref_import_success,
						NavigationHelper.CROUTON_INFO_STYLE);
			} catch (FileNotFoundException e) {
				Crouton.showText(SystemSettingsActivity.this, R.string.error_file_not_found,
						NavigationHelper.CROUTON_ERROR_STYLE);
			} catch (JSONException e) {
				Crouton.showText(SystemSettingsActivity.this,
						getString(R.string.error_no_valid_settings_file, getString(R.string.app_name)),
						NavigationHelper.CROUTON_ERROR_STYLE);
			}
		}
	};

	private OnClickListener exportSettings = new OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SystemSettingsActivity.this);
			try {
				settingsPersistence.exportSettings(prefs, SettingsPersistence.DEFAULT_SETTINGS_FILE);
				Crouton.showText(SystemSettingsActivity.this, R.string.pref_export_success,
						NavigationHelper.CROUTON_INFO_STYLE);
			} catch (JSONException e) {
				Crouton.showText(SystemSettingsActivity.this, R.string.error_cant_write_settings_file,
						NavigationHelper.CROUTON_ERROR_STYLE);
			} catch (IOException e) {
				Crouton.showText(SystemSettingsActivity.this, R.string.error_cant_write_settings_file,
						NavigationHelper.CROUTON_ERROR_STYLE);
			}
		}
	};

}
