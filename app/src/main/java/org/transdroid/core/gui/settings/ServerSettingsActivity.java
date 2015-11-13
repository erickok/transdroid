/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings_;
import org.transdroid.daemon.Daemon;

/**
 * Activity that allows for a configuration of a server. The key can be supplied to update an existing server setting instead of creating a new one.
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(R.menu.activity_deleteableprefs)
public class ServerSettingsActivity extends KeyBoundPreferencesActivity {

	private static final int DIALOG_CONFIRMREMOVE = 0;

	private EditTextPreference extraPass, folder, downloadDir, excludeFilter, includeFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Load the raw preferences to show in this screen
		init(R.xml.pref_server, ApplicationSettings_.getInstance_(this).getMaxNormalServer());
		initTextPreference("server_name");
		initListPreference("server_type");
		initTextPreference("server_address");
		initTextPreference("server_port");
		initTextPreference("server_user");
		initTextPreference("server_pass");
		extraPass = initTextPreference("server_extrapass");
		initTextPreference("server_localnetwork");
		initTextPreference("server_localaddress");
		initTextPreference("server_localport");
		folder = initTextPreference("server_folder");
		initTextPreference("server_timeout");
		initBooleanPreference("server_alarmfinished", true);
		initBooleanPreference("server_alarmnew");
		excludeFilter = initTextPreference("server_exclude");
		includeFilter = initTextPreference("server_include");
		initListPreference("server_os", "type_linux");
		downloadDir = initTextPreference("server_downloaddir");
		initTextPreference("server_ftpurl");
		initTextPreference("server_ftppass");
		initBooleanPreference("server_disableauth");
		initBooleanPreference("server_sslenabled");
		initBooleanPreference("server_ssltrustall", false, "server_sslenabled");
		initTextPreference("server_ssltrustkey", null, "server_sslenabled");
		onPreferencesChanged();

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@SuppressWarnings("deprecation")
	@OptionsItem(R.id.action_removesettings)
	protected void removeSettings() {
		showDialog(DIALOG_CONFIRMREMOVE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_CONFIRMREMOVE:
				return new AlertDialog.Builder(this).setMessage(R.string.pref_confirmremove)
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ApplicationSettings_.getInstance_(ServerSettingsActivity.this).removeNormalServerSettings(key);
								finish();
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
		}
		return null;
	}

	@Override
	protected void onPreferencesChanged() {

		// Use daemon factory to see if the newly selected daemon supports the feature
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Daemon daemonType = Daemon.fromCode(prefs.getString("server_type_" + key, null));
		extraPass.setEnabled(Daemon.supportsExtraPassword(daemonType));
		extraPass.setTitle(getString(daemonType == Daemon.Deluge ? R.string.pref_extrapassword : R.string.pref_secret));
		extraPass.setDialogTitle(extraPass.getTitle());
		folder.setEnabled(daemonType != null && Daemon.supportsCustomFolder(daemonType));
		downloadDir.setEnabled(daemonType != null && Daemon.needsManualPathSpecified(daemonType));
		// sslTrustKey.setEnabled(sslValue && !sslTAValue);

		// Adjust title texts accordingly
		folder.setTitle(daemonType == Daemon.rTorrent ? R.string.pref_scgifolder : R.string.pref_folder);

		// Show the exclude and the include filters if notifying
		boolean alarmFinished = prefs.getBoolean("server_alarmfinished_" + key, true);
		boolean alarmNew = prefs.getBoolean("server_alarmnew_" + key, true);
		excludeFilter.setEnabled(alarmNew || alarmFinished);
		includeFilter.setEnabled(alarmNew || alarmFinished);

	}

}
