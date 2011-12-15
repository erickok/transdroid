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

import org.transdroid.R;
import org.transdroid.preferences.Preferences;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

/**
 * Will provide a search 'suggestion' to the global Quick Search Box to search
 * for torrents in every available (in-app or web) search site
 * 
 * @author erickok
 *
 */
public class GlobalTorrentSearchProvider extends ContentProvider {

    public static final String AUTHORITY = "org.transdroid.GlobalTorrentSearchProvider";
    
    @Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return SearchManager.SUGGEST_MIME_TYPE;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		// Return a suggestion for every available site
		return new SitesCursor(getContext(), selectionArgs[0]);
	}
	
	private class SitesCursor extends MatrixCursor {

		public SitesCursor(Context context, String query) {
			super(new String[] {
					BaseColumns._ID, 
					SearchManager.SUGGEST_COLUMN_TEXT_1, 
					SearchManager.SUGGEST_COLUMN_TEXT_2, 
					SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, 
					SearchManager.SUGGEST_COLUMN_QUERY, 
					SearchManager.SUGGEST_COLUMN_SHORTCUT_ID});
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			// Add a single item for the default search site
			SiteSettings site = Preferences.readDefaultSearchSiteSettings(prefs);
			RowBuilder row = newRow();
			row.add(0);																			// ID
			row.add(query);																		// First text line
			row.add(context.getString(R.string.search_resultsfrom) + " " + site.getName());		// Second text line
			row.add(site.getKey());																// Extra string (daemon key to search against)
			row.add(query);																		// Extra string (query)
			row.add(0);
		}
		
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

    
}
