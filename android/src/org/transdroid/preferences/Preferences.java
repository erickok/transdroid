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
 package org.transdroid.preferences;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.daemon.util.Pair;
import org.transdroid.gui.search.SearchSettings;
import org.transdroid.gui.search.SiteSettings;
import org.transdroid.gui.util.InterfaceSettings;
import org.transdroid.rss.RssFeedSettings;
import org.transdroid.service.AlarmSettings;
import org.transdroid.widget.WidgetSettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.seedm8.transdroid.preferences.SeedM8Settings;
import com.xirvik.transdroid.preferences.XirvikServerType;
import com.xirvik.transdroid.preferences.XirvikSettings;

/**
 * Helper to access and store Transdroid user preferences.
 * 
 * @author erickok
 */
public class Preferences {

	// These are all the (base) keys at which preferences are stored on the device
	public static final String KEY_PREF_LASTUSED	= "transdroid_server_lastused";
	public static final String KEY_PREF_SITE		= "transdroid_search_site";
	public static final String KEY_PREF_LASTSORTBY	= "transdroid_interface_lastusedsortby";
	public static final String KEY_PREF_LASTSORTORD	= "transdroid_interface_lastusedsortorder";

	public static final String KEY_PREF_XNAME		= "transdroid_xserver_name";
	public static final String KEY_PREF_XTYPE		= "transdroid_xserver_type";
	public static final String KEY_PREF_XSERVER		= "transdroid_xserver_server";
	public static final String KEY_PREF_XUSER		= "transdroid_xserver_user";
	public static final String KEY_PREF_XPASS		= "transdroid_xserver_pass";
	public static final String KEY_PREF_XALARMFINISHED	= "transdroid_xserver_alarmfinished";
	public static final String KEY_PREF_XALARMNEW	= "transdroid_xserver_alarmnew";

	public static final String KEY_PREF_8NAME		= "transdroid_8server_name";
	public static final String KEY_PREF_8SERVER		= "transdroid_8server_server";
	public static final String KEY_PREF_8USER		= "transdroid_8server_user";
	public static final String KEY_PREF_8DPASS		= "transdroid_8server_dpass";
	public static final String KEY_PREF_8DPORT		= "transdroid_8server_dprt";
	public static final String KEY_PREF_8TPASS		= "transdroid_8server_tpass";
	public static final String KEY_PREF_8TPORT		= "transdroid_8server_tport";
	public static final String KEY_PREF_8RPASS		= "transdroid_8server_rpass";
	public static final String KEY_PREF_8SPASS		= "transdroid_8server_spass";
	public static final String KEY_PREF_8ALARMFINISHED	= "transdroid_8server_alarmfinished";
	public static final String KEY_PREF_8ALARMNEW	= "transdroid_8server_alarmnew";
	
	public static final String KEY_PREF_NAME		= "transdroid_server_name";
	public static final String KEY_PREF_DAEMON		= "transdroid_server_daemon";
	public static final String KEY_PREF_ADDRESS		= "transdroid_server_address";
	public static final String KEY_PREF_PORT		= "transdroid_server_port";
	public static final String KEY_PREF_SSL			= "transdroid_server_ssl";
	public static final String KEY_PREF_SSL_TRUST_ALL= "transdroid_server_ssl_trust_all";
	public static final String KEY_PREF_SSL_TRUST_KEY= "transdroid_server_ssl_trust_key";
	public static final String KEY_PREF_FOLDER		= "transdroid_server_folder";
	public static final String KEY_PREF_AUTH		= "transdroid_server_auth";
	public static final String KEY_PREF_USER		= "transdroid_server_user";
	public static final String KEY_PREF_PASS		= "transdroid_server_pass";
	public static final String KEY_PREF_OS			= "transdroid_server_os";
	public static final String KEY_PREF_DOWNLOADDIR	= "transdroid_server_downloaddir";
	public static final String KEY_PREF_FTPURL		= "transdroid_server_ftpurl";
	public static final String KEY_PREF_TIMEOUT		= "transdroid_server_timeout";
	public static final String KEY_PREF_ALARMFINISHED= "transdroid_server_alarmfinished";
	public static final String KEY_PREF_ALARMNEW	= "transdroid_server_alarmnew";

	public static final String KEY_PREF_WEBSITE		= "transdroid_website_name";
	public static final String KEY_PREF_WEBURL		= "transdroid_website_url";
	
	public static final String KEY_PREF_DEF_SITE	= "site_isohunt"; // Default site adapter
	public static final String KEY_PREF_COMBINED	= "sort_combined"; // See also @array/pref_sort_values
	public static final String KEY_PREF_SEEDS		= "sort_seeders"; // See also @array/pref_sort_values

	public static final String KEY_PREF_LASTSORTGTZERO	= "transdroid_interface_lastusedgtzero";
	public static final String KEY_PREF_SWIPELABELS	= "transdroid_interface_swipelabels";
	public static final String KEY_PREF_NUMRESULTS	= "transdroid_search_numresults";
	public static final String KEY_PREF_SORT		= "transdroid_search_sort";
	public static final String KEY_PREF_UIREFRESH	= "transdroid_interface_uirefresh";
	public static final String KEY_PREF_ONLYDL		= "transdroid_interface_onlydl";
	public static final String KEY_PREF_HIDEREFRESH= "transdroid_interface_hiderefresh";
	public static final String KEY_PREF_ASKREMOVE 	= "transdroid_interface_askremove";
	public static final String KEY_PREF_ENABLEADS 	= "transdroid_interface_enableads";

	public static final String KEY_PREF_RSSNAME		= "transdroid_rss_name";
	public static final String KEY_PREF_RSSURL		= "transdroid_rss_url";
	public static final String KEY_PREF_RSSAUTH		= "transdroid_rss_needsauth";
	public static final String KEY_PREF_RSSLASTNEW	= "transdroid_rss_lastnew";

	public static final String KEY_PREF_ENABLEALARM	= "transdroid_alarm_enable";
	public static final String KEY_PREF_ALARMINT	= "transdroid_alarm_interval";
	public static final String KEY_PREF_LASTUPDATE	= "transdroid_alarm_lastupdatestats";
	public static final String KEY_PREF_QUEUEDTOADD	= "transdroid_alarm_queuedtoadd";
	public static final String KEY_PREF_CHECKRSSFEEDS = "transdroid_alarm_checkrssfeeds";
	public static final String KEY_PREF_ALARMPLAYSOUND = "transdroid_alarm_playsound";
	public static final String KEY_PREF_ALARMSOUNDURI = "transdroid_alarm_sounduri";
	public static final String KEY_PREF_ALARMVIBRATE = "transdroid_alarm_vibrate";
	public static final String KEY_PREF_ADWNOTIFY    = "transdroid_alarm_adwnotify";
	public static final String KEY_PREF_ADWONLYDL    = "transdroid_alarm_adwonlydl";
	
