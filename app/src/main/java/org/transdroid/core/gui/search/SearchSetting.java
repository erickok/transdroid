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
package org.transdroid.core.gui.search;

import org.transdroid.core.gui.lists.SimpleListItem;

public interface SearchSetting extends SimpleListItem {

	/**
	 * Should return a unique key for this search setting, so that it can be compared (using equals()) to other settings.
	 * @return A unique string identifying this search setting
	 */
	public String getKey();

	/**
	 * Should return an URL (which may still be abstract and not the actual search URL) specific to the search site
	 * @return A clean URL directing to the search site, to, for example, get the favicon of the site
	 */
	public String getBaseUrl();

}
