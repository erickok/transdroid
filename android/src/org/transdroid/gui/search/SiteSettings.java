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
package org.transdroid.gui.search;

import java.net.URLEncoder;

import org.transdroid.R;

/**
 * Represents a torrent site configuration; either in-app or web search
 * 
 * @author erickok
 *
 */
public final class SiteSettings {

	final private boolean isWebSearch;
	final private String key;
	final private String name;
	final private String url;
	
	/**
	 * Instantiates an in-app search site
	 * @param adapterKey The unique key identifying the search adapter
	 * @param name The visible name of the site
	 */
	public SiteSettings(String adapterKey, String name) {
		this.isWebSearch = false;
		this.key = adapterKey;
		this.name = name;
		this.url = null;
	}
	
	/**
	 * Instantiates a web search site
	 * @param uniqueKey The (numeric) identifier for this web search settings (used as postfix on stored preferences)
	 * @param name The visible name of the site
	 * @param encodedUrl The raw URL to send the web search to, where %s will be replaced by the search keywords; for example http://www.google.nl/search?q=%s+ext%3Atorrent
	 */
	public SiteSettings(String uniqueKey, String name, String encodedUrl) {
		this.isWebSearch = true;
		this.key = uniqueKey;
		this.name = name;
		this.url = encodedUrl;
	}
	
	/**
	 * @return If this site performs a web search; it is an in-app search otherwise
	 */
	public boolean isWebSearch() {
		return isWebSearch;
	}
	
	/**
	 * @return For in-app search sites this is the factory key; otherwise it is the unique identifier used to store it in the user preferences
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * @return The visible name of the site
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the raw URL where web searches will be directed to; it has a %s where search keywords will be placed
	 * @return
	 */
	public String getRawurl() {
		return url;
	}
	
	/**
	 * Returns the URL for a search based on the user query. The query will be URL-encoded.
	 * @param query The raw user query text
	 * @return The url that can be send to the browser to show the web search results
	 */
	public String getSubstitutedUrl(String query) {
		return url.replace("%s", URLEncoder.encode(query));
	}

	/**
	 * Tells the user what type of search engine this is
	 * @return The resource id of the text explaining the search type
	 */
	public int getSearchTypeTextResource() {
		if (isWebSearch()) {
			return R.string.websearch;
		} else {
			return R.string.inappsite;
		}
	}
	
}
