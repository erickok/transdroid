package org.transdroid.core.gui.search;

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

/**
 * Provides search suggestions by simply returning previous user entries.
 * @author Eric Kok
 */
public class SearchHistoryProvider extends SearchRecentSuggestionsProvider {

	public final static String AUTHORITY = "org.transdroid.core.gui.search.SearchHistoryProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;

	public SearchHistoryProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}

	public static void clearHistory(Context context) {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, SearchHistoryProvider.AUTHORITY,
				SearchHistoryProvider.MODE);
		suggestions.clearHistory();
	}
}
