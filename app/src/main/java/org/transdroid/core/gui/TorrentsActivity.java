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
package org.transdroid.core.gui;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.transdroid.R;
import org.transdroid.core.app.search.SearchHelper_;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.app.settings.WebsearchSetting;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.log.LogUncaughtExceptionHandler;
import org.transdroid.core.gui.navigation.FilterListAdapter;
import org.transdroid.core.gui.navigation.FilterListAdapter_;
import org.transdroid.core.gui.navigation.Label;
import org.transdroid.core.gui.navigation.NavigationFilter;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.navigation.RefreshableActivity;
import org.transdroid.core.gui.navigation.StatusType;
import org.transdroid.core.gui.rss.RssfeedsActivity_;
import org.transdroid.core.gui.search.BarcodeHelper;
import org.transdroid.core.gui.search.FilePickerHelper;
import org.transdroid.core.gui.search.UrlEntryDialog;
import org.transdroid.core.gui.settings.MainSettingsActivity_;
import org.transdroid.core.service.BootReceiver;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.core.widget.ListWidgetProvider;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentsSortBy;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.ForceRecheckTask;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetStatsTask;
import org.transdroid.daemon.task.GetStatsTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetAlternativeModeTask;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Main activity that holds the fragment that shows the torrents list, presents a way to filter the list (via an action bar spinner or list side list)
 * and potentially shows a torrent details fragment too, if there is room. Task execution such as loading of and adding torrents is performs in this
 * activity, using background methods. Finally, the activity offers navigation elements such as access to settings and showing connection issues.
 * @author Eric Kok
 */
@EActivity(R.layout.activity_torrents)
public class TorrentsActivity extends AppCompatActivity implements TorrentTasksExecutor, RefreshableActivity {

	private static final int RESULT_DETAILS = 0;

	// Fragment uses this to pause the refresh across restarts
	public boolean stopRefresh = false;

	// Navigation components
	@SystemService
	protected SearchManager searchManager;
	@Bean
	protected Log log;
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@ViewById
	protected Toolbar selectionToolbar;
	@ViewById
	protected Toolbar torrentsToolbar;
	@ViewById
	protected Toolbar actionsToolbar;
	@ViewById(R.id.contextual_menu)
	protected ActionMenuView contextualMenu;
	@ViewById
	protected FloatingActionsMenu addmenuButton;
	@ViewById
	protected FloatingActionButton addmenuFileButton;
	@ViewById
	protected DrawerLayout drawerLayout;
	@ViewById(R.id.drawer_container)
	protected ViewGroup drawerContainer;
	@ViewById
	protected ListView drawerList;
	@ViewById
	protected ListView filtersList;
	@ViewById
	protected SearchView filterSearch;
	private ListView navigationList;
	private FilterListAdapter navigationListAdapter;
	private ServerSelectionView serverSelectionView;
	private ServerStatusView serverStatusView;
	private ActionBarDrawerToggle drawerToggle;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected SystemSettings systemSettings;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected String preselectNavigationFilter = null;
	@InstanceState
	protected boolean turtleModeEnabled = false;
	@InstanceState
	protected ArrayList<Label> lastNavigationLabels;
	// Contained torrent and details fragments
	@FragmentById(R.id.torrents_fragment)
	protected TorrentsFragment fragmentTorrents;
	@FragmentById(R.id.torrentdetails_fragment)
	protected DetailsFragment fragmentDetails;
	@InstanceState
	boolean firstStart = true;
	private MenuItem searchMenu = null;
	private IDaemonAdapter currentConnection = null;

	// Auto refresh task
	private AsyncTask<Void, Void, Void> autoRefreshTask;

