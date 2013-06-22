package org.transdroid.core.app.search;

import java.util.Date;

/**
 * Represents a search result as retrieved by querying the Torrent Search package.
 * @author Eric Kok
 */
public class SearchResult {

	private final int id;
	private final String name;
	private final String torrentUrl;
	private final String detailsUrl;
	private final String size;
	private final Date addedOn;
	private final String seeders;
	private final String leechers;

	public SearchResult(int id, String name, String torrentUrl, String detailsUrl, String size, long addedOnTime,
			String seeders, String leechers) {
		this.id = id;
		this.name = name;
		this.torrentUrl = torrentUrl;
		this.detailsUrl = detailsUrl;
		this.size = size;
		this.addedOn = (addedOnTime == -1L) ? null : new Date(addedOnTime);
		this.seeders = seeders;
		this.leechers = leechers;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTorrentUrl() {
		return torrentUrl;
	}

	public String getDetailsUrl() {
		return detailsUrl;
	}

	public String getSize() {
		return size;
	}

	public Date getAddedOn() {
		return addedOn;
	}

	public String getSeeders() {
		return seeders;
	}

	public String getLeechers() {
		return leechers;
	}

}
