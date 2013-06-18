package org.transdroid.core.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.transdroid.core.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.log.*;
import org.transdroid.core.gui.navigation.*;
import org.transdroid.core.gui.search.BarcodeHelper;
import org.transdroid.core.gui.search.FilePickerHelper;
import org.transdroid.core.gui.search.UrlEntryDialog;
import org.transdroid.core.gui.settings.*;
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
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopTask;
import org.transdroid.daemon.util.DLog;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.widget.SearchView;

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
public class TorrentsActivity extends SherlockFragmentActivity implements OnNavigationListener, TorrentTasksExecutor {

	// Navigation components
	@Bean
	protected NavigationHelper navigationHelper;
	@ViewById
	protected SherlockListView filtersList;
	protected FilterListAdapter navigationListAdapter = null;
	protected FilterListDropDownAdapter navigationSpinnerAdapter = null;
	protected ServerStatusView serverStatusView;
	@SystemService
	protected SearchManager searchManager;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	@InstanceState
	boolean firstStart = true;
	boolean skipNextOnNavigationItemSelectedCall = false;
	private IDaemonAdapter currentConnection = null;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected boolean turleModeEnabled = false;

	// Contained torrent and details fragments
	@FragmentById(resName = "torrent_list")
	protected TorrentsFragment fragmentTorrents;
	@FragmentById(resName = "torrent_details")
	protected DetailsFragment fragmentDetails;

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

		// Set up navigation, with an action bar spinner, server status indicator and possibly (if room) with a filter
		// list
		serverStatusView = ServerStatusView_.build(this);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setHomeButtonEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		getSupportActionBar().setCustomView(serverStatusView);
		navigationSpinnerAdapter = FilterListDropDownAdapter_.getInstance_(this);
		// Servers are always added to the action bar spinner
		navigationSpinnerAdapter.updateServers(applicationSettings.getServerSettings());

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
		// Now that all items (or at least their adapters) have been added
		currentFilter = StatusType.getShowAllType(this);
		getSupportActionBar().setListNavigationCallbacks(navigationSpinnerAdapter, this);

		// Log messages from the server daemons using our singleton logger
		DLog.setLogger(Log_.getInstance_(this));

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		if (lastUsed == null) {
			// No server settings yet;
			return;
		}
		// Set this as selection in the action bar spinner; we can use the server setting key since we have stable ids
		getSupportActionBar().setSelectedNavigationItem(lastUsed.getOrder() + 1);
		skipNextOnNavigationItemSelectedCall = true;

		// Handle any start up intents
		if (firstStart && getIntent() != null) {
			handleStartIntent();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Refresh server settings
		navigationSpinnerAdapter.updateServers(applicationSettings.getServerSettings());
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		if (lastUsed == null) {
			// Still no settings
			updateFragmentVisibility(false);
			return;
		}
		// There is a server know (now): forcefully select it to establish a connection
		filterSelected(lastUsed, true);
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
				fragmentTorrents.updateConnectionStatus(false);
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
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
		menu.findItem(R.id.action_filter).setVisible(true);
		menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.findItem(R.id.action_help).setVisible(false);
		if (fragmentTorrents != null)
			fragmentTorrents.updateConnectionStatus(true);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		return true;
	}

	/**
	 * Called when an item in the action bar navigation spinner was selected
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (skipNextOnNavigationItemSelectedCall) {
			skipNextOnNavigationItemSelectedCall = false;
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
			filterSelected((SimpleListItem) filtersList.getAdapter().getItem(position), false);
		}
	};

	/**
	 * A new filter was selected; update the view over the current data
	 * @param item The touched filter item
	 * @param forceNewConnection Whether a new connection should be initialised regardless of the old server selection
	 */
	protected void filterSelected(SimpleListItem item, boolean forceNewConnection) {

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
			currentConnection = server.createServerAdapter();
			applicationSettings.setLastUsedServer(server);
			navigationSpinnerAdapter.updateCurrentServer(currentConnection);
			if (forceNewConnection)
				navigationSpinnerAdapter.updateCurrentFilter(currentFilter);

			// Clear the currently shown list of torrents and perhaps the details
			fragmentTorrents.clear(true);
			if (fragmentDetails != null && fragmentDetails.getActivity() != null) {
				fragmentDetails.clear();
			}
			updateFragmentVisibility(true);
			refreshScreen();
			return;

		}

