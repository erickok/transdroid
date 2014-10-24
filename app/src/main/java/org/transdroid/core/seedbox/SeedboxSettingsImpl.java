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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.transdroid.core.app.settings.ServerSetting;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Abstract class that acts as a helper for actual {@link SeedboxSettings} implementations by implementing some
 * functions (partially) to do away with boileplate code.
 * @author Eric Kok
 */
public abstract class SeedboxSettingsImpl implements SeedboxSettings {

	/**
	 * Helper method to look up the seedbox type-unique order number of the last configured seedbox.
	 * @param prefs The shared preferences object to remove settings from
	 * @param uniqueKeyBase The base of the key that is used as identifying (unique) key, for example
	 *            'seedbox_myseedbox_name_'
	 * @return The order number of the configured seedbox, or 01 if no seedbox is configured of this type
	 */
	public int getMaxSeedboxOrder(SharedPreferences prefs, String uniqueKeyBase) {
		for (int i = 0; true; i++) {
			if (prefs.getString(uniqueKeyBase + i, null) == null) {
				return i - 1;
			}
		}
	}

	/**
	 * Helper method to remove some seedbox setting as identified by the order number.
	 * @param prefs The shared preferences object to remove settings from
	 * @param uniqueKeyBase The base of the key that is used as identifying (unique) key, for example
	 *            'seedbox_myseedbox_name_'
	 * @param removeKeyBases The keys of the stored preferences to remove, for example new String[] {
	 *            'seedbox_myseedbox_name_', 'seedbox_myseedbox_address_' }
	 * @param order The seedbox-unique order number (id) of the seedbox settings to remove
	 */
	public void removeServerSetting(SharedPreferences prefs, String uniqueKeyBase, String[] removeKeyBases, int order) {
		if (prefs.getString(uniqueKeyBase + order, null) == null)
			return;
		Editor edit = prefs.edit();
		int max = getMaxSeedboxOrder(prefs, uniqueKeyBase);
		// Move all settings 'higher' than the one to be removed 'down' one place
		for (int i = order; i < max; i++) {
			for (String keyBase : removeKeyBases) {
				edit.putString(keyBase + i, prefs.getString(keyBase + (i + 1), null));
			}
		}
		// Remove the last seedbox settings, of which we are now sure are no longer required
		for (String keyBase : removeKeyBases) {
			edit.remove(keyBase + max);
		}
		edit.commit();
	}

	/**
	 * Helper method to provide server settings for every configured seedbox of this type.
	 * @param prefs The shared preferences object to remove settings from
	 * @param orderOffset The offset to use when assigning unique ids to the server settings object (added to the
	 *            seedbox-unique internal order number)
	 */
	public List<ServerSetting> getAllServerSettings(SharedPreferences prefs, int orderOffset) {
		List<ServerSetting> servers = new ArrayList<ServerSetting>();
		for (int i = 0; true; i++) {
			ServerSetting settings = getServerSetting(prefs, orderOffset, i);
			if (settings != null)
				servers.add(settings);
			else
				break;
		}
		return Collections.unmodifiableList(servers);
	}

}
