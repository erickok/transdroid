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
package org.transdroid.gui.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.transdroid.R;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.gui.Torrents;
import org.transdroid.gui.Transdroid;
import org.transdroid.gui.util.ActivityUtil;
import org.transdroid.gui.util.SelectableArrayAdapter.OnSelectedChangedListener;
import org.transdroid.preferences.Preferences;
import org.transdroid.util.TLog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.OnNavigationListener;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Provides an activity that allows a search to be performed on a search engine and 
 * lists the results. Individual results can be browsed to using the web browser or the URL
 * is forwarded to the Torrents activity. For web searches, the browser is started at
 * the appropriate search URL. In-app searches are performed with the Transdroid Torrent 
 * Search project's public cursor adapters.
 *  
 * @author erickok
 *
 */
public class Search extends FragmentActivity implements OnTouchListener, OnSelectedChangedListener {

	private static final String LOG_NAME = "Search";
	private final static Uri TTS_MARKET_URI = Uri.parse("market://search?q=pname:org.transdroid.search");
	
	private static final int MENU_REFRESH_ID = 1;
	private static final int MENU_SEARCH_ID = 2;
	private static final int MENU_SITES_ID = 3;
	private static final int MENU_SAVESEARCH_ID = 4;
	
	private static final int SEARCHMENU_ADD_ID = 10;
	private static final int SEARCHMENU_DETAILS_ID = 11;
	private static final int SEARCHMENU_OPENWITH_ID = 12;
	private static final int SEARCHMENU_SHARELINK_ID = 13;

	private static final int DIALOG_SITES = 1;
	private static final int DIALOG_INSTALLSEARCH = 2;