		// Status type or label selection - both of which are navigation filters
		if (item instanceof NavigationFilter) {
			currentFilter = (NavigationFilter) item;
			fragmentTorrents.applyNavigationFilter(currentFilter);
			navigationSpinnerAdapter.updateCurrentFilter(currentFilter);
			// Clear the details view
			if (fragmentDetails != null) {
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
		if (fragmentDetails != null) {
			if (hasServerSettings)
				getSupportFragmentManager().beginTransaction().show(fragmentDetails).commit();
			else
				getSupportFragmentManager().beginTransaction().hide(fragmentDetails).commit();
		}
	}

	/**
	 * If required, add torrents, switch to a specific server, etc.
	 */
	protected void handleStartIntent() {

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
					addTorrentByUrl(urls[i], (titles != null && titles.length >= i ? titles[i] : "Torrent"));
				}
			}
			return;
		}

		// Add a torrent from a local or remote data URI?
		if (dataUri == null)
			return;

		// Adding a torrent from the Android downloads manager
		if (dataUri.getScheme() != null && dataUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			addTorrentFromDownloads(dataUri);
			return;
		}

		// Adding a torrent from http or https URL
		if (dataUri.getScheme().equals("http") || dataUri.getScheme().equals("https")) {
			String title = data.substring(data.lastIndexOf("/"));
			if (intent.hasExtra("TORRENT_TITLE")) {
				title = intent.getStringExtra("TORRENT_TITLE");
			}
			addTorrentByUrl(data, title);
			return;
		}

		// Adding a torrent from magnet URL
		if (dataUri.getScheme().equals("magnet")) {
			addTorrentByMagnetUrl(data);
			return;
		}

		// Adding a local .torrent file
		if (dataUri.getScheme().equals("file")) {
			String title = data.substring(data.lastIndexOf("/"));
			addTorrentByFile(data, title);
			return;
		}

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
		if (data != null && data.getData() != null && !data.getData().equals("")) {
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
		if (query.startsWith("http"))
			addTorrentByUrl(query, "QR code result"); // No torrent title known
		else
			startSearch(query, false, null, false);
	}

	@OptionsItem(resName = "action_refresh")
	protected void refreshScreen() {
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

	@OptionsItem(resName = "action_sort_upspeed")
	protected void sortByUpspeed() {
		fragmentTorrents.sortBy(TorrentsSortBy.UploadSpeed);
	}

	@OptionsItem(resName = "action_sort_ratio")
	protected void sortByRatio() {
		fragmentTorrents.sortBy(TorrentsSortBy.Ratio);
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
		if (fragmentDetails != null) {
			fragmentDetails.updateTorrent(torrent);
		} else {
			DetailsActivity_.intent(this).torrent(torrent).start();
		}
	}

	@Background
	protected void refreshTorrents() {
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute();
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
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute();
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
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute();
		if (result instanceof GetFileListTaskSuccessResult) {
			onTorrentFilesRetrieved(torrent, ((GetFileListTaskSuccessResult) result).getFiles());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void getAdditionalStats() {
		DaemonTaskResult result = GetStatsTask.create(currentConnection).execute();
		if (result instanceof GetStatsTaskSuccessResult) {
			onTurtleModeRetrieved(((GetStatsTaskSuccessResult) result).isAlternativeModeEnabled());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void updateTurtleMode(boolean enable) {
		DaemonTaskResult result = SetAlternativeModeTask.create(currentConnection, enable).execute();
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
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void addTorrentByMagnetUrl(String url) {
		DaemonTaskResult result = AddByMagnetUrlTask.create(currentConnection, url).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, "Torrent"));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	protected void addTorrentByFile(String localFile, String title) {
		DaemonTaskResult result = AddByFileTask.create(currentConnection, localFile).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, title));
			refreshTorrents();
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	private void addTorrentFromDownloads(Uri contentUri) {

		InputStream input = null;
		try {
			// Open the content uri as input stream
			input = getContentResolver().openInputStream(contentUri);

			// Write a temporary file with the torrent contents
			File tempFile = File.createTempFile("transdroid_", ".torrent", getCacheDir());
			FileOutputStream output = new FileOutputStream(tempFile);
			try {
				final byte[] buffer = new byte[1024];
				int read;
				while ((read = input.read(buffer)) != -1)
					output.write(buffer, 0, read);
				output.flush();
				String fileName = Uri.fromFile(tempFile).toString();
				addTorrentByFile(fileName, fileName.substring(fileName.lastIndexOf("/")));
			} finally {
				output.close();
			}
		} catch (SecurityException e) {
			// No longer access to this file
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		} catch (IOException e1) {
			// Can't write temporary file
			Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				Crouton.showText(this, R.string.error_torrentfile, NavigationHelper.CROUTON_ERROR_STYLE);
			}
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
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
		if (result instanceof DaemonTaskResult) {
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
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_started));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void stopTorrent(Torrent torrent) {
		torrent.mimicStop();
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_stopped));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute();
		if (result instanceof DaemonTaskResult) {
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
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_labelset, newLabel));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_trackersupdated));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute();
		if (result instanceof DaemonTaskResult) {
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
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_priotitiesset));
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
		if (isCritical)
			fragmentTorrents.updateError(error);
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {
		
		// Report the newly retrieved list of torrents to the torrents fragment
		fragmentTorrents.updateIsLoading(false);
		fragmentTorrents.updateTorrents(new ArrayList<Torrent>(torrents));
		
		// Update the details fragment if the currently shown torrent is in the newly retrieved list
		if (fragmentDetails != null) {
			fragmentDetails.perhapsUpdateTorrent(torrents);
		}
		
		// Update local list of labels in the navigation
		List<Label> navigationLabels = Label.convertToNavigationLabels(labels,
				getResources().getString(R.string.labels_unlabeled));
		if (navigationListAdapter != null) {
			// Labels are shown in the dedicated side navigation
			navigationListAdapter.updateLabels(navigationLabels);
		} else {
			// Labels are shown in the action bar spinner
			navigationSpinnerAdapter.updateLabels(navigationLabels);
		}
		
		// Update the server status (counts and speeds) in the action bar
		serverStatusView.update(torrents);
		
	}

	@UiThread
	protected void onTorrentDetailsRetrieved(Torrent torrent, TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		if (fragmentDetails != null)
			fragmentDetails.updateTorrentDetails(torrent, torrentDetails);
	}

	@UiThread
	protected void onTorrentFilesRetrieved(Torrent torrent, List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		if (fragmentDetails != null)
			fragmentDetails.updateTorrentFiles(torrent, new ArrayList<TorrentFile>(torrentFiles));
	}

	@UiThread
	protected void onTurtleModeRetrieved(boolean turtleModeEnabled) {
		turleModeEnabled = turtleModeEnabled;
		supportInvalidateOptionsMenu();
	}

}