	public static final String KEY_WIDGET_DAEMON	 = "transdroid_widget_daemon";
	public static final String KEY_WIDGET_REFRESH	 = "transdroid_widget_refresh";
	public static final String KEY_WIDGET_LAYOUT	 = "transdroid_widget_layout";
	
	/**
	 * Determines the order number of the last used daemon settings object
	 * @param prefs The application's shared preferences
     * @param allDaemons All available daemons settings
	 * @return The order number (0-based) of the server that was last used (or 0 if it doesn't exist any more)
	 */
	public static int readLastUsedDaemonOrderNumber(SharedPreferences prefs, List<DaemonSettings> allDaemons) {
    	
		// Get last used number
		String prefLast = prefs.getString(KEY_PREF_LASTUSED, "");
    	int last = (prefLast == ""? 0: Integer.parseInt(prefLast));
    	if (last > allDaemons.size()) {
    		// The used daemon doesn't exist any more
    		return 0;
    	}
    	
    	return last;
	}

	public static void removeDaemonSettings(SharedPreferences prefs, DaemonSettings toRemove) {

		Editor editor = prefs.edit();
		
		// Move all daemon settings 'up' 1 spot (by saving the preferences to an order id number 1 lower)
		int id = (toRemove.getIdString() == ""? 0: Integer.parseInt(toRemove.getIdString()));
		while (prefs.contains(KEY_PREF_ADDRESS + Integer.toString(id + 1))) {
			
			// Copy the preferences
			String fromId = Integer.toString(id + 1);
			String toId = (id == 0? "": Integer.toString(id));
			editor.putString(KEY_PREF_NAME + toId, prefs.getString(KEY_PREF_NAME + fromId, null));
			editor.putString(KEY_PREF_DAEMON + toId, prefs.getString(KEY_PREF_DAEMON + fromId, null));
			editor.putString(KEY_PREF_ADDRESS + toId, prefs.getString(KEY_PREF_ADDRESS + fromId, null));
			editor.putString(KEY_PREF_PORT + toId, prefs.getString(KEY_PREF_PORT + fromId, null));
			editor.putBoolean(KEY_PREF_SSL + toId, prefs.getBoolean(KEY_PREF_SSL + fromId, false));
			editor.putBoolean(KEY_PREF_SSL_TRUST_ALL + toId, prefs.getBoolean(KEY_PREF_SSL_TRUST_ALL + fromId, false));
			editor.putString(KEY_PREF_SSL_TRUST_KEY + toId, prefs.getString(KEY_PREF_SSL_TRUST_KEY + fromId, null));
			editor.putString(KEY_PREF_FOLDER + toId, prefs.getString(KEY_PREF_FOLDER + fromId, null));
			editor.putBoolean(KEY_PREF_AUTH + toId, prefs.getBoolean(KEY_PREF_AUTH + fromId, false));
			editor.putString(KEY_PREF_USER + toId, prefs.getString(KEY_PREF_USER + fromId, null));
			editor.putString(KEY_PREF_PASS + toId, prefs.getString(KEY_PREF_PASS + fromId, null));
			editor.putString(KEY_PREF_OS + toId, prefs.getString(KEY_PREF_OS + fromId, "type_windows"));
			editor.putString(KEY_PREF_DOWNLOADDIR + toId, prefs.getString(KEY_PREF_DOWNLOADDIR + fromId, null));
			editor.putString(KEY_PREF_FTPURL + toId, prefs.getString(KEY_PREF_FTPURL + fromId, null));
			id++;
			
		}
		
		// Remove the last server preferences configuration
		String delId = (id == 0? "": Integer.toString(id));
		editor.remove(KEY_PREF_NAME + delId);
		editor.remove(KEY_PREF_DAEMON + delId);
		editor.remove(KEY_PREF_ADDRESS + delId);
		editor.remove(KEY_PREF_PORT + delId);
		editor.remove(KEY_PREF_SSL + delId);
		editor.remove(KEY_PREF_FOLDER + delId);
		editor.remove(KEY_PREF_AUTH + delId);
		editor.remove(KEY_PREF_USER + delId);
		editor.remove(KEY_PREF_PASS + delId);
		editor.remove(KEY_PREF_OS + delId);
		editor.remove(KEY_PREF_DOWNLOADDIR + delId);
		editor.remove(KEY_PREF_FTPURL + delId);

		// If the last used daemon...
		String lastUsed = prefs.getString(KEY_PREF_LASTUSED, "");
		int lastUsedId = (lastUsed.equals("")? 0: Integer.parseInt(lastUsed));
		int toRemoveId = (toRemove.getIdString() == ""? 0: Integer.parseInt(toRemove.getIdString()));
		
		// ... was removed itself
		if (lastUsed.equals(toRemove.getIdString())) {
			// ... set the default to the very first configuration
			// (this may also not exist any more, but this is handled by the UI)
			editor.putString(KEY_PREF_LASTUSED, "");
			
		// Else if is was move 'up' (now has an order number -1)
		} else if (lastUsedId > toRemoveId) {
			// ... update the last used setting appropriately
			editor.putString(KEY_PREF_LASTUSED, (lastUsedId - 1 == 0? "": Integer.toString(lastUsedId - 1)));
		}
		
		editor.commit();
	}

	public static void removeXirvikSettings(SharedPreferences prefs, XirvikSettings toRemove) {

		Editor editor = prefs.edit();
		
		// Move all xirvik server settings 'up' 1 spot (by saving the preferences to an order id number 1 lower)
		int id = (toRemove.getIdString() == ""? 0: Integer.parseInt(toRemove.getIdString()));
		while (prefs.contains(KEY_PREF_XSERVER + Integer.toString(id + 1))) {
			
			// Copy the preferences
			String fromId = Integer.toString(id + 1);
			String toId = (id == 0? "": Integer.toString(id));
			editor.putString(KEY_PREF_XNAME + toId, prefs.getString(KEY_PREF_XNAME + fromId, null));
			editor.putString(KEY_PREF_XTYPE + toId, prefs.getString(KEY_PREF_XTYPE + fromId, null));
			editor.putString(KEY_PREF_XSERVER+ toId, prefs.getString(KEY_PREF_XSERVER + fromId, null));
			editor.putString(KEY_PREF_XUSER + toId, prefs.getString(KEY_PREF_XUSER + fromId, null));
			editor.putString(KEY_PREF_XPASS + toId, prefs.getString(KEY_PREF_XPASS + fromId, null));
			editor.putString(KEY_PREF_XALARMFINISHED + toId, prefs.getString(KEY_PREF_XALARMFINISHED + fromId, null));
			editor.putString(KEY_PREF_XALARMNEW + toId, prefs.getString(KEY_PREF_XALARMNEW + fromId, null));
			id++;
			
		}
		
		// Remove the last server preferences configuration
		String delId = (id == 0? "": Integer.toString(id));
		editor.remove(KEY_PREF_XNAME + delId);
		editor.remove(KEY_PREF_XTYPE + delId);
		editor.remove(KEY_PREF_XSERVER + delId);
		editor.remove(KEY_PREF_XUSER + delId);
		editor.remove(KEY_PREF_XPASS + delId);
		editor.remove(KEY_PREF_XALARMNEW + delId);
		editor.remove(KEY_PREF_XALARMFINISHED + delId);

		// If the last used daemon...
		String lastUsed = prefs.getString(KEY_PREF_LASTUSED, "");
		
		// no longer exists...
		if (!prefs.contains(KEY_PREF_ADDRESS + lastUsed)) {
			// Just reset the last used number
			editor.putString(KEY_PREF_LASTUSED, "");
		}

		editor.commit();
	}

