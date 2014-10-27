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
package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.transdroid.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.*;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.rssparser.Channel;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

@EActivity(resName = "activity_rssitems")
public class RssitemsActivity extends Activity {

	@Extra
	protected Channel rssfeed = null;
	@Extra
	protected String rssfeedName;

	@FragmentById(resName = "rssitems_fragment")
	protected RssitemsFragment fragmentItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
			getActionBar().setIcon(R.drawable.ic_activity_torrents);
		}
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	protected void init() {

		// We require an RSS feed to be specified; otherwise close the activity
		if (rssfeed == null) {
			finish();
			return;
		}

		// Simple action bar with up button and torrent name as title
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(NavigationHelper.buildCondensedFontString(rssfeedName));

		// Get the intent extras and show them to the already loaded fragment
		fragmentItems.update(rssfeed, false);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

}
