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
package org.transdroid.rss;

public final class RssFeedSettings {

	final private String key;
	final private String name;
	final private String url;
	final private boolean needsAuth;
	private String lastNew;

	public RssFeedSettings(String key, String name, String url, boolean needsAuth, String lastNew) {
		this.key = key;
		this.name = name;
		this.url = url;
		this.needsAuth = needsAuth;
		this.lastNew = lastNew;
	}
	
	/**
	 * Returns the unique key for this feed settings object (used as postfix in storing this settings to the user preferences)
	 * @return The unique rss feed preferences (postfix) key
	 */
	public String getKey() {
		return this.key;
	}
	
	/**
	 * The custom name of the RSS feed, as given by the user
	 * @return The feed name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * The full url of the RSS feed
	 * @return The feed url as url-encoded string
	 */
	public String getUrl() {
		return this.url;
	}
	
	/**
	 * Whether this feed is secured and requires authentication
	 * @return True if it is secured with some authentication mechanism
	 */
	public boolean needsAuthentication() {
		return this.needsAuth;
	}
	
	/**
	 * Returns the url of the item that was the newest last time we checked this feed
	 * @return The last new item's url as url-encoded string
	 */
	public String getLastNew() {
		return this.lastNew;
	}

	public void setLastNew(String lastNew) {
		this.lastNew = lastNew;
	}
	
	
}
