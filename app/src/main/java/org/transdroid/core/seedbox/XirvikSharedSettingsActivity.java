/*
 * Copyright 2010-2018 Eric Kok et al.
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
package org.transdroid.core.seedbox;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.transdroid.R;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.settings.KeyBoundPreferencesActivity;
import org.transdroid.core.gui.settings.MainSettingsActivity_;
import org.transdroid.daemon.util.HttpHelper;

import java.io.InputStream;

/**
 * Activity that allows for the configuration of a Xirvik shared seedbox. The key can be supplied to update an existing server setting instead of
 * creating a new one.
 *
 * @author Eric Kok
 */
@EActivity
@OptionsMenu(resName = "activity_deleteableprefs")
public class XirvikSharedSettingsActivity extends KeyBoundPreferencesActivity {

    @Bean
    protected Log log;
    private EditTextPreference excludeFilter, includeFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load the raw preferences to show in this screen
        init(R.xml.pref_seedbox_xirvikshared,
                SeedboxProvider.XirvikShared.getSettings().getMaxSeedboxOrder(PreferenceManager.getDefaultSharedPreferences(this)));
        initTextPreference("seedbox_xirvikshared_name");
        initTextPreference("seedbox_xirvikshared_server");
        initTextPreference("seedbox_xirvikshared_user");
        initTextPreference("seedbox_xirvikshared_pass");
        initTextPreference("seedbox_xirvikshared_rpc");
        initBooleanPreference("seedbox_xirvikshared_alarmfinished", true);
        initBooleanPreference("seedbox_xirvikshared_alarmnew", true);
        excludeFilter = initTextPreference("seedbox_xirvikshared_alarmexclude");
        includeFilter = initTextPreference("seedbox_xirvikshared_alarminclude");

    }

    @Override
    protected void onPreferencesChanged() {

        // Show the exclude and the include filters if notifying
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean alarmFinished = prefs.getBoolean("seedbox_xirvikshared_alarmfinished_" + key, true);
        boolean alarmNew = prefs.getBoolean("seedbox_xirvikshared_alarmnew_" + key, true);
        excludeFilter.setEnabled(alarmNew || alarmFinished);
        includeFilter.setEnabled(alarmNew || alarmFinished);

        // When the shared server settings change, we also have to update the RPC mount point to use
        String server = prefs.getString("seedbox_xirvikshared_server_" + key, null);
        String user = prefs.getString("seedbox_xirvikshared_user_" + key, null);
        String pass = prefs.getString("seedbox_xirvikshared_pass_" + key, null);
        String token = prefs.getString("seedbox_xirvikshared_token_" + key, null);

        new RetrieveXirvikAutoConfTask(server, user, pass, token) {
            @Override
            protected void onPostExecute(String result) {
                storeScgiMountFolder(result);
            }
        }.execute();

    }

    protected void storeScgiMountFolder(String result) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(XirvikSharedSettingsActivity.this).edit();
        EditTextPreference pref = (EditTextPreference) findPreference("seedbox_xirvikshared_rpc_" + key);
        if (result == null) {
            log.d(this, "Could not retrieve the Xirvik shared seedbox RPC mount point setting");
            SnackbarManager.show(Snackbar.with(this).text(R.string.pref_seedbox_xirviknofolder).colorResource(R.color.red));
            edit.remove("seedbox_xirvikshared_rpc_" + key);
            pref.setSummary("");
        } else {
            edit.putString("seedbox_xirvikshared_rpc_" + key, result);
            pref.setSummary(result);
        }
        edit.apply();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @OptionsItem(android.R.id.home)
    protected void navigateUp() {
        MainSettingsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
    }

    @OptionsItem(resName = "action_removesettings")
    protected void removeSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SeedboxProvider.XirvikShared.getSettings().removeServerSetting(prefs, key);
        finish();
    }

    public static abstract class RetrieveXirvikAutoConfTask extends AsyncTask<Void, Void, String> {
        final String server;
        final String user;
        final String pass;
        final String token;

        public RetrieveXirvikAutoConfTask(String server, String user, String pass, String token) {
            this.server = server;
            this.user = user;
            this.pass = pass;
            this.token = token;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                // Retrieve the RPC mount point setting from the server itself
                DefaultHttpClient httpclient =
                        HttpHelper.createStandardHttpClient(true, user, pass, token, true, null, HttpHelper.DEFAULT_CONNECTION_TIMEOUT, server, 443);
                String url = "https://" + server + ":443/browsers_addons/transdroid_autoconf.txt";
                HttpResponse request = httpclient.execute(new HttpGet(url));
                InputStream stream = request.getEntity().getContent();
                String folder = HttpHelper.convertStreamToString(stream).trim();
                if (folder.startsWith("<?xml")) {
                    folder = null;
                }
                stream.close();
                return folder;

            } catch (Exception e) {
                return null;
            }
        }
    }

}
