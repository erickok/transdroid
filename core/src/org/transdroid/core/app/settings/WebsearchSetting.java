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

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.search.SearchSetting;

import android.net.Uri;
import android.text.TextUtils;

/**
 * Represents a user-specified website that can be searched (by starting the browser, rather than in-app)
 * @author Eric Kok
 */
public class WebsearchSetting implements SimpleListItem, SearchSetting {

	private static final String DEFAULT_NAME = "Default";
	public static final String KEY_PREFIX = "websearch_";
	
	private final int order;
	private final String name;
	private final String baseUrl;
	private final String cookies;

	public WebsearchSetting(int order, String name, String baseUrl, String cookies) {
		this.order = order;
		this.name = name;
		this.baseUrl = baseUrl;
		this.cookies = cookies;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		if (!TextUtils.isEmpty(name))
			return name;
		if (!TextUtils.isEmpty(baseUrl)) {
			String host = Uri.parse(baseUrl).getHost();
			return host == null? DEFAULT_NAME: host;
		}
		return DEFAULT_NAME;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public String getCookies() {
		return cookies;
	}
	
	public String getKey() {
		return KEY_PREFIX + getOrder();
	}

	/**
	 * Returns a nicely formatted identifier containing (a portion of) the search base URL
	 * @return A string to identify this site's search URL
	 */
	public String getHumanReadableIdentifier() {
		return Uri.parse(baseUrl).getHost();
	}
	
}
