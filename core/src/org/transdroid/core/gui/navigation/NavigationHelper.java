package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

@EBean
public class NavigationHelper {

	@RootContext
	protected Context context;

	/**
	 * Whether any search-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public String getAppNameAndVersion() {
		String appName = context.getString(R.string.app_name);
		try {
			PackageInfo m = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return appName + " " + m.versionName + " (" + m.versionCode + ")";
		} catch (NameNotFoundException e) {
			return appName;
		}
	}

	/**
	 * Returns whether the device is considered small (i.e. a phone) rather than large (i.e. a tablet). Can, for example, 
	 * be used to determine if a dialog should be shown full screen. Currently is true if the device's smallest 
	 * dimension is 500 dip.
	 * @return True if the app runs on a small device, false otherwise
	 */
	public boolean isSmallScreen() {
		return context.getResources().getBoolean(R.bool.show_dialog_fullscreen);
	}

	/**
	 * Whether any search-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableSearchUi() {
		return !context.getPackageName().equals("org.transdroid.lite");
	}

	/**
	 * Whether any RSS-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableRssUi() {
		return !context.getPackageName().equals("org.transdroid.lite");
	}
	
	/**
	 * Whether the navigation of server types and labels as filter are shown in a separate fragment.
	 * @return True if navigation is in a separate fragment, false if the items are shown in the action bar spinner
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public boolean showFiltersInFragment() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) { 
			if (context.getResources().getConfiguration().screenWidthDp >= 600) {
				return true;
			}
		}
		return false;
	}
	
}
