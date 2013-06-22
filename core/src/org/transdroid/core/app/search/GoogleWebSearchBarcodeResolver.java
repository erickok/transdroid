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
package org.transdroid.core.app.search;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.daemon.util.HttpHelper;

public class GoogleWebSearchBarcodeResolver {

	public static final String apiUrl = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=%s";
	
	public static String resolveBarcode(String barcode) {
		
		try {
			// We use the Google AJAX Search API to get a JSON-formatted list of web search results
			String callUrl = apiUrl.replace("%s", barcode);
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(callUrl);
			HttpResponse response = httpclient.execute(httpget);
	        InputStream instream = response.getEntity().getContent();
	        String result = HttpHelper.ConvertStreamToString(instream);
			JSONArray results = new JSONObject(result).getJSONObject("responseData").getJSONArray("results");
			
			// We will combine and filter multiple results, if there are any
			if (results.length() < 1) {
				return null;
			}
			return stripGarbage(results, barcode);
		} catch (Exception e) {
			return null;
		}
		
	}

	private static String stripGarbage(JSONArray results, String barcode) throws JSONException {
		
		String good = " abcdefghijklmnopqrstuvwxyz";
		final int MAX_TITLE_CONSIDER = 4;
		final int MAX_MISSING = 1;
		final int MIN_TITLE_CONSIDER = 2;
		
		// First gather the titles for the first MAX_TITLE_CONSIDER results
		List<String> titles = new ArrayList<String>();
		for (int i = 0; i < results.length() && i < MAX_TITLE_CONSIDER; i++) {
			
			String title = results.getJSONObject(i).getString("titleNoFormatting");

			// Make string lowercase first
			title = title.toLowerCase(Locale.US);
			
			// Remove the barcode number if it's there
			title = title.replace(barcode, "");
			
			// Remove unwanted words and HTML special chars
			for (String rem : new String[] { "dvd", "blu-ray", "bluray", "&amp;", "&quot;", "&apos;", "&lt;", "&gt;" }) {
				title = title.replace(rem, "");
			}
			
			// Remove all non-alphanumeric (and space) characters
			String result = "";
			for ( int j = 0; j < title.length(); j++ ) {
				if ( good.indexOf(title.charAt(j)) >= 0 )
					result += title.charAt(j);
			}
			
			// Remove double spaces
			while (result.contains("  ")) {
				result = result.replace("  ", " ");
			}
			
			titles.add(result);
			
		}

		// Only retain the words that are missing in at most one of the search result titles
		List<String> allWords = new ArrayList<String>();
		for (String title : titles) {
			for (String word : Arrays.asList(title.split(" "))) {
				if (!allWords.contains(word)) {
					allWords.add(word);
				}
			}
		}
		List<String> remainingWords = new ArrayList<String>();
		int allowMissing = Math.min(MAX_MISSING, Math.max(titles.size() - MIN_TITLE_CONSIDER, 0));
		for (String word : allWords) {

			int missing = 0;
			for (String title : titles) {
				if (!title.contains(word)) {
					// The word is not contained in this result title
					missing++;
					if (missing > allowMissing) {
						// Already misssing more than once, no need to look further
						break;
					}
				}
			}
			if (missing <= allowMissing) {
				// The word was only missing at most once, so we keep it
				remainingWords.add(word);
			}
		}
		
		// Now the query is the concatenation of the words remaining; with spaces in between
		String query = "";
		for (String word : remainingWords) {
			query += " " + word;
		}
		return query.length() > 0? query.substring(1): null;
		
	}

}
