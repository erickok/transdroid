package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.*;
import org.transdroid.core.gui.navigation.NavigationSelectionView.NavigationFilterManager;
import org.transdroid.core.gui.settings.*;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetStatsTask;
import org.transdroid.daemon.task.GetStatsTaskSuccessResult;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetAlternativeModeTask;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopTask;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.widget.SearchView;

@EActivity(resName="activity_torrents")
@OptionsMenu(resName="activity_torrents")
public class TorrentsActivity extends SherlockFragmentActivity implements OnNavigationListener, TorrentTasksExecutor, NavigationFilterManager {

	// Navigation components
	@Bean
	protected NavigationHelper navigationHelper;
	@ViewById
	protected SherlockListView filtersList;
	protected FilterListAdapter navigationListAdapter = null;
	protected FilterListAdapter navigationSpinnerAdapter = null;
	@SystemService
	protected SearchManager searchManager;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	@InstanceState
	boolean firstStart = true;
	private IDaemonAdapter currentConnection = null;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected boolean turleModeEnabled = false;
	
	// Torrents list components
	@FragmentById(resName="torrent_list")
	protected TorrentsFragment fragmentTorrents;
	
	// Details view components
	@FragmentById(resName="torrent_details")
	protected DetailsFragment fragmentDetails;
	
	@AfterViews
	protected void init() {

		// Set up navigation, with an action bar spinner and possibly (if room) with a filter list
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setHomeButtonEnabled(false);
		navigationSpinnerAdapter = FilterListAdapter_.getInstance_(this).setNavigationFilterManager(this);
		// Servers are always added to the action bar spinner
		navigationSpinnerAdapter.updateServers(applicationSettings.getServerSettings());
		getSupportActionBar().setListNavigationCallbacks(navigationSpinnerAdapter, this);
		if (filtersList != null) {
			// There was room for a dedicated filter list; add the status types
			navigationListAdapter = FilterListAdapter_.getInstance_(this);
			filtersList.setAdapter(navigationListAdapter);
			navigationListAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
			filtersList.setOnItemSelectedListener(onFilterListItemSelected);
		} else {
			// Add status types directly to the action bar spinner
			navigationSpinnerAdapter.updateStatusTypes(StatusType.getAllStatusTypes(this));
		}
		currentFilter = StatusType.getShowAllType(this);

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		if (lastUsed == null) {
			// No server settings yet; 
			return;
		}
		// Set this as selection in the action bar spinner; we can use the server setting key since we have stable ids
		// TODO: Does this call the action bar item selection callback? And refreshes?
		getSupportActionBar().setSelectedNavigationItem(lastUsed.getOrder());
		
		// Handle any start up intents
		if (firstStart) {
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
			return;
		}
		// There is a server now: select it to establish a connection
		filterSelected(lastUsed);
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
		fragmentTorrents.updateConnectionStatus(true);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		return true;
	}
	
	/**
	 * Called when an item in the action bar navigation spinner was selected
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		Object item = navigationSpinnerAdapter.getItem(itemPosition);
		if (item instanceof SimpleListItem) {
			// A filter item was selected form the navigation spinner
			filterSelected((SimpleListItem) item);
			return true;
		}
		// A header was selected; no action
		return false;
	}
	
	// Handles clicks (selections) on the dedicated list of filter items (if it exists)
	// NOTE: Unfortunately we cannot use the @ItemSelect(R.id.filters_list) annotation as it throws NPE exceptions when
	// the list doesn't exist (read: on small screens)
	protected OnItemSelectedListener onFilterListItemSelected = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			filterSelected((SimpleListItem) filtersList.getAdapter().getItem(position));
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO: Check if this happens
		}
	};
	
	/**
	 * A new filter was selected; update the view over the current data
	 * @param selected True if the filter item was selected, false if it was deselected
	 * @param item The touched filter item
	 */
	protected void filterSelected(SimpleListItem item) {
		
		// Server selection
		if (item instanceof ServerSetting) {
			ServerSetting server = (ServerSetting) item;
			
			if (currentConnection != null && server.equals(currentConnection.getSettings())) {
				// Already connected to this server; just ask for a refresh instead
				refreshTorrents();
				return;
			}
			
			// Update connection to the newly selected server and refresh
			currentConnection = server.createServerAdapter();
			applicationSettings.setLastUsedServer(server);
			clearScreens();
			refreshTorrents();
			return;

		}
		
		// Status type or label selection - both of which are navigation filters 
		if (item instanceof NavigationFilter) {
			currentFilter = (NavigationFilter) item;
			fragmentTorrents.applyFilter(currentFilter);
			// Clear the details view
			if (fragmentDetails != null) {
				fragmentDetails.clear();
			}
		}
		
	}

