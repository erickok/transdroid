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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;

import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

/**
 * Implementation of {@link SeedboxSettings} for Dediseedbox seedboxes.
 * @author Eric Kok
 */
public class DediseedboxSettings extends SeedboxSettingsImpl implements SeedboxSettings {

	private EditTextPreference excludeFilter, includeFilter;

	@Override
	public String getName() {
		return "Dediseedbox";
	}

	@Override
	public ServerSetting getServerSetting(SharedPreferences prefs, int orderOffset, int order) {
		// @formatter:off
		String server = prefs.getString("seedbox_dediseedbox_server_" + order, null);
		if (server == null) {
			return null;
		}
		String user = prefs.getString("seedbox_dediseedbox_user_" + order, null);
		String pass = prefs.getString("seedbox_dediseedbox_pass_" + order, null);
		return new ServerSetting(
				orderOffset + order,
				prefs.getString("seedbox_dediseedbox_name_" + order, null),
				Daemon.rTorrent, 
				server,
				null,
				443,
				null,
				443, 
				true, 
				false,
				null,
				"/rutorrent/plugins/httprpc/action.php",
				true, 
				user,
				pass, 
				null,
				OS.Linux, 
				"/",
				"ftp://" + user + "@" + server + "/",
				pass, 
				6, 
				prefs.getBoolean("seedbox_dediseedbox_alarmfinished_" + order, true),
				prefs.getBoolean("seedbox_dediseedbox_alarmnew_" + order, false),
				prefs.getString("seedbox_dediseedbox_alarmexclude_" + order, null),
				prefs.getString("seedbox_dediseedbox_alarminclude_" + order, null),
				true);
		// @formatter:on
	}

	@Override
	public Intent getSettingsActivityIntent(Context context) {
		return DediseedboxSettingsActivity_.intent(context).get();
	}

	@Override
	public int getMaxSeedboxOrder(SharedPreferences prefs) {
		return getMaxSeedboxOrder(prefs, "seedbox_dediseedbox_server_");
	}

	@Override
	public void removeServerSetting(SharedPreferences prefs, int order) {
		removeServerSetting(prefs, "seedbox_dediseedbox_server_", new String[] { "seedbox_dediseedbox_name_",
				"seedbox_dediseedbox_server_", "seedbox_dediseedbox_user_", "seedbox_dediseedbox_pass_" }, order);
	}

}
