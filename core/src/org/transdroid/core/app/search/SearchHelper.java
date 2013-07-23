package org.transdroid.core.app.search;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EBean.Scope;
import org.androidannotations.annotations.RootContext;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

@EBean(scope = Scope.Singleton)
public class SearchHelper {

	static final int CURSOR_SEARCH_ID = 0;
	static final int CURSOR_SEARCH_NAME = 1;
	static final int CURSOR_SEARCH_TORRENTURL = 2;
	static final int CURSOR_SEARCH_DETAILSURL = 3;
	static final int CURSOR_SEARCH_SIZE = 4;
	static final int CURSOR_SEARCH_ADDED = 5;
	static final int CURSOR_SEARCH_SEEDERS = 6;
	static final int CURSOR_SEARCH_LEECHERS = 7;

	static final int CURSOR_SITE_ID = 0;
	static final int CURSOR_SITE_CODE = 1;
	static final int CURSOR_SITE_NAME = 2;
	static final int CURSOR_SITE_RSSURL = 3;

	@RootContext
	protected Context context;

	public enum SearchSortOrder {
		Combined, BySeeders
	}

	/**
	 * Return whether the Torrent Search package is installed and available to query against
	 * @return True if the available sites can be retrieved from the content provider, false otherwise
	 */
	public boolean isTorrentSearchInstalled() {
		return getAvailableSites() != null;
	}

	/**
	 * Queries the Torrent Search package for all available in-app search sites. This method is synchronous.
	 * @return A list of available search sites as POJOs, or null if the Torrent Search package is not installed
	 */
	public List<SearchSite> getAvailableSites() {

		// Try to access the TorrentSitesProvider of the Torrent Search app
		Uri uri = Uri.parse("content://org.transdroid.search.torrentsitesprovider/sites");
		ContentProviderClient test = context.getContentResolver().acquireContentProviderClient(uri);
		if (test == null) {
			// Torrent Search package is not yet installed
			return null;
		}
		
		// Query the available in-app torrent search sites
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		if (cursor.moveToFirst()) {
			List<SearchSite> sites = new ArrayList<SearchSite>();
			do {
				// Read the cursor fields into the SearchSite object
				sites.add(new SearchSite(cursor.getInt(CURSOR_SITE_ID), cursor.getString(CURSOR_SITE_CODE), cursor
						.getString(CURSOR_SITE_NAME), cursor.getString(CURSOR_SITE_RSSURL)));
			} while (cursor.moveToNext());
			cursor.close();
			return sites;
		}

		return null;

	}

	/**
	 * Queries the Torrent Search module to search for torrents on the web. This method is synchronous and should always
	 * be called in a background thread.
	 * @param query The search query to pass to the torrent site
	 * @param site The site to search, as retrieved from the TorrentSitesProvider, or null if the Torrent Search package
	 * @param sortBy.name() The sort order to request from the torrent site, if supported
	 * @return A list of torrent search results as POJOs, or null if the Torrent Search package is not installed
	 */
	public ArrayList<SearchResult> search(String query, SearchSite site, SearchSortOrder sortBy) {

		// Try to query the TorrentSearchProvider to search for torrents on the web
		Uri uri = Uri.parse("content://org.transdroid.search.torrentsearchprovider/search/" + query);
		Cursor cursor;
		if (site == null) {
			// If no explicit site was supplied, rely on the Torrent Search package's default
			cursor = context.getContentResolver().query(uri, null, null, null, sortBy.name());
		} else {
			cursor = context.getContentResolver().query(uri, null, "SITE = ?", new String[] { site.getKey() },
					sortBy.name());
		}
		if (cursor.moveToFirst()) {
			ArrayList<SearchResult> results = new ArrayList<SearchResult>();
			do {
				// Read the cursor fields into the SearchResult object
				results.add(new SearchResult(cursor.getInt(CURSOR_SEARCH_ID), cursor.getString(CURSOR_SEARCH_NAME),
						cursor.getString(CURSOR_SEARCH_TORRENTURL), cursor.getString(CURSOR_SEARCH_DETAILSURL), cursor
								.getString(CURSOR_SEARCH_SIZE), cursor.getLong(CURSOR_SEARCH_ADDED), cursor
								.getString(CURSOR_SEARCH_SEEDERS), cursor.getString(CURSOR_SEARCH_LEECHERS)));
			} while (cursor.moveToNext());
			cursor.close();
			return results;
		}

		// Torrent Search package is not yet installed
		return null;

	}

}
