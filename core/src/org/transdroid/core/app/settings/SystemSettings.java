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
