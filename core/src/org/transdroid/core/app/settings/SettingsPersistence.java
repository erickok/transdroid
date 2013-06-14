package org.transdroid.core.app.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.daemon.util.HttpHelper;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;

@EBean(scope = Scope.Singleton)
public class SettingsPersistence {

	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected SystemSettings systemSettings;

	public static final String DEFAULT_SETTINGS_DIR = Environment.getExternalStorageDirectory().toString()
			+ "/Transdroid";
	public static final String DEFAULT_SETTINGS_FILENAME = "/settings.json";
	public static final File DEFAULT_SETTINGS_FILE = new File(DEFAULT_SETTINGS_DIR + DEFAULT_SETTINGS_FILENAME);

	/**
	 * Synchronously reads the server, web searches, RSS feed, background service and system settings from a file in
	 * JSON format.
	 * @param settingsFile The local file to write the settings to
	 * @throws FileNotFoundException Thrown when the settings file doesn't exist or couln't be read
	 * @throws JSONException Thrown when the file did not contain valid JSON content
	 */
	public void importSettings(SharedPreferences prefs, File settingsFile) throws FileNotFoundException,
			JSONException {

		Editor editor = prefs.edit();

		// Read the settings file
		String raw = HttpHelper.ConvertStreamToString(new FileInputStream(settingsFile));
		JSONObject json = new JSONObject(raw);

		// Import servers
		if (json.has("servers")) {
			JSONArray servers = json.getJSONArray("servers");
			for (int i = 0; i < servers.length(); i++) {
				JSONObject server = servers.getJSONObject(i);
				String postfix = Integer.toString(applicationSettings.getMaxServer() + 1 + i);

				if (server.has("name"))
					editor.putString("server_name_" + postfix, server.getString("name"));
				if (server.has("type"))
					editor.putString("server_type_" + postfix, server.getString("type"));
				if (server.has("host"))
					editor.putString("server_address_" + postfix, server.getString("host"));
				if (server.has("local_network"))
					editor.putString("server_localnetwork_" + postfix, server.getString("local_network"));
				if (server.has("local_host"))
					editor.putString("server_localaddress_" + postfix, server.getString("local_host"));
				if (server.has("port"))
					editor.putString("server_port_" + postfix, server.getString("port"));
				if (server.has("ssl"))
					editor.putBoolean("server_sslenabled_" + postfix, server.getBoolean("ssl"));
				if (server.has("ssl_accept_all"))
					editor.putBoolean("server_ssltrustall_" + postfix, server.getBoolean("ssl_accept_all"));
				if (server.has("ssl_trust_key"))
					editor.putString("server_ssltrustkey_" + postfix, server.getString("ssl_trust_key"));
				if (server.has("folder"))
					editor.putString("server_folder_" + postfix, server.getString("folder"));
				if (server.has("use_auth"))
					editor.putBoolean("server_useauth_" + postfix, server.getBoolean("use_auth"));
				if (server.has("username"))
					editor.putString("server_user_" + postfix, server.getString("username"));
				if (server.has("password"))
					editor.putString("server_pass_" + postfix, server.getString("password"));
				if (server.has("extra_password"))
					editor.putString("server_extrapass_" + postfix, server.getString("extra_password"));
				if (server.has("os_type"))
					editor.putString("server_os_" + postfix, server.getString("os_type"));
				if (server.has("downloads_dir"))
					editor.putString("server_downloaddir_" + postfix, server.getString("downloads_dir"));
				if (server.has("base_ftp_url"))
					editor.putString("server_ftpurl_" + postfix, server.getString("base_ftp_url"));
				if (server.has("ftp_password"))
					editor.putString("server_ftppass_" + postfix, server.getString("ftp_password"));
				if (server.has("server_timeout"))
					editor.putString("server_timeout_" + postfix, server.getString("server_timeout"));
				if (server.has("download_alarm"))
					editor.putBoolean("server_alarmfinished_" + postfix, server.getBoolean("download_alarm"));
				if (server.has("new_torrent_alarm"))
					editor.putBoolean("server_alarmnew_" + postfix, server.getBoolean("new_torrent_alarm"));

			}
		}

		// Import web search sites
		if (json.has("websites")) {
			JSONArray sites = json.getJSONArray("websites");
			for (int i = 0; i < sites.length(); i++) {
				JSONObject site = sites.getJSONObject(i);
				String postfix = Integer.toString(applicationSettings.getMaxWebsearch() + 1 + i);

				if (site.has("name"))
					editor.putString("websearch_name_" + postfix, site.getString("name"));
				if (site.has("url"))
					editor.putString("websearch_baseurl_" + postfix, site.getString("url"));

			}
		}

		// Import RSS feeds
		if (json.has("rssfeeds")) {
			JSONArray feeds = json.getJSONArray("rssfeeds");
			for (int i = 0; i < feeds.length(); i++) {
				JSONObject feed = feeds.getJSONObject(i);
				String postfix = Integer.toString(applicationSettings.getMaxRssfeed() + 1 + i);

				if (feed.has("name"))
					editor.putString("rssfeed_name_" + postfix, feed.getString("name"));
				if (feed.has("url"))
					editor.putString("rssfeed_url_" + postfix, feed.getString("url"));
				if (feed.has("needs_auth"))
					editor.putBoolean("rssfeed_reqauth_" + postfix, feed.getBoolean("needs_auth"));
				if (feed.has("last_seen"))
					editor.putString("rssfeed_lastnew_" + postfix, feed.getString("last_seen"));

			}
		}

		// Import background service and system settings
		if (json.has("alarm_enabled"))
			editor.putBoolean("notifications_enabled", json.getBoolean("alarm_enabled"));
		if (json.has("alarm_interval"))
			editor.putString("notifications_interval", json.getString("alarm_interval"));
		if (json.has("alarm_sound_uri"))
			editor.putString("notifications_sound", json.getString("alarm_sound_uri"));
		if (json.has("alarm_vibrate"))
			editor.putBoolean("notifications_vibrate", json.getBoolean("alarm_vibrate"));
		if (json.has("alarm_ledcolour"))
			editor.putInt("notifications_ledcolour", json.getInt("alarm_ledcolour"));
		if (json.has("alarm_adwnotifications"))
			editor.putBoolean("notifications_adwnotify", json.getBoolean("alarm_adwnotifications"));
		if (json.has("system_checkupdates"))
			editor.putBoolean("system_checkupdates", json.getBoolean("system_checkupdates"));
		if (json.has("system_usedarktheme"))
			editor.putBoolean("system_usedarktheme", json.getBoolean("system_usedarktheme"));

		editor.commit();

	}

