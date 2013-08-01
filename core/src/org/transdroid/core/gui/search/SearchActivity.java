package org.transdroid.core.gui.search;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.search.SearchHelper;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.*;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.widget.SearchView;

/**
 * An activity that shows search results to the user (after a query was supplied by the standard Android search manager)
 * and either shows the list of search sites on the left (e.g. on tablets) or allows switching between search sites via
 * the action bar spinner.
 * @author Eric Kok
 */
@EActivity(resName = "activity_search")
@OptionsMenu(resName = "activity_search")
public class SearchActivity extends SherlockFragmentActivity implements OnNavigationListener {

	@FragmentById(resName = "searchresults_list")
	protected SearchResultsFragment fragmentResults;
	@ViewById
	protected SherlockListView searchsitesList;
	@ViewById
	protected TextView installmoduleText;
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected SearchHelper searchHelper;
	@SystemService
	protected SearchManager searchManager;
	private SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
			TorrentSearchHistoryProvider.AUTHORITY, TorrentSearchHistoryProvider.MODE);

	private List<SearchSetting> searchSites;
	private SearchSetting lastUsedSite;
	private String lastUsedQuery;

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

		// Get the user query, as coming from the standard SearchManager
		handleIntent(getIntent());

		if (!searchHelper.isTorrentSearchInstalled()) {
			// The module install text will be shown instead (in onPrepareOptionsMenu)
			return;
		}

		// Load sites and find the last used (or set as default) search site
		searchSites = applicationSettings.getSearchSettings();
		lastUsedSite = applicationSettings.getLastUsedSearchSite();
		int lastUsedPosition = -1;
		if (lastUsedSite != null) {
			for (int i = 0; i < searchSites.size(); i++) {
				if (searchSites.get(i).getKey().equals(lastUsedSite.getKey())) {
					lastUsedPosition = i;
					break;
				}
			}
		}

		// Allow site selection via list (on large screens) or action bar spinner
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		if (searchsitesList != null) {
			// The current layout has a dedicated list view to select the search site
			SearchSitesAdapter searchSitesAdapter = SearchSitesAdapter_.getInstance_(this);
			searchSitesAdapter.update(searchSites);
			searchsitesList.setAdapter(searchSitesAdapter);
			searchsitesList.setOnItemClickListener(onSearchSiteClicked);
			// Select the last used site; this also starts the search!
			if (lastUsedPosition >= 0)
				searchsitesList.setItemChecked(lastUsedPosition, true);
		} else {
			// Use the action bar spinner to select sites
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			getSupportActionBar().setDisplayShowTitleEnabled(false);
			getSupportActionBar()
					.setListNavigationCallbacks(new SearchSettingsDropDownAdapter(this, searchSites), this);
			// Select the last used site; this also starts the search!
			if (lastUsedPosition >= 0)
				getSupportActionBar().setSelectedNavigationItem(lastUsedPosition);
		}

	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (navigationHelper.enableSearchUi()) {
			// For Android 2.1+, add an expandable SearchView to the action bar
			MenuItem item = menu.findItem(R.id.action_search);
			if (android.os.Build.VERSION.SDK_INT >= 8) {
				final SearchView searchView = new SearchView(this);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
				searchView.setQueryRefinementEnabled(true);
				item.setActionView(searchView);
			}
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		boolean searchInstalled = searchHelper.isTorrentSearchInstalled();
		menu.findItem(R.id.action_search).setVisible(searchInstalled);
		menu.findItem(R.id.action_refresh).setVisible(searchInstalled);
		menu.findItem(R.id.action_downloadsearch).setVisible(!searchInstalled);
		if (searchsitesList != null)
			searchsitesList.setVisibility(searchInstalled ? View.VISIBLE : View.GONE);
		if (searchInstalled)
			getSupportFragmentManager().beginTransaction().show(fragmentResults).commit();
		else
			getSupportFragmentManager().beginTransaction().hide(fragmentResults).commit();
		installmoduleText.setVisibility(searchInstalled ? View.GONE : View.VISIBLE);

		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
		refreshSearch();
	}

	private void handleIntent(Intent intent) {
		lastUsedQuery = parseQuery(intent);
		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(lastUsedQuery));

		// Is this actually a full HTTP URL? Then redirect this request to add the URL directly
		if (lastUsedQuery != null
				&& (lastUsedQuery.startsWith("http") || lastUsedQuery.startsWith("https")
						|| lastUsedQuery.startsWith("magnet") || lastUsedQuery.startsWith("file"))) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lastUsedQuery)));
			finish();
			return;
		}

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	private OnItemClickListener onSearchSiteClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			lastUsedSite = searchSites.get(position);
			refreshSearch();
		}
	};

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		lastUsedSite = searchSites.get(itemPosition);
		refreshSearch();
		return true;
	}

	/**
	 * Extracts the query string from the search {@link Intent}
	 * @return The query string that was entered by the user
	 */
	private String parseQuery(Intent intent) {

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

	@OptionsItem(resName = "action_refresh")
	protected void refreshSearch() {
		if (lastUsedSite instanceof WebsearchSetting) {
			// Start a browser page directly to the requested search results
			WebsearchSetting websearch = (WebsearchSetting) lastUsedSite;
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse(String.format(websearch.getBaseUrl(), lastUsedQuery))));
		} else if (lastUsedSite instanceof SearchSite) {
			// Save the search site currently used to search for future usage
			applicationSettings.setLastUsedSearchSite((SearchSite) lastUsedSite);
			// Ask the results fragment to start a search for the specified query
			fragmentResults.startSearch(lastUsedQuery, (SearchSite) lastUsedSite);
		}
	}

	@OptionsItem(resName = "action_downloadsearch")
	protected void downloadSearchModule() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/latest-search")));
	}

}
