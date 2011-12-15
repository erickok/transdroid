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

import org.transdroid.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class PreferencesRssFeed extends PreferenceActivity {

	public static final String PREFERENCES_FEED_KEY = "PREFERENCES_SERVER_POSTFIX";

	//private static final String URL_FORMAT = "^(http|ftp|https)\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,6}(/\\S*)?$";//"(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?";

	private String feedPostfix;

	private TransdroidEditTextPreference name;
	private TransdroidEditTextPreference url;

	private String nameValue = null;
	private String urlValue = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // For which server?
        feedPostfix = getIntent().getStringExtra(PREFERENCES_FEED_KEY);
        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        nameValue = prefs.getString(Preferences.KEY_PREF_RSSNAME + feedPostfix, null);
        urlValue = prefs.getString(Preferences.KEY_PREF_RSSURL + feedPostfix, null);
        
        // Create preference objects
        getPreferenceScreen().setTitle(R.string.pref_search);
        // Name
        name = new TransdroidEditTextPreference(this);
        name.setTitle(R.string.pref_name);
        name.setKey(Preferences.KEY_PREF_RSSNAME + feedPostfix);
        name.getEditText().setSingleLine();
        name.setDialogTitle(R.string.pref_name);
        name.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(name);
        // Url
        url = new TransdroidEditTextPreference(this);
        url.setTitle(R.string.pref_feed_url);
        url.setKey(Preferences.KEY_PREF_RSSURL + feedPostfix);
        url.getEditText().setSingleLine();
        url.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setDialogTitle(R.string.pref_feed_url);
        url.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(url);
        // NeedsAuth
        TransdroidCheckBoxPreference needsAuth = new TransdroidCheckBoxPreference(this);
        needsAuth.setTitle(R.string.pref_feed_needsauth);
        needsAuth.setSummary(R.string.pref_feed_needsauth_info);
        needsAuth.setKey(Preferences.KEY_PREF_RSSAUTH + feedPostfix);
        needsAuth.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(needsAuth);
        
        updateDescriptionTexts();
        
    }

    private OnPreferenceChangeListener updateHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference == name) {
				nameValue = (String) newValue;
			} else if (preference == url) {
				urlValue = (String) newValue;
				// Validate for a proper http(s) url
				/*String newAddress = (String) newValue;
				if (!newAddress.toLowerCase().matches(URL_FORMAT)) {
					Toast.makeText(getApplicationContext(), R.string.error_invalid_url_form, Toast.LENGTH_LONG).show();
					return false;
				}*/
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

        name.setSummary(nameValue == null? getText(R.string.pref_name_info): nameValue);
        url.setSummary(urlValue == null? "": (urlValue.length() > 35? urlValue.substring(0, 35) + "...": urlValue));
        
    }
    
}