	public static void removeSeedM8Settings(SharedPreferences prefs, SeedM8Settings toRemove) {

		Editor editor = prefs.edit();
		
		// Move all SeedM8 server settings 'up' 1 spot (by saving the preferences to an order id number 1 lower)
		int id = (toRemove.getIdString() == ""? 0: Integer.parseInt(toRemove.getIdString()));
		while (prefs.contains(KEY_PREF_8SERVER + Integer.toString(id + 1))) {
			
			// Copy the preferences
			String fromId = Integer.toString(id + 1);
			String toId = (id == 0? "": Integer.toString(id));
			editor.putString(KEY_PREF_8NAME + toId, prefs.getString(KEY_PREF_8NAME + fromId, null));
			editor.putString(KEY_PREF_8SERVER+ toId, prefs.getString(KEY_PREF_XSERVER + fromId, null));
			editor.putString(KEY_PREF_8USER + toId, prefs.getString(KEY_PREF_XUSER + fromId, null));
			editor.putString(KEY_PREF_8DPASS + toId, prefs.getString(KEY_PREF_8DPASS + fromId, null));
			editor.putString(KEY_PREF_8DPORT + toId, prefs.getString(KEY_PREF_8DPORT + fromId, null));
			editor.putString(KEY_PREF_8TPASS + toId, prefs.getString(KEY_PREF_8TPASS + fromId, null));
			editor.putString(KEY_PREF_8TPORT + toId, prefs.getString(KEY_PREF_8TPORT + fromId, null));
			editor.putString(KEY_PREF_8RPASS + toId, prefs.getString(KEY_PREF_8RPASS + fromId, null));
			editor.putString(KEY_PREF_8SPASS + toId, prefs.getString(KEY_PREF_8SPASS + fromId, null));
			editor.putString(KEY_PREF_8ALARMFINISHED + toId, prefs.getString(KEY_PREF_8ALARMFINISHED + fromId, null));
			editor.putString(KEY_PREF_8ALARMNEW + toId, prefs.getString(KEY_PREF_8ALARMNEW + fromId, null));
			id++;
			
		}
		
		// Remove the last server preferences configuration
		String delId = (id == 0? "": Integer.toString(id));
		editor.remove(KEY_PREF_8NAME + delId);
		editor.remove(KEY_PREF_8SERVER + delId);
		editor.remove(KEY_PREF_8USER + delId);
		editor.remove(KEY_PREF_8DPASS + delId);
		editor.remove(KEY_PREF_8DPORT + delId);
		editor.remove(KEY_PREF_8TPASS + delId);
		editor.remove(KEY_PREF_8TPORT + delId);
		editor.remove(KEY_PREF_8RPASS + delId);
		editor.remove(KEY_PREF_8SPASS + delId);
		editor.remove(KEY_PREF_8ALARMNEW + delId);
		editor.remove(KEY_PREF_8ALARMFINISHED + delId);

		// If the last used daemon...
		String lastUsed = prefs.getString(KEY_PREF_LASTUSED, "");
		
		// no longer exists...
		if (!prefs.contains(KEY_PREF_ADDRESS + lastUsed)) {
			// Just reset the last used number
			editor.putString(KEY_PREF_LASTUSED, "");
		}

		editor.commit();
	}

	public static void removeSiteSettings(SharedPreferences prefs, SiteSettings toRemove) {

		Editor editor = prefs.edit();
		
		// Move all site settings 'up' 1 spot (by saving the preferences to an order id number 1 lower)
		int id = Integer.parseInt(toRemove.getKey());
		while (prefs.contains(KEY_PREF_WEBURL + Integer.toString(id + 1))) {
			
			// Copy the preferences
			String fromId = Integer.toString(id + 1);
			String toId = Integer.toString(id);
			editor.putString(KEY_PREF_WEBSITE + toId, prefs.getString(KEY_PREF_WEBSITE + fromId, null));
			editor.putString(KEY_PREF_WEBURL + toId, prefs.getString(KEY_PREF_WEBURL + fromId, null));
			id++;
			
		}
		
		// Remove the last preferences configuration
		String delId = Integer.toString(id);
		editor.remove(KEY_PREF_WEBSITE + delId);
		editor.remove(KEY_PREF_WEBURL + delId);
		
		// If the default search site...
		SiteSettings defaultSite = readDefaultSearchSiteSettings(prefs);
		// ... is a web search site
		if (defaultSite.isWebSearch()) {
			int toRemoveId = Integer.parseInt(toRemove.getKey());
			int defaultSiteId = Integer.parseInt(defaultSite.getKey());
			
			// ... and was removed itself
			if (defaultSite.getKey().equals(toRemove.getKey())) {
				// ... set the default back to the default default :)
				editor.putString(KEY_PREF_SITE, KEY_PREF_DEF_SITE);
			
			// else if it was moved 'up' (now has an order number -1)
			} else if (defaultSiteId > toRemoveId) {
				// ... update the default site appropriately
				editor.putString(KEY_PREF_SITE, Integer.toString(defaultSiteId - 1));
			}
		}
		
		editor.commit();
	}

