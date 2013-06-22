package org.transdroid.core.gui.settings;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.json.JSONException;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.SettingsPersistence;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.DialogHelper;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.keyboardsurfer.android.widget.crouton.Crouton;

@EActivity
public class SystemSettingsActivity extends SherlockPreferenceActivity {

	protected static final int DIALOG_CHANGELOG = 0;
	protected static final int DIALOG_ABOUT = 1;
	protected static final int DIALOG_IMPORTSETTINGS = 2;
	protected static final int DIALOG_EXPORTSETTINGS = 3;
	protected static final String INSTALLHELP_URI = "http://www.transdroid.org/download/";

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

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Just load the system-related preferences from XML
		addPreferencesFromResource(R.xml.pref_system);

		// Handle outgoing links
		findPreference("system_sendlog").setOnPreferenceClickListener(onSendLogClick);
		findPreference("system_installhelp").setOnPreferenceClickListener(onInstallHelpClick);
		findPreference("system_changelog").setOnPreferenceClickListener(onChangeLogClick);
		findPreference("system_importsettings").setOnPreferenceClickListener(onImportSettingsClick);
		findPreference("system_exportsettings").setOnPreferenceClickListener(onExportSettingsClick);
		findPreference("system_about").setOnPreferenceClickListener(onAboutClick);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	private OnPreferenceClickListener onSendLogClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			errorLogSender.collectAndSendLog(SystemSettingsActivity.this, applicationSettings.getLastUsedServer());
			return true;
		}
	};

	private OnPreferenceClickListener onInstallHelpClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(INSTALLHELP_URI)));
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

	private OnPreferenceClickListener onChangeLogClick = new OnPreferenceClickListener() {
		@SuppressWarnings("deprecation")
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_CHANGELOG);
			return true;
		}
	};

	private OnPreferenceClickListener onAboutClick = new OnPreferenceClickListener() {
		@SuppressWarnings("deprecation")
		@Override
		public boolean onPreferenceClick(Preference preference) {
			showDialog(DIALOG_ABOUT);
			return true;
		}
	};

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CHANGELOG:
			return DialogHelper.showDialog(this, new ChangelogDialog());
		case DIALOG_ABOUT:
			return DialogHelper.showDialog(this, new AboutDialog());
		case DIALOG_IMPORTSETTINGS:
			// @formatter:off
			return new AlertDialog.Builder(this)
					.setMessage(
							getString(R.string.pref_import_dialog, SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
					.setPositiveButton(android.R.string.ok, importSettings)
					.setNegativeButton(android.R.string.cancel, null).create();
			// @formatter:on
		case DIALOG_EXPORTSETTINGS:
			// @formatter:off
			return new AlertDialog.Builder(this)
					.setMessage(
							getString(R.string.pref_export_dialog, SettingsPersistence.DEFAULT_SETTINGS_FILE.toString()))
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
				Crouton.showText(SystemSettingsActivity.this, R.string.error_no_valid_settings_file,
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
