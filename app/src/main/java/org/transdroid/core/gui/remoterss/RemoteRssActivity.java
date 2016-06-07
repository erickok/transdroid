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
package org.transdroid.core.gui.remoterss;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.lists.SimpleListItemAdapter;
import org.transdroid.core.gui.remoterss.data.RemoteRssChannel;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.daemon.IDaemonAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * An activity that displays a list of {@link RemoteRssItem}s via an instance of {@link RemoteRssFragment}.
 * The activity manages the drawer to filter items by the feed they came through.
 *
 * By default it displays the latest items within the last month.
 *
 * @author Twig Nguyen
 */
@EActivity(R.layout.activity_remoterss)
public class RemoteRssActivity extends AppCompatActivity {
	@Extra
	@InstanceState
	protected ArrayList<RemoteRssChannel> feeds;

	@InstanceState
	protected ArrayList<RemoteRssItem> recentItems;

	// Server connection
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	private IDaemonAdapter currentConnection;

	// Details view components
	@ViewById
	protected DrawerLayout drawerLayout;
	@ViewById
	protected LinearLayout drawerContainer;

	@ViewById
	protected Toolbar torrentsToolbar;

	@ViewById
	protected ListView drawerList;

	@FragmentById(R.id.remoterss_fragment)
	protected RemoteRssFragment fragmentRemoteRss;

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

		// We require feeds to be specified; otherwise close the activity
		if (feeds == null) {
			feeds = new ArrayList<>();
		}

		// Simple action bar with up, torrent name as title and refresh button
		torrentsToolbar.setNavigationIcon(R.drawable.ic_action_drawer);
		setSupportActionBar(torrentsToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);

		// Fill in the filter list
		showChannelFilters();

		// Show all items
		showRecentItems();
	}


	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		if (drawerLayout.isDrawerOpen(drawerContainer)) {
			drawerLayout.closeDrawers();
		} else {
			drawerLayout.openDrawer(drawerContainer);
		}
	}

	@Override
	public void onBackPressed() {
		if (drawerLayout.isDrawerOpen(drawerContainer)) {
			drawerLayout.closeDrawers();
		} else {
			finish();
		}
	}

	protected void showRecentItems() {
		if (recentItems == null) {
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
		}

		fragmentRemoteRss.updateRemoteItems(recentItems);
		RemoteRssChannel channel = (RemoteRssChannel) drawerList.getAdapter().getItem(0);
		getSupportActionBar().setSubtitle(channel.getName());
	}

	protected void showChannelFilters() {
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

		drawerList.setAdapter(new SimpleListItemAdapter(this, feedLabels));
	}

	@ItemClick(R.id.drawer_list)
	protected void onFeedSelected(int position) {
		if (position == 0) {
			showRecentItems();
		}
		else {
			fragmentRemoteRss.updateRemoteItems(feeds.get(position -1).getItems());
		}

		RemoteRssChannel channel = (RemoteRssChannel) drawerList.getAdapter().getItem(position);
		getSupportActionBar().setSubtitle(channel.getName());

		drawerLayout.closeDrawers();
	}

	public IDaemonAdapter getCurrentConnection() {
		return currentConnection;
	}
}