	@Override
	public String getActiveFilterText() {
		return currentFilter.getName();
	}

	@Override
	public String getActiveServerText() {
		return currentConnection.getSettings().getName();
	}

	/**
	 * If required, add torrents, switch to a specific server, etc.
	 */
	protected void handleStartIntent() {
		// TODO: Handle start intent
	}

	@OptionsItem(resName="action_refresh")
	protected void refreshScreen() {
		refreshTorrents();
		getAdditionalStats();
	}

	@OptionsItem(resName="action_enableturtle")
	protected void enableTurtleMode() {
		updateTurtleMode(true);
	}

	@OptionsItem(resName="action_disableturtle")
	protected void disableTurtleMode() {
		updateTurtleMode(false);
	}

	@OptionsItem(resName="action_settings")
	protected void openSettings() {
		MainSettingsActivity_.intent(this).start();
	}

	@OptionsItem(resName="action_help")
	protected void openHelp() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.transdroid.org/download/")));
	}
	
	private void clearScreens() {
		// Clear the currently shown list of torrent and perhaps the details
		fragmentTorrents.clear();
		if (fragmentDetails != null) {
			fragmentDetails.clear();
		}
	}

	@Background
	protected void refreshTorrents() {
		fragmentTorrents.updateIsLoading(true);
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute();
		fragmentTorrents.updateIsLoading(false);
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(), ((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}

	@Background
	protected void getAdditionalStats() {
		DaemonTaskResult result = GetStatsTask.create(currentConnection).execute();
		if (result instanceof GetStatsTaskSuccessResult) {
			onTurtleModeRetrieved(((GetStatsTaskSuccessResult) result).isAlternativeModeEnabled());
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}
	
	@Background
	protected void updateTurtleMode(boolean enable) {
		DaemonTaskResult result = SetAlternativeModeTask.create(currentConnection, enable).execute();
		if (result instanceof GetStatsTaskSuccessResult) {
			// Success; no need to retrieve it again - just update the visual indicator
			onTurtleModeRetrieved(enable);
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_resumed);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void pauseTorrent(Torrent torrent) {
		torrent.mimicPause();
		DaemonTaskResult result = PauseTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_paused);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void startTorrent(Torrent torrent, boolean forced) {
		torrent.mimicStart();
		DaemonTaskResult result = StartTask.create(currentConnection, torrent, forced).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_started);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void stopTorrent(Torrent torrent) {
		torrent.mimicStop();
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_stopped);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, withData ? R.string.result_removed_with_data
					: R.string.result_removed);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateLabel(Torrent torrent, String newLabel) {
		torrent.mimicNewLabel(newLabel);
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_labelset, newLabel);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_trackersupdated);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_locationset, newLocation);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, int successMessageId, String... messageParams) {
		// TODO: Properly report this success
		Toast.makeText(this, getString(successMessageId, (Object[]) messageParams), Toast.LENGTH_LONG).show();
	}

	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result) {
		Log.i(this, result.getException().toString());
		// TODO: Properly report this error
		Toast.makeText(this, getString(LocalTorrent.getResourceForDaemonException(result.getException())),
				Toast.LENGTH_LONG).show();
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {
		// Report the newly retrieved list of torrents to the torrents fragment
		fragmentTorrents.updateTorrents(new ArrayList<Torrent>(torrents));
		// Update the details fragment if the currently shown torrent is in the newly retrieved list
		if (fragmentDetails != null) {
			fragmentDetails.perhapsUpdateTorrent(torrents);
		}
		// TODO: Update local list of labels
	}

	@UiThread
	protected void onTurtleModeRetrieved(boolean turtleModeEnabled) {
		turleModeEnabled = turtleModeEnabled;
		supportInvalidateOptionsMenu();
	}

}
