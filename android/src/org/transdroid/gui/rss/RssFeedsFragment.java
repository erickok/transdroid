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

import java.util.List;

import org.transdroid.R;
import org.transdroid.gui.Transdroid;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.PreferencesRss;
import org.transdroid.rss.RssFeedSettings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class RssFeedsFragment extends Fragment {

	// private static final String LOG_NAME = "Transdroid RSS feeds";

	private static final int ACTIVITY_PREFERENCES = 0;
	private static final int MENU_REFRESH_ID = 1;
	private static final int MENU_SETTINGS_ID = 2;

	protected boolean useTabletInterface;
	
	public RssFeedsFragment() {
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_rssfeeds, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		useTabletInterface = Transdroid.isTablet(getResources());
		registerForContextMenu(getView().findViewById(android.R.id.list));
		getSupportActivity().getSupportActionBar().setTitle(R.string.rss);
		getListView().setOnItemClickListener(onFeedClicked);

		getSupportActivity().setTitle(R.string.rss);
		
		loadFeeds();

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Add title bar buttons
		MenuItem miRefresh = menu.add(0, MENU_REFRESH_ID, 0, R.string.refresh);
		miRefresh.setIcon(R.drawable.icon_refresh_title);
		miRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		// Add the settings button
		MenuItem miSettings = menu.add(0, MENU_SETTINGS_ID, 0,
				R.string.menu_settings);
		miSettings.setIcon(android.R.drawable.ic_menu_preferences);
	}

	private void loadFeeds() {

		// Read the feed settings from the user preferences
		// Note that the 'number of new items' is retrieved asynchronously from
		// within the RssFeedListAdapter
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		List<RssFeedSettings> feeds = Preferences.readAllRssFeedSettings(prefs);
		setListAdapter(new RssFeedListAdapter(getActivity(), feeds));

	}

	private OnItemClickListener onFeedClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	    	// Show the feed items in the right of the screen (tablet interface) or separately
	    	if (useTabletInterface) {
	    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
	    		ft.replace(R.id.listing, new RssListingFragment(getListAdapter().getItem(position)));
	    		ft.commit();
	    	} else {
				// Open the listing for the clicked RSS feed
				String postfix = getListAdapter().getItem(position).getKey();
				Intent i = new Intent(getActivity(), RssListing.class);
				i.putExtra(RssListing.RSSFEED_LISTING_KEY, postfix);
				startActivity(i);
	    	}
			
		}
	};

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH_ID:
			loadFeeds();
			break;
		case MENU_SETTINGS_ID:
			startActivityForResult(new Intent(getActivity(),
					PreferencesRss.class), ACTIVITY_PREFERENCES);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_PREFERENCES:

			// Preference screen was called: use new preferences to show new
			// feeds
			loadFeeds();
			break;
		}
	}

	protected ListView getListView() {
		return (ListView) getView().findViewById(android.R.id.list);
	}

	protected RssFeedListAdapter getListAdapter() {
		return (RssFeedListAdapter) getListView().getAdapter();
	}
	
	private View getEmptyText() {
		return getView().findViewById(android.R.id.empty);
	}

	private void setListAdapter(RssFeedListAdapter rssFeedListAdapter) {
		getListView().setAdapter(rssFeedListAdapter);
		if (rssFeedListAdapter == null || rssFeedListAdapter.getCount() <= 0) {
			getListView().setVisibility(View.GONE);
			getEmptyText().setVisibility(View.VISIBLE);
		} else {
			getListView().setVisibility(View.VISIBLE);
			getEmptyText().setVisibility(View.GONE);
		}
	}

}
