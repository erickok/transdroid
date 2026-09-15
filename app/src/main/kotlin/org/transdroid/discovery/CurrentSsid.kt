/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.discovery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

/**
 * Reads the SSID of the Wi-Fi network the device is currently connected to, for
 * [org.transdroid.data.ServerProfile]'s local-network connection override. Android only returns
 * the real SSID (rather than a placeholder) when the caller holds fine location permission and
 * is actually connected to Wi-Fi - both checked here, so this simply returns null whenever the
 * SSID isn't knowable rather than a fake or stale value.
 */
object CurrentSsid {

    fun read(context: Context): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        @Suppress("DEPRECATION")
        val wifiManager = context.getSystemService(WifiManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        val ssid = wifiManager.connectionInfo?.ssid?.removeSurrounding("\"") ?: return null
        return ssid.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }
    }
}
