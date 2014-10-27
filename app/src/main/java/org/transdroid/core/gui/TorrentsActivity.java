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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.transdroid.R;
import org.transdroid.core.app.search.*;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.lists.NoProgressHeaderTransformer;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.log.*;
import org.transdroid.core.gui.navigation.*;
import org.transdroid.core.gui.rss.*;
import org.transdroid.core.gui.search.BarcodeHelper;
import org.transdroid.core.gui.search.FilePickerHelper;
import org.transdroid.core.gui.search.UrlEntryDialog;
import org.transdroid.core.gui.settings.*;
import org.transdroid.core.service.BootReceiver;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.core.widget.ListWidgetProvider;
import org.transdroid.daemon.Daemon;
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
import org.transdroid.daemon.util.DLog;
import org.transdroid.daemon.util.HttpHelper;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher.Options;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Main activity that holds the fragment that shows the torrents list, presents a way to filter the list (via an action
 * bar spinner or list side list) and potentially shows a torrent details fragment too, if there is room. Task execution
 * such as loading of and adding torrents is performs in this activity, using background methods. Finally, the activity
 * offers navigation elements such as access to settings and showing connection issues.
 * @author Eric Kok
 */
@EActivity(resName = "activity_torrents")
@OptionsMenu(resName = "activity_torrents")
public class TorrentsActivity extends Activity implements OnNavigationListener, TorrentTasksExecutor,
		RefreshableActivity {

	private static final int RESULT_DETAILS = 0;

	// Navigation components
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@ViewById
	protected ListView filtersList;
	protected FilterListAdapter navigationListAdapter = null;
	protected FilterListDropDownAdapter navigationSpinnerAdapter = null;
	protected ServerStatusView serverStatusView;
	@SystemService
	protected SearchManager searchManager;
	private MenuItem searchMenu = null;
	private PullToRefreshAttacher pullToRefreshAttacher = null;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected SystemSettings systemSettings;
	@InstanceState
	boolean firstStart = true;
	int skipNextOnNavigationItemSelectedCalls = 2;
	private IDaemonAdapter currentConnection = null;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected String preselectNavigationFilter = null;
	@InstanceState
	protected boolean turleModeEnabled = false;
	@InstanceState
	protected ArrayList<Label> lastNavigationLabels;

	// Contained torrent and details fragments
	@FragmentById(resName = "torrents_fragment")
	protected TorrentsFragment fragmentTorrents;
	@FragmentById(resName = "torrentdetails_fragment")
	protected DetailsFragment fragmentDetails;

	// Auto refresh task
	private AsyncTask<Void, Void, Void> autoRefreshTask;
	// Fragment uses this to pause the refresh across restarts
	public boolean stopRefresh = false;

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

		// Set up navigation, with an action bar spinner, server status indicator and possibly (if room) with a filter
		// list
		serverStatusView = ServerStatusView_.build(this);
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(serverStatusView);
		navigationSpinnerAdapter = FilterListDropDownAdapter_.getInstance_(this);
		// Servers are always added to the action bar spinner
		navigationSpinnerAdapter.updateServers(applicationSettings.getAllServerSettings());

		// Check if there was room for a dedicated filter list (i.e. on tablets)
		if (filtersList != null) {
			// The action bar spinner doesn't have to show the 'servers' label, as it will only contain servers
			navigationSpinnerAdapter.hideServersLabel();
			// Create dedicated side list adapter and add the status types
			navigationListAdapter = FilterListAdapter_.getInstance_(this);
			navigationListAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
			// Add an empty labels list (which will be updated later, but the adapter needs to be created now)
			navigationListAdapter.updateLabels(new ArrayList<Label>());
			filtersList.setAdapter(navigationListAdapter);
			filtersList.setOnItemClickListener(onFilterListItemClicked);
		} else {
			// Add status types directly to the action bar spinner
			navigationSpinnerAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
			// Add an empty labels list (which will be updated later, but the adapter needs to be created now)
			navigationSpinnerAdapter.updateLabels(new ArrayList<Label>());
		}
		// Now that all items (or at least their adapters) have been added, ensure a filter is selected
		// NOTE When this is a fresh start, it might override the filter later (based on the last user selection)
		if (currentFilter == null) {
			currentFilter = StatusType.getShowAllType(this);
		}
		actionBar.setListNavigationCallbacks(navigationSpinnerAdapter, this);

		// Log messages from the server daemons using our singleton logger
		DLog.setLogger(Log_.getInstance_(this));

		// Load the default server or a server that was explicitly supplied in the starting intent
		ServerSetting defaultServer = applicationSettings.getDefaultServer();
		if (defaultServer == null) {
			// No server settings yet
			return;
		}
		Torrent openTorrent = null;
		if (getIntent().getAction() != null && getIntent().getAction().equals(ListWidgetProvider.INTENT_STARTSERVER)
				&& getIntent().getExtras() == null && getIntent().hasExtra(ListWidgetProvider.EXTRA_SERVER)) {
			// A server settings order ID was provided in this org.transdroid.START_SERVER action intent
			int serverId = getIntent().getExtras().getInt(ListWidgetProvider.EXTRA_SERVER);
			if (serverId < 0 || serverId > applicationSettings.getMaxOfAllServers()) {
				Log.e(this, "Tried to start with " + ListWidgetProvider.EXTRA_SERVER + " intent but " + serverId
						+ " is not an existing server order id");
			} else {
				defaultServer = applicationSettings.getServerSetting(serverId);
				if (getIntent().hasExtra(ListWidgetProvider.EXTRA_TORRENT))
					openTorrent = getIntent().getParcelableExtra(ListWidgetProvider.EXTRA_TORRENT);
			}
		}

		// Set this as selection in the action bar spinner; we can use the server setting key since we have stable ids
		// Note: skipNextOnNavigationItemSelectedCalls is used to prevent this event from triggering filterSelected
		actionBar.setSelectedNavigationItem(defaultServer.getOrder() + 1);

		// Connect to the last used server or a server that was explicitly supplied in the starting intent
		if (firstStart) {
			// Force first torrents refresh
			filterSelected(defaultServer, true);
			// Perhaps we can select the last used navigation filter, but only after a first refresh was completed
			preselectNavigationFilter = applicationSettings.getLastUsedNavigationFilter();
			// Handle any start up intents
			if (openTorrent != null) {
				openDetails(openTorrent);
				openTorrent = null;
			} else if (getIntent() != null) {
				handleStartIntent();
			}
		} else {
			// Resume after instead of fully loading the torrents list; create connection and set action bar title
			ServerSetting lastUsed = applicationSettings.getLastUsedServer();
			currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
			navigationSpinnerAdapter.updateCurrentServer(currentConnection);
			navigationSpinnerAdapter.updateCurrentFilter(currentFilter);
		}
		firstStart = false;

		// Start the alarms for the background services, if needed
		BootReceiver.startBackgroundServices(getApplicationContext(), false);
		BootReceiver.startAppUpdatesService(getApplicationContext());

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Refresh server settings
		navigationSpinnerAdapter.updateServers(applicationSettings.getAllServerSettings());
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		if (lastUsed == null) {
			// Still no settings
			updateFragmentVisibility(false);
			return;
		}

		// If we had no connection before, establish it now; otherwise just reload the settings
		if (currentConnection == null)
			filterSelected(lastUsed, true);
		else
			currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);

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
		if (autoRefreshTask != null || stopRefresh || systemSettings.getRefreshIntervalMilliseconds() == 0)
			return;

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
					if (isCancelled())
						return null;

					refreshTorrents();
					if (Daemon.supportsStats(currentConnection.getType()))
						getAdditionalStats();
				}
				return null;
			}

		};
		// Executes serially by default on Honeycomb, was parallel before
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			autoRefreshTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			autoRefreshTask.execute();
	}

	public void stopAutoRefresh() {
		if (autoRefreshTask != null)
			autoRefreshTask.cancel(true);
		autoRefreshTask = null;
	}

	@Override
	protected void onDestroy() {
		Crouton.cancelAllCroutons();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (navigationHelper.enableSearchUi()) {
			// Add an expandable SearchView to the action bar
			MenuItem item = menu.findItem(R.id.action_search);
			SearchView searchView = new SearchView(this);
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
			item.setOnActionExpandListener(new OnActionExpandListener() {
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
			item.setActionView(searchView);
			searchMenu = item;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// No connection yet; hide all menu options except settings
		if (currentConnection == null) {
			menu.findItem(R.id.action_add).setVisible(false);
			menu.findItem(R.id.action_search).setVisible(false);
			menu.findItem(R.id.action_rss).setVisible(false);
			menu.findItem(R.id.action_enableturtle).setVisible(false);
			menu.findItem(R.id.action_disableturtle).setVisible(false);
			menu.findItem(R.id.action_refresh).setVisible(false);
			menu.findItem(R.id.action_sort).setVisible(false);
			menu.findItem(R.id.action_filter).setVisible(false);
			menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.findItem(R.id.action_help).setVisible(true);
			if (fragmentTorrents != null)
				fragmentTorrents.updateConnectionStatus(false, null);
			getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			return true;
		}

		// There is a connection (read: settings to some server known)
		menu.findItem(R.id.action_add).setVisible(true);
		menu.findItem(R.id.action_search).setVisible(navigationHelper.enableSearchUi());
		menu.findItem(R.id.action_rss).setVisible(navigationHelper.enableRssUi());
		boolean hasAltMode = Daemon.supportsSetAlternativeMode(currentConnection.getType());
		menu.findItem(R.id.action_enableturtle).setVisible(hasAltMode && !turleModeEnabled);
		menu.findItem(R.id.action_disableturtle).setVisible(hasAltMode && turleModeEnabled);
		menu.findItem(R.id.action_refresh).setVisible(true);
		menu.findItem(R.id.action_sort).setVisible(true);
		menu.findItem(R.id.action_sort_added).setVisible(Daemon.supportsDateAdded(currentConnection.getType()));
		menu.findItem(R.id.action_filter).setVisible(true);
		menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.findItem(R.id.action_help).setVisible(false);
		if (fragmentTorrents != null)
			fragmentTorrents.updateConnectionStatus(true, currentConnection.getType());
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		return true;
	}

	/**
	 * Called when an item in the action bar navigation spinner was selected
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (skipNextOnNavigationItemSelectedCalls > 0) {
			skipNextOnNavigationItemSelectedCalls--;
			return false;
		}
		Object item = navigationSpinnerAdapter.getItem(itemPosition);
		if (item instanceof SimpleListItem) {
			// A filter item was selected form the navigation spinner
			filterSelected((SimpleListItem) item, false);
			return true;
		}
		// A header was selected; no action
		return false;
	}

	// Handles item selections on the dedicated list of filter items
	private OnItemClickListener onFilterListItemClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			filtersList.setItemChecked(position, true);
			Object item = filtersList.getAdapter().getItem(position);
			if (item instanceof SimpleListItem)
				filterSelected((SimpleListItem) item, false);
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
			navigationSpinnerAdapter.updateCurrentServer(currentConnection);
			if (forceNewConnection)
				navigationSpinnerAdapter.updateCurrentFilter(currentFilter);

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
			navigationSpinnerAdapter.updateCurrentFilter(currentFilter);
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
		if (filtersList != null)
			filtersList.setVisibility(hasServerSettings ? View.VISIBLE : View.GONE);
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			if (hasServerSettings)
				getFragmentManager().beginTransaction().show(fragmentDetails).commit();
			else
				getFragmentManager().beginTransaction().hide(fragmentDetails).commit();
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
		if (applicationSettings.getDefaultServerKey() == ApplicationSettings.DEFAULTSERVER_ASKONADD
				&& getIntent().getData() != null) {
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
		skipNextOnNavigationItemSelectedCalls++; // Prevent this selection from launching filterSelected() again
		getActionBar().setSelectedNavigationItem(position + 1);
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
					String title = (titles != null && titles.length >= i ? titles[i] : NavigationHelper
							.extractNameFromUri(Uri.parse(urls[i])));
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
		if (dataUri == null)
			return;
		if (dataUri.getScheme() == null) {
			Crouton.showText(this, R.string.error_invalid_url_form, NavigationHelper.CROUTON_ERROR_STYLE);
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
			return;
		}

	}

	@Override
	protected void onPause() {
		if (searchMenu != null)
			searchMenu.collapseActionView();
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

	@OptionsItem(resName = "action_add_fromurl")
	protected void startUrlEntryDialog() {
		UrlEntryDialog.startUrlEntry(this);
	}

	@OptionsItem(resName = "action_add_fromfile")
	protected void startFilePicker() {
		FilePickerHelper.startFilePicker(this);
	}

	@Background
	@OnActivityResult(FilePickerHelper.ACTIVITY_FILEPICKER)
	public void onFilePicked(int resultCode, Intent data) {
		// We should have received an Intent with a local torrent's Uri as data from the file picker
		if (data != null && data.getData() != null && !data.getData().toString().equals("")) {
			String url = data.getData().getPath();
			addTorrentByFile(data.getData().toString(), url.substring(url.lastIndexOf("/")));
		}
	}

	@OptionsItem(resName = "action_add_frombarcode")
	protected void startBarcodeScanner() {
		BarcodeHelper.startBarcodeScanner(this);
	}

	@Background
	@OnActivityResult(BarcodeHelper.ACTIVITY_BARCODE)
	public void onBarcodeScanned(int resultCode, Intent data) {
		// We receive from the helper either a URL (as string) or a query we can start a search for
		String query = BarcodeHelper.handleScanResult(resultCode, data);
		if (query.startsWith("http") || query.startsWith("https"))
			addTorrentByUrl(query, "QR code result"); // No torrent title known
		else
			startSearch(query, false, null, false);
	}

	/**
	 * Attaches some view (perhaps contained in a fragment) to this activity's pull to refresh support
	 * @param view The view to attach
	 */
	@Override
	public void addRefreshableView(View view) {
		if (pullToRefreshAttacher == null) {
			// Still need to initialise the PullToRefreshAttacher
			Options options = new PullToRefreshAttacher.Options();
			options.headerTransformer = new NoProgressHeaderTransformer();
			pullToRefreshAttacher = PullToRefreshAttacher.get(this, options);
		}
		pullToRefreshAttacher.addRefreshableView(view, new OnRefreshListener() {
			@Override
			public void onRefreshStarted(View view) {
				// Just refresh the full screen, now that the user has pulled to refresh
				pullToRefreshAttacher.setRefreshComplete();
				refreshScreen();
			}
		});
	}

	@OptionsItem(resName = "action_refresh")
	public void refreshScreen() {
		fragmentTorrents.updateIsLoading(true);
		refreshTorrents();
		if (Daemon.supportsStats(currentConnection.getType()))
			getAdditionalStats();
	}

	@OptionsItem(resName = "action_enableturtle")
	protected void enableTurtleMode() {
		updateTurtleMode(true);
	}

	@OptionsItem(resName = "action_disableturtle")
	protected void disableTurtleMode() {
		updateTurtleMode(false);
	}

	@OptionsItem(resName = "action_rss")
	protected void openRss() {
		RssfeedsActivity_.intent(this).start();
	}

	@OptionsItem(resName = "action_settings")
	protected void openSettings() {
		MainSettingsActivity_.intent(this).start();
	}

	@OptionsItem(resName = "action_help")
	protected void openHelp() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/download/")));
	}

	@OptionsItem(resName = "action_sort_byname")
	protected void sortByName() {
		fragmentTorrents.sortBy(TorrentsSortBy.Alphanumeric);
	}

	@OptionsItem(resName = "action_sort_status")
	protected void sortByStatus() {
		fragmentTorrents.sortBy(TorrentsSortBy.Status);
	}

	@OptionsItem(resName = "action_sort_done")
	protected void sortByDateDone() {
		fragmentTorrents.sortBy(TorrentsSortBy.DateDone);
	}

	@OptionsItem(resName = "action_sort_added")
	protected void sortByDateAdded() {
		fragmentTorrents.sortBy(TorrentsSortBy.DateAdded);
	}

	@OptionsItem(resName = "action_sort_percent")
	protected void sortByPercent() {
		fragmentTorrents.sortBy(TorrentsSortBy.Percent);
	}

	@OptionsItem(resName = "action_sort_downspeed")
	protected void sortByDownspeed() {
		fragmentTorrents.sortBy(TorrentsSortBy.DownloadSpeed);
	}

	@OptionsItem(resName = "action_sort_upspeed")
	protected void sortByUpspeed() {
		fragmentTorrents.sortBy(TorrentsSortBy.UploadSpeed);
	}

	@OptionsItem(resName = "action_sort_ratio")
	protected void sortByRatio() {
		fragmentTorrents.sortBy(TorrentsSortBy.Ratio);
	}

	@OptionsItem(resName = "action_sort_size")
	protected void sortBySize() {
		fragmentTorrents.sortBy(TorrentsSortBy.Size);
	}

	@OptionsItem(resName = "action_filter")
	protected void startFilterEntryDialog() {
		FilterEntryDialog.startFilterEntry(this);
	}

	/**
	 * Redirect the newly entered list filter to the torrents fragment.
	 * @param newFilterText The newly entered filter (or empty to clear the current filter).
	 */
	public void filterTorrents(String newFilterText) {
		fragmentTorrents.applyTextFilter(newFilterText);
	}

	/**
	 * Shows the a details fragment for the given torrent, either in the dedicated details fragment pane, in the same
	 * pane as the torrent list was displayed or by starting a details activity.
	 * @param torrent The torrent to show detailed statistics for
	 */
	public void openDetails(Torrent torrent) {
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.updateTorrent(torrent);
		} else {
			DetailsActivity_.intent(this).torrent(torrent).currentLabels(lastNavigationLabels)
					.startForResult(RESULT_DETAILS);
		}
	}

	@Background
	protected void refreshTorrents() {
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute();
		if (!startConnectionId.equals(currentConnection.getSettings().getIdString())) {
			// During the command execution the user changed the server, so we are no longer interested in the result
			return;
		}
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(),
					((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, true);
		}
	}

	@Background
	public void refreshTorrentDetails(Torrent torrent) {
		if (!Daemon.supportsFineDetails(currentConnection.getType()))
			return;
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute();
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
		if (!Daemon.supportsFileListing(currentConnection.getType()))
			return;
		String startConnectionId = currentConnection.getSettings().getIdString();
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute();
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
		DaemonTaskResult result = GetStatsTask.create(currentConnection).execute();
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
		DaemonTaskResult result = SetAlternativeModeTask.create(currentConnection, enable).execute();
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
		DaemonTaskResult result = AddByUrlTask.create(currentConnection, url, title).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void addTorrentByMagnetUrl(String url, String title) {
		DaemonTaskResult result = AddByMagnetUrlTask.create(currentConnection, url).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void addTorrentByFile(String localFile, String title) {
		DaemonTaskResult result = AddByFileTask.create(currentConnection, localFile).execute();
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
			Log.e(this, "No access given to " + contentUri.toString() + ": " + e.toString());
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		} catch (FileNotFoundException e) {
			Log.e(this, contentUri.toString() + " does not exist: " + e.toString());
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		}
	}

	@Background
	protected void addTorrentFromPrivateSource(String url, String title, String source) {

		try {
			InputStream input = SearchHelper_.getInstance_(this).getFile(source, url);
			addTorrentFromStream(input, title);
		} catch (Exception e) {
			Log.e(this, "Can't download private site torrent " + url + " from " + source + ": " + e.toString());
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		}

	}

	@Background
	protected void addTorrentFromWeb(String url, WebsearchSetting websearchSetting, String title) {

		try {
			// Cookies are taken from the websearchSetting that we already matched against this target URL
			DefaultHttpClient httpclient = HttpHelper.createStandardHttpClient(false, null, null, true, null, 10000,
					null, -1);
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
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
					|| response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
					|| response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				Log.e(this, "Can't retrieve web torrent " + url + ": Unexpected HTTP response status code "
						+ response.getStatusLine().toString());
				Crouton.showText(this, R.string.error_401, NavigationHelper.CROUTON_ERROR_STYLE);
				return;
			}
			InputStream input = response.getEntity().getContent();
			addTorrentFromStream(input, title);
		} catch (Exception e) {
			Log.e(this, "Can't retrieve web torrent " + url + ": " + e.toString());
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
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
				while ((read = input.read(buffer)) != -1)
					output.write(buffer, 0, read);
				output.flush();
				String fileName = Uri.fromFile(tempFile).toString();
				addTorrentByFile(fileName, title);
			} finally {
				output.close();
			}
		} catch (IOException e) {
			Log.e(this, "Can't write input stream to " + tempFile.toString() + ": " + e.toString());
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				Log.e(this, "Error closing the input stream " + tempFile.toString() + ": " + e.toString());
				Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
			}
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute();
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
		DaemonTaskResult result = PauseTask.create(currentConnection, torrent).execute();
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
		DaemonTaskResult result = StartTask.create(currentConnection, torrent, forced).execute();
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
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_stopped, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded(
					(DaemonTaskSuccessResult) result,
					getString(withData ? R.string.result_removed_with_data : R.string.result_removed, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLabel(Torrent torrent, String newLabel) {
		torrent.mimicNewLabel(newLabel);
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel == null ? "" : newLabel)
				.execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded(
					(DaemonTaskSuccessResult) result,
					newLabel == null ? getString(R.string.result_labelremoved) : getString(R.string.result_labelset,
							newLabel));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void forceRecheckTorrent(Torrent torrent) {
		torrent.mimicCheckingStatus();
		DaemonTaskResult result = ForceRecheckTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result,
					getString(R.string.result_recheckedstarted, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_trackersupdated));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_locationset, newLocation));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updatePriority(Torrent torrent, List<TorrentFile> files, Priority priority) {
		DaemonTaskResult result = SetFilePriorityTask.create(currentConnection, torrent, priority,
				new ArrayList<TorrentFile>(files)).execute();
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_priotitiesset));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void updateMaxSpeeds(Integer maxDownloadSpeed, Integer maxUploadSpeed) {
		DaemonTaskResult result = SetTransferRatesTask.create(currentConnection, maxUploadSpeed, maxDownloadSpeed)
				.execute();
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
		Crouton.showText(this, successMessage, NavigationHelper.CROUTON_INFO_STYLE);
	}

	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result, boolean isCritical) {
		Log.i(this, result.getException().toString());
		String error = getString(LocalTorrent.getResourceForDaemonException(result.getException()));
		Crouton.showText(this, error, NavigationHelper.CROUTON_ERROR_STYLE);
		fragmentTorrents.updateIsLoading(false);
		if (isCritical) {
			fragmentTorrents.updateError(error);
			if (fragmentDetails != null && fragmentDetails.isAdded())
				fragmentDetails.updateIsLoading(false, error);
		}
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {

		lastNavigationLabels = Label.convertToNavigationLabels(labels,
				getResources().getString(R.string.labels_unlabeled));

		// Report the newly retrieved list of torrents to the torrents fragment
		fragmentTorrents.updateIsLoading(false);
		fragmentTorrents.updateTorrents(new ArrayList<Torrent>(torrents), lastNavigationLabels);

		// Update the details fragment if the currently shown torrent is in the newly retrieved list
		if (fragmentDetails != null && fragmentDetails.isAdded()) {
			fragmentDetails.perhapsUpdateTorrent(torrents);
		}

		// Update local list of labels in the navigation
		if (navigationListAdapter != null) {
			// Labels are shown in the dedicated side navigation
			navigationListAdapter.updateLabels(lastNavigationLabels);
		} else {
			// Labels are shown in the action bar spinner
			navigationSpinnerAdapter.updateLabels(lastNavigationLabels);
		}
		if (fragmentDetails != null && fragmentDetails.isAdded())
			fragmentDetails.updateLabels(lastNavigationLabels);

		// Perhaps we were still waiting to preselect the last used filter (on a fresh application start)
		if (preselectNavigationFilter != null) {
			FilterListAdapter adapter = navigationListAdapter != null ? navigationListAdapter
					: navigationSpinnerAdapter;
			for (int i = 0; i < adapter.getCount(); i++) {
				// Regardless of the navigation style (side list or action bar spinner), we can look up the navigation
				// filter item, which is represented as simple list item (and might not exist any more, such as with a
				// label that is deleted on the server)
				Object item = adapter.getItem(i);
				if (item instanceof SimpleListItem && item instanceof NavigationFilter
						&& ((NavigationFilter) item).getCode().equals(preselectNavigationFilter)) {
					filterSelected((SimpleListItem) item, false);
					break;
				}
			}
			// Only preselect after the first update we receive (even if the filter wasn't found any more)
			preselectNavigationFilter = null;
		}

		// Update the server status (counts and speeds) in the action bar
		serverStatusView.update(torrents, systemSettings.treatDormantAsInactive());

	}

	@UiThread
	protected void onTorrentDetailsRetrieved(Torrent torrent, TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		if (fragmentDetails != null && fragmentDetails.isAdded())
			fragmentDetails.updateTorrentDetails(torrent, torrentDetails);
	}

	@UiThread
	protected void onTorrentFilesRetrieved(Torrent torrent, List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		if (fragmentDetails != null && fragmentDetails.isAdded())
			fragmentDetails.updateTorrentFiles(torrent, new ArrayList<TorrentFile>(torrentFiles));
	}

	@UiThread
	protected void onTurtleModeRetrieved(boolean turtleModeEnabled) {
		turleModeEnabled = turtleModeEnabled;
		invalidateOptionsMenu();
	}

}
