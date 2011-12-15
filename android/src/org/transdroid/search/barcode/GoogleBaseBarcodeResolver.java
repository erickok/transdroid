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
package org.transdroid.search.barcode;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.util.TLog;
import org.xml.sax.SAXException;

import android.os.AsyncTask;

public abstract class GoogleBaseBarcodeResolver extends AsyncTask<String, Void, String> {

	private static final String LOG_NAME = "GoogleBaseBarcodeResolver";
	public static final String apiUrl = "http://www.google.com/base/feeds/snippets?bq=%s&max-results=1&alt=rss";
	
	@Override
	protected String doInBackground(String... params) {
		
		if (params.length < 1) {
			return null;
		}
		
		// We use the Google Base API to get meaningful results from a barcode number
		TLog.d(LOG_NAME, "Getting RSS feed at " + apiUrl.replace("%s", params[0]));
		RssParser feed = new RssParser(apiUrl.replace("%s", params[0]));
		try {
			feed.parse();
		} catch (ParserConfigurationException e) {
			TLog.d(LOG_NAME, e.toString());
			return null;
			//throw new SearchException(R.string.error_parsingrss, e.toString());
		} catch (SAXException e) {
			TLog.d(LOG_NAME, e.toString());
			return null;
			//throw new SearchException(R.string.error_parsingrss, e.toString());
		} catch (IOException e) {
			TLog.d(LOG_NAME, e.toString());
			return null;
			//throw new SearchException(R.string.error_httperror, e.toString());
		}
		
		// For now, just return the first 5 actual words of the first item
		List<Item> items = feed.getChannel().getItems();
		TLog.d(LOG_NAME, "Feed contains " + items.size() + " items");
		if (items.size() < 1 || items.get(0).getTitle() == null) {
			return null;
		}
		// Remove the barcode number if it's there
		String cleanup = items.get(0).getTitle().replace(params[0], "");
		return stripGarbage(cleanup);
		
	}

	@Override
	protected void onPostExecute(String result) {
    	onBarcodeLookupComplete(result);
	}
	
	protected abstract void onBarcodeLookupComplete(String result);

	private static String stripGarbage(String s) {
		
		String good = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String result = "";
		
		// Remove all non-alphanumeric (and space) characters
		for ( int i = 0; i < s.length(); i++ ) {
			if ( good.indexOf(s.charAt(i)) >= 0 )
				result += s.charAt(i);
		}
		
		// Remove double spaces
		while (result.contains("  ")) {
			result = result.replace("  ", " ");
		}
		
		// Only retain first four words
		int i, j = 0;
		for ( i = 0; i < s.length(); i++ ) {
			if (s.charAt(i) == ' ') {
				j++;
			}
			if (j > 4) {
				break;
			}
		}
		return i > 0? result.substring(0, i - 1): "";
		
	}

}
