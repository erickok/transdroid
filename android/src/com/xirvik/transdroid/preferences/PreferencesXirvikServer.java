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
package com.xirvik.transdroid.preferences;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.transdroid.R;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.TransdroidCheckBoxPreference;
import org.transdroid.preferences.TransdroidEditTextPreference;
import org.transdroid.preferences.TransdroidListPreference;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
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

public class PreferencesXirvikServer extends PreferenceActivity {

	public static final String PREFERENCES_XSERVER_KEY = "PREFERENCES_XSERVER_POSTFIX";
	/* public static final String[] validAddressStart = { "dedi", "semi" }; */
	public static final String[] validAddressEnding = { ".xirvik.com", ".xirvik.net" };

	private String serverPostfix;
	// These preferences are members so they can be accessed by the updateOptionAvailibility event
	private TransdroidEditTextPreference name;
	private TransdroidListPreference type;
	private TransdroidEditTextPreference server;
	private TransdroidEditTextPreference folder;
	private TransdroidEditTextPreference user;
	private TransdroidEditTextPreference pass;
	private TransdroidCheckBoxPreference alarmFinished;
	private TransdroidCheckBoxPreference alarmNew;

	private String nameValue = null;
	private String typeValue = null;
	private String serverValue = null;
	private String folderValue = null;
	private String userValue = null;
	private String passValue = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// For which server?
		serverPostfix = getIntent().getStringExtra(PREFERENCES_XSERVER_KEY);
		// Create the preferences screen here: this takes care of saving/loading, but also contains the
		// ListView adapter, etc.
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		nameValue = prefs.getString(Preferences.KEY_PREF_XNAME + serverPostfix, null);
		typeValue = prefs.getString(Preferences.KEY_PREF_XTYPE + serverPostfix, null);
		serverValue = prefs.getString(Preferences.KEY_PREF_XSERVER + serverPostfix, null);
		folderValue = prefs.getString(Preferences.KEY_PREF_XFOLDER + serverPostfix, null);
		userValue = prefs.getString(Preferences.KEY_PREF_XUSER + serverPostfix, null);
		passValue = prefs.getString(Preferences.KEY_PREF_XPASS + serverPostfix, null);

		// Create preference objects
		getPreferenceScreen().setTitle(R.string.xirvik_pref_title);
		// Name
		name = new TransdroidEditTextPreference(this);
		name.setTitle(R.string.pref_name);
		name.setKey(Preferences.KEY_PREF_XNAME + serverPostfix);
		name.getEditText().setSingleLine();
		name.setDialogTitle(R.string.pref_name);
		name.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(name);
		// Type
		type = new TransdroidListPreference(this);
		type.setTitle(R.string.xirvik_pref_type);
		type.setKey(Preferences.KEY_PREF_XTYPE + serverPostfix);
		type.setEntries(R.array.pref_xirvik_types);
		type.setEntryValues(R.array.pref_xirvik_values);
		type.setDialogTitle(R.string.xirvik_pref_type);
		type.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(type);
		// Server
		server = new TransdroidEditTextPreference(this);
		server.setTitle(R.string.xirvik_pref_server);
		server.setKey(Preferences.KEY_PREF_XSERVER + serverPostfix);
		server.getEditText().setSingleLine();
		server.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		server.setDialogTitle(R.string.xirvik_pref_server);
		server.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(server);
		// Folder
		folder = new TransdroidEditTextPreference(this);
		folder.setTitle(R.string.xirvik_pref_folder);
		folder.setKey(Preferences.KEY_PREF_XFOLDER + serverPostfix);
		folder.setEnabled(false);
		folder.setSummary(R.string.xirvik_pref_setautomatically);
		getPreferenceScreen().addItemFromInflater(folder);
		// User
		user = new TransdroidEditTextPreference(this);
		user.setTitle(R.string.pref_user);
		user.setKey(Preferences.KEY_PREF_XUSER + serverPostfix);
		user.getEditText().setSingleLine();
		user.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
		user.setDialogTitle(R.string.pref_user);
		user.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(user);
		// Pass
		pass = new TransdroidEditTextPreference(this);
		pass.setTitle(R.string.pref_pass);
		pass.setKey(Preferences.KEY_PREF_XPASS + serverPostfix);
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
		alarmFinished.setKey(Preferences.KEY_PREF_XALARMFINISHED + serverPostfix);
		alarmFinished.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(alarmFinished);
		// AlertNew
		alarmNew = new TransdroidCheckBoxPreference(this);
		alarmNew.setTitle(R.string.pref_alarmnew);
		alarmNew.setSummary(R.string.pref_alarmnew_info);
		alarmNew.setKey(Preferences.KEY_PREF_XALARMNEW + serverPostfix);
		alarmNew.setOnPreferenceChangeListener(updateHandler);
		getPreferenceScreen().addItemFromInflater(alarmNew);

