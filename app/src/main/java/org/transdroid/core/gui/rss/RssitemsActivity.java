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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.rssparser.Channel;

@EActivity(R.layout.activity_rssitems)
public class RssitemsActivity extends AppCompatActivity {

	@Extra
	protected Channel rssfeed = null;
	@Extra
	protected String rssfeedName;
	@Extra
	protected boolean requiresExternalAuthentication;

	@FragmentById(R.id.rssitems_fragment)
	protected RssitemsFragment fragmentItems;
	@ViewById
	protected Toolbar rssfeedsToolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
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

		setSupportActionBar(rssfeedsToolbar);
		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(rssfeedName));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Get the intent extras and show them to the already loaded fragment
		fragmentItems.update(rssfeed, false, requiresExternalAuthentication);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

}
