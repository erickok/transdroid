/*
 * Copyright 2010-2018 Eric Kok et al.
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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.SystemService;
import org.transdroid.R;

@EBean(scope = Scope.Singleton)
public class ConnectivityHelper {

    private static final int REQUEST_LOCATION_PERMISSION = 0;

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
        if (wifiManager != null && wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null) {
            return wifiManager.getConnectionInfo().getSSID().replace("\"", "");
        }
        return null;
    }

    public boolean hasNetworkNamePermission(final Context activityContext) {
        return ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    public void askNetworkNamePermission(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.pref_local_permission_title)
                .setMessage(activity.getString(R.string.pref_local_permission_rationale,
                        activity.getString(R.string.app_name)))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public boolean requestedPermissionWasGranted(int requestCode, String[] permissions, int[] grantResults) {
        return (requestCode == REQUEST_LOCATION_PERMISSION
                && permissions != null
                && grantResults != null
                && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                && grantResults[0] == PackageManager.PERMISSION_GRANTED);
    }

}