		updateDescriptionTexts();

	}

	private OnPreferenceChangeListener updateHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference == name) {
				nameValue = (String) newValue;
			} else if (preference == type) {
				typeValue = (String) newValue;
			} else if (preference == server) {
				String newServer = (String) newValue;
				// Validate Xirvik server address
				boolean valid = newServer != null && !newServer.equals("") && !(newServer.indexOf(" ") >= 0);
				boolean validEnd = false;
				for (int i = 0; i < validAddressEnding.length && valid; i++) {
					validEnd |= newServer.endsWith(validAddressEnding[i]);
				}
				if (!valid || !validEnd) {
					Toast
						.makeText(getApplicationContext(), R.string.xirvik_error_invalid_servername, Toast.LENGTH_LONG)
						.show();
					return false;
				}
				serverValue = newServer;
			} else if (preference == user) {
				userValue = (String) newValue;
			} else if (preference == pass) {
				passValue = (String) newValue;
			}

			updateDescriptionTexts();
			updateScgiMountFolder();
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
			((TransdroidListPreference) item).click();
		} else if (item instanceof TransdroidCheckBoxPreference) {
			((TransdroidCheckBoxPreference) item).click();
		} else if (item instanceof TransdroidEditTextPreference) {
			if (((TransdroidEditTextPreference) item).isEnabled()) {
				((TransdroidEditTextPreference) item).click();
			}
		}

	}

	private void updateScgiMountFolder() {
		if (typeValue != null && XirvikServerType.fromCode(typeValue) == XirvikServerType.SharedRtorrent) {
			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {
					try {
						// Get, from the server, the RPC SCGI mount address
						DefaultHttpClient httpclient = HttpHelper.createStandardHttpClient(true, userValue, passValue,
							true, null, HttpHelper.DEFAULT_CONNECTION_TIMEOUT, serverValue, 443);
						String url = "https://" + serverValue + ":443/browsers_addons/transdroid_autoconf.txt";
						HttpResponse request = httpclient.execute(new HttpGet(url));
						InputStream stream = request.getEntity().getContent();
						String folderVal = HttpHelper.ConvertStreamToString(stream).trim();
						if (folderVal.startsWith("<?xml")) {
							folderVal = null;
						}
						stream.close();
						return folderVal;
					} catch (DaemonException e) {
					} catch (ClientProtocolException e) {
					} catch (IOException e) {
					}
					return null;
				}
				@Override
				protected void onPostExecute(String result) {
					storeScgiMountFolder(result);
				}
			}.execute();
		} else {
			// No need to retrieve this value
			storeScgiMountFolder(XirvikSettings.RTORRENT_FOLDER);
		}
	}

	protected void storeScgiMountFolder(String result) {
		if (result == null) {
			// The RPC SCGI mount folder address couldn't be retrieved, so we cannot continue: show an error
			Toast.makeText(getApplicationContext(), R.string.xirvik_error_nofolder, Toast.LENGTH_LONG).show();
			folder.setSummary(R.string.xirvik_error_nofolder);
			result = "";
		}
		
		// Store the new folder setting
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Editor edit = prefs.edit();
		edit.putString(Preferences.KEY_PREF_XFOLDER + serverPostfix, result);
		edit.commit();
		folderValue = result;
		updateDescriptionTexts();
	}

	private void updateDescriptionTexts() {

		// Update the 'summary' labels of all preferences to show their current value
		XirvikServerType typeType = XirvikServerType.fromCode(typeValue);

		name.setSummary(nameValue == null ? getText(R.string.pref_name_info) : nameValue);
		type.setSummary(typeType == null ? getText(R.string.xirvik_pref_type_info) : typeType.toString());
		server.setSummary(serverValue == null ? getText(R.string.xirvik_pref_server_info) : serverValue);
		user.setSummary(userValue == null ? "" : userValue);
		folder.setSummary(folderValue == null ? "" : folderValue);

	}

}
