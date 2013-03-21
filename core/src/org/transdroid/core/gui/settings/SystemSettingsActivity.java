package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.DialogHelper;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

@EActivity
public class SystemSettingsActivity extends SherlockPreferenceActivity {

	protected static final int DIALOG_CHANGELOG = 0;
	protected static final int DIALOG_ABOUT = 1;
	protected static final String INSTALLHELP_URI = "http://www.transdroid.org/download/";

	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected ErrorLogSender errorLogSender;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Just load the system-related preferences from XML
		addPreferencesFromResource(R.xml.pref_system);
		
		// Handle outgoing links
		findPreference("system_sendlog").setOnPreferenceClickListener(onSendLogClick);
		findPreference("system_installhelp").setOnPreferenceClickListener(onInstallHelpClick);
		findPreference("system_changelog").setOnPreferenceClickListener(onChangeLogClick);
		findPreference("system_about").setOnPreferenceClickListener(onAboutClick);
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
		}
		return null;
	};
}
