package org.transdroid.core.app.settings;

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
	 * Returns the user-specified server settings for a specific server. WARNING: This method does not check if the
	 * settings actually exist and may rely on empty defaults if called not a non-existing server.
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The server settings object, loaded from shared preferences
	 */
	public ServerSetting getServerSetting(int order) {
		return new ServerSetting(order, prefs.getString("server_name_" + order, null), Daemon.fromCode(prefs.getString(
				"server_type_" + order, null)), prefs.getString("server_address_" + order, null), prefs.getString(
				"server_localaddress_" + order, null), prefs.getString("server_localnetwork_" + order, null),
				Integer.parseInt(prefs.getString("server_port_" + order, "-1")), prefs.getBoolean("server_sslenabled_"
						+ order, false), prefs.getBoolean("server_ssltrustall_" + order, false), prefs.getString(
						"server_ssltrustkey_" + order, null), prefs.getString("server_folder_" + order, null),
				prefs.getBoolean("server_useauth_" + order, true), prefs.getString("server_user_" + order, null),
				prefs.getString("server_pass_" + order, null), prefs.getString("server_extrapass_" + order, null),
				OS.fromCode(prefs.getString("server_os_" + order, null)), prefs.getString(
						"server_downloaddir_" + order, null), prefs.getString("server_ftpurl_" + order, null),
				prefs.getString("server_ftppass_" + order, null), Integer.parseInt(prefs.getString("server_timeout_"
						+ order, "8")), prefs.getBoolean("server_alarmfinished_" + order, true), prefs.getBoolean(
						"server_alarmnew_" + order, false), false);
	}

	/**
	 * Returns the settings of the server that was last used by the user. As opposed to getLastUsedServerKey(int), this
	 * method checks whether a server was already registered as being last used and check whether the server still
	 * exists. It returns the first server if that fails. If no servers are configured, null is returned.
	 * @return A server settings object of the last used server (or, if not known, the first server), or null if no
	 *         servers exist
	 */
	public ServerSetting getLastUsedServer() {
		int max = getMaxServer(); // Zero-based index, so with max == 0 there is 1 server
		if (max < 0) {
			// No servers configured
			return null;
		}
		int last = getLastUsedServerKey();
		if (last < 0 || last > max) {
			// Last server was never set or no longer exists
			return getServerSetting(0);
		}
		return getServerSetting(last);
	}

	/**
	 * Returns the order number/unique key of the server that the used last used; use with getServerSettings(int) or
	 * call getLastUsedServer directly. WARNING: the returned integer may no longer refer to a known server settings
	 * object: check the bounds.
	 * @return An integer indicating the order number/key or the last used server, or -1 if it was not set
	 */
	public int getLastUsedServerKey() {
		return prefs.getInt("system_lastusedserver", -1);
	}

	/**
	 * Registers some server as being the last used by the user
	 * @param server The settings of the server that the user last used
	 */
	public void setLastUsedServer(ServerSetting server) {
		setLastUsedServerKey(server.getOrder());
	}

	/**
	 * Registers the order number/unique key of some server as being last used by the user
	 * @param order The key identifying the specific server
	 */
	public void setLastUsedServerKey(int order) {
		prefs.edit().putInt("system_lastusedserver", order).commit();
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
