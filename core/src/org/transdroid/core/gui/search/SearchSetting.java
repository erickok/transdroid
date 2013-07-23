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
