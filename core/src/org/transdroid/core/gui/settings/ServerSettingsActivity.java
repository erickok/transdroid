package org.transdroid.core.gui.settings;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.daemon.Daemon;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * Activity that allows for a configuration of a server. The key can be supplied to update an existing server setting
 * instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName="activity_deleteableprefs")
public class ServerSettingsActivity extends SherlockPreferenceActivity {

	@Extra
	protected int key = -1;

	@Bean
	protected ApplicationSettings applicationSettings;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the raw preferences to show in this screen
		addPreferencesFromResource(R.xml.pref_server);

		// Bind the preferences to the correct storage key, e.g. the first server setting stores its address in the
		// 'server_address_0' shared preferences field
		if (key < 0) {
			key = applicationSettings.getMaxWebsearch() + 1;
		}
		findPreference("server_name").setKey("server_name_" + key);
		findPreference("server_type").setKey("server_type_" + key);
		findPreference("server_address").setKey("server_address_" + key);
		findPreference("server_port").setKey("server_port_" + key);
		findPreference("server_user").setKey("server_user_" + key);
		findPreference("server_pass").setKey("server_pass_" + key);
		findPreference("server_extrapass").setKey("server_extrapass_" + key);
		findPreference("server_localaddress").setKey("server_localaddress_" + key);
		findPreference("server_localnetwork").setKey("server_localnetwork_" + key);
		findPreference("server_folder").setKey("server_folder_" + key);
		findPreference("server_timeout").setKey("server_timeout_" + key);
		findPreference("server_alarmfinished").setKey("server_alarmfinished_" + key);
		findPreference("server_alarmnew").setKey("server_alarmnew_" + key);
		findPreference("server_os").setKey("server_os_" + key);
		findPreference("server_downloaddir").setKey("server_downloaddir_" + key);
		findPreference("server_ftpurl").setKey("server_ftpurl_" + key);
		findPreference("server_ftppass").setKey("server_ftppass_" + key);
		findPreference("server_sslenabled").setKey("server_sslenabled_" + key);
		findPreference("server_ssltrustall").setKey("server_ssltrustall_" + key);
		findPreference("server_ssltrustall_" + key).setDependency("server_sslenabled_" + key);
		findPreference("server_ssltrustkey").setKey("server_ssltrustkey_" + key);
		findPreference("server_ssltrustkey_" + key).setDependency("server_sslenabled_" + key);

		// Monitor preference changes
		getPreferenceScreen().setOnPreferenceChangeListener(onPreferenceChangeListener);
	}

	private OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			// TODO: This doesn't get called
			updatePreferenceAvailability();
			return true;
		}
	};

    @SuppressWarnings("deprecation")
	private void updatePreferenceAvailability() {
    	
		// Use daemon factory to see if the newly selected daemon supports the feature
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	Daemon daemonType = Daemon.fromCode(prefs.getString("server_type_" + key, null));
    	findPreference("server_extrapass_" + key).setEnabled(Daemon.supportsExtraPassword(daemonType));
    	findPreference("server_folder_" + key).setEnabled(daemonType == null? false: Daemon.supportsCustomFolder(daemonType));
    	findPreference("server_downloaddir_" + key).setEnabled(daemonType == null? false: Daemon.needsManualPathSpecified(daemonType));
    	//findPreference("server_ssltrustkey_" + key).setEnabled(sslValue && !sslTAValue);
    	
    	// Adjust title texts accordingly
    	findPreference("server_folder_" + key).setTitle(daemonType == Daemon.rTorrent? R.string.pref_scgifolder: R.string.pref_folder);
    	
    }
    
}
