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

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
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

import java.util.List;

/**
 * An activity that shows search results to the user (after a query was supplied by the standard Android search manager) and either shows the list of
 * search sites on the left (e.g. on tablets) or allows switching between search sites via the action bar spinner.
 * @author Eric Kok
 */
@EActivity(R.layout.activity_search)
public class SearchActivity extends AppCompatActivity {

	@ViewById
	protected Toolbar searchToolbar;
	@ViewById
	protected Spinner sitesSpinner;
	@FragmentById(R.id.searchresults_fragment)
	protected SearchResultsFragment fragmentResults;
	@ViewById
	protected ListView searchsitesList;
	@ViewById
	protected TextView installmoduleText;
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected SearchHelper searchHelper;
	@SystemService
	protected SearchManager searchManager;
	private MenuItem searchMenu = null;
	private SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);

	private List<SearchSetting> searchSites;
	private SearchSetting lastUsedSite;
	private String lastUsedQuery;

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

		searchToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		searchToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TorrentsActivity_.intent(SearchActivity.this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
			}
		});
		setSupportActionBar(searchToolbar);

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
		if (searchsitesList != null) {
			// The current layout has a dedicated list view to select the search site
			SearchSitesAdapter searchSitesAdapter = SearchSitesAdapter_.getInstance_(this);
			searchSitesAdapter.update(searchSites);
			searchsitesList.setAdapter(searchSitesAdapter);
			searchsitesList.setOnItemClickListener(onSearchSiteClicked);
			// Select the last used site and start the search
			if (lastUsedPosition >= 0) {
				searchsitesList.setItemChecked(lastUsedPosition, true);
				lastUsedSite = searchSites.get(lastUsedPosition);
				refreshSearch();
			} else {
				fragmentResults.clearResults();
			}
		} else {
			// Use the action bar spinner to select sites
			if (getSupportActionBar() != null)
				getSupportActionBar().setTitle("");
			sitesSpinner.setVisibility(View.VISIBLE);
			sitesSpinner.setAdapter(new SearchSettingsDropDownAdapter(searchToolbar.getContext(), searchSites));
			sitesSpinner.setOnItemSelectedListener(onSearchSiteSelected);
			// Select the last used site; this also starts the search!
			if (lastUsedPosition >= 0) {
				sitesSpinner.setSelection(lastUsedPosition);
				lastUsedSite = searchSites.get(lastUsedPosition);
				refreshSearch();
			} else {
				fragmentResults.clearResults();
			}
		}
		invalidateOptionsMenu();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Manually insert the actions into the main torrent and secondary actions toolbars
		searchToolbar.inflateMenu(R.menu.activity_search);
		// Add an expandable SearchView to the action bar
		MenuItem item = menu.findItem(R.id.action_search);
		final SearchView searchView = new SearchView(searchToolbar.getContext());
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setQueryRefinementEnabled(true);
		searchView.setIconified(false);
		searchView.setIconifiedByDefault(false);
		MenuItemCompat.setActionView(item, searchView);
		searchMenu = item;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		boolean searchInstalled = searchHelper.isTorrentSearchInstalled();
		searchToolbar.getMenu().findItem(R.id.action_search).setVisible(searchInstalled);
		searchToolbar.getMenu().findItem(R.id.action_refresh).setVisible(searchInstalled);
		searchToolbar.getMenu().findItem(R.id.action_downloadsearch).setVisible(!searchInstalled);
		if (searchsitesList != null) {
			searchsitesList.setVisibility(searchInstalled ? View.VISIBLE : View.GONE);
		}
		if (searchInstalled) {
			getFragmentManager().beginTransaction().show(fragmentResults).commit();
		} else {
			getFragmentManager().beginTransaction().hide(fragmentResults).commit();
		}
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
		if (lastUsedQuery != null && (lastUsedQuery.startsWith("http") || lastUsedQuery.startsWith("https") ||
				lastUsedQuery.startsWith("magnet") || lastUsedQuery.startsWith("file"))) {
			// Don't broadcast this intent; we can safely assume this is intended for Transdroid only
			Intent i = TorrentsActivity_.intent(this).get();
			i.setData(Uri.parse(lastUsedQuery));
			startActivity(i);
			finish();
		}

	}

	@Override
	public boolean onSearchRequested() {
		if (searchMenu != null) {
			searchMenu.expandActionView();
		}
		return true;
	}

	private OnItemClickListener onSearchSiteClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			lastUsedSite = searchSites.get(position);
			refreshSearch();
		}
	};
	private AdapterView.OnItemSelectedListener onSearchSiteSelected = new AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			lastUsedSite = searchSites.get(position);
			refreshSearch();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	};

	/**
	 * Extracts the query string from the search {@link Intent}
	 * @return The query string that was entered by the user
	 */
	private String parseQuery(Intent intent) {

		String query = null;
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			query = intent.getStringExtra(SearchManager.QUERY).trim();
		} else if (intent.getAction().equals(Intent.ACTION_SEND)) {
			query = SendIntentHelper.cleanUpText(intent).trim();
		}
		if (query != null && query.length() > 0) {

			// Remember this search query to later show as a suggestion
			suggestions.saveRecentQuery(query, null);
			return query;

		}
		return null;

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(R.id.action_refresh)
	protected void refreshSearch() {

		if (searchMenu != null) {
			// Close the search view in the action bar
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
			if (sitesSpinner == null && getSupportActionBar() != null)
				getSupportActionBar()
						.setTitle(NavigationHelper.buildCondensedFontString(getString(R.string.search_queryonsite, lastUsedQuery, lastUsedSite.getName())));
			// Ask the results fragment to start a search for the specified query
			fragmentResults.startSearch(lastUsedQuery, (SearchSite) lastUsedSite);

		}
	}

	@OptionsItem(R.id.action_downloadsearch)
	protected void downloadSearchModule() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/latest-search")));
	}

}
