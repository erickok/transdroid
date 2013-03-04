package org.transdroid.lite.gui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ItemSelect;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.lite.app.settings.ApplicationSettings;
import org.transdroid.lite.gui.navigation.FilterAdapter;
import org.transdroid.lite.gui.navigation.FilterItem;
import org.transdroid.lite.gui.navigation.NavigationHelper;
import org.transdroid.lite.gui.navigation.StatusType;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.SherlockListView;

@EActivity(R.layout.activity_torrents)
@OptionsMenu(R.menu.activity_torrents)
public class TorrentsActivity extends SherlockFragmentActivity implements OnNavigationListener {

	// Navigation components
	@Bean
	protected NavigationHelper navigationHelper;
	@ViewById
	protected SherlockListView filtersList;
	protected FilterAdapter navigationListAdapter = null;
	protected FilterAdapter navigationSpinnerAdapter = null;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	
	// Torrents list components
	@FragmentById(R.id.torrent_list)
	protected TorrentsFragment fragmentTorrents;
	
	// Details view components
	@FragmentById(R.id.torrent_details)
	protected DetailsFagment fragmentDetails;
	
	@AfterViews
	protected void init() {

		// Set up navigation, with an action bar spinner and possibly (if room) with a filter list
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setHomeButtonEnabled(false);
		navigationSpinnerAdapter = new FilterAdapter(this);
		// Servers are always added to the action bar spinner
		navigationSpinnerAdapter.updateServers(applicationSettings.getServerSettings());
		getSupportActionBar().setListNavigationCallbacks(navigationSpinnerAdapter, this);
		if (filtersList != null) {
			// There was room for a dedicated filter list; add the status types
			navigationListAdapter = new FilterAdapter(this);
			filtersList.setAdapter(navigationListAdapter);
			navigationListAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
		} else {
			// Add status types directly to the action bar spinner
			navigationSpinnerAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
		}
		
		
	}

	/**
	 * Called when an item in the action bar navigation spinner was selected
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		Object item = navigationSpinnerAdapter.getItem(itemPosition);
		if (item instanceof FilterItem) {
			// A filter item was selected form the navigation spinner
			filterSelected(true, (FilterItem) item);
			return true;
		}
		// A header was selected; no action
		return false;
	}
	
	/**
	 * A new filter was selected; update the view over the current data
	 * @param selected True if the filter item was selected, false if it was deselected
	 * @param item The touched filter item
	 */
	@ItemSelect(R.id.filters_list)
	protected void filterSelected(boolean selected, FilterItem item) {
		// TODO: Update the torrent list view
	}
	
}
