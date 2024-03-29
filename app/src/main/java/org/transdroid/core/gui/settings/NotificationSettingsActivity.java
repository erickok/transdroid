/*
 * Copyright 2010-2024 Eric Kok et al.
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.R;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.service.RssCheckerJob;
import org.transdroid.core.service.ServerCheckerJob;

@EActivity
public class NotificationSettingsActivity extends PreferenceCompatActivity implements OnSharedPreferenceChangeListener {

    @Bean
    protected NavigationHelper navigationHelper;
    @Bean
    protected NotificationSettings notificationSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load the notification-related preferences from XML and update availability thereof
        addPreferencesFromResource(R.xml.pref_notifications);
        updatePrefsEnabled();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (navigationHelper.handleNotificationPermissionResult(requestCode, grantResults)) {
            // Now that we have permission, schedule the jobs
            ServerCheckerJob.schedule(getApplicationContext());
            RssCheckerJob.schedule(getApplicationContext());
            updatePrefsEnabled();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ((RingtonePreference) findPreference("notifications_sound")).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start/stop the background service appropriately
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean needsPermission = notificationSettings.isEnabledForRss() || notificationSettings.isEnabledForTorrents();
        if (needsPermission && !navigationHelper.checkOrRequestNotificationPermission(this)) {
            return;
        }
        // Already have permission to show notifications, so update the jobs now
        ServerCheckerJob.schedule(getApplicationContext());
        RssCheckerJob.schedule(getApplicationContext());
        updatePrefsEnabled();
    }

    private void updatePrefsEnabled() {
        boolean disabled = !notificationSettings.isEnabledForRss() && !notificationSettings.isEnabledForTorrents();
        findPreference("notifications_interval").setEnabled(!disabled);
        findPreference("notifications_sound").setEnabled(!disabled);
        findPreference("notifications_vibrate").setEnabled(!disabled);
        findPreference("notifications_ledcolour").setEnabled(!disabled);
        findPreference("notifications_adwnotify").setEnabled(!disabled);
    }

}
