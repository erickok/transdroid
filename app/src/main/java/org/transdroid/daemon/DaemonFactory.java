/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.transdroid.daemon;

import java.util.HashMap;
import java.util.Map;

public class DaemonFactory {

    private static final Map<String, IDaemonAdapter> adapterMap = new HashMap<>();
    private static final Map<String, DaemonSettings> settingsMap = new HashMap<>();

    public static synchronized IDaemonAdapter getServerAdapter(DaemonSettings daemonSettings) {

        String idString = daemonSettings.getIdString();
        IDaemonAdapter daemonAdapter = adapterMap.get(idString);

        //If there is no adapter or the settings have changed, generate a new instance
        if(daemonAdapter == null || !daemonSettings.equals(settingsMap.get(idString))) {
            daemonAdapter = daemonSettings.getType().createAdapter(daemonSettings);
            adapterMap.put(idString, daemonAdapter);
            settingsMap.put(idString, daemonSettings);
        }

        return daemonAdapter;
    }
}
