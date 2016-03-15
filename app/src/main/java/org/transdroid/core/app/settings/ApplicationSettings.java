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

import java.security.InvalidParameterException;
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
import org.transdroid.core.gui.navigation.NavigationFilter;
import org.transdroid.core.gui.navigation.StatusType;
import org.transdroid.core.gui.search.SearchSetting;
import org.transdroid.core.seedbox.SeedboxProvider;
import org.transdroid.core.widget.ListWidgetConfig;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.TorrentsSortBy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * Singleton object to access all application settings, including stored servers, web search sites and RSS feeds.
 * @author Eric Kok
 */
@EBean(scope = Scope.Singleton)
public class ApplicationSettings {

	public static final int DEFAULTSERVER_LASTUSED = -2;
	public static final int DEFAULTSERVER_ASKONADD = -1;

	@RootContext
	protected Context context;
	private SharedPreferences prefs;
	@Bean
	protected SearchHelper searchHelper;

	protected ApplicationSettings(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Returns all available user-configured normal and seed servers
	 * @return A list of all stored server settings objects
	 */
	public List<ServerSetting> getAllServerSettings() {
		List<ServerSetting> all = new ArrayList<>();
		all.addAll(getNormalServerSettings());
		for (SeedboxProvider provider : SeedboxProvider.values()) {
			all.addAll(provider.getSettings().getAllServerSettings(prefs, all.size()));
		}
		return all;
	}

	/**
	 * Returns the order number/identifying key of the last server, normal or seedbox configured
	 * @return The zero-based order number (index) of the last stored server settings
	 */
	public int getMaxOfAllServers() {
		int max = getMaxNormalServer();
		for (SeedboxProvider provider : SeedboxProvider.values()) {
			max += provider.getSettings().getMaxSeedboxOrder(prefs) + 1;
		}
		return max;
	}

	/**
	 * Returns the server settings for either a normal or a seedbox server as the user configured. WARNING: This method
	 * does not check if the settings actually exist and may reply on empty default if called for a non-existing server.
	 * @param order The order number/identifying key of the server's settings to retrieve, where the normal servers are
	 *            first and the seedboxes are numbers thereafter onwards
	 * @return The server settings object, loaded from shared preferences
	 */
	public ServerSetting getServerSetting(int order) {
		int max = getMaxNormalServer() + 1;
		if (order < max) {
			return getNormalServerSetting(order);
		}
		for (SeedboxProvider provider : SeedboxProvider.values()) {
			int offset = max;
			max += provider.getSettings().getMaxSeedboxOrder(prefs) + 1;
			if (order < max) {
				return provider.getSettings().getServerSetting(prefs, offset, order - offset);
			}
		}
		return null;
	}

	/**
	 * Returns all available normal, user-configured servers (so no seedbox settings)
	 * @return A list of all stored server settings objects
	 */
	public List<ServerSetting> getNormalServerSettings() {
		List<ServerSetting> servers = new ArrayList<>();
		for (int i = 0; i <= getMaxNormalServer(); i++) {
			servers.add(getNormalServerSetting(i));
		}
		return Collections.unmodifiableList(servers);
	}

	/**
	 * Returns the order number/identifying key of the last normal server
	 * @return The zero-based order number (index) of the last stored normal server settings
	 */
	public int getMaxNormalServer() {
		for (int i = 0; true; i++) {
			if (prefs.getString("server_type_" + i, null) == null || prefs.getString("server_address_" + i, null) == null)
				return i - 1;
		}
	}

	/**
	 * Returns the user-specified server settings for a normal (non-seedbox) server. WARNING: This method does not check
	 * if the settings actually exist and may rely on empty defaults if called for a non-existing server.
	 * @param order The order number/identifying key of the normal server's settings to retrieve
	 * @return The server settings object, loaded from shared preferences
	 */
	public ServerSetting getNormalServerSetting(int order) {
		// @formatter:off
		Daemon type = Daemon.fromCode(prefs.getString("server_type_" + order, null));
		boolean ssl = prefs.getBoolean("server_sslenabled_" + order, false);

		String port = prefs.getString("server_port_" + order, null);
		if (TextUtils.isEmpty(port))
			port = Integer.toString(Daemon.getDefaultPortNumber(type, ssl));
		String localPort = prefs.getString("server_localport_" + order, null);
		if (TextUtils.isEmpty(localPort))
			localPort = port; // Default to the normal (non-local) port
		try {
			parseInt(port, Daemon.getDefaultPortNumber(type, ssl));
		} catch (NumberFormatException e) {
			port = Integer.toString(Daemon.getDefaultPortNumber(type, ssl));
		}
		try {
			parseInt(localPort, parseInt(port, Daemon.getDefaultPortNumber(type, ssl)));
		} catch (NumberFormatException e) {
			localPort = port;
		}

		return new ServerSetting(order, 
				prefs.getString("server_name_" + order, null), 
				type, 
				trim(prefs.getString("server_address_" + order, null)),
				trim(prefs.getString("server_localaddress_" + order, null)),
				parseInt(localPort, parseInt(port, Daemon.getDefaultPortNumber(type, ssl))),
				prefs.getString("server_localnetwork_" + order, null), 
				parseInt(port, Daemon.getDefaultPortNumber(type, ssl)),
				ssl, 
				prefs.getBoolean("server_ssltrustall_" + order, false), 
				prefs.getString("server_ssltrustkey_" + order, null),
				prefs.getString("server_folder_" + order, null),
				!prefs.getBoolean("server_disableauth_" + order, false), 
				prefs.getString("server_user_" + order, null),
				prefs.getString("server_pass_" + order, null), 
				prefs.getString("server_extrapass_" + order, null),
				OS.fromCode(prefs.getString("server_os_" + order, "type_linux")), 
				prefs.getString("server_downloaddir_" + order, null), 
				prefs.getString("server_ftpurl_" + order, null), 
				prefs.getString("server_ftppass_" + order, null), 
				parseInt(prefs.getString("server_timeout_" + order, "8"), 8),
				prefs.getBoolean("server_alarmfinished_" + order, true), 
				prefs.getBoolean("server_alarmnew_" + order, false),
				prefs.getString("server_alarmexclude_" + order, null),
				prefs.getString("server_alarminclude_" + order, null),
				false);
		// @formatter:on
	}

	/**
	 * Removes all settings related to a configured server. Since servers are ordered, the order of the remaining
	 * servers will be updated accordingly.
	 * @param order The identifying order number/key of the settings to remove
	 */
	public void removeNormalServerSettings(int order) {
		if (prefs.getString("server_type_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist

		// Copy all settings higher than the supplied order number to the previous spot
		Editor edit = prefs.edit();
		int max = getMaxNormalServer();
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
			edit.putBoolean("server_disableauth_" + i, prefs.getBoolean("server_disableauth_" + (i + 1), false));
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
		edit.remove("server_disableauth_" + max);
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

		// Perhaps we should also update the default server to match the server's new id or remove the default selection
		// in case it was this server that was removed
		int defaultServer = getDefaultServerKey();
		if (defaultServer == order) {
			edit.remove("header_defaultserver");
		} else if (defaultServer > order) {
			// Move 'up' one place to account for the removed server setting
			edit.putInt("header_defaultserver", --order);
		}

		edit.apply();

	}

	/**
	 * Returns the settings of the server that was explicitly selected by the user to select as default or, when no
	 * specific default server was selected, the last used server settings. As opposed to getDefaultServerKey(int), this
	 * method checks whether the particular server still exists (and returns the first server if not). If no servers are
	 * configured, null is returned.
	 * @return A server settings object of the server to use by default, or null if no server is yet configured
	 */
	public ServerSetting getDefaultServer() {

		int defaultServer = getDefaultServerKey();
		if (defaultServer == DEFAULTSERVER_LASTUSED || defaultServer == DEFAULTSERVER_ASKONADD) {
			return getLastUsedServer();
		}

		// Use the explicitly selected default server
		int max = getMaxOfAllServers(); // Zero-based index, so with max == 0 there is 1 server
		if (max < 0) {
			// No servers configured
			return null;
		}
		if (defaultServer < 0 || defaultServer > max) {
			// Last server was never set or no longer exists
			return getServerSetting(0);
		}
		return getServerSetting(defaultServer);

	}

	/**
	 * Returns the unique key of the server setting that the user selected as their default server, or code indicating
	 * that the last used server should be selected by default; use with getDefaultServer directly. WARNING: the
	 * returned string may no longer refer to a known server setting key.
	 * @return An integer; if it is 0 or higher it represents the unique key of a configured server setting, -2 means
	 *         the last used server should be selected as default instead and -1 means the last used server should be
	 *         selected by default for viewing yet it should always ask when adding a new torrent
	 */
	public int getDefaultServerKey() {
		String defaultServer = prefs.getString("header_defaultserver", Integer.toString(DEFAULTSERVER_LASTUSED));
		try {
			return Integer.parseInt(defaultServer);
		} catch (NumberFormatException e) {
			// This should NEVER happen but if the setting somehow is not a number, return the default
			return DEFAULTSERVER_LASTUSED;
		}
	}

	/**
	 * Returns the settings of the server that was last used by the user. As opposed to getLastUsedServerKey(int), this
	 * method checks whether a server was already registered as being last used and check whether the server still
	 * exists. It returns the first server if that fails. If no servers are configured, null is returned.
	 * @return A server settings object of the last used server (or, if not known, the first server), or null if no
	 *         servers exist
	 */
	public ServerSetting getLastUsedServer() {
		int max = getMaxOfAllServers(); // Zero-based index, so with max == 0 there is 1 server
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
		prefs.edit().putInt("system_lastusedserver", order).apply();
	}

	/**
	 * Returns the unique code that (should) uniquely identify a navigation filter, such as a label, in the list of all
	 * available filters
	 * @return A code that the last used navigation filter reported as uniquely identifying itself, or null if no last
	 *         used filter is known
	 */
	public String getLastUsedNavigationFilter() {
		return prefs.getString("system_lastusedfilter", null);
	}

	/**
	 * Registers some navigation filter as being the last used by the user
	 * @param filter The navigation filter that the user last used in the interface
	 */
	public void setLastUsedNavigationFilter(NavigationFilter filter) {
		prefs.edit().putString("system_lastusedfilter", filter.getCode()).apply();
	}

	/**
	 * Returns all available user-configured web-based (as opped to in-app) search sites
	 * @return A list of all stored web search site settings objects
	 */
	public List<WebsearchSetting> getWebsearchSettings() {
		List<WebsearchSetting> websearches = new ArrayList<>();
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
		edit.apply();

	}

	/**
	 * Returns all available user-configured RSS feeds
	 * @return A list of all stored RSS feed settings objects
	 */
	public List<RssfeedSetting> getRssfeedSettings() {
		List<RssfeedSetting> rssfeeds = new ArrayList<>();
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
				prefs.getBoolean("rssfeed_alarmnew_" + order, true),
				prefs.getString("rssfeed_exclude_" + order, null),
				prefs.getString("rssfeed_include_" + order, null),
				lastViewed == -1L ? null : new Date(lastViewed),
				prefs.getString("rssfeed_lastvieweditemurl_" + order, null));
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
			edit.putBoolean("rssfeed_alarmnew_" + i, prefs.getBoolean("rssfeed_alarmnew_" + (i + 1), true));
			edit.putString("rssfeed_exclude_" + i, prefs.getString("rssfeed_exclude_" + (i + 1), null));
			edit.putString("rssfeed_include_" + i, prefs.getString("rssfeed_include_" + (i + 1), null));
			edit.putLong("rssfeed_lastviewed_" + i, prefs.getLong("rssfeed_lastviewed_" + (i + 1), -1));
			edit.putLong("rssfeed_lastvieweditemurl_" + i, prefs.getLong("rssfeed_lastvieweditemurl_" + (i + 1), -1));
		}

		// Remove the last settings, of which we are now sure are no longer required
		edit.remove("rssfeed_name_" + max);
		edit.remove("rssfeed_url_" + max);
		edit.remove("rssfeed_reqauth_" + max);
		edit.remove("rssfeed_alarmnew_" + max);
		edit.remove("rssfeed_exclude_" + max);
		edit.remove("rssfeed_include_" + max);
		edit.remove("rssfeed_lastviewed_" + max);
		edit.remove("rssfeed_lastvieweditemurl_" + max);
		edit.apply();

	}

	/**
	 * Registers for some RSS feed (as identified by its order numbe/key) the last date and time that it was viewed by
	 * the user. This is used to determine which items in an RSS feed are 'new'. Warning: any previously retrieved
	 * {@link RssfeedSetting} object is now no longer in sync, as this will not automatically be updated in the object.
	 * Use {@link #getRssfeedSetting(int)} to get fresh data.
	 * @param order The identifying order number/key of the settings of te RSS feed that was viewed
	 * @param lastViewed The date and time that the feed was last viewed; typically now
	 * @param lastViewedItemUrl The url of the last item the last time that the feed was viewed
	 */
	public void setRssfeedLastViewer(int order, Date lastViewed, String lastViewedItemUrl) {
		if (prefs.getString("rssfeed_url_" + order, null) == null)
			return; // The settings that were requested to be removed do not exist
		Editor edit = prefs.edit();
		edit.putLong("rssfeed_lastviewed_" + order, lastViewed.getTime());
		edit.putString("rssfeed_lastvieweditemurl_" + order, lastViewedItemUrl);
		edit.apply();
	}

	/**
	 * Registers the torrents list sort order as being last used by the user
	 * @param currentSortOrder The sort order property the user selected last
	 * @param currentSortAscending The sort order direction that was last used
	 */
	public void setLastUsedSortOrder(TorrentsSortBy currentSortOrder, boolean currentSortAscending) {
		Editor edit = prefs.edit();
		edit.putInt("system_lastusedsortorder", currentSortOrder.getCode());
		edit.putBoolean("system_lastusedsortdirection", currentSortAscending);
		edit.apply();
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
		List<SearchSetting> all = new ArrayList<>();
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

		if (lastKey == null) {
			// No site yet set specified; return the first in-app one, if available
			if (allsites != null) {
				return allsites.get(0);
			}
			return null;
		}

		int lastWebsearch = -1;
		if (lastKey.startsWith(WebsearchSetting.KEY_PREFIX)) {
			try {
				lastWebsearch = Integer.parseInt(lastKey.substring(WebsearchSetting.KEY_PREFIX.length()));
			} catch (Exception e) {
				// Not an in-app search site, but probably an in-app search
			}
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
		if (allsites != null && !allsites.isEmpty()) {
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
	 * @param site The site settings to register as being last used
	 */
	public void setLastUsedSearchSite(SearchSetting site) {
		prefs.edit().putString("header_setsearchsite", site.getKey()).apply();
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
		prefs.edit().putString(server.getUniqueIdentifier(), lastStats.toString()).apply();
	}

	/**
	 * Returns the user configuration for some specific app widget, if the widget is known at all.
	 * @param appWidgetId The unique ID of the app widget to retrieve settings for, as supplied by the AppWidgetManager
	 * @return A widget configuration object, or null if no settings were stored for the widget ID
	 */
	public ListWidgetConfig getWidgetConfig(int appWidgetId) {
		if (!prefs.contains("widget_server_" + appWidgetId))
			return null;
		// @formatter:off
		return new ListWidgetConfig(
				prefs.getInt("widget_server_" + appWidgetId, -1), 
				StatusType.valueOf(prefs.getString("widget_status_" + appWidgetId, StatusType.ShowAll.name())), 
				TorrentsSortBy.valueOf(prefs.getString("widget_sortby_" + appWidgetId, TorrentsSortBy.Alphanumeric.name())), 
				prefs.getBoolean("widget_reverse_" + appWidgetId, false), 
				prefs.getBoolean("widget_showstatus_" + appWidgetId, false), 
				prefs.getBoolean("widget_darktheme_" + appWidgetId, false));
		// @formatter:on
	}

	/**
	 * Stores the user settings for some specific app widget. Existing settings for the supplied app widget ID will be
	 * overridden.
	 * @param appWidgetId The unique ID of the app widget to store settings for, as supplied by the AppWidgetManager
	 * @param settings A widget configuration object, which may not be null
	 */
	public void setWidgetConfig(int appWidgetId, ListWidgetConfig settings) {
		if (settings == null)
			throw new InvalidParameterException(
					"The widget setting may not be null. Use removeWidgetConfig instead to remove existing settings for some app widget.");
		Editor edit = prefs.edit();
		edit.putInt("widget_server_" + appWidgetId, settings.getServerId());
		edit.putString("widget_status_" + appWidgetId, settings.getStatusType().name());
		edit.putString("widget_sortby_" + appWidgetId, settings.getSortBy().name());
		edit.putBoolean("widget_reverse_" + appWidgetId, settings.shouldReserveSort());
		edit.putBoolean("widget_showstatus_" + appWidgetId, settings.shouldShowStatusView());
		edit.putBoolean("widget_darktheme_" + appWidgetId, settings.shouldUseDarkTheme());
		edit.apply();
	}

	/**
	 * Remove the setting for some specific app widget.
	 * @param appWidgetId The unique ID of the app widget to store settings for, as supplied by the AppWidgetManager
	 */
	public void removeWidgetConfig(int appWidgetId) {
		Editor edit = prefs.edit();
		edit.remove("widget_server_" + appWidgetId);
		edit.remove("widget_status_" + appWidgetId);
		edit.remove("widget_sortby_" + appWidgetId);
		edit.remove("widget_reverse_" + appWidgetId);
		edit.remove("widget_showstatus_" + appWidgetId);
		edit.remove("widget_darktheme_" + appWidgetId);
		edit.apply();
	}

	/**
	 * Trims away whitespace around a string, or returns null if str is null
	 * @param str The string to trim, or null
	 * @return The trimmed string, or null if str is null
	 */
	private String trim(String str) {
		if (str == null) return null;
		return str.trim();
	}

	private int parseInt(String string, int defaultValue) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

}
