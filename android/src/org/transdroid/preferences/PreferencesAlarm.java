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

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.transdroid.R;
import org.transdroid.service.BootReceiver;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;

public class PreferencesAlarm extends PreferenceActivity {

	private SharedPreferences prefs;
	private TransdroidCheckBoxPreference enable;
	private TransdroidListPreference interval;
	private TransdroidCheckBoxPreference checkRssFeeds;
	private TransdroidCheckBoxPreference alarmPlaySound;
	private TransdroidNotificationListPreference alarmSoundURI;
	private TransdroidCheckBoxPreference alarmVibrate;
	private ColorPickerPreference alarmColour;
	private TransdroidCheckBoxPreference adwNotify;
	private TransdroidCheckBoxPreference adwOnlyDl;
	private TransdroidCheckBoxPreference checkForUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean isEnabled = prefs.getBoolean(Preferences.KEY_PREF_ENABLEALARM, false);
		boolean isAdwEnabled = prefs.getBoolean(Preferences.KEY_PREF_ADWNOTIFY, false);

        // Create preference objects
        getPreferenceScreen().setTitle(R.string.pref_alarm);
        // Enable
        enable = new TransdroidCheckBoxPreference(this);
        enable.setTitle(R.string.pref_enablealarm);
        enable.setSummary(R.string.pref_enablealarm_info);
        enable.setKey(Preferences.KEY_PREF_ENABLEALARM);
        getPreferenceScreen().addItemFromInflater(enable);
        // Inteval
        interval = new TransdroidListPreference(this);
        interval.setTitle(R.string.pref_alarmtime);
        interval.setSummary(R.string.pref_alarmtime_info);
        interval.setKey(Preferences.KEY_PREF_ALARMINT);
        interval.setEntries(R.array.pref_alarminterval_types);
        interval.setEntryValues(R.array.pref_alarminterval_values);
        interval.setDefaultValue("600");
        interval.setDialogTitle(R.string.pref_alarmtime);
        interval.setEnabled(isEnabled);
        getPreferenceScreen().addItemFromInflater(interval);
        // CheckRssFeeds
        checkRssFeeds = new TransdroidCheckBoxPreference(this);
        checkRssFeeds.setTitle(R.string.pref_checkrssfeeds);
        checkRssFeeds.setSummary(R.string.pref_checkrssfeeds_info);
        checkRssFeeds.setKey(Preferences.KEY_PREF_CHECKRSSFEEDS);
        checkRssFeeds.setEnabled(isEnabled);
        getPreferenceScreen().addItemFromInflater(checkRssFeeds);
        // Alarm play sound
        alarmPlaySound = new TransdroidCheckBoxPreference(this);
        alarmPlaySound.setTitle(R.string.pref_alarmplaysound);
        alarmPlaySound.setSummary(R.string.pref_alarmplaysound_info);
        alarmPlaySound.setKey(Preferences.KEY_PREF_ALARMPLAYSOUND);
        alarmPlaySound.setEnabled(isEnabled);
        getPreferenceScreen().addItemFromInflater(alarmPlaySound);
        // Alarm sound URI
        alarmSoundURI = new TransdroidNotificationListPreference(this);
        alarmSoundURI.setTitle(R.string.pref_alarmsounduri);
        alarmSoundURI.setSummary(R.string.pref_alarmsounduri_info);
        alarmSoundURI.setKey(Preferences.KEY_PREF_ALARMSOUNDURI);
		alarmSoundURI.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_ALARM);
		alarmSoundURI.setShowDefault(true);
		alarmSoundURI.setShowSilent(false);
		alarmSoundURI.setEnabled(isEnabled);
		getPreferenceScreen().addItemFromInflater(alarmSoundURI);
		// Vibrate
		alarmVibrate = new TransdroidCheckBoxPreference(this);
		alarmVibrate.setTitle(R.string.pref_alarmvibrate);
		alarmVibrate.setSummary(R.string.pref_alarmvibrate_info);
		alarmVibrate.setKey(Preferences.KEY_PREF_ALARMVIBRATE);
		alarmVibrate.setEnabled(isEnabled);
		getPreferenceScreen().addItemFromInflater(alarmVibrate);
		// Notification LED colour
		alarmColour = new ColorPickerPreference(this);
		alarmColour.setTitle(R.string.pref_alarmcolour);
		alarmColour.setSummary(R.string.pref_alarmcolour_info);
		alarmColour.setKey(Preferences.KEY_PREF_ALARMCOLOUR);
		alarmColour.setAlphaSliderEnabled(false);
		alarmColour.setDefaultValue(0xff7dbb21);
		alarmColour.setEnabled(isEnabled);
		getPreferenceScreen().addItemFromInflater(alarmColour);
		// Enable ADW notifications
		adwNotify = new TransdroidCheckBoxPreference(this);
		adwNotify.setTitle(R.string.pref_adwnotify);
		adwNotify.setSummary(R.string.pref_adwnotify_info);
		adwNotify.setKey(Preferences.KEY_PREF_ADWNOTIFY);
		adwNotify.setEnabled(isEnabled);
		getPreferenceScreen().addItemFromInflater(adwNotify);
		// Send ADW notifications
		adwOnlyDl = new TransdroidCheckBoxPreference(this);
		adwOnlyDl.setTitle(R.string.pref_adwonlydl);
		adwOnlyDl.setSummary(R.string.pref_adwonlydl_info);
		adwOnlyDl.setKey(Preferences.KEY_PREF_ADWONLYDL);
		adwOnlyDl.setEnabled(isEnabled && isAdwEnabled);
		getPreferenceScreen().addItemFromInflater(adwOnlyDl);
        // Enable
        checkForUpdates = new TransdroidCheckBoxPreference(this);
        checkForUpdates.setTitle(R.string.pref_checkforupdates);
        checkForUpdates.setSummary(R.string.pref_checkforupdates_info);
        checkForUpdates.setKey(Preferences.KEY_PREF_CHECKUPDATES);
        checkForUpdates.setDefaultValue(true);
        getPreferenceScreen().addItemFromInflater(checkForUpdates);
        
        prefs.registerOnSharedPreferenceChangeListener(changesHandler);
    }

    @Override
	protected void onResume() {
        prefs.registerOnSharedPreferenceChangeListener(changesHandler);
		super.onResume();
	}

	@Override
	protected void onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(changesHandler);
		super.onPause();
	}

	private OnSharedPreferenceChangeListener changesHandler = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			
			if (key.equals(Preferences.KEY_PREF_CHECKUPDATES)) {
				// Only the update checker setting changed
				BootReceiver.cancelUpdateCheck();
				boolean shouldCheckForUpdates = sharedPreferences.getBoolean(Preferences.KEY_PREF_CHECKUPDATES, true);
				if (shouldCheckForUpdates) {
					BootReceiver.startUpdateCheck(getApplicationContext());
				}
				return;
			}
			
			// First cancel the alarm
			BootReceiver.cancelAlarm();
			
			// Start the alarm again, if requested
			boolean isEnabled = sharedPreferences.getBoolean(Preferences.KEY_PREF_ENABLEALARM, true);
			if (isEnabled) {
				BootReceiver.startAlarm(getApplicationContext());
			}
			boolean isAdwEnabled = sharedPreferences.getBoolean(Preferences.KEY_PREF_ADWNOTIFY, true);
			
			// Adapt the option accordingly
			interval.setEnabled(isEnabled);
			checkRssFeeds.setEnabled(isEnabled);
			alarmPlaySound.setEnabled(isEnabled);
			alarmSoundURI.setEnabled(isEnabled);
			alarmVibrate.setEnabled(isEnabled);
			alarmColour.setEnabled(isEnabled);
			adwNotify.setEnabled(isEnabled);
			adwOnlyDl.setEnabled(isEnabled && isAdwEnabled);
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

}