	/**
	 * Synchronously writes the server, web searches, RSS feed, background service and system settings to a file in JSON
	 * format.
	 * @param prefs The application-global preferences object to write settings to
	 * @param settingsFile The local file to read the settings from
	 * @throws JSONException Thrown when the JSON content could not be constructed properly
	 * @throws IOException Thrown when the settings file could not be created or written to
	 */
	public void exportSettings(SharedPreferences prefs, File settingsFile) throws JSONException, IOException {

		// Create a single JSON object that will contain all settings
		JSONObject json = new JSONObject();

		// Convert server settings into JSON
		JSONArray servers = new JSONArray();
		int i = 0;
		String postfixi = "0";
		while (prefs.contains("server_type_" + postfixi)) {

			JSONObject server = new JSONObject();
			server.put("name", prefs.getString("server_name_" + postfixi, null));
			server.put("type", prefs.getString("server_type_" + postfixi, null));
			server.put("host", prefs.getString("server_address_" + postfixi, null));
			server.put("local_network", prefs.getString("server_localnetwork_" + postfixi, null));
			server.put("local_host", prefs.getString("server_localaddress_" + postfixi, null));
			server.put("port", prefs.getString("server_port_" + postfixi, null));
			server.put("ssl", prefs.getBoolean("server_sslenabled_" + postfixi, false));
			server.put("ssl_accept_all", prefs.getBoolean("server_ssltrustall_" + postfixi, false));
			server.put("ssl_trust_key", prefs.getString("server_ssltrustkey_" + postfixi, null));
			server.put("folder", prefs.getString("server_folder_" + postfixi, null));
			server.put("use_auth", prefs.getBoolean("server_useauth_" + postfixi, true));
			server.put("username", prefs.getString("server_user_" + postfixi, null));
			server.put("password", prefs.getString("server_pass_" + postfixi, null));
			server.put("extra_password", prefs.getString("server_extrapass_" + postfixi, null));
			server.put("os_type", prefs.getString("server_os_" + postfixi, null));
			server.put("downloads_dir", prefs.getString("server_downloaddir_" + postfixi, null));
			server.put("base_ftp_url", prefs.getString("server_ftpurl_" + postfixi, null));
			server.put("ftp_password", prefs.getString("server_ftppass_" + postfixi, null));
			server.put("server_timeout", prefs.getString("server_ftppass_" + postfixi, null));
			server.put("download_alarm", prefs.getBoolean("server_alarmfinished_" + postfixi, false));
			server.put("new_torrent_alarm", prefs.getBoolean("server_alarmnew_" + postfixi, false));

			servers.put(server);
			i++;
			postfixi = Integer.toString(i);
		}
		json.put("servers", servers);

		// Convert web search settings into JSON
		JSONArray sites = new JSONArray();
		int j = 0;
		String postfixj = "0";
		while (prefs.contains("websearch_baseurl_" + postfixj)) {

			JSONObject site = new JSONObject();
			site.put("name", prefs.getString("websearch_name_" + postfixj, null));
			site.put("url", prefs.getString("websearch_baseurl_" + postfixj, null));

			sites.put(site);
			j++;
			postfixj = Integer.toString(j);
		}
		json.put("websites", sites);

		// Convert RSS feed settings into JSON
		JSONArray feeds = new JSONArray();
		int k = 0;
		String postfixk = "0";
		while (prefs.contains("rssfeed_url_" + postfixk)) {

			JSONObject feed = new JSONObject();
			feed.put("name", prefs.getString("rssfeed_name_" + postfixk, null));
			feed.put("url", prefs.getString("rssfeed_url_" + postfixk, null));
			feed.put("needs_auth", prefs.getBoolean("rssfeed_reqauth_" + postfixk, false));
			feed.put("last_seen", prefs.getString("rssfeed_lastnew_" + postfixk, null));

			feeds.put(feed);
			k++;
			postfixk = Integer.toString(k);
		}
		json.put("rssfeeds", feeds);

		// Convert background service and system settings into JSON
		json.put("alarm_enabled", prefs.getBoolean("notifications_enabled", true));
		json.put("alarm_interval", prefs.getString("notifications_interval", null));
		json.put("alarm_sound_uri", prefs.getString("notifications_sound", null));
		json.put("alarm_vibrate", prefs.getBoolean("notifications_vibrate", false));
		json.put("alarm_ledcolour", prefs.getInt("notifications_ledcolour", -1));
		json.put("alarm_adwnotifications", prefs.getBoolean("notifications_adwnotify", false));
		json.put("system_checkupdates", prefs.getBoolean("system_checkupdates", true));
		json.put("system_usedarktheme", prefs.getBoolean("system_usedarktheme", false));

		// Serialise the JSON object to a file
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
