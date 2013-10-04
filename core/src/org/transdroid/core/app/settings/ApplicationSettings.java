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
package org.transdroid.core.app.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.transdroid.core.app.search.SearchHelper;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.gui.search.SearchSetting;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.TorrentsSortBy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	@Bean
	protected SearchHelper searchHelper;

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
		return Collections.unmodifiableList(servers);
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
		// @formatter:off
		Daemon type = Daemon.fromCode(prefs.getString("server_type_" + order, null));
		boolean ssl = prefs.getBoolean("server_sslenabled_" + order, false);
		
		String port = prefs.getString("server_port_" + order, "");
		if(port.equals(""))
			port = Integer.toString(Daemon.getDefaultPortNumber(type, ssl));
		
		return new ServerSetting(order, 
				prefs.getString("server_name_" + order, null), type, 
				prefs.getString("server_address_" + order, null), 
				prefs.getString("server_localaddress_" + order, null),
				prefs.getString("server_localnetwork_" + order, null), 
				Integer.parseInt(port), 
				ssl, 
				prefs.getBoolean("server_ssltrustall_" + order, false),
				prefs.getString("server_ssltrustkey_" + order, null), 
				prefs.getString("server_folder_" + order, null),
				prefs.getBoolean("server_useauth_" + order, true), 
				prefs.getString("server_user_" + order, null),
				prefs.getString("server_pass_" + order, null), 
				prefs.getString("server_extrapass_" + order, null),
				OS.fromCode(prefs.getString("server_os_" + order, "type_linux")), 
				prefs.getString("server_downloaddir_" + order, null), 
				prefs.getString("server_ftpurl_" + order, null), 
				prefs.getString("server_ftppass_" + order, null), 
				Integer.parseInt(prefs.getString("server_timeout_" + order, "8")), 
				prefs.getBoolean("server_alarmfinished_" + order, true), 
				prefs.getBoolean("server_alarmnew_" + order, false), 
				false);
		// @formatter:on
	}

	/**
	 * Removes all settings related to a configured server. Since servers are ordered, the order of the remaining
	 * servers will be updated accordingly.
	 * @param order The identifying order number/key of the settings to remove
	 */
	public void removeServerSettings(int order) {
		if (prefs.getString("server_type_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist

		// Copy all settings higher than the supplied order number to the previous spot
		Editor edit = prefs.edit();
		int max = getMaxServer();
		for (int i = order; i < max; i++) {
			edit.putString("server_name_" + i, prefs.getString("server_name_" + (i + 1), null));
			edit.putString("server_type_" + i, prefs.getString("server_type_" + (i + 1), null));
			edit.putString("server_address_" + i, prefs.getString("server_address_" + (i + 1), null));
			edit.putString("server_localaddress_" + i, prefs.getString("server_localaddress_" + (i + 1), null));
			edit.putString("server_localnetwork_" + i, prefs.getString("server_localnetwork_" + (i + 1), null));
			edit.putString("server_port_" + i, prefs.getString("server_port_" + (i + 1), null));
			edit.putBoolean("server_sslenabled_" + i, prefs.getBoolean("server_sslenabled_" + (i + 1), false));
			edit.putBoolean("server_ssltrustall_" + i, prefs.getBoolean("server_ssltrustall_" + (i + 1), false));
			edit.putString("server_ssltrustkey_" + i, prefs.getString("server_ssltrustkey_" + (i + 1), null));
			edit.putString("server_folder_" + i, prefs.getString("server_folder_" + (i + 1), null));
			edit.putBoolean("server_useauth_" + i, prefs.getBoolean("server_useauth_" + (i + 1), true));
			edit.putString("server_user_" + i, prefs.getString("server_user_" + (i + 1), null));
			edit.putString("server_pass_" + i, prefs.getString("server_pass_" + (i + 1), null));
			edit.putString("server_extrapass_" + i, prefs.getString("server_extrapass_" + (i + 1), null));
			edit.putString("server_os_" + i, prefs.getString("server_os_" + (i + 1), null));
			edit.putString("server_downloaddir_" + i, prefs.getString("server_downloaddir_" + (i + 1), null));
			edit.putString("server_ftpurl_" + i, prefs.getString("server_ftpurl_" + (i + 1), null));
			edit.putString("server_ftppass_" + i, prefs.getString("server_ftppass_" + (i + 1), null));
			edit.putString("server_timeout_" + i, prefs.getString("server_timeout_" + (i + 1), null));
			edit.putBoolean("server_alarmfinished_" + i, prefs.getBoolean("server_alarmfinished_" + (i + 1), true));
			edit.putBoolean("server_alarmfinished_" + i, prefs.getBoolean("server_alarmfinished_" + (i + 1), false));
		}

		// Remove the last settings, of which we are now sure are no longer required
		edit.remove("server_name_" + max);
		edit.remove("server_type_" + max);
		edit.remove("server_address_" + max);
		edit.remove("server_localaddress_" + max);
		edit.remove("server_localnetwork_" + max);
		edit.remove("server_port_" + max);
		edit.remove("server_sslenabled_" + max);
		edit.remove("server_ssltrustall_" + max);
		edit.remove("server_ssltrustkey_" + max);
		edit.remove("server_folder_" + max);
		edit.remove("server_useauth_" + max);
		edit.remove("server_user_" + max);
		edit.remove("server_pass_" + max);
		edit.remove("server_extrapass_" + max);
		edit.remove("server_os_" + max);
		edit.remove("server_downloaddir_" + max);
		edit.remove("server_ftpurl_" + max);
		edit.remove("server_ftppass_" + max);
		edit.remove("server_timeout_" + max);
		edit.remove("server_alarmfinished_" + max);
		edit.remove("server_alarmfinished_" + max);
		edit.commit();

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
		return Collections.unmodifiableList(websearches);
	}

	/**
	 * Returns the order number/identifying key of the last web search site
	 * @return The zero-based order number (index) of the last stored web search site
	 */
	public int getMaxWebsearch() {
		for (int i = 0; true; i++) {
			if (prefs.getString("websearch_baseurl_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified web-based search site setting for a specific site
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The web search site settings object, loaded from shared preferences
	 */
	public WebsearchSetting getWebsearchSetting(int order) {
		// @formatter:off
		return new WebsearchSetting(order, 
				prefs.getString("websearch_name_" + order, null), 
				prefs.getString("websearch_baseurl_" + order, null), 
				prefs.getString("websearch_cookies_" + order, null));
		// @formatter:on
	}

	/**
	 * Removes all settings related to a configured web-based search site. Since sites are ordered, the order of the
	 * remaining sites will be updated accordingly.
	 * @param order The identifying order number/key of the settings to remove
	 */
	public void removeWebsearchSettings(int order) {
		if (prefs.getString("websearch_baseurl_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist

		// Copy all settings higher than the supplied order number to the previous spot
		Editor edit = prefs.edit();
		int max = getMaxWebsearch();
		for (int i = order; i < max; i++) {
			edit.putString("websearch_name_" + i, prefs.getString("websearch_name_" + (i + 1), null));
			edit.putString("websearch_baseurl_" + i, prefs.getString("websearch_baseurl_" + (i + 1), null));
			edit.putString("websearch_cookies_" + i, prefs.getString("websearch_cookies_" + (i + 1), null));
		}

		// Remove the last settings, of which we are now sure are no longer required
		edit.remove("websearch_name_" + max);
		edit.remove("websearch_baseurl_" + max);
		edit.remove("websearch_cookies_" + max);
		edit.commit();

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
		return Collections.unmodifiableList(rssfeeds);
	}

	/**
	 * Returns the order number/identifying key of the last stored RSS feed
	 * @return The zero-based order number (index) of the last stored RSS feed
	 */
	public int getMaxRssfeed() {
		for (int i = 0; true; i++) {
			if (prefs.getString("rssfeed_url_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified RSS feed setting for a specific feed
	 * @param order The order number/identifying key of the settings to retrieve
	 * @return The RSS feed settings object, loaded from shared preferences
	 */
	public RssfeedSetting getRssfeedSetting(int order) {
		// @formatter:off
		long lastViewed = prefs.getLong("rssfeed_lastviewed_" + order, -1);
		return new RssfeedSetting(order, 
				prefs.getString("rssfeed_name_" + order, null), 
				prefs.getString("rssfeed_url_" + order, null), 
				prefs.getBoolean("rssfeed_reqauth_" + order, false), 
				lastViewed == -1L ? null: new Date(lastViewed));
		// @formatter:on
	}

	/**
	 * Removes all settings related to a configured RSS feed. Since feeds are ordered, the order of the remaining feeds
	 * will be updated accordingly.
	 * @param order The identifying order number/key of the settings to remove
	 */
	public void removeRssfeedSettings(int order) {
		if (prefs.getString("rssfeed_url_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist

		// Copy all settings higher than the supplied order number to the previous spot
		Editor edit = prefs.edit();
		int max = getMaxRssfeed();
		for (int i = order; i < max; i++) {
			edit.putString("rssfeed_name_" + i, prefs.getString("rssfeed_name_" + (i + 1), null));
			edit.putString("rssfeed_url_" + i, prefs.getString("rssfeed_url_" + (i + 1), null));
			edit.putBoolean("rssfeed_reqauth_" + i, prefs.getBoolean("rssfeed_reqauth_" + (i + 1), false));
			edit.putLong("rssfeed_lastviewed_" + i, prefs.getLong("rssfeed_lastviewed_" + (i + 1), -1));
		}

		// Remove the last settings, of which we are now sure are no longer required
		edit.remove("rssfeed_name_" + max);
		edit.remove("rssfeed_url_" + max);
		edit.remove("rssfeed_reqauth_" + max);
		edit.remove("rssfeed_lastviewed_" + max);
		edit.commit();

	}

	/**
	 * Registers for some RSS feed (as identified by its order numbe/key) the last date and time that it was viewed by
	 * the user. This is used to determine which items in an RSS feed are 'new'. Warning: any previously retrieved
	 * {@link RssfeedSetting} object is now no longer in sync, as this will not automatically be updated in the object.
	 * Use {@link #getRssfeedSetting(int)} to get fresh data.
	 * @param order The identifying order number/key of the settings of te RSS feed that was viewed
	 * @param lastViewed The date and time that the feed was last viewed; typically now
	 */
	public void setRssfeedLastViewer(int order, Date lastViewed) {
		if (prefs.getString("rssfeed_url_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist
		prefs.edit().putLong("rssfeed_lastviewed_" + order, lastViewed.getTime()).commit();
	}

	/**
	 * Registers the torrents list sort order as being last used by the user
	 * @param currentSortOrder The sort order property the user selected last
	 * @param currentSortAscending The sort order direction that was last used
	 */
	public void setLastUsedSortOrder(TorrentsSortBy currentSortOrder, boolean currentSortAscending) {
		prefs.edit().putInt("system_lastusedsortorder", currentSortOrder.getCode()).commit();
		prefs.edit().putBoolean("system_lastusedsortdirection", currentSortAscending).commit();
	}

	/**
	 * Returns the sort order property that the user last used. Use together with {@link #getLastUsedSortDescending()}
	 * to get the full last used sort settings.
	 * @return The last used sort order enumeration value
	 */
	public TorrentsSortBy getLastUsedSortOrder() {
		return TorrentsSortBy
				.getStatus(prefs.getInt("system_lastusedsortorder", TorrentsSortBy.Alphanumeric.getCode()));
	}

	/**
	 * Returns the sort order direction that the user last used. Use together with {@link #getLastUsedSortOrder()} to
	 * get the full last used sort settings.
	 * @return True if the last used sort direction was descending, false otherwise (i.e. the default ascending
	 *         direction)
	 */
	public boolean getLastUsedSortDescending() {
		return prefs.getBoolean("system_lastusedsortdirection", false);
	}

	/**
	 * Returns the list of all available in-app search sites as well as all web searches that the user configured.
	 * @return A list of search settings, all of which are either a {@link SearchSite} or {@link WebsearchSetting}
	 */
	public List<SearchSetting> getSearchSettings() {
		List<SearchSetting> all = new ArrayList<SearchSetting>();
		all.addAll(searchHelper.getAvailableSites());
		all.addAll(getWebsearchSettings());
		return Collections.unmodifiableList(all);
	}
	
	/**
	 * Returns the settings of the search site that was last used by the user or was selected by the user as default
	 * site in the main settings. As opposed to getLastUsedSearchSiteKey(int), this method checks whether a site was
	 * already registered as being last used (or set as default) and checks whether the site still exists. It returns
	 * the first in-app search site if that fails.
	 * @return A site settings object of the last used server (or, if not known, the first server), or null if no
	 *         servers exist
	 */
	public SearchSetting getLastUsedSearchSite() {
		String lastKey = getLastUsedSearchSiteKey();
		List<SearchSite> allsites = searchHelper.getAvailableSites();
		int lastWebsearch = -1;
		try {
			lastWebsearch = Integer.parseInt(lastKey);
		} catch (Exception e) {
			// Not an in-app search site, but probably an in-app search
		}

		if (lastKey == null) {
			// No site yet set specified; return the first in-app one, if available
			if (allsites != null) {
				return allsites.get(0);
			}
			return null;
		}
		
		if (lastWebsearch >= 0) {
			// The last used site should be a user-configured web search site
			int max = getMaxWebsearch(); // Zero-based index, so with max == 0 there is 1 server
			if (max < 0 || lastWebsearch > max) {
				// No web search sites configured
				return null;
			}
			return getWebsearchSetting(lastWebsearch);
		}
		
		// Should be an in-app search key
		if (allsites != null) {
			for (SearchSite searchSite : allsites) {
				if (searchSite.getKey().equals(lastKey)) {
					return searchSite;
				}
			}
			// Not found at all; probably a no longer existing web search; return the first in-app one
			return allsites.get(0);
		}
		
		return null;
	}

	/**
	 * Returns the unique key of the site that the used last used or selected as default form the main settings; use
	 * with getLastUsedSearchSite directly. WARNING: the returned string may no longer refer to a known web search site
	 * or in-app search settings object.
	 * @return A string indicating the key of the last used search site, or null if no site was yet used or set as
	 *         default
	 */
	private String getLastUsedSearchSiteKey() {
		return prefs.getString("header_setsearchsite", null);
	}

	/**
	 * Registers the unique key of some web search or in-app search site as being last used by the user
	 * @param order The key identifying the specific server
	 */
	public void setLastUsedSearchSite(SearchSite site) {
		prefs.edit().putString("header_setsearchsite", site.getKey()).commit();
	}

	/**
	 * Returns the statistics of this server as it was last seen by the background server checker service.
	 * @param server The server for which to retrieved the statistics from the stored preferences
	 * @return A JSON array of JSON objects, each which represent a since torrent
	 */
	public JSONArray getServerLastStats(ServerSetting server) {
		String lastStats = prefs.getString(server.getUniqueIdentifier(), null);
		if (lastStats == null)
			return null;
		try {
			return new JSONArray(lastStats);
		} catch (JSONException e) {
			return null;
		}
	}

	/**
	 * Stores the now-last seen statistics of the supplied server by the background server checker service to the
	 * internal stored preferences.
	 * @param server The server to which the statistics apply to
	 * @param lastStats A JSON array of JSON objects that each represent a single seen torrent
	 */
	public void setServerLastStats(ServerSetting server, JSONArray lastStats) {
		prefs.edit().putString(server.getUniqueIdentifier(), lastStats.toString()).commit();
	}
	
}
