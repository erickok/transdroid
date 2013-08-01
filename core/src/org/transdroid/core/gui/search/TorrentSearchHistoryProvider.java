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
