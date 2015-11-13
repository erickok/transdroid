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

import java.util.Date;

import org.transdroid.core.gui.lists.SimpleListItem;

import android.net.Uri;
import android.text.TextUtils;

/**
 * Represents a user-specified RSS feed.
 * @author Eric Kok
 */
public class RssfeedSetting implements SimpleListItem {

	private static final String DEFAULT_NAME = "Default";

	private final int order;
	private final String name;
	private final String url;
	private final boolean requiresAuth;
	private final boolean alarm;
	private final String excludeFilter;
	private final String includeFilter;
	private Date lastViewed;
	private final String lastViewedItemUrl;

	public RssfeedSetting(int order, String name, String baseUrl, boolean needsAuth, boolean alarm, String excludeFilter, String includeFilter, Date lastViewed,
			String lastViewedItemUrl) {
		this.order = order;
		this.name = name;
		this.url = baseUrl;
		this.requiresAuth = needsAuth;
		this.alarm = alarm;
		this.excludeFilter = excludeFilter;
		this.includeFilter = includeFilter;
		this.lastViewed = lastViewed;
		this.lastViewedItemUrl = lastViewedItemUrl;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		if (!TextUtils.isEmpty(name))
			return name;
		if (!TextUtils.isEmpty(url)) {
			String host = Uri.parse(url).getHost();
			return host == null ? DEFAULT_NAME : host;
		}
		return DEFAULT_NAME;
	}

	public String getUrl() {
		return url;
	}

	public boolean requiresExternalAuthentication() {
		return requiresAuth;
	}

	public boolean shouldAlarmOnNewItems() {
		return alarm;
	}

	public String getExcludeFilter() {
		return excludeFilter;
	}

	public String getIncludeFilter() {
		return includeFilter;
	}

	/**
	 * Returns the date on which we last checked this feed. Note that this is NOT updated automatically after the
	 * settings were loaded from {@link ApplicationSettings}; instead the settings have to be manually loaded again
	 * using {@link ApplicationSettings#getRssfeedSetting(int)}.
	 * @return The last new item's URL as URL-encoded string
	 */
	public Date getLastViewed() {
		return this.lastViewed;
	}

	/**
	 * Returns the URL of the item that was the newest last time we checked this feed. Note that this is NOT updated
	 * automatically after the settings were loaded from {@link ApplicationSettings}; instead the settings have to be
	 * manually loaded again using {@link ApplicationSettings#getRssfeedSetting(int)}.
	 * @return The last new item's URL as URL-encoded string
	 */
	public String getLastViewedItemUrl() {
		return this.lastViewedItemUrl;
	}

	/**
	 * Returns a nicely formatted identifier containing (a portion of) the feed URL
	 * @return A string to identify this feed's URL
	 */
	public String getHumanReadableIdentifier() {
		String host = Uri.parse(url).getHost();
		String path = Uri.parse(url).getPath();
		return (host == null ? null : host + (path == null ? "" : path));
	}

}
