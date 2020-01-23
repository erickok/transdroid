/* 
 * Copyright 2010-2018 Eric Kok et al.
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.NonConfigurationInstance;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.remoterss.RemoteRssFragment;
import org.transdroid.core.gui.remoterss.data.RemoteRssChannel;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;
import org.transdroid.core.gui.remoterss.data.RemoteRssSupplier;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.RssParser;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@EActivity(R.layout.activity_rssfeeds)
public class RssFeedsActivity extends AppCompatActivity {

	// Settings and local data
	@Bean
	protected Log log;
	@Bean
	protected ApplicationSettings applicationSettings;

	protected static final int RSS_FEEDS_LOCAL = 0;
	protected static final int RSS_FEEDS_REMOTE = 1;

	@FragmentById(R.id.rssfeeds_fragment)
	protected RssFeedsFragment fragmentLocalFeeds;
	@FragmentById(R.id.rssitems_fragment)
	protected RssItemsFragment fragmentItems;
	@FragmentById(R.id.remoterss_fragment)
	protected RemoteRssFragment fragmentRemoteFeeds;

	@ViewById(R.id.rssfeeds_toolbar)
	protected Toolbar rssFeedsToolbar;
	@ViewById(R.id.rssfeeds_tabs)
	protected TabLayout tabLayout;
	@ViewById(R.id.rssfeeds_pager)
	protected ViewPager viewPager;

	// remote RSS stuff
	@NonConfigurationInstance
	protected ArrayList<RemoteRssChannel> feeds;
	@InstanceState
	protected int selectedFilter;
	@NonConfigurationInstance
	protected ArrayList<RemoteRssItem> recentItems;
	@Bean
	protected ConnectivityHelper connectivityHelper;


	protected class LayoutPagerAdapter extends PagerAdapter {
		boolean hasRemoteRss;
		String serverName;

		public LayoutPagerAdapter(boolean hasRemoteRss, String name) {
			super();

			this.hasRemoteRss = hasRemoteRss;
			this.serverName = (name.length() > 0 ? name : getString(R.string.navigation_rss_tabs_remote));
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position) {
			int resId = 0;

			if (position == RSS_FEEDS_LOCAL) {
				resId = R.id.layout_rssfeeds_local;
			}
			else if (position == RSS_FEEDS_REMOTE) {
				resId = R.id.layout_rss_feeds_remote;
			}

			return findViewById(resId);
		}

		@Override
		public int getCount() {
			return (this.hasRemoteRss ? 2 : 1);
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
			return (view == o);
		}

		@Override
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			container.removeView((View) object);
		}

		@Nullable
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case RSS_FEEDS_LOCAL:
					return getString(R.string.navigation_rss_tabs_local);
				case RSS_FEEDS_REMOTE:
					return this.serverName;
			}

			return super.getPageTitle(position);
		}
	}

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
		setSupportActionBar(rssFeedsToolbar);
		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(getString(R.string.rss_feeds)));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		IDaemonAdapter currentConnection = this.getCurrentConnection();
		boolean hasRemoteRss = Daemon.supportsRemoteRssManagement(currentConnection.getType());

		PagerAdapter pagerAdapter = new LayoutPagerAdapter(hasRemoteRss, currentConnection.getSettings().getName());
		viewPager.setAdapter(pagerAdapter);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.setCurrentItem(0);

		if (!hasRemoteRss) {
			tabLayout.setVisibility(View.GONE);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	/**
	 * Reload the RSS feed settings and start loading all the feeds. To be called from contained fragments.
	 */
	public void refreshFeeds() {
		List<RssfeedLoader> loaders = new ArrayList<>();
		// For each RSS feed setting the user created, start a loader that retrieved the RSS feed (via a background
		// thread) and, on success, determines the new items in the feed
		for (RssfeedSetting setting : applicationSettings.getRssfeedSettings()) {
			RssfeedLoader loader = new RssfeedLoader(setting);
			loaders.add(loader);
			loadRssfeed(loader);
		}

		fragmentLocalFeeds.update(loaders);
	}

	/**
	 * Performs the loading of the RSS feed content and parsing of items, in a background thread.
	 * @param loader The RSS feed loader for which to retrieve the contents
	 */
	@Background
	protected void loadRssfeed(RssfeedLoader loader) {
		try {
			// Load and parse the feed
			RssParser parser =
					new RssParser(loader.getSetting().getUrl(), loader.getSetting().getExcludeFilter(), loader.getSetting().getIncludeFilter());
			parser.parse();
			handleRssfeedResult(loader, parser.getChannel(), false);
		} catch (Exception e) {
			// Catch any error that may occurred and register this failure
			handleRssfeedResult(loader, null, true);
			log.i(this, "RSS feed " + loader.getSetting().getUrl() + " error: " + e.toString());
		}
	}

	/**
	 * Stores the retrieved RSS feed content channel into the loader and updates the RSS feed in the feeds list fragment.
	 * @param loader The RSS feed loader that was executed
	 * @param channel The data that was retrieved, or null if it could not be parsed
	 * @param hasError True if a connection error occurred in the loading of the feed; false otherwise
	 */
	@UiThread
	protected void handleRssfeedResult(RssfeedLoader loader, Channel channel, boolean hasError) {
		loader.update(channel, hasError);

		fragmentLocalFeeds.notifyDataSetChanged();
	}

	/**
	 * Opens an RSS feed in the dedicated fragment (if there was space in the UI) or a new {@link RssItemsActivity}. Optionally this also registers in
	 * the user preferences that the feed was now viewed, so that in the future the new items can be properly marked.
	 * @param loader The RSS feed loader (with settings and the loaded content channel) to show
	 * @param markAsViewedNow True if the user settings should be updated to reflect this feed's last viewed date; false otherwise
	 */
	public void openRssfeed(RssfeedLoader loader, boolean markAsViewedNow) {

		// The RSS feed content was loaded and can now be shown in the dedicated fragment or a new activity
		if (fragmentItems != null && fragmentItems.isAdded()) {

			// If desired, update the lastViewedDate and lastViewedItemUrl of this feed in the user setting; this won't
			// be loaded until the RSS feeds screen in opened again.
			if (!loader.hasError() && loader.getChannel() != null && markAsViewedNow) {
				String lastViewedItemUrl = null;
				if (loader.getChannel().getItems() != null && loader.getChannel().getItems().size() > 0) {
					lastViewedItemUrl = loader.getChannel().getItems().get(0).getTheLink();
				}
				applicationSettings.setRssfeedLastViewer(loader.getSetting().getOrder(), new Date(), lastViewedItemUrl);
			}
			fragmentItems.update(loader.getChannel(), loader.hasError(), loader.getSetting().requiresExternalAuthentication());

		} else {

			// Error message or not yet loaded? Show a toast message instead of opening the items activity
			if (loader.hasError()) {
				SnackbarManager.show(Snackbar.with(this).text(R.string.rss_error).colorResource(R.color.red));
				return;
			}
			if (loader.getChannel() == null || loader.getChannel().getItems().size() == 0) {
				SnackbarManager.show(Snackbar.with(this).text(R.string.rss_notloaded).colorResource(R.color.red));
				return;
			}

			// If desired, update the lastViewedDate and lastViewedItemUrl of this feed in the user setting; this won't
			// be loaded until the RSS feeds screen in opened again
			if (markAsViewedNow) {
				String lastViewedItemUrl = null;
				if (loader.getChannel().getItems() != null && loader.getChannel().getItems().size() > 0) {
					lastViewedItemUrl = loader.getChannel().getItems().get(0).getTheLink();
				}
				applicationSettings.setRssfeedLastViewer(loader.getSetting().getOrder(), new Date(), lastViewedItemUrl);
			}

			String name = loader.getChannel().getTitle();
			if (TextUtils.isEmpty(name)) {
				name = loader.getSetting().getName();
			}
			if (TextUtils.isEmpty(name) && !TextUtils.isEmpty(loader.getSetting().getUrl())) {
				name = Uri.parse(loader.getSetting().getUrl()).getHost();
			}
			RssItemsActivity_.intent(this).rssfeed(loader.getChannel()).rssfeedName(name)
					.requiresExternalAuthentication(loader.getSetting().requiresExternalAuthentication()).start();

		}
	}

	protected IDaemonAdapter getCurrentConnection() {
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		return lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
	}

    // @Background
	public void refreshRemoteFeeds() {
		// Connect to the last used server
		IDaemonAdapter currentConnection = this.getCurrentConnection();

		// remote rss not supported for this connection type
		if (currentConnection instanceof RemoteRssSupplier == false) {
			return;
		}

		try {
			feeds = ((RemoteRssSupplier) (currentConnection)).getRemoteRssChannels(log);

			//  By default it displays the latest items within the last month.
			recentItems = new ArrayList<>();
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, -1);
			Date oneMonthAgo = calendar.getTime();

			for (RemoteRssChannel feed : feeds) {
				for (RemoteRssItem item : feed.getItems()) {
					if (item.getTimestamp().after(oneMonthAgo)) {
						recentItems.add(item);
					}
				}
			}

			// Sort by -newest
			Collections.sort(recentItems, new Comparator<RemoteRssItem>() {
				@Override
				public int compare(RemoteRssItem lhs, RemoteRssItem rhs) {
					return rhs.getTimestamp().compareTo(lhs.getTimestamp());
				}
			});
		} catch (DaemonException e) {
			onCommunicationError(e);
			return;
		}

		// @UIThread
		fragmentRemoteFeeds.updateRemoteItems(
			selectedFilter == 0 ? recentItems : feeds.get(selectedFilter -1).getItems(),
			false /* allow android to restore scroll position */ );
		showRemoteChannelFilters();
	}

	@UiThread
	protected void onCommunicationError(DaemonException daemonException) {
		//noinspection ThrowableResultOfMethodCallIgnored
		log.i(this, daemonException.toString());
		String error = getString(LocalTorrent.getResourceForDaemonException(daemonException));
		SnackbarManager.show(Snackbar.with(this).text(error).colorResource(R.color.red).type(SnackbarType.MULTI_LINE));
	}


	public void onFeedSelected(int position) {
		selectedFilter = position;

		if (position == 0) {
			fragmentRemoteFeeds.updateRemoteItems(recentItems, true);
		}
		else {
			RemoteRssChannel channel = feeds.get(selectedFilter -1);
			fragmentRemoteFeeds.updateRemoteItems(channel.getItems(), true);
		}
	}

	/**
	 * Download the item in a background thread and display success/fail accordingly.
	 */
	@Background
	public void downloadRemoteRssItem(RemoteRssItem item) {
		final RemoteRssSupplier supplier = (RemoteRssSupplier) this.getCurrentConnection();

		try {
			RemoteRssChannel channel = feeds.get(selectedFilter);
			supplier.downloadRemoteRssItem(log, item, channel);
			onTaskSucceeded(null, getString(R.string.result_added, item.getTitle()));
		} catch (DaemonException e) {
			onTaskFailed(getString(LocalTorrent.getResourceForDaemonException(e)));
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, String successMessage) {
		SnackbarManager.show(Snackbar.with(this).text(successMessage));
	}

	@UiThread
	protected void onTaskFailed(String message) {
		SnackbarManager.show(Snackbar.with(this)
			.text(message)
			.colorResource(R.color.red)
			.type(SnackbarType.MULTI_LINE)
		);
	}

	private void showRemoteChannelFilters() {
		List<RemoteRssChannel> feedLabels = new ArrayList<>(feeds.size() +1);
		feedLabels.add(new RemoteRssChannel() {
			@Override
			public String getName() {
				return getString(R.string.remoterss_filter_allrecent);
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
			}
		});
		feedLabels.addAll(feeds);

		fragmentRemoteFeeds.updateChannelFilters(feedLabels);
	}
}