	public static void removeRssFeedSettings(SharedPreferences prefs, RssFeedSettings toRemove) {

		Editor editor = prefs.edit();
		
		// Move all feed settings 'up' 1 spot (by saving the preferences to an order id number 1 lower)
		int id = Integer.parseInt(toRemove.getKey());
		while (prefs.contains(KEY_PREF_RSSURL + Integer.toString(id + 1))) {
			
			// Copy the preferences
			String fromId = Integer.toString(id + 1);
			String toId = Integer.toString(id);
			editor.putString(KEY_PREF_RSSNAME + toId, prefs.getString(KEY_PREF_RSSNAME + fromId, null));
			editor.putString(KEY_PREF_RSSURL + toId, prefs.getString(KEY_PREF_RSSURL + fromId, null));
			editor.putBoolean(KEY_PREF_RSSAUTH + toId, prefs.getBoolean(KEY_PREF_RSSAUTH + fromId, false));
			editor.putString(KEY_PREF_RSSLASTNEW + toId, prefs.getString(KEY_PREF_RSSLASTNEW + fromId, null));
			id++;
			
		}
		
		// Remove the last preferences configuration
		String delId = Integer.toString(id);
		editor.remove(KEY_PREF_RSSNAME + delId);
		editor.remove(KEY_PREF_RSSURL + delId);
		editor.remove(KEY_PREF_RSSAUTH + delId);
		editor.remove(KEY_PREF_RSSLASTNEW + delId);
		
		editor.commit();
	}

	public static void moveRssFeedSettings(SharedPreferences prefs, RssFeedSettings toMove, boolean moveUp) {

		Editor editor = prefs.edit();
		
		// Which feeds to switch position of (bounds are NOT checked)
		int from = Integer.parseInt(toMove.getKey());
		int to;
		if (moveUp) {
			to = from - 1;
		} else {
			to = from + 1;
		}
		
		// Switch their settings
		String fromName = prefs.getString(KEY_PREF_RSSNAME + from, null);
		String fromUrl = prefs.getString(KEY_PREF_RSSURL + from, null);
		boolean fromAuth = prefs.getBoolean(KEY_PREF_RSSAUTH + from, false);
		String fromLast = prefs.getString(KEY_PREF_RSSLASTNEW + from, null);
		editor.putString(KEY_PREF_RSSNAME + from, prefs.getString(KEY_PREF_RSSNAME + to, null));
		editor.putString(KEY_PREF_RSSURL + from, prefs.getString(KEY_PREF_RSSURL + to, null));
		editor.putBoolean(KEY_PREF_RSSAUTH + from, prefs.getBoolean(KEY_PREF_RSSAUTH + to, false));
		editor.putString(KEY_PREF_RSSLASTNEW + from, prefs.getString(KEY_PREF_RSSLASTNEW + to, null));
		editor.putString(KEY_PREF_RSSNAME + to, fromName);
		editor.putString(KEY_PREF_RSSURL + to, fromUrl);
		editor.putBoolean(KEY_PREF_RSSAUTH + to, fromAuth);
		editor.putString(KEY_PREF_RSSLASTNEW + to, fromLast);

		editor.commit();
	}

	/**
	 * Build a list of xirvik server setting objects, available in the stored preferences
	 * @param prefs The application's shared preferences
	 * @return A list of all xirvik server configurations available
	 */
	public static List<XirvikSettings> readAllXirvikSettings(SharedPreferences prefs) {

        // Build a list of xirvik server setting objects, available in the stored preferences
        List<XirvikSettings> xservers = new ArrayList<XirvikSettings>();
        int i = 0;
        String nextName = KEY_PREF_XSERVER;
        while (prefs.contains(nextName)) {
        	
        	// The first server is stored without number, subsequent ones have an order number after the regular pref key
        	String postfix = (i == 0? "": Integer.toString(i));
        	
        	// Add an entry for this server
        	xservers.add(readXirvikSettings(prefs, postfix));
        	
        	// Search for more
        	i++;
        	nextName = KEY_PREF_XSERVER + Integer.toString(i);
        }

        return xservers;
	}

	/**
	 * Build a list of seedm8 server setting objects, available in the stored preferences
	 * @param prefs The application's shared preferences
	 * @return A list of all seedm8 server configurations available
	 */
	public static List<SeedM8Settings> readAllSeedM8Settings(SharedPreferences prefs) {

        // Build a list of seedm8 server setting objects, available in the stored preferences
        List<SeedM8Settings> s8servers = new ArrayList<SeedM8Settings>();
        int i = 0;
        String nextName = KEY_PREF_8SERVER;
        while (prefs.contains(nextName)) {
        	
        	// The first server is stored without number, subsequent ones have an order number after the regular pref key
        	String postfix = (i == 0? "": Integer.toString(i));
        	
        	// Add an entry for this server
        	s8servers.add(readSeedM8Settings(prefs, postfix));
        	
        	// Search for more
        	i++;
        	nextName = KEY_PREF_8SERVER + Integer.toString(i);
        }

        return s8servers;
	}

	/**
	 * Build a list of server setting objects, available in the stored preferences
	 * @param prefs The application's shared preferences
	 * @return A list of all daemon configurations available
	 */
	public static List<DaemonSettings> readAllNormalDaemonSettings(SharedPreferences prefs) {

        // Build a list of server setting objects, available in the stored preferences
        List<DaemonSettings> daemons = new ArrayList<DaemonSettings>();
        int i = 0;
        String nextName = KEY_PREF_ADDRESS;
        while (prefs.contains(nextName)) {
        	
        	// The first server is stored without number, subsequent ones have an order number after the regular pref key
        	String postfix = (i == 0? "": Integer.toString(i));
        	
        	// Add an entry for this server
        	daemons.add(readDaemonSettings(prefs, postfix));
        	
        	// Search for more
        	i++;
        	nextName = KEY_PREF_ADDRESS + Integer.toString(i);
        }

        return daemons;
	}
	
	public static List<DaemonSettings> readAllDaemonSettings(SharedPreferences prefs) {
		
		// Build a list of all 'normal' (manual) and all xirvik daemons
		List<DaemonSettings> daemons = readAllNormalDaemonSettings(prefs);
		int max = -1;
		if (daemons.size() > 0) {
			String maxId = daemons.get(daemons.size() - 1).getIdString();
			max = maxId.equals("")? 0: Integer.parseInt(maxId);
		}
		for (XirvikSettings xirvik : readAllXirvikSettings(prefs)) {
			daemons.addAll(xirvik.createDaemonSettings(max + 1));
		}
		if (daemons.size() > 0) {
			String maxId = daemons.get(daemons.size() - 1).getIdString();
			max = maxId.equals("")? 0: Integer.parseInt(maxId);
		}
		for (SeedM8Settings seedm8 : readAllSeedM8Settings(prefs)) {
			daemons.addAll(seedm8.createDaemonSettings(max + 1));
		}
		return daemons;
		
	}

	/**
	 * Build a list of site setting objects; in-app ones and those available in the stored preferences
	 * @param prefs The application's shared preferences
	 * @return A list of all site configurations available
	 */
	public static List<SiteSettings> readAllSiteSettings(SharedPreferences prefs) {

        // Build a list of in-app sites
        List<SiteSettings> sites = getSupportedSiteSettings();
        
        // Add site setting objects for web search sites, available in the stored preferences
        sites.addAll(readAllWebSearchSiteSettings(prefs));

        return sites;
	}

