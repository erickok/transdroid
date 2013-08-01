package org.transdroid.core.service;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.EBean.Scope;

import android.content.Context;
import android.net.ConnectivityManager;

@EBean(scope = Scope.Singleton)
public class ConnectivityHelper {

	@SystemService
	protected ConnectivityManager connectivityManager;

	public ConnectivityHelper(Context context) {
	}

	@SuppressWarnings("deprecation")
	public boolean shouldPerformActions() {
		// First check the old background data setting (this will always be true for ICS+)
		if (!connectivityManager.getBackgroundDataSetting())
			return false;

		// Still good? Check the current active network instead
		return connectivityManager.getActiveNetworkInfo().isConnected();
	}

}
