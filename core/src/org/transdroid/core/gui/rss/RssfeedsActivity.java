package org.transdroid.core.gui.rss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.UiThread;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.*;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.RssParser;
import org.xml.sax.SAXException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

@EActivity(resName = "activity_rssfeeds")
public class RssfeedsActivity extends SherlockFragmentActivity {

	// Settings and local data
	@Bean
	protected ApplicationSettings applicationSettings;
	protected List<RssfeedLoader> loaders;

	// Contained feeds and items fragments
	@FragmentById(resName = "rssfeeds_list")
	protected RssfeedsFragment fragmentFeeds;
	@FragmentById(resName = "rssitems_list")
	protected RssitemsFragment fragmentItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
			getSupportActionBar().setIcon(R.drawable.ic_activity_torrents);
		}
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	protected void init() {
		// Simple action bar with up button and correct title font
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(getString(R.string.rss_feeds)));
	}

	@Override
	protected void onResume() {
		super.onResume();
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	/**
	 * Performs the loading of the RSS feed content and parsing of items, in a background thread.
	 * @param loader The RSS feed loader for which to retrieve the contents
	 */
	@Background
	protected void loadRssfeed(RssfeedLoader loader) {
		RssParser parser = new RssParser(loader.getSetting().getUrl());
		try {
			parser.parse();
			handleRssfeedResult(loader, parser.getChannel(), true);
		} catch (ParserConfigurationException e) {
			handleRssfeedResult(loader, null, true);
		} catch (SAXException e) {
			handleRssfeedResult(loader, null, true);
		} catch (IOException e) {
			handleRssfeedResult(loader, null, true);
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
	 * @param loader The RSS feed loader (with settings and the loaded content channel) to show
	 */
	public void openRssfeed(RssfeedLoader loader) {
		if (fragmentItems != null) {
			fragmentItems.update(loader.getChannel());
		} else {
			RssitemsActivity_.intent(this).rssfeed(loader.getChannel()).start();
		}
	}

}
