package org.transdroid.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.daemon.util.HttpHelper;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;

public class ImportExport {

	public static final String DEFAULT_SETTINGS_DIR = Environment.getExternalStorageDirectory().toString() + "/Transdroid";
	public static final String DEFAULT_SETTINGS_FILENAME = "/settings.json";
	public static final File DEFAULT_SETTINGS_FILE = new File(DEFAULT_SETTINGS_DIR + DEFAULT_SETTINGS_FILENAME);
	
	/**
	 * Synchronously writes the user preferences on servers, web searches and 
	 * RSS feeds to a file in JSON format.
	 * @param settingsFile The file to write the settings to
	 * @throws FileNotFoundException Thrown when the settings file doesn't exist
	 * @throws JSONException Thrown when the file did not contain valid JSON content
	 */
	public static void importSettings(SharedPreferences prefs, File settingsFile) throws FileNotFoundException, JSONException {
		
		Editor editor = prefs.edit();

		// Read the settings file
		String raw = HttpHelper.convertStreamToString(new FileInputStream(settingsFile));
		JSONObject json = new JSONObject(raw);
		
		if (json.has("servers")) {
			
			// Clean old servers
			int j = 0;
			String postfixj = "";
			while (prefs.contains(Preferences.KEY_PREF_ADDRESS + postfixj)) {
				editor.remove(Preferences.KEY_PREF_NAME + postfixj);
				editor.remove(Preferences.KEY_PREF_DAEMON + postfixj);
				editor.remove(Preferences.KEY_PREF_ADDRESS + postfixj);
				editor.remove(Preferences.KEY_PREF_PORT + postfixj);
				editor.remove(Preferences.KEY_PREF_AUTH + postfixj);
				editor.remove(Preferences.KEY_PREF_USER + postfixj);
				editor.remove(Preferences.KEY_PREF_PASS + postfixj);
				editor.remove(Preferences.KEY_PREF_EXTRAPASS + postfixj);
				editor.remove(Preferences.KEY_PREF_FOLDER + postfixj);
				editor.remove(Preferences.KEY_PREF_ALARMFINISHED + postfixj);
				editor.remove(Preferences.KEY_PREF_ALARMNEW + postfixj);
				editor.remove(Preferences.KEY_PREF_OS + postfixj);
				editor.remove(Preferences.KEY_PREF_DOWNLOADDIR + postfixj);
				editor.remove(Preferences.KEY_PREF_FTPURL + postfixj);
				editor.remove(Preferences.KEY_PREF_SSL + postfixj);
				editor.remove(Preferences.KEY_PREF_SSL_TRUST_ALL + postfixj);
				editor.remove(Preferences.KEY_PREF_SSL_TRUST_KEY + postfixj);
				
				j++;
				postfixj = Integer.toString(j);
			}

			// Import servers
			JSONArray servers = json.getJSONArray("servers");
			for (int i = 0; i < servers.length(); i++) {
				JSONObject server = servers.getJSONObject(i);
				String postfix = (i == 0? "": Integer.toString(i));
	
				if (server.has("name")) editor.putString(Preferences.KEY_PREF_NAME + postfix, server.getString("name"));
				if (server.has("type")) editor.putString(Preferences.KEY_PREF_DAEMON + postfix, server.getString("type"));
				if (server.has("host")) editor.putString(Preferences.KEY_PREF_ADDRESS + postfix, server.getString("host"));
				if (server.has("port")) editor.putString(Preferences.KEY_PREF_PORT + postfix, server.getString("port"));
				if (server.has("use_auth")) editor.putBoolean(Preferences.KEY_PREF_AUTH + postfix, server.getBoolean("use_auth"));
				if (server.has("username")) editor.putString(Preferences.KEY_PREF_USER + postfix, server.getString("username"));
				if (server.has("password")) editor.putString(Preferences.KEY_PREF_PASS + postfix, server.getString("password"));
				if (server.has("extra_password")) editor.putString(Preferences.KEY_PREF_EXTRAPASS + postfix, server.getString("extra_password"));
				if (server.has("folder")) editor.putString(Preferences.KEY_PREF_FOLDER + postfix, server.getString("folder"));
				if (server.has("download_alarm")) editor.putBoolean(Preferences.KEY_PREF_ALARMFINISHED + postfix, server.getBoolean("download_alarm"));
				if (server.has("new_torrent_alarm")) editor.putBoolean(Preferences.KEY_PREF_ALARMNEW + postfix, server.getBoolean("new_torrent_alarm"));
				if (server.has("os_type")) editor.putString(Preferences.KEY_PREF_OS + postfix, server.getString("os_type"));
				if (server.has("downloads_dir")) editor.putString(Preferences.KEY_PREF_DOWNLOADDIR + postfix, server.getString("downloads_dir"));
				if (server.has("base_ftp_url")) editor.putString(Preferences.KEY_PREF_FTPURL + postfix, server.getString("base_ftp_url"));
				if (server.has("ssl")) editor.putBoolean(Preferences.KEY_PREF_SSL + postfix, server.getBoolean("ssl"));
				if (server.has("ssl_accept_all")) editor.putBoolean(Preferences.KEY_PREF_SSL_TRUST_ALL + postfix, server.getBoolean("ssl_accept_all"));
				if (server.has("ssl_trust_key")) editor.putString(Preferences.KEY_PREF_SSL_TRUST_KEY + postfix, server.getString("ssl_trust_key"));
				
			}
		}

		if (json.has("websites")) {

			// Clean old web search sites
			int j = 0;
			String postfixj = "0";
			while (prefs.contains(Preferences.KEY_PREF_WEBURL + postfixj)) {
				editor.remove(Preferences.KEY_PREF_WEBSITE + postfixj);
				editor.remove(Preferences.KEY_PREF_WEBURL + postfixj);
				
				j++;
				postfixj = Integer.toString(j);
			}

			// Import web search sites
			JSONArray sites = json.getJSONArray("websites");
			for (int i = 0; i < sites.length(); i++) {
				JSONObject site = sites.getJSONObject(i);
				String postfix = Integer.toString(i);
	
				if (site.has("name")) editor.putString(Preferences.KEY_PREF_WEBSITE + postfix, site.getString("name"));
				if (site.has("url")) editor.putString(Preferences.KEY_PREF_WEBURL + postfix, site.getString("url"));
					
			}
		}

		if (json.has("rssfeeds")) {

			// Clean old web search sites
			int j = 0;
			String postfixj = "0";
			while (prefs.contains(Preferences.KEY_PREF_RSSURL + postfixj)) {
				editor.remove(Preferences.KEY_PREF_RSSNAME + postfixj);
				editor.remove(Preferences.KEY_PREF_RSSURL + postfixj);
				editor.remove(Preferences.KEY_PREF_RSSAUTH + postfixj);
				editor.remove(Preferences.KEY_PREF_RSSLASTNEW + postfixj);
				
				j++;
				postfixj = Integer.toString(j);
			}

			// Import RSS feeds
			JSONArray feeds = json.getJSONArray("rssfeeds");
			for (int i = 0; i < feeds.length(); i++) {
				JSONObject feed = feeds.getJSONObject(i);
				String postfix = Integer.toString(i);
	
				if (feed.has("name")) editor.putString(Preferences.KEY_PREF_RSSNAME + postfix, feed.getString("name"));
				if (feed.has("url")) editor.putString(Preferences.KEY_PREF_RSSURL + postfix, feed.getString("url"));
				if (feed.has("needs_auth")) editor.putBoolean(Preferences.KEY_PREF_RSSAUTH + postfix, feed.getBoolean("needs_auth"));
				if (feed.has("last_seen")) editor.putString(Preferences.KEY_PREF_RSSLASTNEW + postfix, feed.getString("last_seen"));
					
			}
		}

		// Search settings
		editor.putString(Preferences.KEY_PREF_NUMRESULTS, json.getString("search_num_results"));
		editor.putString(Preferences.KEY_PREF_SORT, json.getString("search_sort_by"));
		
		// Interface settings
		editor.putString(Preferences.KEY_PREF_UIREFRESH, json.getString("ui_refresh_interval"));
		editor.putBoolean(Preferences.KEY_PREF_SWIPELABELS, json.getBoolean("ui_swipe_labels"));
		editor.putBoolean(Preferences.KEY_PREF_ONLYDL, json.getBoolean("ui_only_show_transferring"));
		editor.putBoolean(Preferences.KEY_PREF_HIDEREFRESH, json.getBoolean("ui_hide_refresh"));
		editor.putBoolean(Preferences.KEY_PREF_ENABLEADS, json.getBoolean("ui_enable_ads"));
		editor.putBoolean(Preferences.KEY_PREF_ASKREMOVE, json.getBoolean("ui_ask_before_remove"));

		// Alarm service settings
		editor.putBoolean(Preferences.KEY_PREF_ENABLEALARM, json.getBoolean("alarm_enabled"));
		editor.putString(Preferences.KEY_PREF_ALARMINT, json.getString("alarm_interval"));
		editor.putBoolean(Preferences.KEY_PREF_CHECKRSSFEEDS, json.getBoolean("alarm_check_rss_feeds"));
		editor.putBoolean(Preferences.KEY_PREF_ALARMPLAYSOUND, json.getBoolean("alarm_play_sound"));
		if (json.has("alarm_sound_uri")) editor.putString(Preferences.KEY_PREF_ALARMSOUNDURI, json.getString("alarm_sound_uri"));
		editor.putBoolean(Preferences.KEY_PREF_ALARMVIBRATE, json.getBoolean("alarm_vibrate"));
		
		editor.commit();
		
	}
	
