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
 package com.seedm8.transdroid.preferences;

import org.transdroid.R;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.TransdroidCheckBoxPreference;
import org.transdroid.preferences.TransdroidEditTextPreference;
import org.transdroid.preferences.TransdroidListPreference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.Toast;

public class PreferencesSeedM8Server extends PreferenceActivity {

	public static final String PREFERENCES_8SERVER_KEY = "PREFERENCES_8SERVER_POSTFIX";
	public static final String[] validAddressEnding = { ".seedm8.com" };

	private String serverPostfix;
	// These preferences are members so they can be accessed by the updateOptionAvailibility event
	private TransdroidEditTextPreference name;
	private TransdroidEditTextPreference server;
	private TransdroidEditTextPreference user;
	private TransdroidEditTextPreference dpass;
	private TransdroidEditTextPreference dport;
	private TransdroidEditTextPreference tpass;
	private TransdroidEditTextPreference tport;
	private TransdroidEditTextPreference rpass;
	private TransdroidEditTextPreference spass;
	private TransdroidCheckBoxPreference alarmFinished;
	private TransdroidCheckBoxPreference alarmNew;

	private String nameValue = null;
	private String serverValue = null;
	private String userValue = null;
	private String dportValue;
	private String tportValue;
	//private String passValue = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // For which server?
        serverPostfix = getIntent().getStringExtra(PREFERENCES_8SERVER_KEY);
        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        nameValue = prefs.getString(Preferences.KEY_PREF_8NAME + serverPostfix, null);
        serverValue = prefs.getString(Preferences.KEY_PREF_8SERVER + serverPostfix, null);
        userValue = prefs.getString(Preferences.KEY_PREF_8USER + serverPostfix, null);
        dportValue = prefs.getString(Preferences.KEY_PREF_8DPORT + serverPostfix, null);
        tportValue = prefs.getString(Preferences.KEY_PREF_8TPORT + serverPostfix, null);

        // Create preference objects
        getPreferenceScreen().setTitle(R.string.seedm8_pref_title);
        // Name
        name = new TransdroidEditTextPreference(this);
        name.setTitle(R.string.pref_name);
        name.setKey(Preferences.KEY_PREF_8NAME + serverPostfix);
        name.getEditText().setSingleLine();
        name.setDialogTitle(R.string.pref_name);
        name.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(name);
        // Server
        server = new TransdroidEditTextPreference(this);
        server.setTitle(R.string.seedm8_pref_server);
        server.setKey(Preferences.KEY_PREF_8SERVER + serverPostfix);
        server.getEditText().setSingleLine();
        server.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        server.setDialogTitle(R.string.seedm8_pref_server);
        server.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(server);
        // User
        user = new TransdroidEditTextPreference(this);
        user.setTitle(R.string.pref_user);
        user.setKey(Preferences.KEY_PREF_8USER + serverPostfix);
        user.getEditText().setSingleLine();
        user.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
        user.setDialogTitle(R.string.pref_user);
        user.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(user);

        // Deluge Port
        dport = new TransdroidEditTextPreference(this);
        dport.setTitle("Deluge " + getString(R.string.pref_port));
        dport.setKey(Preferences.KEY_PREF_8DPORT + serverPostfix);
        dport.getEditText().setSingleLine();
        dport.getEditText().setInputType(dport.getEditText().getInputType() | EditorInfo.TYPE_CLASS_NUMBER);
        dport.setDialogTitle(R.string.pref_port);
        dport.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(dport);
        // Deluge Pass
        dpass = new TransdroidEditTextPreference(this);
        dpass.setTitle("Deluge " + getString(R.string.pref_pass));
        dpass.setKey(Preferences.KEY_PREF_8DPASS + serverPostfix);
        dpass.getEditText().setSingleLine();
        dpass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        dpass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        dpass.setDialogTitle(R.string.pref_pass);
        dpass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(dpass);

