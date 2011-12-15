/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.preferences;

import java.net.URI;
import java.net.URISyntaxException;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.Toast;

public class PreferencesServer extends PreferenceActivity {

	public static final String PREFERENCES_SERVER_KEY = "PREFERENCES_SERVER_POSTFIX";
	private static final String IP_ADDRESS_FORMAT = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
	// From http://regexlib.com/REDetails.aspx?regexp_id=829
	private static final String IPv6_ADDRESS_FORMAT = "^([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}$";
	// From http://stackoverflow.com/questions/1418423/the-hostname-regex
	private static final String HOST_NAME_FORMAT = "^(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|\\b-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|\\b-){0,61}[0-9A-Za-z])?)*\\.?$";

	private String serverPostfix;
	// These preferences are members so they can be accessed by the updateOptionAvailibility event
	private TransdroidEditTextPreference name;
	private TransdroidListPreference daemon;
	private TransdroidEditTextPreference address;
	private TransdroidEditTextPreference port;
	private TransdroidCheckBoxPreference ssl;
	private TransdroidCheckBoxPreference sslTrustAll;
	private TransdroidEditTextPreference sslTrustKey;
	private TransdroidEditTextPreference folder;
	private TransdroidCheckBoxPreference auth;
	private TransdroidEditTextPreference user;
	private TransdroidEditTextPreference pass;
	private TransdroidListPreference os;
	private TransdroidEditTextPreference downloadDir;
	private TransdroidEditTextPreference ftpUrl;
	private TransdroidEditTextPreference timeout;
	private TransdroidCheckBoxPreference alarmFinished;
	private TransdroidCheckBoxPreference alarmNew;

	private String nameValue = null;
	private String daemonValue = null;
	private String addressValue = null;
	private String portValue = null;
	
	private boolean authValue = false;
	private String userValue = null;
	//private String passValue = null;
	
	private boolean sslValue = false;
	private boolean sslTAValue = false;
	private String sslTrustKeyValue = null;
	private String folderValue = null;
	private String osValue = null;
	private String downloadDirValue = null;
	private String ftpUrlValue = null;
	private String timeoutValue = null;
	//private boolean alarmFinishedValue = false;
	//private boolean alarmNewValue = false;
	
	// TODO: Allow setting of FTP password
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // For which server?
        serverPostfix = getIntent().getStringExtra(PREFERENCES_SERVER_KEY);
        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        nameValue = prefs.getString(Preferences.KEY_PREF_NAME + serverPostfix, null);
        daemonValue = prefs.getString(Preferences.KEY_PREF_DAEMON + serverPostfix, null);
        addressValue = prefs.getString(Preferences.KEY_PREF_ADDRESS + serverPostfix, null);
        portValue = prefs.getString(Preferences.KEY_PREF_PORT + serverPostfix, null);

        authValue = prefs.getBoolean(Preferences.KEY_PREF_AUTH + serverPostfix, false);
        userValue = prefs.getString(Preferences.KEY_PREF_USER + serverPostfix, null);
        //passValue = prefs.getString(Preferences.KEY_PREF_PASS + serverPostfix, null);

        sslValue = prefs.getBoolean(Preferences.KEY_PREF_SSL + serverPostfix, false);
        //sslTAValue = prefs.getBoolean(Preferences.KEY_PREF_SSL_TRUST_ALL + serverPostfix, false);
        sslTrustKeyValue = prefs.getString(Preferences.KEY_PREF_SSL_TRUST_KEY + serverPostfix, null);
        folderValue = prefs.getString(Preferences.KEY_PREF_FOLDER + serverPostfix, null);
        osValue = prefs.getString(Preferences.KEY_PREF_OS + serverPostfix, "type_windows");
        downloadDirValue = prefs.getString(Preferences.KEY_PREF_DOWNLOADDIR + serverPostfix, null);
        ftpUrlValue = prefs.getString(Preferences.KEY_PREF_FTPURL + serverPostfix, null);
        timeoutValue = prefs.getString(Preferences.KEY_PREF_TIMEOUT + serverPostfix, null);
        //alertFinishedValue = prefs.getBoolean(Preferences.KEY_PREF_ALERT_FINISHED + serverPostfix, true);

