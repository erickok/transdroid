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
 package ca.seedstuff.transdroid.preferences;

import org.transdroid.R;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.TransdroidCheckBoxPreference;
import org.transdroid.preferences.TransdroidEditTextPreference;
import org.transdroid.preferences.TransdroidListPreference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.Toast;

public class PreferencesSeedstuffServer extends PreferenceActivity {

	public static final String PREFERENCES_SSERVER_KEY = "PREFERENCES_SSERVER_POSTFIX";
	public static final String[] validAddressEnding = { ".seedstuff.ca" };

	private String serverPostfix;
	// These preferences are members so they can be accessed by the updateOptionAvailibility event
	private TransdroidEditTextPreference name;
	private TransdroidEditTextPreference server;
	private TransdroidEditTextPreference user;
	private TransdroidEditTextPreference pass;
	private TransdroidCheckBoxPreference alarmFinished;
	private TransdroidCheckBoxPreference alarmNew;

	private String nameValue = null;
	private String serverValue = null;
	private String userValue = null;
	//private String passValue = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // For which server?
        serverPostfix = getIntent().getStringExtra(PREFERENCES_SSERVER_KEY);
        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        nameValue = prefs.getString(Preferences.KEY_PREF_SNAME + serverPostfix, null);
        serverValue = prefs.getString(Preferences.KEY_PREF_SSERVER + serverPostfix, null);
        userValue = prefs.getString(Preferences.KEY_PREF_SUSER + serverPostfix, null);
        //passValue = prefs.getString(Preferences.KEY_PREF_SPASS + serverPostfix, null);

        // Create preference objects
        getPreferenceScreen().setTitle(R.string.seedstuff_pref_title);
        // Name
        name = new TransdroidEditTextPreference(this);
        name.setTitle(R.string.pref_name);
        name.setKey(Preferences.KEY_PREF_SNAME + serverPostfix);
        name.getEditText().setSingleLine();
        name.setDialogTitle(R.string.pref_name);
        name.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(name);
        // Server
        server = new TransdroidEditTextPreference(this);
        server.setTitle(R.string.seedstuff_pref_server);
        server.setKey(Preferences.KEY_PREF_SSERVER + serverPostfix);
        server.getEditText().setSingleLine();
        server.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        server.setDialogTitle(R.string.seedstuff_pref_server);
        server.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(server);
        // User
        user = new TransdroidEditTextPreference(this);
        user.setTitle(R.string.pref_user);
        user.setKey(Preferences.KEY_PREF_SUSER + serverPostfix);
        user.getEditText().setSingleLine();
        user.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
        user.setDialogTitle(R.string.pref_user);
        user.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(user);
        // Pass
        pass = new TransdroidEditTextPreference(this);
        pass.setTitle(R.string.pref_pass);
        pass.setKey(Preferences.KEY_PREF_SPASS + serverPostfix);
        pass.getEditText().setSingleLine();
        pass.getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        pass.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        pass.setDialogTitle(R.string.pref_pass);
        pass.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(pass);

        // AlertFinished
        alarmFinished = new TransdroidCheckBoxPreference(this);
        alarmFinished.setDefaultValue(true);
        alarmFinished.setTitle(R.string.pref_alarmfinished);
        alarmFinished.setSummary(R.string.pref_alarmfinished_info);
        alarmFinished.setKey(Preferences.KEY_PREF_SALARMFINISHED + serverPostfix);
        alarmFinished.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(alarmFinished); 
        // AlertNew
        alarmNew = new TransdroidCheckBoxPreference(this);
        alarmNew.setTitle(R.string.pref_alarmnew);
        alarmNew.setSummary(R.string.pref_alarmnew_info);
        alarmNew.setKey(Preferences.KEY_PREF_SALARMNEW + serverPostfix);
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
				// Validate seedstuff server address
				boolean valid = newServer != null && !newServer.equals("") && !(newServer.indexOf(" ") >= 0);
				boolean validEnd = false;
				for (int i = 0; i < validAddressEnding.length && valid; i++) {
					validEnd |= newServer.endsWith(validAddressEnding[i]);
				}
				if (!valid || !validEnd) {
					Toast.makeText(getApplicationContext(), R.string.seedstuff_error_invalid_servername, Toast.LENGTH_LONG).show();
					return false;
				}
				serverValue = newServer;
			} else if (preference == user) {
				userValue = (String) newValue;
			} else if (preference == pass) {
				//passValue = (String) newValue;
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
        server.setSummary(serverValue == null? getText(R.string.seedstuff_pref_server_info): serverValue);
        user.setSummary(userValue == null? "": userValue);
        
    }
    
}
