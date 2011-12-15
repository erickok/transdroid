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

import org.transdroid.preferences.Preferences;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * Task that helps in retrieving torrent search results from the Transdroid Torrent Search content providers.
 * Use getSupportedSites() to see which torrent search sites are available to query against.
 * 
 * @author erickok
 * 
 */
public abstract class TorrentSearchTask extends AsyncTask<String, Void, Cursor> {

	// public static final String[] SEARCH_FIELDS = new String[] { "_ID", "NAME", "TORRENTURL", "DETAILSURL",
	// "SIZE", "ADDED", "SEEDERS", "LEECHERS" };
	// public static final String[] SITE_FIELDS = new String[] { "_ID", "CODE", "NAME", "RSSURL" };

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

	/**
	 * Returns the list of supported torrent search sites
	 * @param activity The displaying activity against which the query for Transdroid Torrent Search can be executed
	 * @return A cursor with search sites (with fields SITE_FIELDS)
	 */
	public static Cursor getSupportedSites(Activity activity) {
		// Create the URI of the TorrentSitesProvider
		String uriString = "content://org.transdroid.search.torrentsitesprovider/sites";
		Uri uri = Uri.parse(uriString);
		// Then query all torrent sites (no selection nor projection nor sort):
		return activity.managedQuery(uri, null, null, null, null);
	}

	/**
	 * Build an RSS feed URL for some site and some user query
	 * @param activity The calling activity (used to connect to Transdroid Torrent Search)
	 * @param preferencesKey The Transdroid-preferences key, e.g. 'site_mininova'
	 * @param query The user query that was searched for
	 * @return The RSS feed URL, or null if the site isn't supporting RSS feeds (or no site with preferencesKey exists)
	 */
	public static String buildRssFeedFromSearch(Activity activity, String preferencesKey, String query) {
		String key = Preferences.getCursorKeyForPreferencesKey(preferencesKey);
		Cursor cursor = getSupportedSites(activity);
		if (cursor.moveToFirst()) {
			do {
				if (cursor.getString(CURSOR_SITE_CODE).equals(key)) {
					if (cursor.getString(CURSOR_SITE_RSSURL) == null || cursor.getString(CURSOR_SITE_RSSURL).equals("")) {
						// Not supported by this site
						return null;
					}
					return cursor.getString(CURSOR_SITE_RSSURL).replace("%s", query);
				}
			} while (cursor.moveToNext());
		}
		// Site is not supported by Transdroid Torrent Search
		return null;
	}

	/**
	 * Return whether Transdroid Torrent Search is installed and available to query against
	 * @return True if the available sites can be retrieved from the content provider, false otherwise
	 */
	public static boolean isTorrentSearchInstalled(Activity activity) {
		return getSupportedSites(activity) != null;
	}

	private Activity activity;

	public TorrentSearchTask(Activity activity) {
		this.activity = activity;
	}

	@Override
	protected final Cursor doInBackground(String... params) {

		// Search query, site and sort order specified?
		if (params == null || params.length != 3) {
			return null;
		}

		// Create the URI of the TorrentProvider
		String uriString = "content://org.transdroid.search.torrentsearchprovider/search/" + params[0];
		Uri uri = Uri.parse(uriString);

		// Then query for this specific search, site and sort order
		return activity.managedQuery(uri, null, "SITE = ?", new String[] { params[1] }, params[2]);
		// Actual catching of run-time exceptions doesn't work cross-process
		/*
		 * } catch (RuntimeException e) { // Hold on to the error message; onPostExecute will post it back to
		 * make sure it's posted on the UI thread errorMessage = e.toString(); return null; }
		 */

	}

	@Override
	protected final void onPostExecute(Cursor cursor) {
		if (cursor == null)
			onError();
		else
			onResultsRetrieved(cursor);
	}

	/**
	 * Method that needs to be implemented to handle the search results
	 * 
	 * @param errorMessage
	 *            The (technical) error message text
	 */
	protected abstract void onResultsRetrieved(Cursor cursor);

	/**
	 * Method that needs to be implemented to catch error occurring during the retrieving or parsing of search
	 * results
	 */
	protected abstract void onError();

}