    /**
     * Returns a list of search site settings for supported torrent search sites
     * @return An ordered list of site settings for all available adapters
     */
    public static List<SiteSettings> getSupportedSiteSettings() {
            List<SiteSettings> settings = new ArrayList<SiteSettings>();
            settings.add(new SiteSettings("site_btjunkie", "BTJunkie"));
            settings.add(new SiteSettings("site_extratorrent", "ExtraTorrent"));
            settings.add(new SiteSettings("site_ezrss", "EzRss"));
            settings.add(new SiteSettings("site_isohunt", "isoHunt"));
            settings.add(new SiteSettings("site_kickasstorrents", "KickassTorrents"));
            settings.add(new SiteSettings("site_mininova", "Mininova"));
            settings.add(new SiteSettings("site_monova", "Monova"));
            settings.add(new SiteSettings("site_thepiratebay", "The Pirate Bay"));
            settings.add(new SiteSettings("site_torrentdownloads", "Torrent Downloads"));
            settings.add(new SiteSettings("site_torrentreactor", "Torrent Reactor"));
            settings.add(new SiteSettings("site_vertor", "Vertor"));
            return settings;
    }
    
    /**
     * Returns the unique key to use in search request to the content provider 
     * from the unique key as use din Transdroid's settings (for compatibility)
     * @param preferencesKey The Transdroid preferences key, f.e. 'iso_mininova'
     * @return The Transdroid Torrent Search site key, f.e. 'Mininova'
     */
    public static String getCursorKeyForPreferencesKey(String preferencesKey) {
    	if (preferencesKey.equals("site_btjunkie")) {
    		return "Btjunkie";
    	} else if (preferencesKey.equals("site_extratorrent")) {
    		return "ExtraTorrent";
    	} else if (preferencesKey.equals("site_ezrss")) {
    		return "EzRss";
    	} else if (preferencesKey.equals("site_isohunt")) {
    		return "Isohunt";
    	} else if (preferencesKey.equals("site_kickasstorrents")) {
    		return "KickassTorents";
    	} else if (preferencesKey.equals("site_mininova")) {
    		return "Mininova";
    	} else if (preferencesKey.equals("site_monova")) {
    		return "Monova";
    	} else if (preferencesKey.equals("site_thepiratebay")) {
    		return "ThePirateBay";
    	} else if (preferencesKey.equals("site_torrentdownloads")) {
    		return "TorrentDownloads";
    	} else if (preferencesKey.equals("site_torrentreactor")) {
    		return "TorrentReactor";
    	} else if (preferencesKey.equals("site_vertor")) {
    		return "Vertor";
    	}
    	return null;
    }

    public static SiteSettings getSupportedSiteSetting(String preferencesKey) {
    	if (preferencesKey.equals("site_btjunkie")) {
    		return new SiteSettings(preferencesKey, "BTJunkie");
    	} else if (preferencesKey.equals("site_extratorrent")) {
    		return new SiteSettings(preferencesKey, "ExtraTorrent");
    	} else if (preferencesKey.equals("site_ezrss")) {
    		return new SiteSettings(preferencesKey, "EzRss");
    	} else if (preferencesKey.equals("site_isohunt")) {
    		return new SiteSettings(preferencesKey, "Isohunt");
    	} else if (preferencesKey.equals("site_kickasstorrents")) {
    		return new SiteSettings(preferencesKey, "KickassTorents");
    	} else if (preferencesKey.equals("site_mininova")) {
    		return new SiteSettings(preferencesKey, "Mininova");
    	} else if (preferencesKey.equals("site_monova")) {
    		return new SiteSettings(preferencesKey, "Monova");
    	} else if (preferencesKey.equals("site_thepiratebay")) {
    		return new SiteSettings(preferencesKey, "ThePirateBay");
    	} else if (preferencesKey.equals("site_torrentdownloads")) {
    		return new SiteSettings(preferencesKey, "TorrentDownloads");
    	} else if (preferencesKey.equals("site_torrentreactor")) {
    		return new SiteSettings(preferencesKey, "TorrentReactor");
    	} else if (preferencesKey.equals("site_vertor")) {
    		return new SiteSettings(preferencesKey, "Vertor");
    	}
    	return null;
    }

    public static SiteSettings readDefaultSearchSiteSettings(SharedPreferences prefs) {
    	
    	String prefName = prefs.getString(KEY_PREF_SITE, KEY_PREF_DEF_SITE);
    	
    	// In-app search?
    	SiteSettings inapp = getSupportedSiteSetting(prefName);
    	if (inapp != null) {
    		return inapp;
    	}
    	
    	// Web-based search
    	if (!prefs.contains(KEY_PREF_WEBURL + prefName)) {
    		// The set default doesn't exists any more: return the default default :) search
    		return getSupportedSiteSetting(KEY_PREF_DEF_SITE);
    	}
    	return readSiteSettings(prefs, prefName);
    }

	/**
	 * Build a list of web-based search sites
	 * @param prefs The application's shared preferences
	 * @return A list of all web search configurations available
	 */
	public static List<SiteSettings> readAllWebSearchSiteSettings(SharedPreferences prefs) {

		List<SiteSettings> sites = new ArrayList<SiteSettings>();
		
        // Add site setting objects for web search sites, available in the stored preferences
        int i = 0;
        String nextName = KEY_PREF_WEBURL + Integer.toString(i);
        while (prefs.contains(nextName)) {
        	
        	// Stored sites have a zero-based order number
        	String postfix = Integer.toString(i);
        	
        	// Add an entry for this server
        	sites.add(readSiteSettings(prefs, postfix));
        	
        	// Search for more
        	i++;
        	nextName = KEY_PREF_WEBURL + Integer.toString(i);
        }

        return sites;
	}

	/**
	 * Build a list of rss feed setting objects as available in the stored preferences
	 * @param prefs The application's shared preferences
	 * @return A list of all feeds available
	 */
	public static List<RssFeedSettings> readAllRssFeedSettings(SharedPreferences prefs) {

        // Build a list of in-app sites
        List<RssFeedSettings> feeds = new ArrayList<RssFeedSettings>();
        
        // Add feed setting objects available in the stored preferences
        int i = 0;
        String nextName = KEY_PREF_RSSURL + Integer.toString(i);
        while (prefs.contains(nextName)) {
        	
        	// Stored sites have a zero-based order number
        	String postfix = Integer.toString(i);
        	
        	// Add an entry for this server
        	feeds.add(readRssFeedSettings(prefs, postfix));
        	
        	// Search for more
        	i++;
        	nextName = KEY_PREF_RSSURL + Integer.toString(i);
        }

        return feeds;
	}

