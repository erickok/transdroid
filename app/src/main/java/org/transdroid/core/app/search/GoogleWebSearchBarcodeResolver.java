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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.daemon.util.HttpHelper;

import java.io.InputStream;
import java.util.Locale;

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
			String result = HttpHelper.convertStreamToString(instream);
			JSONArray results = new JSONObject(result).getJSONObject("responseData").getJSONArray("results");

			// Use the first result, if any, after cleaning it from special characters
			if (results.length() < 1) {
				return null;
			}
			return stripGarbage(results.getJSONObject(0), barcode);
		} catch (Exception e) {
			return null;
		}

	}

	private static String stripGarbage(JSONObject item, String barcode) throws JSONException {

		String good = " abcdefghijklmnopqrstuvwxyz1234567890";

		// Find the unformatted title
		String title = item.getString("titleNoFormatting");

		// Make string lowercase first
		title = title.toLowerCase(Locale.US);

		// Remove the barcode number if it's there
		title = title.replace(barcode, "");

		// Remove unwanted words and HTML special chars
		for (String rem : new String[]{"dvd", "blu-ray", "bluray", "&amp;", "&quot;", "&apos;", "&lt;", "&gt;"}) {
			title = title.replace(rem, "");
		}

		// Remove all non-alphanumeric (and space) characters
		String result = "";
		for (int j = 0; j < title.length(); j++) {
			if (good.indexOf(title.charAt(j)) >= 0) {
				result += title.charAt(j);
			}
		}

		// Remove double spaces
		while (result.contains("  ")) {
			result = result.replace("  ", " ");
		}

		return result;

	}

}
