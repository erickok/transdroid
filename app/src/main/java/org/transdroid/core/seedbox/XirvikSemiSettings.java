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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

/**
 * Implementation of {@link SeedboxSettings} for a Xirvik semi-dedicated seedbox.
 *
 * @author Eric Kok
 */
public class XirvikSemiSettings extends SeedboxSettingsImpl implements SeedboxSettings {

    @Override
    public String getName() {
        return "Xirvik semi-dedi";
    }

    @Override
    public ServerSetting getServerSetting(SharedPreferences prefs, int orderOffset, int order) {
        // @formatter:off
        String server = prefs.getString("seedbox_xirviksemi_server_" + order, null);
        if (server == null) {
            return null;
        }
        String user = prefs.getString("seedbox_xirviksemi_user_" + order, null);
        String pass = prefs.getString("seedbox_xirviksemi_pass_" + order, null);
        String authToken = prefs.getString("seedbox_xirviksemi_token_" + order, null);
        return new ServerSetting(
                orderOffset + order,
                prefs.getString("seedbox_xirviksemi_name_" + order, null),
                Daemon.rTorrent,
                server,
                null,
                0,
                null,
                443,
                true,
                true,
                false,
                null,
                "/RPC2",
                true,
                user,
                pass,
                null,
                authToken,
                OS.Linux,
                null,
                "ftp://" + user + "@" + server + "/downloads",
                pass,
                6,
                prefs.getBoolean("seedbox_xirviksemi_alarmfinished_" + order, true),
                prefs.getBoolean("seedbox_xirviksemi_alarmnew_" + order, false),
                prefs.getString("seedbox_xirviksemi_alarmexclude_" + order, null),
                prefs.getString("seedbox_xirviksemi_alarminclude_" + order, null),
                true);
        // @formatter:on
    }

    @Override
    public Intent getSettingsActivityIntent(Context context) {
        return XirvikSemiSettingsActivity_.intent(context).get();
    }

    @Override
    public int getMaxSeedboxOrder(SharedPreferences prefs) {
        return getMaxSeedboxOrder(prefs, "seedbox_xirviksemi_server_");
    }

    @Override
    public void removeServerSetting(SharedPreferences prefs, int order) {
        removeServerSetting(prefs, "seedbox_xirviksemi_server_", new String[]{"seedbox_xirviksemi_name_",
                "seedbox_xirviksemi_server_", "seedbox_xirviksemi_user_", "seedbox_xirviksemi_pass_", "seedbox_xirviksemi_token_"}, order);
    }

    public void saveServerSetting(Context context, String server, String token) {
        // Get server order
        int key = SeedboxProvider.XirvikSemi.getSettings().getMaxSeedboxOrder(PreferenceManager.getDefaultSharedPreferences(context)) + 1;

        // Shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check server already exists to replace token
        for (int i = 0; i <= SeedboxProvider.XirvikSemi.getSettings().getMaxSeedboxOrder(PreferenceManager.getDefaultSharedPreferences(context)); i++) {
            if (prefs.getString("seedbox_xirviksemi_server_" + i, "").equals(server)) {
                key = i;
            }
        }

        // Store new seedbox pref
        prefs.edit()
                .putString("seedbox_xirviksemi_client_" + key, Daemon.toCode(Daemon.rTorrent))
                .putString("seedbox_xirviksemi_name" + key, "QR Server " + key)
                .putString("seedbox_xirviksemi_server_" + key, server)
                .putString("seedbox_xirviksemi_user_" + key, "")
                .putString("seedbox_xirviksemi_pass_" + key, "")
                .putString("seedbox_xirviksemi_token_" + key, token)
                .apply();

    }

}
