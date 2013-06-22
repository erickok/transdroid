package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.daemon.Daemon;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;

/**
 * Activity that allows for a configuration of a server. The key can be supplied to update an existing server setting
 * instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName = "activity_deleteableprefs")
public class ServerSettingsActivity extends KeyBoundPreferencesActivity {

	private EditTextPreference extraPass, folder, downloadDir;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Load the raw preferences to show in this screen
		init(R.xml.pref_server, ApplicationSettings_.getInstance_(this).getMaxServer());
		initTextPreference("server_name");
		initListPreference("server_type");
		initTextPreference("server_address");
		initTextPreference("server_port");
		initTextPreference("server_user");
		initTextPreference("server_pass");
		extraPass = initTextPreference("server_extrapass");
		initTextPreference("server_localaddress");
		initTextPreference("server_localnetwork");
		folder = initTextPreference("server_folder");
		initTextPreference("server_timeout", "8");
		initBooleanPreference("server_alarmfinished", true);
		initBooleanPreference("server_alarmnew");
		initListPreference("server_os", "type_linux");
		downloadDir = initTextPreference("server_downloaddir");
		initTextPreference("server_ftpurl");
		initTextPreference("server_ftppass");
		initBooleanPreference("server_sslenabled");
		initBooleanPreference("server_ssltrustall", false, "server_sslenabled");
		initTextPreference("server_ssltrustkey", null, "server_sslenabled");

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(resName = "action_removesettings")
	protected void removeSettings() {
		ApplicationSettings_.getInstance_(this).removeServerSettings(key);
		finish();
	}
	
	@Override
	protected void onPreferencesChanged() {

		// Use daemon factory to see if the newly selected daemon supports the feature
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Daemon daemonType = Daemon.fromCode(prefs.getString("server_type_" + key, null));
		extraPass.setEnabled(Daemon.supportsExtraPassword(daemonType));
		folder.setEnabled(daemonType == null ? false : Daemon.supportsCustomFolder(daemonType));
		downloadDir.setEnabled(daemonType == null ? false : Daemon.needsManualPathSpecified(daemonType));
		// sslTrustKey.setEnabled(sslValue && !sslTAValue);

		// Adjust title texts accordingly
		folder.setTitle(daemonType == Daemon.rTorrent ? R.string.pref_scgifolder : R.string.pref_folder);

	}

}