    /**
     * Read the settings of the last used daemon configuration
     * @param prefs The preferences object to retrieve the settings from
     * @param allDaemons All available daemons settings
     * @return The daemon settings of the last used configuration
     */
    public static DaemonSettings readLastUsedDaemonSettings(SharedPreferences prefs, List<DaemonSettings> allDaemons) {
    	
    	if (allDaemons == null || allDaemons.size() == 0) {
    		return null;
    	}
    	
    	int last = readLastUsedDaemonOrderNumber(prefs, allDaemons);
    	return allDaemons.get(last);
    	
    }

    /**
     * Store the new default search site, so all 'traffic' will be directed here
     * @param context The application context to get the preferences from
     * @param siteKey The unique key of the search site that needs to be made default
     */
	public static void storeLastUsedSearchSiteSettings(Context context, String siteKey) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putString(KEY_PREF_SITE, siteKey);
		edit.commit();
	}
	
    /**
     * Store the last used daemon settings, so all 'traffic' will be directed here
     * @param context The application context to get the preferences from
     * @param daemonOrderNumber The order number of the last used daemon settings object
     */
	public static void storeLastUsedDaemonSettings(Context context, int daemonOrderNumber) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putString(KEY_PREF_LASTUSED, (daemonOrderNumber == 0? "": Integer.toString(daemonOrderNumber)));
		edit.commit();
	}

    private static XirvikSettings readXirvikSettings(SharedPreferences prefs, String postfix) {

    	// Read saved preferences
    	String prefType = prefs.getString(KEY_PREF_XTYPE + postfix, null);
    		
    	// Return daemon settings
    	return new XirvikSettings(
    		prefs.getString(KEY_PREF_XNAME + postfix, null), 
			XirvikServerType.fromCode(prefType), 
			prefs.getString(KEY_PREF_XSERVER + postfix, null),
			prefs.getString(KEY_PREF_XUSER + postfix, null),
    		prefs.getString(KEY_PREF_XPASS + postfix, null),
    		prefs.getBoolean(KEY_PREF_XALARMFINISHED + postfix, true),
    		prefs.getBoolean(KEY_PREF_XALARMNEW + postfix, false),
    		postfix);

    }

    private static SeedM8Settings readSeedM8Settings(SharedPreferences prefs, String postfix) {

    	// Read saved preferences
        String prefDPort = prefs.getString(KEY_PREF_8DPORT + postfix, null);
        String prefTPort = prefs.getString(KEY_PREF_8TPORT + postfix, null);
        
        // Test if ports are set
        int prefDPortI = Daemon.getDefaultPortNumber(Daemon.Deluge, false);
        try {
        	int dportI = Integer.parseInt(prefDPort);
        	if (dportI > 0) {
        		prefDPortI = dportI;
        	}
        } catch (NumberFormatException e) {	}
        int prefTPortI = Daemon.getDefaultPortNumber(Daemon.Transmission, false);
        try {
        	int tportI = Integer.parseInt(prefTPort);
        	if (tportI > 0) {
        		prefTPortI = tportI;
        	}
        } catch (NumberFormatException e) {	}
        
    	// Return daemon settings
    	return new SeedM8Settings(
    		prefs.getString(KEY_PREF_8NAME + postfix, null), 
			prefs.getString(KEY_PREF_8SERVER + postfix, null),
			prefs.getString(KEY_PREF_8USER + postfix, null),
			prefDPortI,
    		prefs.getString(KEY_PREF_8DPASS + postfix, null),
    		prefTPortI,
    		prefs.getString(KEY_PREF_8TPASS + postfix, null),
    		prefs.getString(KEY_PREF_8RPASS + postfix, null),
    		prefs.getString(KEY_PREF_8SPASS + postfix, null),
    		prefs.getBoolean(KEY_PREF_XALARMFINISHED + postfix, true),
    		prefs.getBoolean(KEY_PREF_XALARMNEW + postfix, false),
    		postfix);

    }

    private static DaemonSettings readDaemonSettings(SharedPreferences prefs, String postfix) {

    	// Read saved preferences
    	String prefName = prefs.getString(KEY_PREF_NAME + postfix, null);
    	Daemon prefDaemon = Daemon.fromCode(prefs.getString(KEY_PREF_DAEMON + postfix, null));
    	String prefAddress = prefs.getString(KEY_PREF_ADDRESS + postfix, null);
    	String prefFolder = prefs.getString(KEY_PREF_FOLDER + postfix, "");
        String prefPort = prefs.getString(KEY_PREF_PORT + postfix, null);
        boolean prefSsl = prefs.getBoolean(KEY_PREF_SSL + postfix, false);
        String prefTimeout = prefs.getString(KEY_PREF_TIMEOUT + postfix, null);

        // Parse port number
        int prefPortI = Daemon.getDefaultPortNumber(prefDaemon, prefSsl);
        try {
        	int portI = Integer.parseInt(prefPort);
        	if (portI > 0) {
        		prefPortI = portI;
        	}
        } catch (NumberFormatException e) {	}

        // Parse timeout time
        int prefTimeoutI = HttpHelper.DEFAULT_CONNECTION_TIMEOUT;
        try {
        	int timeoutI = Integer.parseInt(prefTimeout);
        	if (timeoutI > 0) {
        		prefTimeoutI = timeoutI;
        	}
        } catch (NumberFormatException e) {	}
        	
    	// Return daemon settings
    	return new DaemonSettings(
			prefName, 
			prefDaemon, 
			prefAddress.trim().replace("\t", ""), 
			prefPortI, 
			prefSsl,
			prefs.getBoolean(KEY_PREF_SSL_TRUST_ALL + postfix, false),
			prefs.getString(KEY_PREF_SSL_TRUST_KEY + postfix, null),
			prefFolder,
    		prefs.getBoolean(KEY_PREF_AUTH + postfix, false),
    		prefs.getString(KEY_PREF_USER + postfix, null),
    		prefs.getString(KEY_PREF_PASS + postfix, null),
    		OS.fromCode(prefs.getString(KEY_PREF_OS + postfix, "type_windows")),
    		prefs.getString(KEY_PREF_DOWNLOADDIR + postfix, null),
    		prefs.getString(KEY_PREF_FTPURL + postfix, null),
    		prefs.getString(KEY_PREF_PASS + postfix, null),
    		prefTimeoutI,
			prefs.getBoolean(KEY_PREF_ALARMFINISHED + postfix, true),
			prefs.getBoolean(KEY_PREF_ALARMNEW + postfix, false),
    		postfix,
    		false);

    }

    private static SiteSettings readSiteSettings(SharedPreferences prefs, String postfix) {

    	// Read saved preferences
    	String prefName = prefs.getString(KEY_PREF_WEBSITE + postfix, null);
    	String prefUrl = prefs.getString(KEY_PREF_WEBURL + postfix, null);
            	
    	// Return site settings
    	return new SiteSettings(postfix, prefName, prefUrl);

    }

    public static RssFeedSettings readRssFeedSettings(SharedPreferences prefs, String postfix) {

    	// Read saved preferences
    	String prefName = prefs.getString(KEY_PREF_RSSNAME + postfix, null);
    	String prefUrl = prefs.getString(KEY_PREF_RSSURL + postfix, null);
    	boolean prefNeedsAuth = prefs.getBoolean(KEY_PREF_RSSAUTH + postfix, false);
    	String prefLastNew = prefs.getString(KEY_PREF_RSSLASTNEW + postfix, null);
            	
    	// Return rss feed settings
    	return new RssFeedSettings(postfix, prefName, prefUrl, prefNeedsAuth, prefLastNew);

    }

    /**
     * Read the global settings for searches
     * @param prefs The preferences object to retrieve the settings from
     * @return The current search settings
     */
    public static SearchSettings readSearchSettings(SharedPreferences prefs) {

    	// Read saved preferences
        String prefNumResults = prefs.getString(KEY_PREF_NUMRESULTS, "25");
        String prefSort = prefs.getString(KEY_PREF_SORT, KEY_PREF_SEEDS);

        // Return search settings
        return new SearchSettings(Integer.parseInt(prefNumResults), !prefSort.equals(KEY_PREF_COMBINED));

    }
    
    /**
     * Read global the interface settings
     * @param prefs The preferences object to retrieve the settings from
     * @return The current interface settings
     */
    public static InterfaceSettings readInterfaceSettings(SharedPreferences prefs) {

    	// Read saved preferences
        String prefUIRefresh = prefs.getString(KEY_PREF_UIREFRESH, "-1");
    	
        // Return interface settings
        return new InterfaceSettings(
        		prefs.getBoolean(KEY_PREF_SWIPELABELS, false),
        		Integer.parseInt(prefUIRefresh), 
        		prefs.getBoolean(KEY_PREF_ONLYDL, false),
        		prefs.getBoolean(KEY_PREF_HIDEREFRESH, false),
        		prefs.getBoolean(KEY_PREF_ASKREMOVE, true),
        		prefs.getBoolean(KEY_PREF_ENABLEADS, true));
        
    }

    /**
     * Read the global alarm settings
     * @param prefs The preferences object to retrieve the settings from
     * @return The current alarm settings
     */
    public static AlarmSettings readAlarmSettings(SharedPreferences prefs) {

    	// Read saved preferences
        String prefAlarmInt = prefs.getString(KEY_PREF_ALARMINT, "600000");
        
        // Return alarm settings
        return new AlarmSettings(
        		prefs.getBoolean(KEY_PREF_ENABLEALARM, false), 
        		Integer.parseInt(prefAlarmInt),
        		prefs.getBoolean(KEY_PREF_CHECKRSSFEEDS, false),
        		prefs.getBoolean(KEY_PREF_ALARMPLAYSOUND, false),
        		prefs.getString(KEY_PREF_ALARMSOUNDURI, null),
        		prefs.getBoolean(KEY_PREF_ALARMVIBRATE, false),
        		prefs.getBoolean(KEY_PREF_ADWNOTIFY, false),
        		prefs.getBoolean(KEY_PREF_ADWONLYDL, false));
    }

    /**
     * Retrieve a structure containing the stats of the last alarm service update
     * @param prefs The preferences object to retrieve the stats from
     * @return For each daemon (identified by code) a list of torrent ids with an indication whether they were finished
     */
    public static Map<String, ArrayList<Pair<String, Boolean>>> readLastAlarmStatsUpdate(SharedPreferences prefs) {
    	
    	// Get the last update stats from the user preferences
    	String lastUpdate = prefs.getString(KEY_PREF_LASTUPDATE, null);
    	if (lastUpdate == null || lastUpdate.equals("")) {
    		return null;
    	}
    	
    	// Explode this list of torrent stats
    	try {
        	
    		HashMap<String, ArrayList<Pair<String, Boolean>>> last = new HashMap<String, ArrayList<Pair<String, Boolean>>>();
	    	if (!lastUpdate.equals("")) {
	    		String[] daemons = lastUpdate.split("\\|");
	    		for (String daemon : daemons) {
		    		
		    		if (!daemon.equals("")) {
			    		String[] daemonData = daemon.split(";");
			    		String daemonCode = "";
			    		if (daemonData.length != 0) {
			    			daemonCode = daemonData[0];
			    		}
			    		ArrayList<Pair<String, Boolean>> torentList = new ArrayList<Pair<String, Boolean>>();
			    		if (daemonData.length > 1) {
							String[] torrents = daemonData[1].split(":");
							for (String torrent : torrents) {
								if (!torrent.equals("")) {
									
					    			// Add this torrent
					    			String[] stats = torrent.split(",");
					    			torentList.add(new Pair<String, Boolean>(stats[0], Boolean.parseBoolean(stats[1])));
								
								}
				    		}
			    		}
			
						// Add this torrent (stats)
						last.put(daemonCode, torentList);
		    		}
		    		
		    	}
			}
	    	
	    	return last;
	    	
    	} catch (Exception e) {
    		// A malformed string was present in the user preferences
    		return null;
    	}
    	    	
    }
    
    /**
     * Store the torrent stats from a recent alarm service update
     * @param prefs The preferences object to retrieve the stats from
     * @param thisUpdate The newly retrieved list of running torrents on the server
     */
    public static void storeLastAlarmStatsUpdate(SharedPreferences prefs, HashMap<String, ArrayList<Pair<String, Boolean>>> thisUpdate) {
    	
    	StringBuilder lastUpdate = new StringBuilder();
    	
    	// For each daemon, store its code and torrents list
    	int id = 0;
    	for (Entry<String, ArrayList<Pair<String, Boolean>>> daemon : thisUpdate.entrySet()) {
    		if (id > 0) {
    			lastUpdate.append("|");
    		}
    		lastUpdate.append(daemon.getKey());
    		lastUpdate.append(";");
    		int it = 0;
    		for (Pair<String, Boolean> torrent : daemon.getValue()) {
    			if (it > 0) {
    				lastUpdate.append(":");
    			}
    			lastUpdate.append(torrent.first);
        		lastUpdate.append(",");
    			lastUpdate.append(torrent.second.toString());
    			it++;
    		}
    		id++;
    	}
    	
    	Editor editor = prefs.edit();
    	editor.putString(KEY_PREF_LASTUPDATE, lastUpdate.toString());
    	editor.commit();
    }

    /**
     * Reads the list of torrents that are queued to be added later and clears the queue immediately
     * @param prefs The preferences object to retrieve the stats from
     * @return The list of torrent uris as strings
     */
    public static List<Pair<String, String>> readAndClearTorrentAddQueue(SharedPreferences prefs) {
    	
    	// Get the list or torrents stored to add later from the user preferences
    	String queue = prefs.getString(KEY_PREF_QUEUEDTOADD, "");
    	
    	// Clear queue
    	if (!queue.equals("")) {
			Editor editor = prefs.edit();
			editor.putString(KEY_PREF_QUEUEDTOADD, "");
			editor.commit();
    	}
		
		// Parse and return as list
    	try {
	    		
			ArrayList<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
	    	if (queue.equals("")) {
	    		return list;
	    	}
	    	for (String torrent : queue.split("\\|")) {
	    		String[] torrentData = torrent.split(";");
	    		list.add(new Pair<String, String>(torrentData[0], torrentData[1]));
	    	}
	    	return list;
		
		} catch (Exception e) {
			// A malformed string was present in the user preferences
			return new ArrayList<Pair<String, String>>();
		}
		
    }
    

    /**
     * Add a torrent uri to the list of torrents to add later
     * @param prefs The preferences object to retrieve the stats from
     * @param daemonStringId The string ID for the daemon to add the torrent to
     * @param torrentToStore The uri of the torrent to add as a string
     */
    public static void addToTorrentAddQueue(SharedPreferences prefs, String daemonStringId, String torrentToStore) {
    	addToTorrentAddQueue(prefs, daemonStringId + ";" + torrentToStore);		
	}

    /**
     * Add a torrent uri to the list of torrents to add later
     * @param prefs The preferences object to retrieve the stats from
     * @param daemonStringId The string ID for the daemon to add the torrent to
     * @param torrentData ;-combined daemon id strings and torrent uris
     */
    public static void addToTorrentAddQueue(SharedPreferences prefs, String torrentData) {
	    
    	// Add the given torrent uri to the existing queue
    	String queue = prefs.getString(KEY_PREF_QUEUEDTOADD, "");
    	if (!queue.equals("")) {
    		queue += "|";
    	}
    	queue += torrentData;
    	
    	// Store it
		Editor editor = prefs.edit();
		editor.putString(KEY_PREF_QUEUEDTOADD, queue);
		editor.commit();
		
	}

    /**
     * Indicates whether this torrent uri points to a local file
     * @param torrentUri The raw torrent uri string
     * @return False it it seems like a web uri (starting with http or magnet); true otherwise
     */
    public static boolean isQueuedTorrentToAddALocalFile(String torrentUri) {
    	return torrentUri != null && !(torrentUri.startsWith(HttpHelper.SCHEME_HTTP) || torrentUri.startsWith(HttpHelper.SCHEME_MAGNET));
    }

    /**
     * Indicates whether this torrent uri points to a magnet url
     * @param torrentUri The raw torrent uri string
     * @return True it it seems like a magnet link (starting with magnet); true otherwise
     */
    public static boolean isQueuedTorrentToAddAMagnetUrl(String torrentUri) {
    	return torrentUri != null && torrentUri.startsWith(HttpHelper.SCHEME_MAGNET);
    }
    
    public static CharSequence parseArrayEntryFromValue(Context context, int entryArray, int valueArray, String findValue) {
    	
    	CharSequence[] values = context.getResources().getTextArray(valueArray);
    	CharSequence[] entries = context.getResources().getTextArray(entryArray);
    	
    	// Check array length
    	if (values.length != entries.length) {
    		throw new InvalidParameterException("Arrays need to be of equal length.");
    	}
    	
    	int i = 0;
    	for (CharSequence value : values) {
    		if (value.equals(findValue)) {
    			return entries[i]; 
    		}
    		i++;
    	}
    	// Not found
    	return null;
    	
    }

    /**
     * Read the global settings for searches
     * @param prefs The preferences object to retrieve the settings from
     * @param widgetId The id of the widget for which to get the settings
     * @return The current search settings
     */
    public static WidgetSettings readWidgetSettings(SharedPreferences prefs, int widgetID, List<DaemonSettings> allDaemons) {

    	// Read and parse daemon
        String prefDaemon = prefs.getString(KEY_WIDGET_DAEMON + widgetID, null);
        DaemonSettings daemonSettings;
    	int daemonId = (prefDaemon == null || prefDaemon == ""? 0: Integer.parseInt(prefDaemon));
        if (allDaemons == null || allDaemons.size() == 0 || daemonId < 0 || daemonId > allDaemons.size()) {
        	daemonSettings = null;
        } else {
        	daemonSettings = allDaemons.get(daemonId);
        }
        
        // Read and parse layout
        String prefLayout = prefs.getString(KEY_WIDGET_LAYOUT + widgetID, "style_black");
        int layoutId = R.layout.appwidget_black;
        if (prefLayout.equals("style_small")) {
        	layoutId = R.layout.appwidget_small;
        } else if (prefLayout.equals("style_15")) {
            layoutId = R.layout.appwidget_15;
        } else if (prefLayout.equals("style_16")) {
            layoutId = R.layout.appwidget_16;
        } else if (prefLayout.equals("style_qsb")) {
            layoutId = R.layout.appwidget_qsb;
        } else if (prefLayout.equals("style_transparent")) {
            layoutId = R.layout.appwidget_transparent;
        }

        // Return widget settings
        return new WidgetSettings(
        		widgetID,
        		daemonSettings, 
        		readWidgetIntervalSettin(prefs, widgetID),
        		layoutId);

    }

    /**
     * Returns only the refresh interval setting for a certain widget
     * @param prefs The preferences object to retrieve the settings from
     * @param widgetId The id of the widget for which to get the setting
     * @return The set refresh interval in seconds
     */
	public static int readWidgetIntervalSettin(SharedPreferences prefs, int widgetId) {
		return Integer.parseInt(prefs.getString(KEY_WIDGET_REFRESH + widgetId, "86400"));
	}

	/**
	 * Stores a new refresh interval value to the user settings. Don't 
	 * forget to call readInterfaceSettings(prefs) to update these.
	 * @param prefs The preferences object to retrieve the settings from
	 * @param value The new interval as String (which is in R.array.pref_uirefresh_values)
	 */
	public static void storeNewRefreshInterval(SharedPreferences prefs, String value) {
		Editor editor = prefs.edit();
		editor.putString(KEY_PREF_UIREFRESH, value);
		editor.commit();
	}
    
}
