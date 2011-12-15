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
 package org.transdroid.preferences;

import java.util.List;

import org.transdroid.R;
import org.transdroid.preferences.PreferencesAdapter.PreferencesListButton;
import org.transdroid.rss.RssFeedSettings;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Provides an activity to create and edit the RSS feeds.
 * 
 * @author erickok
 *
 */
public class PreferencesRss extends ListActivity {

	private static final int MENU_REMOVE_ID = 1;
	private static final int MENU_MOVEUP_ID = 2;
	private static final int MENU_MOVEDOWN_ID = 3;
	
	PreferencesAdapter adapter;
	int feedCount;
	SharedPreferences prefs;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
        // Make sure a context menu is created on long-presses
        registerForContextMenu(getListView());

        buildAdapter();
        
    }

	private void buildAdapter() {
		
        // Build a list of RSS feed settings objects to show
		List<RssFeedSettings> feeds = Preferences.readAllRssFeedSettings(prefs);
		feedCount = feeds.size();
		
        // Set the list items
        adapter = new PreferencesAdapter(this, feeds, Integer.MIN_VALUE);
        setListAdapter(adapter);
        
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Perform click action depending on the clicked list item (note that dividers are ignored)
    	Object item = getListAdapter().getItem(position);

    	// Handle button clicks first
    	if (item instanceof PreferencesListButton) {

    		PreferencesListButton button = (PreferencesListButton) item;
    		if (button.getKey().equals(PreferencesAdapter.ADD_NEW_RSSFEED)) {
	
				// What is the max current feed ID number?
				int max = 0;
				while (prefs.contains(Preferences.KEY_PREF_RSSURL + Integer.toString(max))) {
					max++;
				}
				
				// Start a new rss feed settings screen
	    		Intent i = new Intent(this, PreferencesRssFeed.class);
	    		i.putExtra(PreferencesRssFeed.PREFERENCES_FEED_KEY, Integer.toString(max));
	    		startActivityForResult(i, 0);
	
			} else if (button.getKey().equals(PreferencesAdapter.ADD_EZRSS_FEED)) {
	    		Intent i = new Intent(this, EzRssFeedBuilder.class);
	    		startActivityForResult(i, 0);
	
			}

    	} else if (item instanceof RssFeedSettings) { 
    		
    		RssFeedSettings feed = (RssFeedSettings) item;
    		// Open the feed settings edit activity for the clicked rss feed
    		Intent i = new Intent(this, PreferencesRssFeed.class);
    		i.putExtra(PreferencesRssFeed.PREFERENCES_FEED_KEY, feed.getKey());
    		startActivityForResult(i, 0);
    	}
		
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// One of the server settings has been updated: refresh the list
		buildAdapter();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		int id = (int) ((AdapterContextMenuInfo) item.getMenuInfo()).id;
		Object selected = adapter.getItem(id);
		if (selected instanceof RssFeedSettings) {
			
			if (item.getItemId() == MENU_REMOVE_ID) {

				// Remove this RSS feed and reload this screen
				Preferences.removeRssFeedSettings(prefs, (RssFeedSettings)selected);
				buildAdapter();
				return true;

			} else if (item.getItemId() == MENU_MOVEUP_ID) {

				// Move this RSS feed up and reload this screen
				Preferences.moveRssFeedSettings(prefs, (RssFeedSettings)selected, true);
				buildAdapter();
				return true;

			} else if (item.getItemId() == MENU_MOVEDOWN_ID) {

				// Move this RSS feed up and reload this screen
				Preferences.moveRssFeedSettings(prefs, (RssFeedSettings)selected, false);
				buildAdapter();
				return true;
				
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// Allow removing of daemon and site settings
		int id = (int) ((AdapterContextMenuInfo)menuInfo).id;
		Object item = adapter.getItem(id);

		// For RssFeedSettings, allow removing of the feed
		if (item instanceof RssFeedSettings) {
			menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
			// Allow moving up/down, if appropriate
			if (id > 0) {
				menu.add(0, MENU_MOVEUP_ID, 0, R.string.menu_moveup);
			}
			if (id < feedCount - 1) {
				menu.add(0, MENU_MOVEDOWN_ID, 0, R.string.menu_movedown);
			}
		}
		
	}

}
