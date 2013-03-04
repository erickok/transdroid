package org.transdroid.lite.app.settings;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Singleton object to access all application settings, including stored servers, web search sites and RSS feeds.
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class ApplicationSettings {

	@RootContext
	protected Context context;
	private SharedPreferences prefs;

	protected ApplicationSettings(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Returns all available user-configured servers
	 * @return A list of all stored server settings objects
	 */
	public List<ServerSetting> getServerSettings() {
		List<ServerSetting> servers = new ArrayList<ServerSetting>();
		for (int i = 0; i <= getMaxServer(); i++) {
			servers.add(getServerSetting(i));
		}
		return servers;
	}

	/**
	 * Returns the order number/identifying key of the last server
	 * @return The zero-based order number (index) of the last stored server settings
	 */
	public int getMaxServer() {
		for (int i = 0; true; i++) {
			if (prefs.getString("server_type_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified server settings for a specific server
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The server settings object, loaded from shared preferences
	 */
	public ServerSetting getServerSetting(int order) {
		return new ServerSetting(order, prefs.getString("server_name_" + order, null), Daemon.fromCode(prefs.getString(
				"server_type_" + order, null)), prefs.getString("server_address_" + order, null), prefs.getString(
				"server_localaddress_" + order, null), prefs.getString("server_localnetwork_" + order, null),
				prefs.getInt("server_port_" + order, -1), prefs.getBoolean("server_sslenabled_" + order, false),
				prefs.getBoolean("server_ssltrustall_" + order, false), prefs.getString("server_ssltrustkey_" + order,
						null), prefs.getString("server_folder_" + order, null), prefs.getBoolean("server_useauth_"
						+ order, true), prefs.getString("server_user_" + order, null), prefs.getString("server_pass_"
						+ order, null), prefs.getString("server_extrapass_" + order, null), OS.fromCode(prefs
						.getString("server_os_" + order, null)), prefs.getString("server_downloaddir_" + order, null),
				prefs.getString("server_ftpurl_" + order, null), prefs.getString("server_ftppass_" + order, null),
				prefs.getInt("server_timeout_" + order, -1), prefs.getBoolean("server_alarmfinished_" + order, true),
				prefs.getBoolean("server_alarmnew_" + order, false), false);
	}

	/**
	 * Returns all available user-configured web-based (as opped to in-app) search sites
	 * @return A list of all stored web search site settings objects
	 */
	public List<WebsearchSetting> getWebsearchSettings() {
		List<WebsearchSetting> websearches = new ArrayList<WebsearchSetting>();
		for (int i = 0; i <= getMaxWebsearch(); i++) {
			websearches.add(getWebsearchSetting(i));
		}
		return websearches;
	}

	/**
	 * Returns the order number/identifying key of the last web search site
	 * @return The zero-based order number (index) of the last stored web search site
	 */
	public int getMaxWebsearch() {
		for (int i = 0; true; i++) {
			if (prefs.getString("websearch_url_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified web-based search site setting for a specific site
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The web search site settings object, loaded from shared preferences
	 */
	public WebsearchSetting getWebsearchSetting(int order) {
		return new WebsearchSetting(order, prefs.getString("websearch_name_" + order, null), prefs.getString(
				"websearch_url_" + order, null));
	}

	/**
	 * Returns all available user-configured RSS feeds
	 * @return A list of all stored RSS feed settings objects
	 */
	public List<RssfeedSetting> getRssfeedSettings() {
		List<RssfeedSetting> rssfeeds = new ArrayList<RssfeedSetting>();
		for (int i = 0; i <= getMaxRssfeed(); i++) {
			rssfeeds.add(getRssfeedSetting(i));
		}
		return rssfeeds;
	}

	/**
	 * Returns the order number/identifying key of the last stored RSS feed
	 * @return The zero-based order number (index) of the last stored RSS feed
	 */
	public int getMaxRssfeed() {
		for (int i = 0; true; i++) {
			if (prefs.getString("rssfeed_feedurl_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified RSS feed setting for a specific feed
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The RSS feed settings object, loaded from shared preferences
	 */
	public RssfeedSetting getRssfeedSetting(int order) {
		return new RssfeedSetting(order, prefs.getString("rssfeed_name_" + order, null), prefs.getString(
				"rssfeed_feedurl_" + order, null), prefs.getBoolean("rssfeed_reqauth_" + order, false));
	}

}
