package org.transdroid.lite.app.settings;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.EBean.Scope;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Allows instantiation of the settings specified in R.xml.pref_about.
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class AboutSettings {

	@RootContext
	protected Context context;
	private SharedPreferences prefs;
	
	protected AboutSettings(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public boolean checkForUpdates() {
		return prefs.getBoolean("about_checkupdates", true);
	}
	
}
