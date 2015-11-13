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
package org.transdroid.core.seedbox;

import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;

/**
 * Implementation of {@link SeedboxSettings} for Seedstuff seedboxes.
 * @author Eric Kok
 */
public class SeedstuffSettings extends SeedboxSettingsImpl implements SeedboxSettings {

	private EditTextPreference excludeFilter, includeFilter;

	@Override
	public String getName() {
		return "Seedstuff";
	}

	@Override
	public ServerSetting getServerSetting(SharedPreferences prefs, int orderOffset, int order) {
		// @formatter:off
		String server = prefs.getString("seedbox_seedstuff_server_" + order, null);
		if (server == null) {
			return null;
		}
		String user = prefs.getString("seedbox_seedstuff_user_" + order, null);
		String pass = prefs.getString("seedbox_seedstuff_pass_" + order, null);
		return new ServerSetting(
				orderOffset + order,
				prefs.getString("seedbox_seedstuff_name_" + order, null), 
				Daemon.rTorrent, 
				server,
				null,
				443,
				null,
				443, 
				true, 
				false,
				null,
				"/user/" + user,
				true, 
				user,
				pass, 
				null,
				OS.Linux, 
				"/rtorrent/downloads", 
				"ftp://" + user + "@" + server + ":32001/rtorrent/downloads", 
				pass, 
				6, 
				prefs.getBoolean("seedbox_seedstuff_alarmfinished_" + order, true),
				prefs.getBoolean("seedbox_seedstuff_alarmnew_" + order, false),
				prefs.getString("seedbox_seedstuff_alarmexclude_" + order, null),
				prefs.getString("seedbox_seedstuff_alarminclude_" + order, null),
				true);
		// @formatter:on
	}

	@Override
	public Intent getSettingsActivityIntent(Context context) {
		return SeedstuffSettingsActivity_.intent(context).get();
	}

	@Override
	public int getMaxSeedboxOrder(SharedPreferences prefs) {
		return getMaxSeedboxOrder(prefs, "seedbox_seedstuff_server_");
	}

	@Override
	public void removeServerSetting(SharedPreferences prefs, int order) {
		removeServerSetting(prefs, "seedbox_seedstuff_server_", new String[] { "seedbox_seedstuff_name_",
				"seedbox_seedstuff_server_", "seedbox_seedstuff_user_", "seedbox_seedstuff_pass_" }, order);
	}

}