        // Create preference objects
        getPreferenceScreen().setTitle(R.string.pref_server);
        // Basic
        PreferenceCategory basic = new PreferenceCategory(this);
        basic.setTitle(R.string.pref_basic);
        getPreferenceScreen().addItemFromInflater(basic);        
        // Name
        name = new TransdroidEditTextPreference(this);
        name.setTitle(R.string.pref_name);
        name.setKey(Preferences.KEY_PREF_NAME + serverPostfix);
        name.getEditText().setSingleLine();
        name.setDialogTitle(R.string.pref_name);
        name.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(name);
        // Daemon
        daemon = new TransdroidListPreference(this);
        daemon.setTitle(R.string.pref_daemon);
        daemon.setKey(Preferences.KEY_PREF_DAEMON + serverPostfix);
        daemon.setEntries(R.array.pref_daemon_types);
        daemon.setEntryValues(R.array.pref_daemon_values);
        daemon.setDialogTitle(R.string.pref_daemon);
        daemon.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(daemon);
        // Address
        address = new TransdroidEditTextPreference(this);
        address.setTitle(R.string.pref_address);
        address.setKey(Preferences.KEY_PREF_ADDRESS + serverPostfix);
        address.getEditText().setSingleLine();
        address.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setDialogTitle(R.string.pref_address);
        address.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(address);
        // Port
        port = new TransdroidEditTextPreference(this);
        port.setTitle(R.string.pref_port);
        port.setKey(Preferences.KEY_PREF_PORT + serverPostfix);
        port.getEditText().setSingleLine();
        port.getEditText().setInputType(port.getEditText().getInputType() | EditorInfo.TYPE_CLASS_NUMBER);
        port.setDialogTitle(R.string.pref_port);
        port.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(port);

        // Auth
        auth = new TransdroidCheckBoxPreference(this);
        auth.setTitle(R.string.pref_auth);
        auth.setKey(Preferences.KEY_PREF_AUTH + serverPostfix);
        auth.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(auth);
        // User
        user = new TransdroidEditTextPreference(this);
        user.setTitle(R.string.pref_user);
        user.setKey(Preferences.KEY_PREF_USER + serverPostfix);
        user.getEditText().setSingleLine();
        user.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
        user.setDialogTitle(R.string.pref_user);
        user.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(user);
        // Pass
        pass = new TransdroidEditTextPreference(this);
        pass.setTitle(R.string.pref_pass);
        pass.setKey(Preferences.KEY_PREF_PASS + serverPostfix);
        pass.getEditText().setSingleLine();
        pass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        pass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        pass.setDialogTitle(R.string.pref_pass);
        pass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(pass);

        // Folder
        folder = new TransdroidEditTextPreference(this);
        if (Daemon.fromCode(daemonValue) == Daemon.rTorrent) {
	        folder.setTitle(R.string.pref_folder);
	        folder.setSummary(R.string.pref_folder_info);
        } else if (Daemon.fromCode(daemonValue) == Daemon.Tfb4rt){
	        folder.setTitle(R.string.pref_folder2);
	        folder.setSummary(R.string.pref_folder2_info);
        } else {
	        folder.setTitle(R.string.pref_folder2);
	        folder.setSummary(R.string.pref_folder3_info);
        }
        folder.setKey(Preferences.KEY_PREF_FOLDER + serverPostfix);
        folder.getEditText().setSingleLine();
        folder.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        folder.setDialogTitle(R.string.pref_folder);
        folder.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(folder);

