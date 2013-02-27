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
package org.transdroid.gui.rss;

import org.transdroid.R;
import org.transdroid.preferences.Preferences;
import org.transdroid.rss.RssFeedSettings;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class RssListing extends SherlockFragmentActivity {

	public static final String RSSFEED_LISTING_KEY = "RSSFEED_LISTING_KEY";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rsslisting);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
		if (savedInstanceState == null) {
			// Get the ID of the RSS feed to load
			String postfix = getIntent().getStringExtra(RSSFEED_LISTING_KEY);		
			// Get settings form user preferences
			RssFeedSettings feedSettings = Preferences.readRssFeedSettings(PreferenceManager.getDefaultSharedPreferences(this), postfix);

			// Start the fragment for this torrent
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.items, new RssListingFragment(feedSettings));
			ft.commit();
		}

	}

}