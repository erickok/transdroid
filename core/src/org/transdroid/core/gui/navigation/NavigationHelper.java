package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.annotation.TargetApi;
import android.content.Context;
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