        // Advanced
        PreferenceCategory advanced = new PreferenceCategory(this);
        advanced.setTitle(R.string.pref_advanced);
        getPreferenceScreen().addItemFromInflater(advanced); 
        // AlertFinished
        alarmFinished = new TransdroidCheckBoxPreference(this);
        alarmFinished.setDefaultValue(true);
        alarmFinished.setTitle(R.string.pref_alarmfinished);
        alarmFinished.setSummary(R.string.pref_alarmfinished_info);
        alarmFinished.setKey(Preferences.KEY_PREF_ALARMFINISHED + serverPostfix);
        alarmFinished.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(alarmFinished); 
        // AlertNew
        alarmNew = new TransdroidCheckBoxPreference(this);
        alarmNew.setTitle(R.string.pref_alarmnew);
        alarmNew.setSummary(R.string.pref_alarmnew_info);
        alarmNew.setKey(Preferences.KEY_PREF_ALARMNEW + serverPostfix);
        alarmNew.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(alarmNew); 
        // OS
        os = new TransdroidListPreference(this);
        os.setTitle(R.string.pref_os);
        os.setKey(Preferences.KEY_PREF_OS + serverPostfix);
        os.setEntries(R.array.pref_os_types);
        os.setEntryValues(R.array.pref_os_values);
        os.setDialogTitle(R.string.pref_os);
        os.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(os);
        // Download directory
        downloadDir = new TransdroidEditTextPreference(this);
        downloadDir.setTitle(R.string.pref_downloaddir);
        downloadDir.setSummary(R.string.pref_downloaddir_info);
        downloadDir.setKey(Preferences.KEY_PREF_DOWNLOADDIR + serverPostfix);
        downloadDir.getEditText().setSingleLine();
        downloadDir.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        downloadDir.setDialogTitle(R.string.pref_downloaddir);
        downloadDir.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(downloadDir);
        // FTP URL
        ftpUrl = new TransdroidEditTextPreference(this);
        ftpUrl.setTitle(R.string.pref_ftpurl);
        ftpUrl.setSummary(R.string.pref_ftpurl_info);
        ftpUrl.setKey(Preferences.KEY_PREF_FTPURL + serverPostfix);
        ftpUrl.getEditText().setSingleLine();
        ftpUrl.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        ftpUrl.setDialogTitle(R.string.pref_ftpurl);
        ftpUrl.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(ftpUrl);
        // Timeout
        timeout = new TransdroidEditTextPreference(this);
        timeout.setTitle(R.string.pref_timeout);
        timeout.setSummary(R.string.pref_timeout);
        timeout.setKey(Preferences.KEY_PREF_TIMEOUT + serverPostfix);
        timeout.getEditText().setSingleLine();
        timeout.getEditText().setInputType(timeout.getEditText().getInputType() | EditorInfo.TYPE_CLASS_NUMBER);
        timeout.setDialogTitle(R.string.pref_timeout);
        timeout.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(timeout);
        
        // SSL
        ssl = new TransdroidCheckBoxPreference(this);
        ssl.setTitle(R.string.pref_ssl);
        ssl.setSummary(R.string.pref_ssl_info);
        ssl.setKey(Preferences.KEY_PREF_SSL + serverPostfix);
        ssl.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(ssl);
		// SSL trust key
		sslTrustKey = new TransdroidEditTextPreference(this);
		sslTrustKey.setTitle(R.string.pref_ssl_trust_key);
        sslTrustKey.setSummary(R.string.pref_ssl_trust_key_info);
        sslTrustKey.getEditText().setSingleLine();
        sslTrustKey.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
        sslTrustKey.setDialogTitle(R.string.pref_ssl_trust_key);
		sslTrustKey.setKey(Preferences.KEY_PREF_SSL_TRUST_KEY + serverPostfix);
		sslTrustKey.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(sslTrustKey);
        // SSL trust all
        sslTrustAll = new TransdroidCheckBoxPreference(this);
        sslTrustAll.setTitle(R.string.pref_ssl_trust_all);
        sslTrustAll.setSummary(R.string.pref_ssl_trust_all_info);
        sslTrustAll.setKey(Preferences.KEY_PREF_SSL_TRUST_ALL + serverPostfix);
        sslTrustAll.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(sslTrustAll);

