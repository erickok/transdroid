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

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

/**
 * Provides a wrapper for the {@link SearchRecentSuggestionsProvider} to show the last torrent searches to the user.
 * @author Eric Kok
 */
public class TorrentSearchHistoryProvider extends SearchRecentSuggestionsProvider {

	public static final String AUTHORITY = "org.transdroid.core.gui.search.TorrentSearchHistoryProvider";
	public static final int MODE = DATABASE_MODE_QUERIES;

	public TorrentSearchHistoryProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}

	public static void clearHistory(Context context) {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
				TorrentSearchHistoryProvider.AUTHORITY, TorrentSearchHistoryProvider.MODE);
		suggestions.clearHistory();
	}
}
