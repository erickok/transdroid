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

import org.transdroid.BuildConfig;

/**
 * Provides search suggestions by simply returning previous user entries.
 * @author Eric Kok
 */
public class SearchHistoryProvider extends SearchRecentSuggestionsProvider {

	public final static String AUTHORITY = BuildConfig.APPLICATION_ID + ".search.SearchHistoryProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;

	public SearchHistoryProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	public static void clearHistory(Context context) {
		new SearchRecentSuggestions(context, AUTHORITY, MODE).clearHistory();
	}
	
}