	public static void exportSettings(SharedPreferences prefs, File settingsFile) throws JSONException, IOException {
		
		// Create a single JSON object with all settings
		JSONObject json = new JSONObject();

		// Add servers
		JSONArray servers = new JSONArray();
		int i = 0;
		String postfixi = "";
		while (prefs.contains(Preferences.KEY_PREF_ADDRESS + postfixi)) {
			
			JSONObject server = new JSONObject();
			server.put("name", prefs.getString(Preferences.KEY_PREF_NAME + postfixi, null));
			server.put("type", prefs.getString(Preferences.KEY_PREF_DAEMON + postfixi, null));
			server.put("host", prefs.getString(Preferences.KEY_PREF_ADDRESS + postfixi, null));
			server.put("port", prefs.getString(Preferences.KEY_PREF_PORT + postfixi, null));
			server.put("use_auth", prefs.getBoolean(Preferences.KEY_PREF_AUTH + postfixi, false));
			server.put("username", prefs.getString(Preferences.KEY_PREF_USER + postfixi, null));
			server.put("password", prefs.getString(Preferences.KEY_PREF_PASS + postfixi, null));
			server.put("extra_password", prefs.getString(Preferences.KEY_PREF_EXTRAPASS + postfixi, null));
			server.put("folder", prefs.getString(Preferences.KEY_PREF_FOLDER + postfixi, null));
			server.put("download_alarm", prefs.getBoolean(Preferences.KEY_PREF_ALARMFINISHED + postfixi, false));
			server.put("new_torrent_alarm", prefs.getBoolean(Preferences.KEY_PREF_ALARMNEW + postfixi, false));
			server.put("os_type", prefs.getString(Preferences.KEY_PREF_OS + postfixi, null));
			server.put("downloads_dir", prefs.getString(Preferences.KEY_PREF_DOWNLOADDIR + postfixi, null));
			server.put("base_ftp_url", prefs.getString(Preferences.KEY_PREF_FTPURL + postfixi, null));
			server.put("ssl", prefs.getBoolean(Preferences.KEY_PREF_SSL + postfixi, false));
			server.put("ssl_accept_all", prefs.getBoolean(Preferences.KEY_PREF_SSL_TRUST_ALL + postfixi, false));
			server.put("ssl_trust_key", prefs.getString(Preferences.KEY_PREF_SSL_TRUST_KEY + postfixi, null));
			
			servers.put(server);
			i++;
			postfixi = Integer.toString(i);
		}
		json.put("servers", servers);

		// Add web search sites
		JSONArray sites = new JSONArray();
		int j = 0;
		String postfixj = "0";
		while (prefs.contains(Preferences.KEY_PREF_WEBURL + postfixj)) {
			
			JSONObject site = new JSONObject();
			site.put("name", prefs.getString(Preferences.KEY_PREF_WEBSITE + postfixj, null));
			site.put("url", prefs.getString(Preferences.KEY_PREF_WEBURL + postfixj, null));

			sites.put(site);
			j++;
			postfixj = Integer.toString(j);
		}
		json.put("websites", sites);

		// Add RSS feeds
		JSONArray feeds = new JSONArray();
		int k = 0;
		String postfixk = "0";
		while (prefs.contains(Preferences.KEY_PREF_RSSURL + postfixk)) {
			
			JSONObject feed = new JSONObject();
			feed.put("name", prefs.getString(Preferences.KEY_PREF_RSSNAME + postfixk, null));
			feed.put("url", prefs.getString(Preferences.KEY_PREF_RSSURL + postfixk, null));
			feed.put("needs_auth", prefs.getBoolean(Preferences.KEY_PREF_RSSAUTH + postfixk, false));
			feed.put("last_seen", prefs.getString(Preferences.KEY_PREF_RSSLASTNEW + postfixk, null));

			feeds.put(feed);
			k++;
			postfixk = Integer.toString(k);
		}
		json.put("rssfeeds", feeds);
		
		// Search settings
		json.put("search_num_results", prefs.getString(Preferences.KEY_PREF_NUMRESULTS, "25"));
		json.put("search_sort_by", prefs.getString(Preferences.KEY_PREF_SORT, Preferences.KEY_PREF_SEEDS));
		
		// Interface settings
		json.put("ui_refresh_interval", prefs.getString(Preferences.KEY_PREF_UIREFRESH, "-1"));
		json.put("ui_swipe_labels", prefs.getBoolean(Preferences.KEY_PREF_SWIPELABELS, false));
		json.put("ui_only_show_transferring", prefs.getBoolean(Preferences.KEY_PREF_ONLYDL, false));
		json.put("ui_hide_refresh", prefs.getBoolean(Preferences.KEY_PREF_HIDEREFRESH, false));
		json.put("ui_ask_before_remove", prefs.getBoolean(Preferences.KEY_PREF_ASKREMOVE, true));
		json.put("ui_enable_ads", prefs.getBoolean(Preferences.KEY_PREF_ENABLEADS, true));
		
		// Alarm service settings
		json.put("alarm_enabled", prefs.getBoolean(Preferences.KEY_PREF_ENABLEALARM, false));
		json.put("alarm_interval", prefs.getString(Preferences.KEY_PREF_ALARMINT, "600000"));
		json.put("alarm_check_rss_feeds", prefs.getBoolean(Preferences.KEY_PREF_CHECKRSSFEEDS, false));
		json.put("alarm_play_sound", prefs.getBoolean(Preferences.KEY_PREF_ALARMPLAYSOUND, false));
		json.put("alarm_sound_uri", prefs.getString(Preferences.KEY_PREF_ALARMSOUNDURI, null));
		json.put("alarm_vibrate", prefs.getBoolean(Preferences.KEY_PREF_ALARMVIBRATE, false));
		
		// Serialize the JSON object to a file
		if (settingsFile.exists()) {
			settingsFile.delete();
		}
		settingsFile.getParentFile().mkdirs();
		settingsFile.createNewFile();
		FileWriter writer = new FileWriter(settingsFile);
		writer.write(json.toString(2));
		writer.flush();
		writer.close();
		
	}

}
