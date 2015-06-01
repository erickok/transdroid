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
package org.transdroid.core.service;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.EBean.Scope;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

@EBean(scope = Scope.Singleton)
public class ConnectivityHelper {

	@SystemService
	protected ConnectivityManager connectivityManager;
	@SystemService
	protected WifiManager wifiManager;

	public boolean shouldPerformBackgroundActions() {
		// Check the current active network whether we are connected
		return connectivityManager.getActiveNetworkInfo() != null
				&& connectivityManager.getActiveNetworkInfo().isConnected();
	}

	public String getConnectedNetworkName() {
		if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null) {
			return wifiManager.getConnectionInfo().getSSID().replace("\"", "");
		}
		return null;
	}

}
