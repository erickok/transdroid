package org.transdroid.lite.gui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.FragmentByTag;
import org.androidannotations.annotations.ItemSelect;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.lite.R;
import org.transdroid.lite.gui.navigation.FilterAdapter;
import org.transdroid.lite.gui.navigation.FilterItem;
import org.transdroid.lite.gui.navigation.FilterSeparatorView;
import org.transdroid.lite.gui.navigation.NavigationHelper;

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
	
	// Torrents list components
	@FragmentById(R.id.torrent_list)
	protected TorrentsFragment fragmentTorrents;
	
	// Details view components
	@FragmentById(R.id.torrent_details)
	protected DetailsFagment fragmentDetails;
	
	@AfterViews
	protected void init() {

		// Set up navigation
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setHomeButtonEnabled(false);
		navigationSpinnerAdapter = new FilterAdapter(this);
		getSupportActionBar().setListNavigationCallbacks(navigationSpinnerAdapter, this);
		if (filtersList != null) {
			navigationListAdapter = new FilterAdapter(this);
			filtersList.setAdapter(navigationListAdapter);
		}
		
		// Load settings
		
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
	 * @param selected True if
	 * @param item
	 */
	@ItemSelect(R.id.filters_list)
	protected void filterSelected(boolean selected, FilterItem item) {
		// TODO: Update the view
	}
	
}