		updateOptionAvailability();
        updateDescriptionTexts();

    }

    private OnPreferenceChangeListener updateHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference == name) {
				nameValue = (String) newValue;
			} else if (preference == daemon) {
				daemonValue = (String) newValue;
			} else if (preference == address) {
				addressValue = (String) newValue;
				// Validate for an IP address or host name
				String newAddress = (String) newValue;
				if (!(newAddress.matches(IP_ADDRESS_FORMAT) || newAddress.matches(IPv6_ADDRESS_FORMAT) || newAddress.matches(HOST_NAME_FORMAT))) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_ip_or_hostname, Toast.LENGTH_LONG).show();
					return false;
				}
			} else if (preference == port) {
				portValue = (String) newValue;
				// Validate user port input (should be non-empty; the text box already ensures that any input is actually a number)
				if (((String)newValue).equals("")) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_port_number, Toast.LENGTH_LONG).show();
					return false;
				}
			} else if (preference == auth) {
				authValue = (Boolean) newValue;
			} else if (preference == user) {
				userValue = (String) newValue;
			} else if (preference == pass) {
				//passValue = (String) newValue;
			} else if (preference == ssl) {
				sslValue = (Boolean) newValue;
			} else if (preference == sslTrustAll) {
				sslTAValue = (Boolean) newValue;
			} else if (preference == sslTrustKey) {
				sslTrustKeyValue = (String) newValue;
			} else if (preference == folder) {
				folderValue = (String) newValue;
			} else if (preference == os) {
				osValue = (String) newValue;
			} else if (preference == downloadDir) {
				downloadDirValue = (String) newValue;
				// Validate the final / or \
				if (downloadDirValue != null && !downloadDirValue.equals("") && !(downloadDirValue.endsWith("/") || downloadDirValue.endsWith("\\"))) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_directory, Toast.LENGTH_LONG).show();
					return false;
				}
			} else if (preference == ftpUrl) {
				ftpUrlValue = (String) newValue;
				// Do basic URL validation
				if (ftpUrlValue != null && !ftpUrlValue.equals("")) {
					try {
						new URI(ftpUrlValue); // If parsing fails, it was not properly formatted
					} catch (URISyntaxException e) {
						Toast.makeText(getApplicationContext(), R.string.error_invalid_url_form, Toast.LENGTH_LONG).show();
						return false;
					}
				}
			} else if (preference == timeout) {
				timeoutValue = (String) newValue;
				// Validate user port input (should be non-empty; the text box already ensures that any input is actually a number)
				if (((String)newValue).equals("")) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_timeout, Toast.LENGTH_LONG).show();
					return false;
				}
			} else if (preference == alarmFinished) {
				//alarmFinishedValue = (Boolean) newValue;
			} else if (preference == alarmNew) {
				//alarmNewValue = (Boolean) newValue;
			}
			updateOptionAvailability();
			updateDescriptionTexts();
			// Set the value as usual
			return true;
		}
    };
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Perform click action, which always is a Preference
    	Preference item = (Preference) getListAdapter().getItem(position);
    	
		// Let the Preference open the right dialog
		if (item instanceof TransdroidListPreference) {
			((TransdroidListPreference)item).click();
		} else if (item instanceof TransdroidCheckBoxPreference) {
    		((TransdroidCheckBoxPreference)item).click();
		} else if (item instanceof TransdroidEditTextPreference) {
    		((TransdroidEditTextPreference)item).click();
		}
		
    }

    private void updateOptionAvailability() {
    	
		// Use daemon factory to see if the newly selected daemon supports the feature
    	// Then set the availability of the options according to the (other) settings
    	Daemon daemonType = Daemon.fromCode(daemonValue);
        user.setEnabled(authValue && (daemonType == null? true: Daemon.supportsUsername(Daemon.fromCode(daemonValue))));
        pass.setEnabled(authValue);
        sslTrustAll.setEnabled(sslValue);
		folder.setEnabled(daemonType == null? false: Daemon.supportsCustomFolder(daemonType));
		downloadDir.setEnabled(daemonType == null? false: Daemon.needsManualPathSpecified(daemonType));
		sslTrustKey.setEnabled(sslValue && !sslTAValue);
    }
    
    private void updateDescriptionTexts() {
    	
    	// Update the 'summary' labels of all preferences to show their current value
    	Daemon daemonType = Daemon.fromCode(daemonValue);
    	
        name.setSummary(nameValue == null? getText(R.string.pref_name_info): nameValue);
        daemon.setSummary(daemonType == null? "": daemonType.toString());
        address.setSummary(addressValue == null? getText(R.string.pref_address_info): addressValue);
        port.setSummary(portValue == null? getText(R.string.pref_port_info).toString().trim() + " " + Daemon.getDefaultPortNumber(daemonType, sslValue): portValue);

        auth.setSummary(R.string.pref_auth_info);
        user.setSummary(userValue == null? "": userValue);

        if (daemonType == Daemon.rTorrent) {
	        folder.setTitle(R.string.pref_folder);
	        folder.setSummary(folderValue == null? getText(R.string.pref_folder_info): folderValue);
        } else if (daemonType == Daemon.Tfb4rt) {
	        folder.setTitle(R.string.pref_folder2);
	        folder.setSummary(folderValue == null? getText(R.string.pref_folder2_info): folderValue);
        } else {
	        folder.setTitle(R.string.pref_folder2);
	        folder.setSummary(folderValue == null? getText(R.string.pref_folder3_info): folderValue);
        }
        os.setSummary(osValue == null? "": OS.fromCode(osValue).toString());
        downloadDir.setSummary(downloadDirValue == null? getText(R.string.pref_downloaddir_info): downloadDirValue);
        ftpUrl.setSummary(ftpUrlValue == null? getText(R.string.pref_ftpurl_info): ftpUrlValue);
        timeout.setSummary(timeoutValue == null? getText(R.string.pref_timeout_info): timeoutValue);
        
        sslTrustKey.setSummary(sslTrustKeyValue == null ? getText(R.string.pref_ssl_trust_key_info) : sslTrustKeyValue );
    }
    
}

