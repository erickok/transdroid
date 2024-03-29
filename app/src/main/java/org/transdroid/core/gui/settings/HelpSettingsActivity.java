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

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.SettingsPersistence;
import org.transdroid.core.gui.log.ErrorLogSender;
import org.transdroid.core.gui.navigation.DialogHelper;
import org.transdroid.core.gui.navigation.NavigationHelper;

@EActivity
public class HelpSettingsActivity extends PreferenceCompatActivity {

    protected static final int DIALOG_CHANGELOG = 0;
    protected static final int DIALOG_ABOUT = 1;
    protected static final String INSTALLHELP_URI = "http://www.transdroid.org/download/";

    @Bean
    protected NavigationHelper navigationHelper;
    @Bean
    protected ApplicationSettings applicationSettings;
    @Bean
    protected ErrorLogSender errorLogSender;
    @Bean
    protected SettingsPersistence settingsPersistence;
    private OnPreferenceClickListener onSendLogClick = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            errorLogSender.collectAndSendLog(HelpSettingsActivity.this, applicationSettings.getLastUsedServer());
            return true;
        }
    };
    private OnPreferenceClickListener onInstallHelpClick = preference -> {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(INSTALLHELP_URI)));
        return true;
    };
    private OnPreferenceClickListener onChangeLogClick = preference -> {
        showDialog(DIALOG_CHANGELOG);
        return true;
    };
    private OnPreferenceClickListener onAboutClick = preference -> {
        showDialog(DIALOG_ABOUT);
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Just load the system-related preferences from XML
        addPreferencesFromResource(R.xml.pref_help);

        // Handle outgoing links and preference changes
        findPreference("system_sendlog").setOnPreferenceClickListener(onSendLogClick);
        findPreference("system_installhelp").setOnPreferenceClickListener(onInstallHelpClick);
        findPreference("system_changelog").setOnPreferenceClickListener(onChangeLogClick);
        findPreference("system_about").setTitle(getString(R.string.pref_about, getString(R.string.app_name)));
        findPreference("system_about").setOnPreferenceClickListener(onAboutClick);
    }

    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CHANGELOG:
                return DialogHelper.showDialog(this, new ChangelogDialog());
            case DIALOG_ABOUT:
                return DialogHelper.showDialog(this, new AboutDialog());
        }
        return null;
    }

}
