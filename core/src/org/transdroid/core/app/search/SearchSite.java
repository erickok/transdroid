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
package org.transdroid.core.app.search;

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.search.SearchSetting;

/**
 * Represents an available torrent site that can be searched using the Torrent Search package.
 * @author Eric Kok
 */
public class SearchSite implements SimpleListItem, SearchSetting {

	private final int id;
	private final String key;
	private final String name;
	private final String rssFeedUrl;
	private final boolean isPrivate;
	
	public SearchSite(int id, String key, String name, String rssFeedUrl, boolean isPrivate) {
		this.id = id;
		this.key = key;
		this.name = name;
		this.rssFeedUrl = rssFeedUrl;
		this.isPrivate = isPrivate;
	}

	public int getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getRssFeedUrl() {
		return rssFeedUrl;
	}

	@Override
	public String getBaseUrl() {
		return rssFeedUrl;
	}

	public boolean isPrivate() {
		return isPrivate;
	}
	
}
