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

import java.util.List;

import org.transdroid.core.app.settings.ServerSetting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Interface to implement by any seedbox type (as appears in the {@link SeedboxProvider} enum) to access and modify 
 * seedbox settings for a certain seedbox provider.
 * @author Eric Kok
 */
public interface SeedboxSettings {

	/**
	 * Should return the name of the seedbox (and perhaps the seedbox type)
	 * @return A human-readable name of this seedbox provider
	 */
	public String getName();

	/**
	 * Should return the order number of the last specified seedbox of this type (regardless of other seedbox types).
	 * @param prefs The shared preferences to load the settings from
	 * @return The order number (id) of the last configured seedbox, or -1 if none are configured.
	 */
	public int getMaxSeedboxOrder(SharedPreferences prefs);

	/**
	 * Should return a {@link ServerSetting} object that can connect to the seedbox as identified by the given seedbox
	 * provider-unique order.
	 * @param prefs The shared preferences to load the settings from
	 * @param orderOffset An offset integer to add to the normal order number to assign a app-unique server id to this
	 *            seedbox
	 * @param order The seedbox provider-specific order id referring to the specific seedbox to load settings for
	 * @return A server settings object corresponding to the user's seedbox settings for the specified order id
	 */
	public ServerSetting getServerSetting(SharedPreferences prefs, int orderOffset, int order);

	/**
	 * Should return a list of all the {@link ServerSetting}s available for this seedbox type.
	 * @param prefs The shared preferences to load the settings from
	 * @param orderOffset An offset integer to add to the normal order number to assign a app-unique server id to this
	 *            seedbox
	 * @return A list of all server settings objects that are stored for this seedbox type
	 */
	public List<ServerSetting> getAllServerSettings(SharedPreferences prefs, int orderOffset);
	
	/**
	 * Should remove the settings of a specific seedbox specification as identified by its seedbox provider-unique order
	 * number.
	 * @param prefs The shared preferences to remove the settings from
	 * @param order The id referring to a specific seedbox order number within this type of seedbox
	 */
	public void removeServerSetting(SharedPreferences prefs, int order);

	/**
	 * The settings activity in which the user can supply and edit its settings for this specific seedbox type.
	 * @param context The activity context from where the settings activity will be started
	 * @return An already prepared intent that points to the settings activity for this specific type of seedbox
	 */
	public Intent getSettingsActivityIntent(Context context);

}
