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
import org.transdroid.core.gui.TorrentsActivity;
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
 * An activity that holds a single torrents details fragment. It is used on devices (i.e. phones) where there is no room to show details in the {@link
 * TorrentsActivity} directly. Task execution, such as loading of more details and updating file priorities, is performed in this activity via
 * background methods.
 * @author Eric Kok
 */
@EActivity(R.layout.activity_remoterss)
//@OptionsMenu(R.menu.activity_details)
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
			finish();
			return;
		}

		// Simple action bar with up, torrent name as title and refresh button
		torrentsToolbar.setNavigationIcon(R.drawable.ic_action_drawer);
		setSupportActionBar(torrentsToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(torrent.getName()));

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);

		// Show all items
		showRecentItems();

		// Fill in the filter list
		showChannelFilters();
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

		fragmentRemoteRss.updateTorrentFiles(recentItems);
	}

	protected void showChannelFilters() {
		List<RemoteRssChannel> feedLabels = new ArrayList<>(feeds.size() +1);
		feedLabels.add(new RemoteRssChannel() {
			@Override
			public String getName() {
				return "(All recent)";
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
			fragmentRemoteRss.updateTorrentFiles(feeds.get(position -1).getItems());
		}

		drawerLayout.closeDrawers();
	}

	public IDaemonAdapter getCurrentConnection() {
		return currentConnection;
	}
}
