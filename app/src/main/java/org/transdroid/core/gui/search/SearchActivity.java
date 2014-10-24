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
import org.transdroid.R;
import org.transdroid.core.app.search.SearchHelper;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.app.settings.WebsearchSetting;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * An activity that shows search results to the user (after a query was supplied by the standard Android search manager)
 * and either shows the list of search sites on the left (e.g. on tablets) or allows switching between search sites via
 * the action bar spinner.
 * @author Eric Kok
 */
@EActivity(resName = "activity_search")
@OptionsMenu(resName = "activity_search")
public class SearchActivity extends Activity implements OnNavigationListener {

	@FragmentById(resName = "searchresults_fragment")
	protected SearchResultsFragment fragmentResults;
	@ViewById
	protected ListView searchsitesList;
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
	private MenuItem searchMenu = null;
	private SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchHistoryProvider.AUTHORITY,
			SearchHistoryProvider.MODE);

	private List<SearchSetting> searchSites;
	private SearchSetting lastUsedSite;
	private String lastUsedQuery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
			getActionBar().setIcon(R.drawable.ic_activity_torrents);
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
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
			getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			getActionBar().setDisplayShowTitleEnabled(false);
			getActionBar().setListNavigationCallbacks(new SearchSettingsDropDownAdapter(this, searchSites), this);
			// Select the last used site; this also starts the search!
			if (lastUsedPosition >= 0)
				getActionBar().setSelectedNavigationItem(lastUsedPosition);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (navigationHelper.enableSearchUi()) {
			// Add an expandable SearchView to the action bar
			MenuItem item = menu.findItem(R.id.action_search);
			final SearchView searchView = new SearchView(this);
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setQueryRefinementEnabled(true);
			item.setActionView(searchView);
			searchMenu = item;
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
			getFragmentManager().beginTransaction().show(fragmentResults).commit();
		else
			getFragmentManager().beginTransaction().hide(fragmentResults).commit();
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
		
		// Is this actually a full HTTP URL? Then redirect this request to add the URL directly
		if (lastUsedQuery != null
				&& (lastUsedQuery.startsWith("http") || lastUsedQuery.startsWith("https")
						|| lastUsedQuery.startsWith("magnet") || lastUsedQuery.startsWith("file"))) {
			// Don't broadcast this intent; we can safely assume this is intended for Transdroid only
			Intent i = TorrentsActivity_.intent(this).get();
			i.setData(Uri.parse(lastUsedQuery));
			startActivity(i);
			finish();
			return;
		}

	}

	@Override
	public boolean onSearchRequested() {
		if (searchMenu != null) {
			searchMenu.expandActionView();
		}
		return true;
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
		query = query.trim();
		if (query != null && query.length() > 0) {

			// Remember this search query to later show as a suggestion
			suggestions.saveRecentQuery(query, null);
			return query;

		}
		return null;

	}

	@OptionsItem(resName = "action_refresh")
	protected void refreshSearch() {

		if (searchMenu != null) {
			// Close the search view in the ation bar
			searchMenu.collapseActionView();
		}

		if (lastUsedSite instanceof WebsearchSetting) {

			// Start a browser page directly to the requested search results
			WebsearchSetting websearch = (WebsearchSetting) lastUsedSite;
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(websearch.getBaseUrl().replace("%s", lastUsedQuery))));
			finish();

		} else if (lastUsedSite instanceof SearchSite) {

			// Save the search site currently used to search for future usage
			applicationSettings.setLastUsedSearchSite(lastUsedSite);
			// Update the activity title (only shown on large devices)
			getActionBar().setTitle(
					NavigationHelper.buildCondensedFontString(getString(R.string.search_queryonsite, lastUsedQuery,
							lastUsedSite.getName())));
			// Ask the results fragment to start a search for the specified query
			fragmentResults.startSearch(lastUsedQuery, (SearchSite) lastUsedSite);

		}
	}

	@OptionsItem(resName = "action_downloadsearch")
	protected void downloadSearchModule() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/latest-search")));
	}

}
