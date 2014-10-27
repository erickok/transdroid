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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.UiThread;
import org.transdroid.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.*;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.RssParser;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import de.keyboardsurfer.android.widget.crouton.Crouton;

@EActivity(resName = "activity_rssfeeds")
public class RssfeedsActivity extends Activity {

	// Settings and local data
	@Bean
	protected ApplicationSettings applicationSettings;
	protected List<RssfeedLoader> loaders;

	// Contained feeds and items fragments
	@FragmentById(resName = "rssfeeds_fragment")
	protected RssfeedsFragment fragmentFeeds;
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
		// Simple action bar with up button and correct title font
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(NavigationHelper.buildCondensedFontString(getString(R.string.rss_feeds)));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshFeeds();
	}

	/**
	 * Reload the RSS feed settings and start loading all the feeds. To be called from contained fragments.
	 */
	public void refreshFeeds() {
		loaders = new ArrayList<RssfeedLoader>();
		// For each RSS feed setting the user created, start a loader that retrieved the RSS feed (via a background
		// thread) and, on success, determines the new items in the feed
		for (RssfeedSetting setting : applicationSettings.getRssfeedSettings()) {
			RssfeedLoader loader = new RssfeedLoader(setting);
			loaders.add(loader);
			loadRssfeed(loader);
		}
		fragmentFeeds.update(loaders);
	}

	/**
	 * Performs the loading of the RSS feed content and parsing of items, in a background thread.
	 * @param loader The RSS feed loader for which to retrieve the contents
	 */
	@Background
	protected void loadRssfeed(RssfeedLoader loader) {
		try {
			// Load and parse the feed
			RssParser parser = new RssParser(loader.getSetting().getUrl());
			parser.parse();
			handleRssfeedResult(loader, parser.getChannel(), false);
		} catch (Exception e) {
			// Catch any error that may occurred and register this failure
			handleRssfeedResult(loader, null, true);
			Log.i(this, "RSS feed " + loader.getSetting().getUrl() + " error: " + e.toString());
		}

	}

	/**
	 * Stores the retrieved RSS feed content channel into the loader and updates the RSS feed in the feeds list
	 * fragment.
	 * @param loader The RSS feed loader that was executed
	 * @param channel The data that was retrieved, or null if it could not be parsed
	 * @param hasError True if a connection error occurred in the loading of the feed; false otherwise
	 */
	@UiThread
	protected void handleRssfeedResult(RssfeedLoader loader, Channel channel, boolean hasError) {
		loader.update(channel, hasError);
		fragmentFeeds.notifyDataSetChanged();
	}

	/**
	 * Opens an RSS feed in the dedicated fragment (if there was space in the UI) or a new {@link RssitemsActivity}.
	 * Optionally this also registers in the user preferences that the feed was now viewed, so that in the future the
	 * new items can be properly marked.
	 * @param loader The RSS feed loader (with settings and the loaded content channel) to show
	 * @param markAsViewedNow True if the user settings should be updated to reflect this feed's last viewed date; false
	 *            otherwise
	 */
	public void openRssfeed(RssfeedLoader loader, boolean markAsViewedNow) {

		// The RSS feed content was loaded and can now be shown in the dedicated fragment or a new activity
		if (fragmentItems != null && fragmentItems.isAdded()) {

			// If desired, update the lastViewedDate and lastViewedItemUrl of this feed in the user setting; this won't
			// be loaded until the RSS feeds screen in opened again.
			if (!loader.hasError() && loader.getChannel() != null && markAsViewedNow) {
				String lastViewedItemUrl = null;
				if (loader.getChannel().getItems() != null && loader.getChannel().getItems().size() > 0)
					lastViewedItemUrl = loader.getChannel().getItems().get(0).getTheLink();
				applicationSettings.setRssfeedLastViewer(loader.getSetting().getOrder(), new Date(), lastViewedItemUrl);
			}
			fragmentItems.update(loader.getChannel(), loader.hasError());

		} else {

			// Error message or not yet loaded? Show a toast message instead of opening the items activity
			if (loader.hasError()) {
				Crouton.showText(this, R.string.rss_error, NavigationHelper.CROUTON_INFO_STYLE);
				return;
			}
			if (loader.getChannel() == null || loader.getChannel().getItems().size() == 0) {
				Crouton.showText(this, R.string.rss_notloaded, NavigationHelper.CROUTON_INFO_STYLE);
				return;
			}

			// If desired, update the lastViewedDate and lastViewedItemUrl of this feed in the user setting; this won't
			// be loaded until the RSS feeds screen in opened again
			if (markAsViewedNow) {
				String lastViewedItemUrl = null;
				if (loader.getChannel().getItems() != null && loader.getChannel().getItems().size() > 0)
					lastViewedItemUrl = loader.getChannel().getItems().get(0).getTheLink();
				applicationSettings.setRssfeedLastViewer(loader.getSetting().getOrder(), new Date(), lastViewedItemUrl);
			}

			String name = loader.getChannel().getTitle();
			if (TextUtils.isEmpty(name))
				name = loader.getSetting().getName();
			if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(loader.getSetting().getUrl())) {
				String host = Uri.parse(loader.getSetting().getUrl()).getHost();
				name = host;
			}
			RssitemsActivity_.intent(this).rssfeed(loader.getChannel()).rssfeedName(name).start();

		}

	}

}
