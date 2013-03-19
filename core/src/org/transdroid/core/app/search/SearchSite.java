package org.transdroid.core.app.search;

import org.transdroid.core.gui.lists.SimpleListItem;

/**
 * Represents an available torrent site that can be searched using the Torrent Search package.
 * @author Eric Kok
 */
public class SearchSite implements SimpleListItem {

	private final int id;
	private final String key;
	private final String name;
	private final String rssFeedUrl;
	
	public SearchSite(int id, String key, String name, String rssFeedUrl) {
		this.id = id;
		this.key = key;
		this.name = name;
		this.rssFeedUrl = rssFeedUrl;
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
	
}
