package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.gui.AboutFragment;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.DialogHolderActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

@EActivity
public class SystemSettingsActivity extends SherlockPreferenceActivity {

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
		@Override
		public boolean onPreferenceClick(Preference preference) {
			// TODO: Implement change log screen
			Toast.makeText(SystemSettingsActivity.this, "TODO: Implement change log screen", Toast.LENGTH_LONG).show();
			return true;
		}
	};

	private OnPreferenceClickListener onAboutClick = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			DialogHolderActivity.showDialog(SystemSettingsActivity.this, AboutFragment.class);
			return true;
		}
	};
	
}