	private String awaitingAddLocalFile;
	private String awaitingAddTitle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
		}
		// Catch any uncaught exception to log it
		Thread.setDefaultUncaughtExceptionHandler(new LogUncaughtExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler()));
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	protected void init() {

		// Use custom views as action bar content, showing filter selection and current torrent counts/speeds
		serverSelectionView = ServerSelectionView_.build(this);
		serverStatusView = ServerStatusView_.build(this);
		if (selectionToolbar != null) {
			selectionToolbar.addView(serverSelectionView);
		} else {
			torrentsToolbar.addView(serverSelectionView);
		}
		actionsToolbar.addView(serverStatusView);
		actionsToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				// Redirect to the classic activity implementation so we can use @OptionsItem methods
				return onOptionsItemSelected(menuItem);
			}
		});
		setSupportActionBar(torrentsToolbar); // For direct menu item inflation by the contained fragments
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		// Construct the filters list, i.e. the list of servers, status types and labels
		navigationListAdapter = FilterListAdapter_.getInstance_(this);
		navigationListAdapter.updateServers(applicationSettings.getAllServerSettings());
		navigationListAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
		// Add an empty labels list (which will be updated later, but the adapter needs to be created now)
		navigationListAdapter.updateLabels(new ArrayList<Label>());

		// Apply the filters list to the navigation drawer (on phones) or the dedicated side bar (i.e. on tablets)
		if (filtersList != null) {
			navigationList = filtersList;
		} else {
			navigationList = drawerList;
			drawerToggle =
					new ActionBarDrawerToggle(this, drawerLayout, torrentsToolbar, R.string.navigation_opendrawer, R.string.navigation_closedrawer);
			drawerToggle.setDrawerIndicatorEnabled(true);
			drawerLayout.setDrawerListener(drawerToggle);
		}
		navigationList.setAdapter(navigationListAdapter);
		navigationList.setOnItemClickListener(onFilterListItemClicked);
		// Now that all items (or at least their adapters) have been added, ensure a filter is selected
		// NOTE When this is a fresh start, we might override the filter later (based on the last user selection)
		if (currentFilter == null) {
			currentFilter = StatusType.getShowAllType(this);
		}
		filterSearch.setOnQueryTextListener(filterQueryTextChanged);

		// Load the default server or a server that was explicitly supplied in the starting intent
		ServerSetting defaultServer = applicationSettings.getDefaultServer();
		if (defaultServer == null) {
			// No server settings yet
			return;
		}
		Torrent openTorrent = null;
		if (getIntent().getAction() != null && getIntent().getAction().equals(ListWidgetProvider.INTENT_STARTSERVER) &&
				getIntent().getExtras() == null && getIntent().hasExtra(ListWidgetProvider.EXTRA_SERVER)) {
			// A server settings order ID was provided in this org.transdroid.START_SERVER action intent
			int serverId = getIntent().getExtras().getInt(ListWidgetProvider.EXTRA_SERVER);
			if (serverId < 0 || serverId > applicationSettings.getMaxOfAllServers()) {
				log.e(this, "Tried to start with " + ListWidgetProvider.EXTRA_SERVER + " intent but " + serverId +
						" is not an existing server order id");
			} else {
				defaultServer = applicationSettings.getServerSetting(serverId);
				if (getIntent().hasExtra(ListWidgetProvider.EXTRA_TORRENT)) {
					openTorrent = getIntent().getParcelableExtra(ListWidgetProvider.EXTRA_TORRENT);
				}
			}
		}

		// Connect to the last used server or a server that was explicitly supplied in the starting intent
		if (firstStart) {
			// Force first torrents refresh
			filterSelected(defaultServer, true);
			// Perhaps we can select the last used navigation filter, but only after a first refresh was completed
			preselectNavigationFilter = applicationSettings.getLastUsedNavigationFilter();
			// Handle any start up intents
			if (openTorrent != null) {
				openDetails(openTorrent);
			} else if (getIntent() != null) {
				handleStartIntent();
			}
		} else {
			// Resume after instead of fully loading the torrents list; create connection and set action bar title
			ServerSetting lastUsed = applicationSettings.getLastUsedServer();
			currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
			serverSelectionView.updateCurrentServer(currentConnection);
			serverSelectionView.updateCurrentFilter(currentFilter);
		}
		firstStart = false;

		// Start the alarms for the background services, if needed
		BootReceiver.startBackgroundServices(getApplicationContext(), false);
		BootReceiver.startAppUpdatesService(getApplicationContext());

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred
		if (drawerToggle != null) {
			drawerToggle.syncState();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Refresh server settings
		navigationListAdapter.updateServers(applicationSettings.getAllServerSettings());
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		if (lastUsed == null) {
			// Still no settings
			updateFragmentVisibility(false);
			return;
		}

		// If we had no connection before, establish it now; otherwise just reload the settings
		if (currentConnection == null) {
			filterSelected(lastUsed, true);
		} else {
			currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
		}

		// Start auto refresh
		startAutoRefresh();

	}

	@OnActivityResult(RESULT_DETAILS)
	protected void onDetailsScreenResult(Intent result) {
		// If the details activity returns whether the torrent was removed or updated, update the torrents list as well
		// (the details fragment is the source, so no need to update that)
		if (result != null && result.hasExtra("affected_torrent")) {
			Torrent affected = result.getParcelableExtra("affected_torrent");
			fragmentTorrents.quickUpdateTorrent(affected, result.getBooleanExtra("torrent_removed", false));
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void startAutoRefresh() {
		// Check if already running
		if (autoRefreshTask != null || stopRefresh || systemSettings.getRefreshIntervalMilliseconds() == 0) {
			return;
		}

		autoRefreshTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				while (!isCancelled()) {
					try {
						Thread.sleep(systemSettings.getRefreshIntervalMilliseconds());
					} catch (InterruptedException e) {
						// Ignore
					}
					// Just in case it was cancelled during sleep
					if (isCancelled()) {
						return null;
					}

					refreshTorrents();
					if (Daemon.supportsStats(currentConnection.getType())) {
						getAdditionalStats();
					}
				}
				return null;
			}

		};
		autoRefreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void stopAutoRefresh() {
		if (autoRefreshTask != null) {
			autoRefreshTask.cancel(true);
		}
		autoRefreshTask = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Manually insert the actions into the main torrent and secondary actions toolbars
		torrentsToolbar.inflateMenu(R.menu.activity_torrents_main);
		if (actionsToolbar.getMenu().size() == 0) {
			actionsToolbar.inflateMenu(R.menu.activity_torrents_secondary);
		}
		if (navigationHelper.enableSearchUi()) {
			// Add an expandable SearchView to the action bar
			MenuItem item = menu.findItem(R.id.action_search);
			SearchView searchView = new SearchView(torrentsToolbar.getContext());
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			searchView.setQueryRefinementEnabled(true);
			searchView.setOnSearchClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Pause autorefresh
					stopRefresh = true;
					stopAutoRefresh();
				}
			});
			MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
				@Override
				public boolean onMenuItemActionExpand(MenuItem item) {
					return true;
				}

				@Override
				public boolean onMenuItemActionCollapse(MenuItem item) {
					stopRefresh = false;
					startAutoRefresh();
					return true;
				}
			});
			MenuItemCompat.setActionView(item, searchView);
			searchMenu = item;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// No connection yet; hide all menu options except settings
		if (currentConnection == null) {
			torrentsToolbar.setNavigationIcon(null);
			if (selectionToolbar != null)
				selectionToolbar.setVisibility(View.GONE);
			addmenuButton.setVisibility(View.GONE);
			actionsToolbar.setVisibility(View.GONE);
			if (filtersList != null)
				filtersList.setVisibility(View.GONE);
			filterSearch.setVisibility(View.GONE);
			torrentsToolbar.getMenu().findItem(R.id.action_search).setVisible(false);
			torrentsToolbar.getMenu().findItem(R.id.action_rss).setVisible(false);
			torrentsToolbar.getMenu().findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			torrentsToolbar.getMenu().findItem(R.id.action_help).setVisible(true);
			actionsToolbar.getMenu().findItem(R.id.action_enableturtle).setVisible(false);
			actionsToolbar.getMenu().findItem(R.id.action_disableturtle).setVisible(false);
			actionsToolbar.getMenu().findItem(R.id.action_refresh).setVisible(false);
			actionsToolbar.getMenu().findItem(R.id.action_sort).setVisible(false);
			if (fragmentTorrents != null) {
				fragmentTorrents.updateConnectionStatus(false, null);
			}
			return true;
		}

		// There is a connection (read: settings to some server known)
		if (drawerToggle != null)
			torrentsToolbar.setNavigationIcon(R.drawable.ic_action_drawer);
		if (selectionToolbar != null)
			selectionToolbar.setVisibility(View.VISIBLE);
		addmenuButton.setVisibility(View.VISIBLE);
		actionsToolbar.setVisibility(View.VISIBLE);
		if (filtersList != null)
			filtersList.setVisibility(View.VISIBLE);
		filterSearch.setVisibility(View.VISIBLE);
		boolean addByFile = Daemon.supportsAddByFile(currentConnection.getType());
		addmenuFileButton.setVisibility(addByFile ? View.VISIBLE : View.GONE);
		// Primary toolbar menu
		torrentsToolbar.getMenu().findItem(R.id.action_search).setVisible(navigationHelper.enableSearchUi());
		torrentsToolbar.getMenu().findItem(R.id.action_rss).setVisible(navigationHelper.enableRssUi());
		torrentsToolbar.getMenu().findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		torrentsToolbar.getMenu().findItem(R.id.action_help).setVisible(false);
		// Secondary toolbar menu
		boolean hasAltMode = Daemon.supportsSetAlternativeMode(currentConnection.getType());
		actionsToolbar.getMenu().findItem(R.id.action_enableturtle).setVisible(hasAltMode && !turtleModeEnabled);
		actionsToolbar.getMenu().findItem(R.id.action_disableturtle).setVisible(hasAltMode && turtleModeEnabled);
		actionsToolbar.getMenu().findItem(R.id.action_refresh).setVisible(true);
		actionsToolbar.getMenu().findItem(R.id.action_sort).setVisible(true);
		actionsToolbar.getMenu().findItem(R.id.action_sort_added).setVisible(Daemon.supportsDateAdded(currentConnection.getType()));
		if (fragmentTorrents != null) {
			fragmentTorrents.updateConnectionStatus(true, currentConnection.getType());
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle only if this is the drawer toggle; otherwise the AndroidAnnotations will be used
		return drawerToggle != null && drawerToggle.onOptionsItemSelected(item);
	}

	/**
	 * Handles item selections on the dedicated list of filter items
	 */
	private OnItemClickListener onFilterListItemClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			navigationList.setItemChecked(position, true);
			Object item = navigationList.getAdapter().getItem(position);
			if (item instanceof SimpleListItem) {
				filterSelected((SimpleListItem) item, false);
			}
			if (drawerLayout != null)
				drawerLayout.closeDrawer(drawerContainer);
		}
	};

	/**
	 * A new filter was selected; update the view over the current data
	 * @param item The touched filter item
	 * @param forceNewConnection Whether a new connection should be initialised regardless of the old server selection
	 */
	protected void filterSelected(SimpleListItem item, boolean forceNewConnection) {

		// No longer apply the last used filter (on a fresh application start), if we still needed to
		preselectNavigationFilter = null;

		// Server selection
		if (item instanceof ServerSetting) {
			ServerSetting server = (ServerSetting) item;

			if (!forceNewConnection && currentConnection != null && server.equals(currentConnection.getSettings())) {
				// Already connected to this server; just ask for a refresh instead
				fragmentTorrents.updateIsLoading(true);
				refreshTorrents();
				return;
			}

			// Update connection to the newly selected server and refresh
			currentConnection = server.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
			applicationSettings.setLastUsedServer(server);
			serverSelectionView.updateCurrentServer(currentConnection);
			if (forceNewConnection) {
				serverSelectionView.updateCurrentFilter(currentFilter);
			}

			// Clear the currently shown list of torrents and perhaps the details
			fragmentTorrents.clear(true, true);
			if (fragmentDetails != null && fragmentDetails.isAdded() && fragmentDetails.getActivity() != null) {
				fragmentDetails.updateIsLoading(false, null);
				fragmentDetails.clear();
				fragmentDetails.setCurrentServerSettings(server);
			}
			updateFragmentVisibility(true);
			refreshScreen();
			return;

		}

		// Status type or label selection - both of which are navigation filters
		if (item instanceof NavigationFilter) {
			// Set new filter
			currentFilter = (NavigationFilter) item;
			fragmentTorrents.applyNavigationFilter(currentFilter);
			serverSelectionView.updateCurrentFilter(currentFilter);
			// Remember that the user last selected this
			applicationSettings.setLastUsedNavigationFilter(currentFilter);
			// Clear the details view
			if (fragmentDetails != null && fragmentDetails.isAdded()) {
				fragmentDetails.updateIsLoading(false, null);
				fragmentDetails.clear();
			}
		}

	}

	/**
	 * Hides the filter list and details fragment's full view if there is no configured connection
	 * @param hasServerSettings Whether there are server settings available, so we can continue to connect
	 */
	private void updateFragmentVisibility(boolean hasServerSettings) {
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			if (hasServerSettings) {
				getFragmentManager().beginTransaction().show(fragmentDetails).commit();
			} else {
				getFragmentManager().beginTransaction().hide(fragmentDetails).commit();
			}
		}
		invalidateOptionsMenu();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleStartIntent();
	}

	protected void handleStartIntent() {
		// For intents that come from out of the application, perhaps we can not directly add them
		if (applicationSettings.getDefaultServerKey() == ApplicationSettings.DEFAULTSERVER_ASKONADD && getIntent().getData() != null) {
			// First ask which server to use before adding any intent from the extras
			ServerPickerDialog.startServerPicker(this, applicationSettings.getAllServerSettings());
			return;
		}
		addFromIntent();
	}

	public void switchServerAndAddFromIntent(int position) {
		// Callback from the ServerPickerDialog; force a connection before selecting it (in the navigation)
		// Note: we can just use the list position as we have stable server setting ids
		ServerSetting selectedServer = applicationSettings.getAllServerSettings().get(position);
		filterSelected(selectedServer, false);
		addFromIntent();
	}

	/**
	 * If required, add torrents from the supplied intent extras.
	 */
	protected void addFromIntent() {
		Intent intent = getIntent();
		Uri dataUri = intent.getData();
		String data = intent.getDataString();
		String action = intent.getAction();

		// Adding multiple torrents at the same time (as found in the Intent extras Bundle)
		if (action != null && action.equals("org.transdroid.ADD_MULTIPLE")) {
			// Intent should have some extras pointing to possibly multiple torrents
			String[] urls = intent.getStringArrayExtra("TORRENT_URLS");
			String[] titles = intent.getStringArrayExtra("TORRENT_TITLES");
			if (urls != null) {
				for (int i = 0; i < urls.length; i++) {
					String title = (titles != null && titles.length >= i ? titles[i] : NavigationHelper.extractNameFromUri(Uri.parse(urls[i])));
					if (intent.hasExtra("PRIVATE_SOURCE")) {
						// This is marked by the Search Module as being a private source site; get the url locally first
						addTorrentFromPrivateSource(urls[i], title, intent.getStringExtra("PRIVATE_SOURCE"));
					} else {
						addTorrentByUrl(urls[i], title);
					}
				}
			}
			return;
		}

		// Add a torrent from a local or remote data URI?
		if (dataUri == null) {
			return;
		}
		if (dataUri.getScheme() == null) {
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_invalid_url_form).colorResource(R.color.red));
			return;
		}

		// Get torrent title
		String title = NavigationHelper.extractNameFromUri(dataUri);
		if (intent.hasExtra("TORRENT_TITLE")) {
			title = intent.getStringExtra("TORRENT_TITLE");
		}

		// Adding a torrent from the Android downloads manager
		if (dataUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			addTorrentFromDownloads(dataUri, title);
			return;
		}

		// Adding a torrent from http or https URL
		if (dataUri.getScheme().equals("http") || dataUri.getScheme().equals("https")) {

			String privateSource = getIntent().getStringExtra("PRIVATE_SOURCE");

			WebsearchSetting match = null;
			if (privateSource == null) {
				// Check if the target URL is also defined as a web search in the user's settings
				List<WebsearchSetting> websearches = applicationSettings.getWebsearchSettings();
				for (WebsearchSetting setting : websearches) {
					Uri uri = Uri.parse(setting.getBaseUrl());
					if (uri.getHost() != null && uri.getHost().equals(dataUri.getHost())) {
						match = setting;
						break;
					}
				}
			}

			// If the URL is also a web search and it defines cookies, use the cookies by downloading the targeted
			// torrent file (while supplies the cookies to the HTTP request) instead of sending the URL directly to the
			// torrent client. If instead it is marked (by the Torrent Search module) as being form a private site, use
			// the Search Module instead to download the url locally first.
			if (match != null && match.getCookies() != null) {
				addTorrentFromWeb(data, match, title);
			} else if (privateSource != null) {
				addTorrentFromPrivateSource(data, title, privateSource);
			} else {
				// Normally send the URL to the torrent client
				addTorrentByUrl(data, title);
			}
			return;
		}

		// Adding a torrent from magnet URL
		if (dataUri.getScheme().equals("magnet")) {
			addTorrentByMagnetUrl(data, title);
			return;
		}

		// Adding a local .torrent file; the title we show is just the file name
		if (dataUri.getScheme().equals("file")) {
			addTorrentByFile(data, title);
		}

	}

	@Override
	protected void onPause() {
		if (searchMenu != null) {
			searchMenu.collapseActionView();
		}
		stopAutoRefresh();
		super.onPause();
	}

	@Override
	public boolean onSearchRequested() {
		if (searchMenu != null) {
			searchMenu.expandActionView();
		}
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (awaitingAddLocalFile != null && awaitingAddTitle != null &&
				Boolean.TRUE.equals(navigationHelper.handleTorrentReadPermissionResult(requestCode, grantResults))) {
			addTorrentByFile(awaitingAddLocalFile, awaitingAddTitle);
		}
	}

	@Click(R.id.addmenu_link_button)
	protected void startUrlEntryDialog() {
		addmenuButton.collapse();
		UrlEntryDialog.show(this);
	}

	@Click(R.id.addmenu_file_button)
	protected void startFilePicker() {
		addmenuButton.collapse();
		FilePickerHelper.startFilePicker(this);
	}

	@Background
	@OnActivityResult(FilePickerHelper.ACTIVITY_FILEPICKER)
	public void onFilePicked(int resultCode, Intent data) {
		// We should have received an Intent with a local torrent's Uri as data from the file picker
		if (data != null && data.getData() != null && !data.getData().toString().equals("")) {
			Uri dataUri = data.getData();

			// Get torrent title
			String title = NavigationHelper.extractNameFromUri(dataUri);

			// Adding a torrent from the via a content:// scheme (access through content provider stream)
			if (dataUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
				addTorrentFromDownloads(dataUri, title);
				return;
			}

			// Adding a .torrent file directly via the file:// scheme (we can access it directly)
			if (dataUri.getScheme().equals("file")) {
				addTorrentByFile(data.getDataString(), title);
			}

		}
	}

	@Click(R.id.addmenu_barcode_button)
	protected void startBarcodeScanner() {
		addmenuButton.collapse();
		BarcodeHelper.startBarcodeScanner(this, BarcodeHelper.ACTIVITY_BARCODE_ADDTORRENT);
	}

	@Background
	@OnActivityResult(BarcodeHelper.ACTIVITY_BARCODE_ADDTORRENT)
	public void onBarcodeScanned(int resultCode, Intent data) {
		if (data != null) {
			// We receive from the helper either a URL (as string) or a query we can start a search for
			String query = BarcodeHelper.handleScanResult(resultCode, data, navigationHelper.enableSearchUi());
			onBarcodeScanHandled(data.getStringExtra("SCAN_RESULT"), query);
		}
	}

	@UiThread
	protected void onBarcodeScanHandled(String barcode, String result) {
		log.d(this, "Scanned barcode " + barcode + " and got " + result);
		if (TextUtils.isEmpty(result)) {
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_noproductforcode).colorResource(R.color.red).type(SnackbarType.MULTI_LINE));
		} else if (result.startsWith("http") || result.startsWith("https")) {
			addTorrentByUrl(result, "QR code result"); // No torrent title known
		} else if (result.startsWith("magnet")) {
			String title = NavigationHelper.extractNameFromUri(Uri.parse(result));
			addTorrentByMagnetUrl(result, title);
		} else if (navigationHelper.enableSearchUi()) {
			startSearch(result, false, null, false);
		}
	}

	@OptionsItem(R.id.action_refresh)
	public void refreshScreen() {
		fragmentTorrents.updateIsLoading(true);
		refreshTorrents();
		if (Daemon.supportsStats(currentConnection.getType())) {
			getAdditionalStats();
		}
	}

	@OptionsItem(R.id.action_enableturtle)
	protected void enableTurtleMode() {
		updateTurtleMode(true);
	}

	@OptionsItem(R.id.action_disableturtle)
	protected void disableTurtleMode() {
		updateTurtleMode(false);
	}

	@OptionsItem(R.id.action_rss)
	protected void openRss() {
		RssfeedsActivity_.intent(this).start();
	}

	@OptionsItem(R.id.action_settings)
	protected void openSettings() {
		MainSettingsActivity_.intent(this).start();
	}

	@OptionsItem(R.id.action_help)
	protected void openHelp() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/download/")));
	}

	@OptionsItem(R.id.action_sort_byname)
	protected void sortByName() {
		fragmentTorrents.sortBy(TorrentsSortBy.Alphanumeric);
	}

	@OptionsItem(R.id.action_sort_status)
	protected void sortByStatus() {
		fragmentTorrents.sortBy(TorrentsSortBy.Status);
	}

	@OptionsItem(R.id.action_sort_done)
	protected void sortByDateDone() {
		fragmentTorrents.sortBy(TorrentsSortBy.DateDone);
	}

	@OptionsItem(R.id.action_sort_added)
	protected void sortByDateAdded() {
		fragmentTorrents.sortBy(TorrentsSortBy.DateAdded);
	}

	@OptionsItem(R.id.action_sort_percent)
	protected void sortByPercent() {
		fragmentTorrents.sortBy(TorrentsSortBy.Percent);
	}

	@OptionsItem(R.id.action_sort_downspeed)
	protected void sortByDownspeed() {
		fragmentTorrents.sortBy(TorrentsSortBy.DownloadSpeed);
	}

	@OptionsItem(R.id.action_sort_upspeed)
	protected void sortByUpspeed() {
		fragmentTorrents.sortBy(TorrentsSortBy.UploadSpeed);
	}

	@OptionsItem(R.id.action_sort_ratio)
	protected void sortByRatio() {
		fragmentTorrents.sortBy(TorrentsSortBy.Ratio);
	}

	@OptionsItem(R.id.action_sort_size)
	protected void sortBySize() {
		fragmentTorrents.sortBy(TorrentsSortBy.Size);
	}

	private SearchView.OnQueryTextListener filterQueryTextChanged = new SearchView.OnQueryTextListener() {
		@Override
		public boolean onQueryTextSubmit(String query) {
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText) {
			// Redirect to filter method which will directly apply it
			filterTorrents(newText);
			return true;
		}
	};

	/**
	 * Redirect the newly entered list filter to the torrents fragment.
	 * @param newFilterText The newly entered filter (or empty to clear the current filter).
	 */
	public void filterTorrents(String newFilterText) {
		fragmentTorrents.applyTextFilter(newFilterText);
	}

	/**
	 * Shows the a details fragment for the given torrent, either in the dedicated details fragment pane, in the same pane as the torrent list was
	 * displayed or by starting a details activity.
	 * @param torrent The torrent to show detailed statistics for
	 */
	public void openDetails(Torrent torrent) {
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.updateTorrent(torrent);
		} else {
			DetailsActivity_.intent(this).torrent(torrent).currentLabels(lastNavigationLabels).startForResult(RESULT_DETAILS);
		}
	}

	@Background
	protected void refreshTorrents() {
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute(log);
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(), ((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, true);
		}
	}

	@Background
	public void refreshTorrentDetails(Torrent torrent) {
		if (!Daemon.supportsFineDetails(currentConnection.getType())) {
			return;
		}
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute(log);
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof GetTorrentDetailsTaskSuccessResult) {
			onTorrentDetailsRetrieved(torrent, ((GetTorrentDetailsTaskSuccessResult) result).getTorrentDetails());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void refreshTorrentFiles(Torrent torrent) {
		if (!Daemon.supportsFileListing(currentConnection.getType())) {
			return;
		}
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute(log);
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof GetFileListTaskSuccessResult) {
			onTorrentFilesRetrieved(torrent, ((GetFileListTaskSuccessResult) result).getFiles());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void getAdditionalStats() {
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = GetStatsTask.create(currentConnection).execute(log);
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof GetStatsTaskSuccessResult) {
			onTurtleModeRetrieved(((GetStatsTaskSuccessResult) result).isAlternativeModeEnabled());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void updateTurtleMode(boolean enable) {
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = SetAlternativeModeTask.create(currentConnection, enable).execute(log);
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof DaemonTaskSuccessResult) {
			// Success; no need to retrieve it again - just update the visual indicator
			onTurtleModeRetrieved(enable);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void addTorrentByUrl(String url, String title) {
		DaemonTaskResult result = AddByUrlTask.create(currentConnection, url, title).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void addTorrentByMagnetUrl(String url, String title) {

		// Since v39 Chrome sends application/x-www-form-urlencoded magnet links and most torrent clients do not understand those, so decode first
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Ignore: UTF-8 is always available on Android devices
		}

		AddByMagnetUrlTask addByMagnetUrlTask = AddByMagnetUrlTask.create(currentConnection, url);
		if (!Daemon.supportsAddByMagnetUrl(currentConnection.getType())) {
			// No support for magnet links: forcefully let the task fail to report the error
			onCommunicationError(new DaemonTaskFailureResult(addByMagnetUrlTask, new DaemonException(DaemonException.ExceptionType.MethodUnsupported,
					currentConnection.getType().name() + " does not support magnet links")), false);
			return;
		}

		DaemonTaskResult result = addByMagnetUrlTask.execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}

	}

	@Background
	protected void addTorrentByFile(String localFile, String title) {
		if (!navigationHelper.checkTorrentReadPermission(this)) {
			// No read permission yet (which we get the result of in onRequestPermissionsResult)
			awaitingAddLocalFile = localFile;
			awaitingAddTitle = title;
			return;
		}
		DaemonTaskResult result = AddByFileTask.create(currentConnection, localFile).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	private void addTorrentFromDownloads(Uri contentUri, String title) {

		try {
			// Open the content uri as input stream and this via a local temporary file
			addTorrentFromStream(getContentResolver().openInputStream(contentUri), title);
		} catch (SecurityException e) {
			// No longer access to this file
			log.e(this, "No access given to " + contentUri.toString() + ": " + e.toString());
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
		} catch (FileNotFoundException e) {
			log.e(this, contentUri.toString() + " does not exist: " + e.toString());
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
		}
	}

	@Background
	protected void addTorrentFromPrivateSource(String url, String title, String source) {

		try {
			InputStream input = SearchHelper_.getInstance_(this).getFile(source, url);
			addTorrentFromStream(input, title);
		} catch (Exception e) {
			log.e(this, "Can't download private site torrent " + url + " from " + source + ": " + e.toString());
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
		}

	}

	@Background
	protected void addTorrentFromWeb(String url, WebsearchSetting websearchSetting, String title) {

		try {
			// Cookies are taken from the websearchSetting that we already matched against this target URL
			DefaultHttpClient httpclient = HttpHelper.createStandardHttpClient(false, null, null, true, null, 10000, null, -1);
			Map<String, String> cookies = HttpHelper.parseCookiePairs(websearchSetting.getCookies());
			String domain = Uri.parse(url).getHost();
			for (Entry<String, String> pair : cookies.entrySet()) {
				BasicClientCookie cookie = new BasicClientCookie(pair.getKey(), pair.getValue());
				cookie.setPath("/");
				cookie.setDomain(domain);
				httpclient.getCookieStore().addCookie(cookie);
			}

			// Download the torrent at the specified URL (which will first be written to a temporary file)
			// If we get an HTTP 401, 403 or 404 response, show an error to the user
			HttpResponse response = httpclient.execute(new HttpGet(url));
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED ||
					response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN ||
					response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				log.e(this, "Can't retrieve web torrent " + url + ": Unexpected HTTP response status code " +
						response.getStatusLine().toString());
				SnackbarManager.show(Snackbar.with(this).text(R.string.error_401).colorResource(R.color.red));
				return;
			}
			InputStream input = response.getEntity().getContent();
			addTorrentFromStream(input, title);
		} catch (Exception e) {
			log.e(this, "Can't retrieve web torrent " + url + ": " + e.toString());
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
		}
	}

	@Background
	protected void addTorrentFromStream(InputStream input, String title) {

		File tempFile = new File("/not/yet/set");
		try {
			// Write a temporary file with the torrent contents
			tempFile = File.createTempFile("transdroid_", ".torrent", getCacheDir());
			FileOutputStream output = new FileOutputStream(tempFile);
			try {
				final byte[] buffer = new byte[1024];
				int read;
				while ((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
				output.flush();
				String fileName = Uri.fromFile(tempFile).toString();
				addTorrentByFile(fileName, title);
			} finally {
				output.close();
			}
		} catch (IOException e) {
			log.e(this, "Can't write input stream to " + tempFile.toString() + ": " + e.toString());
			SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				log.e(this, "Error closing the input stream " + tempFile.toString() + ": " + e.toString());
				SnackbarManager.show(Snackbar.with(this).text(R.string.error_torrentfile).colorResource(R.color.red));
			}
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_resumed, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void pauseTorrent(Torrent torrent) {
		torrent.mimicPause();
		DaemonTaskResult result = PauseTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_paused, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void startTorrent(Torrent torrent, boolean forced) {
		torrent.mimicStart();
		DaemonTaskResult result = StartTask.create(currentConnection, torrent, forced).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_started, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void stopTorrent(Torrent torrent) {
		torrent.mimicStop();
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_stopped, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result,
					getString(withData ? R.string.result_removed_with_data : R.string.result_removed, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLabel(Torrent torrent, String newLabel) {
		torrent.mimicNewLabel(newLabel);
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel == null ? "" : newLabel).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result,
					newLabel == null ? getString(R.string.result_labelremoved) : getString(R.string.result_labelset, newLabel));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void forceRecheckTorrent(Torrent torrent) {
		torrent.mimicCheckingStatus();
		DaemonTaskResult result = ForceRecheckTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_recheckedstarted, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_trackersupdated));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_locationset, newLocation));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updatePriority(Torrent torrent, List<TorrentFile> files, Priority priority) {
		DaemonTaskResult result = SetFilePriorityTask.create(currentConnection, torrent, priority, new ArrayList<>(files)).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_priotitiesset));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void updateMaxSpeeds(Integer maxDownloadSpeed, Integer maxUploadSpeed) {
		DaemonTaskResult result = SetTransferRatesTask.create(currentConnection, maxUploadSpeed, maxDownloadSpeed).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_maxspeedsset));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, String successMessage) {
		// Refresh the screen as well
		refreshScreen();
		SnackbarManager.show(Snackbar.with(this).text(successMessage));
	}

	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result, boolean isCritical) {
		//noinspection ThrowableResultOfMethodCallIgnored
		log.i(this, result.getException().toString());
		String error = getString(LocalTorrent.getResourceForDaemonException(result.getException()));
		SnackbarManager.show(Snackbar.with(this).text(error).colorResource(R.color.red).type(SnackbarType.MULTI_LINE));
		fragmentTorrents.updateIsLoading(false);
		if (isCritical) {
			fragmentTorrents.updateError(error);
			if (fragmentDetails != null && fragmentDetails.isAdded()) {
				fragmentDetails.updateIsLoading(false, error);
			}
		}
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {

		lastNavigationLabels = Label.convertToNavigationLabels(labels, getResources().getString(R.string.labels_unlabeled));

		// Report the newly retrieved list of torrents to the torrents fragment
		fragmentTorrents.updateIsLoading(false);
		fragmentTorrents.updateTorrents(new ArrayList<>(torrents), lastNavigationLabels);

		// Update the details fragment if the currently shown torrent is in the newly retrieved list
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.perhapsUpdateTorrent(torrents);
		}

		// Update local list of labels in the navigation
		navigationListAdapter.updateLabels(lastNavigationLabels);
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.updateLabels(lastNavigationLabels);
		}

		// Perhaps we were still waiting to preselect the last used filter (on a fresh application start)
		if (preselectNavigationFilter != null && navigationListAdapter != null) {
			for (int i = 0; i < navigationListAdapter.getCount(); i++) {
				// Look up the navigation filter item, which is represented as simple list item (and might not exist any
				// more, such as with a label that is deleted on the server)
				Object item = navigationListAdapter.getItem(i);
				if (item instanceof SimpleListItem && item instanceof NavigationFilter &&
						((NavigationFilter) item).getCode().equals(preselectNavigationFilter)) {
					filterSelected((SimpleListItem) item, false);
					break;
				}
			}
			// Only preselect after the first update we receive (even if the filter wasn't found any more)
			preselectNavigationFilter = null;
		}

		// Update the server status (counts and speeds) in the action bar
		serverStatusView
				.updateStatus(torrents, systemSettings.treatDormantAsInactive(), Daemon.supportsSetTransferRates(currentConnection.getType()));

	}

	@UiThread
	protected void onTorrentDetailsRetrieved(Torrent torrent, TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.updateTorrentDetails(torrent, torrentDetails);
		}
	}

	@UiThread
	protected void onTorrentFilesRetrieved(Torrent torrent, List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.updateTorrentFiles(torrent, new ArrayList<>(torrentFiles));
		}
	}

	@UiThread
	protected void onTurtleModeRetrieved(boolean turtleModeEnabled) {
		this.turtleModeEnabled = turtleModeEnabled;
		invalidateOptionsMenu();
	}

}
