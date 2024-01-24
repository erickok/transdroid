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
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings_;

/**
 * Activity that allows for a configuration of a web search site. The key can be supplied to update an existing web
 * search site setting instead of creating a new one.
 *
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(R.menu.activity_deleteableprefs)
public class WebsearchSettingsActivity extends KeyBoundPreferencesActivity {

    private static final int DIALOG_CONFIRMREMOVE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load the raw preferences to show in this screen
        init(R.xml.pref_websearch, ApplicationSettings_.getInstance_(this).getMaxWebsearch());
        initTextPreference("websearch_name");
        initTextPreference("websearch_baseurl");
        initTextPreference("websearch_cookies");

    }

    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    @OptionsItem(R.id.action_removesettings)
    protected void removeSettings() {
        showDialog(DIALOG_CONFIRMREMOVE);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_CONFIRMREMOVE) {
            return new AlertDialog.Builder(this)
                    .setMessage(R.string.pref_confirmremove)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        ApplicationSettings_.getInstance_(WebsearchSettingsActivity.this).removeWebsearchSettings(key);
                        finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }
        return null;
    }

}