        // Transmission Port
        tport = new TransdroidEditTextPreference(this);
        tport.setTitle("Transmission " + getString(R.string.pref_port));
        tport.setKey(Preferences.KEY_PREF_8TPORT + serverPostfix);
        tport.getEditText().setSingleLine();
        tport.getEditText().setInputType(tport.getEditText().getInputType() | EditorInfo.TYPE_CLASS_NUMBER);
        tport.setDialogTitle(R.string.pref_port);
        tport.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(tport);
        // Transmission Pass
        tpass = new TransdroidEditTextPreference(this);
        tpass.setTitle("Transmission " + getString(R.string.pref_pass));
        tpass.setKey(Preferences.KEY_PREF_8TPASS + serverPostfix);
        tpass.getEditText().setSingleLine();
        tpass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        tpass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        tpass.setDialogTitle(R.string.pref_pass);
        tpass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(tpass);

        // rTorrent Pass
        rpass = new TransdroidEditTextPreference(this);
        rpass.setTitle("rTorrent RPC " + getString(R.string.pref_pass));
        rpass.setKey(Preferences.KEY_PREF_8RPASS + serverPostfix);
        rpass.getEditText().setSingleLine();
        rpass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        rpass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        rpass.setDialogTitle(R.string.pref_pass);
        rpass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(rpass);

        // SFTP Pass
        spass = new TransdroidEditTextPreference(this);
        spass.setTitle("SFTP " + getString(R.string.pref_pass));
        spass.setKey(Preferences.KEY_PREF_8RPASS + serverPostfix);
        spass.getEditText().setSingleLine();
        spass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        spass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        spass.setDialogTitle(R.string.pref_pass);
        spass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(spass);

        // AlertFinished
        alarmFinished = new TransdroidCheckBoxPreference(this);
        alarmFinished.setDefaultValue(true);
        alarmFinished.setTitle(R.string.pref_alarmfinished);
        alarmFinished.setSummary(R.string.pref_alarmfinished_info);
        alarmFinished.setKey(Preferences.KEY_PREF_8ALARMFINISHED + serverPostfix);
        alarmFinished.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(alarmFinished); 
        // AlertNew
        alarmNew = new TransdroidCheckBoxPreference(this);
        alarmNew.setTitle(R.string.pref_alarmnew);
        alarmNew.setSummary(R.string.pref_alarmnew_info);
        alarmNew.setKey(Preferences.KEY_PREF_8ALARMNEW + serverPostfix);
        alarmNew.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(alarmNew); 
        
        updateDescriptionTexts();

    }

    private OnPreferenceChangeListener updateHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference == name) {
				nameValue = (String) newValue;
			} else if (preference == server) {
				String newServer = (String) newValue;
				// Validate SeedM8 server address
				boolean valid = newServer != null && !newServer.equals("") && newServer.indexOf(" ") == -1;
				boolean validEnd = false;
				for (int i = 0; i < validAddressEnding.length && valid; i++) {
					validEnd |= newServer.endsWith(validAddressEnding[i]);
				}
				if (!valid || !validEnd) {
					Toast.makeText(getApplicationContext(), R.string.seedm8_error_invalid_servername, Toast.LENGTH_LONG).show();
					return false;
				}
				serverValue = newServer;
			} else if (preference == user) {
				userValue = (String) newValue;
			} else if (preference == dport) {
				dportValue = (String) newValue;
				// Validate user port input (should be non-empty; the text box already ensures that any input is actually a number)
				if (((String)newValue).equals("")) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_port_number, Toast.LENGTH_LONG).show();
					return false;
				}
			} else if (preference == tport) {
				tportValue = (String) newValue;
				// Validate user port input (should be non-empty; the text box already ensures that any input is actually a number)
				if (((String)newValue).equals("")) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_port_number, Toast.LENGTH_LONG).show();
					return false;
				}
			}
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

    private void updateDescriptionTexts() {
    	
    	// Update the 'summary' labels of all preferences to show their current value    	
        name.setSummary(nameValue == null? getText(R.string.pref_name_info): nameValue);
        server.setSummary(serverValue == null? getText(R.string.seedm8_pref_server_info): serverValue);
        user.setSummary(userValue == null? "": userValue);
        dport.setSummary(dportValue == null? "": dportValue);
        tport.setSummary(tportValue == null? "": tportValue);
        
    }
    
}