	private TextView empty;
	private LinearLayout addSelected;
	private Button addSelectedButton;
	private SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
			TorrentSearchHistoryProvider.AUTHORITY, TorrentSearchHistoryProvider.MODE);
	private GestureDetector gestureDetector;
	
	private List<SiteSettings> allSites;
	private SiteSettings defaultSite;
	private SearchSettings searchSettings;
	private String query;

	private boolean inProgress = false;
	private boolean disableListNavigation = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        registerForContextMenu(findViewById(R.id.results));
        getListView().setTextFilterEnabled(true);
        getListView().setOnItemClickListener(onSearchResultSelected);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        empty = (TextView) findViewById(android.R.id.empty);
        addSelected = (LinearLayout) findViewById(R.id.add_selected);
        addSelectedButton = (Button) findViewById(R.id.add_selectedbutton);
        addSelectedButton.setOnClickListener(addSelectedClicked);
        // Swiping or flinging between server configurations
        gestureDetector = new GestureDetector(new SearchScreenGestureListener());
        getListView().setOnTouchListener(this);
        
        handleIntent(getIntent());
        
    }

    private SpinnerAdapter buildProvidersAdapter() {
    	ArrayAdapter<String> ad = new ArrayAdapter<String>(this, R.layout.abs__simple_spinner_item, buildSiteTextsForDialog());
    	ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	return ad;
	}

	/** Called when a new intent is delivered */
    @Override
    public void onNewIntent(final Intent newIntent) {
        super.onNewIntent(newIntent);
        
        handleIntent(newIntent);
    }
    
    private void handleIntent(Intent startIntent) {

    	// Get the query string from the intent
    	String thequery = getQuery(startIntent);

    	// Is this actually a full HTTP URL?
		if (thequery != null && (thequery.startsWith(HttpHelper.SCHEME_HTTP) || 
			thequery.startsWith(HttpHelper.SCHEME_MAGNET) || thequery.startsWith(HttpHelper.SCHEME_FILE))) {
			
			// Redirect this request to the main screen to add the URL directly
			Intent i = new Intent(this, Torrents.class);
			i.setData(Uri.parse(thequery));
			startActivity(i);
			finish();
			return;
			
		}
    	
		// Check if Transdroid Torrent Search is installed
		if (!TorrentSearchTask.isTorrentSearchInstalled(this)) {
			showDialog(DIALOG_INSTALLSEARCH);
			empty.setText("");
			return;
		}
		
    	// Load preferences
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	defaultSite = Preferences.readDefaultSearchSiteSettings(prefs);
    	allSites = Preferences.readAllSiteSettings(prefs);
    	searchSettings = Preferences.readSearchSettings(prefs);

    	// Update selection spinner
		disableListNavigation = true;
    	getSupportActionBar().setListNavigationCallbacks(buildProvidersAdapter(), onProviderSelected);

    	// Switch to a certain site?
    	if (startIntent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
    		String switchToKey = startIntent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
    		// See if this site (key) exists
            for (SiteSettings site : allSites) {
            	if (site.getKey().equals(switchToKey)) {
            		// If it is different than the last used (default) site, switch to it
            		if (!defaultSite.getKey().equals(switchToKey)) {
        				// Set the default site search to this new site
        	    		Preferences.storeLastUsedSearchSiteSettings(getApplicationContext(), switchToKey);
        				defaultSite = allSites.get(siteSettingsIndex(site));
            		}
            		break;
            	}
            }
    		
    	}

    	if (defaultSite == null) {
    		return;
    	}

    	// Start a web search?
    	if (defaultSite.isWebSearch()) {
    		
    		if (thequery == null || thequery.equals("")) {
    			// No search term was provided: show a message and close this search activity
    			Toast.makeText(this, R.string.no_query, Toast.LENGTH_LONG).show();
        		finish();
    			return;
    		}
    		
    		doWebSearch(defaultSite, thequery);
    		finish();
    		return;
    		
    	}

    	// Select the now-selected site in the spinner
    	getSupportActionBar().setSelectedNavigationItem(siteSettingsIndex(defaultSite));
		disableListNavigation = false;

    	// Normal in-app search
    	query = thequery;
        handleQuery();
        
    }
    
    private void doWebSearch(SiteSettings thesite, String thequery) {

		// Check for a valid url
		Uri uri = Uri.parse(thesite.getSubstitutedUrl(thequery));
		if (uri == null || uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
			// The url doesn't even have a http or https scheme, so the intent will fail
			Toast.makeText(this, getResources().getText(R.string.error_invalid_search_url) + " " + uri.toString(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// Do not load this activity, but immediately start a web search via a new Intent
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
		
    }

	/**
	 * Extracts the query string from the search Intent
	 * @return The string that was entered by the user
	 */
	private String getQuery(Intent intent) {
		
		// Extract string from Intent
		String query = null;
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			query = intent.getStringExtra(SearchManager.QUERY);
		} else if (intent.getAction().equals(Intent.ACTION_SEND)) {
			query = SendIntentHelper.cleanUpText(intent);
		}
		if (query != null && query.length() > 0) {
        	
        	// Remember this search query to later show as a suggestion
        	suggestions.saveRecentQuery(query, null);        	
        	return query;
        	
        }        
        return null;
        
	}
	
	/**
	 * Actually calls the search on the local preset query (if not empty)
	 */
	private void handleQuery() {
		
		if (query == null || query.equals("")) {
        	// No search (activity was started incorrectly or query was empty)
        	empty.setText(R.string.no_query);
        	
        	// Provide search input
        	onSearchRequested();
        	return;
		}

    	// Execute search
		doSearch(query);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.add(0, SEARCHMENU_ADD_ID, 0, R.string.searchmenu_add);
		menu.add(0, SEARCHMENU_DETAILS_ID, 0, R.string.searchmenu_details);
		menu.add(0, SEARCHMENU_OPENWITH_ID, 0, R.string.searchmenu_openwith);
		menu.add(0, SEARCHMENU_SHARELINK_ID, 0, R.string.searchmenu_sharelink);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		// Add title bar buttons
		MenuItem miRefresh = menu.add(0, MENU_REFRESH_ID, 0, R.string.refresh);
		miRefresh.setIcon(R.drawable.icon_refresh_title);
		miRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (inProgress) {
			// Show progress spinner instead of the option item
			View view = getLayoutInflater().inflate(R.layout.part_actionbar_progressitem, null);
			miRefresh.setActionView(view);
		}
		MenuItem miSearch = menu.add(0, MENU_SEARCH_ID, 0, R.string.search);
		miSearch.setIcon(R.drawable.icon_search_title);
		miSearch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		if (TorrentSearchTask.isTorrentSearchInstalled(this)) {
			// Add the switch sites button
			MenuItem miSwitch = menu.add(0, MENU_SITES_ID, 0, R.string.searchmenu_switchsite);
			miSwitch.setIcon(android.R.drawable.ic_menu_myplaces);
	
			// Add the save search button
			MenuItem miSave = menu.add(0, MENU_SAVESEARCH_ID, 0, R.string.searchmenu_savesearch);
			miSave.setIcon(android.R.drawable.ic_menu_save);
		}

		return result;		
	}

	private OnItemClickListener onSearchResultSelected = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
			// If something was already selected before
	    	Cursor item = (Cursor) getListAdapter().getItem(position);
			String url = item.getString(TorrentSearchTask.CURSOR_SEARCH_TORRENTURL);
			if (!getSearchListAdapter().getSelectedUrls().isEmpty()) {
				
				// Use an item click as selection check box click
				getSearchListAdapter().itemChecked(url, !getSearchListAdapter().isItemChecked(url));
				getListView().invalidateViews();
				
			} else {
			
		    	// Directly return the URL of the clicked torrent file
				ReturnUrlResult(url, item.getString(TorrentSearchTask.CURSOR_SEARCH_NAME));
			
			}
		}
	};

	private SearchListAdapter getSearchListAdapter() {
		return (SearchListAdapter) getListAdapter();
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor result = (Cursor) getListAdapter().getItem((int) info.id);
		
		switch (item.getItemId()) {
		case SEARCHMENU_ADD_ID:
			
			// Return the url of the selected list item
			ReturnUrlResult(result.getString(TorrentSearchTask.CURSOR_SEARCH_TORRENTURL),
					result.getString(TorrentSearchTask.CURSOR_SEARCH_NAME));
			break;

		case SEARCHMENU_DETAILS_ID:
			
			// Open the browser to show the website details page
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.getString(TorrentSearchTask.CURSOR_SEARCH_DETAILSURL))));
			break;

		case SEARCHMENU_OPENWITH_ID:
			
			// Start a VIEW (open) Intent with the .torrent url that other apps can catch
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result.getString(TorrentSearchTask.CURSOR_SEARCH_TORRENTURL))));
			break;

		case SEARCHMENU_SHARELINK_ID:
			
			// Start a SEND (share) Intent with the .torrent url so the user can send the link to someone else
			startActivity(Intent.createChooser(
					new Intent(Intent.ACTION_SEND)
						.setType("text/plain")
						.putExtra(Intent.EXTRA_TEXT, result.getString(TorrentSearchTask.CURSOR_SEARCH_TORRENTURL))
						.putExtra(Intent.EXTRA_SUBJECT, result.getString(TorrentSearchTask.CURSOR_SEARCH_NAME)), 
					getText(R.string.searchmenu_sharelink)));
			break;
			
		}
		return true;
	}

	private OnNavigationListener onProviderSelected = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			if (!disableListNavigation) {
				switchProvider(itemPosition);
			}
			return false;
		}
	};
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		
		case MENU_REFRESH_ID:
			
			if (TorrentSearchTask.isTorrentSearchInstalled(this) && defaultSite != null) {
				handleQuery();
			}
			break;
			
		case MENU_SEARCH_ID:
			
			onSearchRequested();
			break;
			
		case MENU_SITES_ID:

			// Present a dialog with all available in-app sites
			showDialog(DIALOG_SITES);
			break;
			
		case MENU_SAVESEARCH_ID:
			
			// Save the current query as an RSS feed
			saveSearch();
			break;
			
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_SITES:
			
			// Build a dialog with a radio box per in-app search site
			AlertDialog.Builder serverDialog = new AlertDialog.Builder(this);
			serverDialog.setTitle(R.string.searchmenu_switchsite);
			serverDialog.setSingleChoiceItems(
					buildSiteTextsForDialog(), // The strings of the available in-app search sites 
					siteSettingsIndex(defaultSite), 
					new DialogInterface.OnClickListener() {
				
						@Override
						// When the server is clicked (and it is different from the current active configuration), 
						// reload the daemon and update the torrent list
						public void onClick(DialogInterface dialog, int which) {
							switchProvider(which);
							removeDialog(DIALOG_SITES);
						}
			});
			return serverDialog.create();

		case DIALOG_INSTALLSEARCH:
			
			return ActivityUtil.buildInstallDialog(this, R.string.tts_not_found, TTS_MARKET_URI, true);
			
		}
		return super.onCreateDialog(id);

	}

	private void switchProvider(int which) {
		SiteSettings selected = allSites.get(which);
		if (selected.getKey() != defaultSite.getKey()) {

			if (selected.isWebSearch()) {
				// Start a web search, but do not change the defaultSite
				doWebSearch(selected, query);
			} else {
				// Set the default site search to this new site
				Preferences.storeLastUsedSearchSiteSettings(getApplicationContext(), selected.getKey());
				defaultSite = selected;
				// Search again with the same query
				handleQuery();
			}
			
		}
	}
	private int siteSettingsIndex(SiteSettings asite) {
		if (asite == null ) {
			return 0;
		}
		int i = 0;
		for (SiteSettings site : allSites) {
			if (site.getKey().equals(asite.getKey())) {
				return i;
			}
			i++;
		}
		return -1;
	}

	private String[] buildSiteTextsForDialog() {

		// Build a textual list of in-app search sites available
		ArrayList<String> sites = new ArrayList<String>();
		for (SiteSettings site : allSites) {
			//if (!site.isWebSearch()) {
				sites.add(site.getName());
			//}
		}
		return sites.toArray(new String[sites.size()]);
		
	}

	private void ReturnUrlResult(String uri, String title) {

		if (uri == null) {
			setResult(RESULT_CANCELED);
			finish();
		}
		try {
			Uri.parse(uri);
		} catch (Exception e) {
			Toast.makeText(this, R.string.error_no_url_enclosure, Toast.LENGTH_LONG).show();
			return;
		}
		
		// Build new intent that Transdroid can pick up again
		Intent i = new Intent(this, Torrents.class);
		i.setData(Uri.parse(uri));
		i.putExtra(Transdroid.INTENT_TORRENT_TITLE, title);

		// Create a result for the calling activity
		setResult(RESULT_OK);
		startActivity(i);
		finish();
	}

	private void ReturnUrlResult(Set<String> results) {

		// Build new intent with multiple result url's that Transdroid can pick up again
		Intent i = new Intent(this, Torrents.class);
		i.setAction(Transdroid.INTENT_ADD_MULTIPLE);
		i.putExtra(Transdroid.INTENT_TORRENT_URLS, results.toArray(new String[] {}));

		// Create a result for the calling activity
		setResult(RESULT_OK);
		startActivity(i);
		finish();
	}

	private void doSearch(String query) {

		// Show the 'loading' icon (rotating indeterminate progress bar)
		setProgressBar(true);
		// Show the searching text again (if search was started from within this screen itself, the results screen was populated)
		empty.setText(R.string.searching);
		if (getListAdapter() != null) {
			setListAdapter(null);
		}
		
		// Start a new search
		TLog.d(LOG_NAME, "Starting a " + defaultSite.getKey() + " search with query: " + query);
		setTitle(getText(R.string.search_resultsfrom) + " " + defaultSite.getName());
		new TorrentSearchTask(this) {
			@Override
			protected void onResultsRetrieved(Cursor cursor) {
				Search.this.onResultsRetrieved(cursor);
			}
			@Override
			protected void onError() {
				Search.this.onError(getString(R.string.error_httperror));
			}
		}.execute(query, Preferences.getCursorKeyForPreferencesKey(defaultSite.getKey()), searchSettings.getSortBySeeders()? "BySeeders": "Combined");
		
	}

	private void setProgressBar(boolean b) {
		inProgress  = b;
		invalidateOptionsMenu();
	}

	public void onResultsRetrieved(Cursor cursor) {

		// Not loading any more, turn off status indicator
		setProgressBar(false);

		// Update the list
		empty.setText(R.string.no_results);
        setListAdapter(new SearchListAdapter(this, cursor, this));
		getListView().requestFocus();
        getListView().setOnTouchListener(this);
		
	}

	public void onError(String errorMessage) {

		// Not loading any more, turn off status indicator
		setProgressBar(false);

		// Update the list
		empty.setText(errorMessage);
        setListAdapter(null);
		
	}
	
	private void saveSearch() {
		
		// Find the url to match an RSS feed to the last query
		String url = TorrentSearchTask.buildRssFeedFromSearch(this, defaultSite.getKey(), query);
		if (url != null) {
			
			// Build new RSS feed settings object
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			int i = 0;
	        String nextUrl = Preferences.KEY_PREF_RSSURL + Integer.toString(i);
	        while (prefs.contains(nextUrl)) {
	        	i++;
	        	nextUrl = Preferences.KEY_PREF_RSSURL + Integer.toString(i);
	        }
	
			// Store an RSS feed setting for this feed
	        Editor editor = prefs.edit();
	        editor.putString(Preferences.KEY_PREF_RSSNAME + Integer.toString(i), query);
	        editor.putString(nextUrl, url);
	        editor.commit();
	        
	        Toast.makeText(this, R.string.search_savedrssfeed, Toast.LENGTH_SHORT).show();
	        
		} else {
			Toast.makeText(this, R.string.search_savenotsupported, Toast.LENGTH_SHORT).show();
		}
	}

	protected ListView getListView() {
		return (ListView) findViewById(R.id.results);
	}

	private SearchListAdapter getListAdapter() {
		return (SearchListAdapter) getListView().getAdapter();
	}

	private View getEmptyText() {
		return findViewById(android.R.id.empty);
	}

	private void setListAdapter(SearchListAdapter adapter) {
		getListView().setAdapter(adapter);
		if (adapter == null || adapter.getCount() <= 0) {
			getListView().setVisibility(View.GONE);
			getEmptyText().setVisibility(View.VISIBLE);
		} else {
			getListView().setVisibility(View.VISIBLE);
			getEmptyText().setVisibility(View.GONE);
		}
	}

    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	return gestureDetector.onTouchEvent(me);
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
    	return gestureDetector.onTouchEvent(event);
	}
	
	/**
	 * Internal class that handles gestures from the search screen (a 'swipe' or 'fling').
	 * 
	 * More at http://stackoverflow.com/questions/937313/android-basic-gesture-detection
	 */
	class SearchScreenGestureListener extends SimpleOnGestureListener {
		
		private static final int SWIPE_MIN_DISTANCE = 120;
		private static final int SWIPE_MAX_OFF_PATH = 250;
		private static final int SWIPE_THRESHOLD_VELOCITY = 200;
		
		@Override
		public boolean onDoubleTap (MotionEvent e) {
			return false;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			if (e1 != null && e2 != null) {			
	            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
	                return false;	
	            }
	            
	            // Determine to which daemon we are now switching
	            int newSite = siteSettingsIndex(defaultSite);
	            // right to left swipe
	            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	newSite += 1;
	                if (newSite >= allSites.size() || allSites.get(newSite).isWebSearch()) {
	                	newSite = 0;
	                }
	            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	newSite -= 1;
	                if (newSite < 0) {
	                	newSite = allSites.size() - 1;
	                	while (allSites.get(newSite).isWebSearch()) {
	                		// Skip the web search sites
	                		newSite -= 1;
	                	}
	                }
	            }
	            
	            
	            // Make the switch, if needed
	            SiteSettings newSiteSettings = allSites.get(newSite);
	            if (!newSiteSettings.getKey().equals(defaultSite.getKey())) {
					// Set the default site search to this new site
					Preferences.storeLastUsedSearchSiteSettings(getApplicationContext(), newSiteSettings.getKey());
					defaultSite = newSiteSettings;
					disableListNavigation = true;
					getSupportActionBar().setSelectedNavigationItem(newSite);
					disableListNavigation = false;
					handleQuery();
	            }
			}
            
	        return false;			
		}

	}

	/**
	 * Called by the SelectableArrayAdapter when the set of selected search results changed
	 */
	public void onSelectedResultsChanged() {
		SearchListAdapter adapter = (SearchListAdapter) getListAdapter();
		if (adapter.getSelectedUrls().size() == 0) {
			// Hide the 'add selected' button
			addSelected.setVisibility(View.GONE);
		} else {
			addSelected.setVisibility(View.VISIBLE);
		}
	}
	
	private OnClickListener addSelectedClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Send the urls of all selected search result back to Transdroid
			SearchListAdapter adapter = (SearchListAdapter) getListAdapter();
			ReturnUrlResult(adapter.getSelectedUrls());
		}
	};
}
