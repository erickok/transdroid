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
package org.transdroid.core.app.settings;

import java.util.Date;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.EBean.Scope;
import org.transdroid.core.service.AppUpdateService;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.afollestad.materialdialogs.Theme;

/**
 * Allows instantiation of the settings specified in R.xml.pref_system.
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class SystemSettings {

	@RootContext
	protected Context context;
	private SharedPreferences prefs;

	protected SystemSettings(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public boolean treatDormantAsInactive() {
		return prefs.getBoolean("system_dormantasinactive", false);
	}

	/**
	 * Returns the interval in which automatic screen refreshes should be scheduled.
	 * @return The selected refresh interval in milliseconds or 0 if automatic refreshes should be disabled
	 */
	public long getRefreshIntervalMilliseconds() {
		return Integer.parseInt(prefs.getString("system_autorefresh", "0")) * 1000;
	}

	public boolean checkForUpdates() {
		return prefs.getBoolean("system_checkupdates", true);
	}

	public boolean useDarkTheme() {
		return prefs.getBoolean("system_usedarktheme", false);
	}

	public Theme getMaterialDialogtheme() {
		return useDarkTheme() ? Theme.DARK: Theme.LIGHT;
	}

	/**
	 * Returns the date when we last checked transdroid.org for the latest app version.
	 * @return The date/time when the {@link AppUpdateService} checked on the server for updates
	 */
	public Date getLastCheckedForAppUpdates() {
		long lastChecked = prefs.getLong("system_lastappupdatecheck", -1L);
		return lastChecked == -1 ? null : new Date(lastChecked);
	}

	/**
	 * Stores the date at which was last successfully, fully checked for new updates to the app.
	 * @param lastChecked The date/time at which the {@link AppUpdateService} last checked the server for updates
	 */
	public void setLastCheckedForAppUpdates(Date lastChecked) {
		prefs.edit().putLong("system_lastappupdatecheck", lastChecked == null ? -1L : lastChecked.getTime()).apply();
	}

}
