package org.transdroid.lite.gui.navigation;

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
