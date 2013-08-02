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

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.EBean.Scope;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

	public boolean checkForUpdates() {
		return prefs.getBoolean("system_checkupdates", true);
	}

	public boolean useDarkTheme() {
		return prefs.getBoolean("system_usedarktheme", false);
	}
	
}
