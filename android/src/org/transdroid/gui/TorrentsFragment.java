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
package org.transdroid.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.IDaemonCallback;
import org.transdroid.daemon.TaskQueue;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.TorrentsComparator;
import org.transdroid.daemon.TorrentsSortBy;
import org.transdroid.daemon.Label;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetStatsTask;
import org.transdroid.daemon.task.GetStatsTaskSuccessResult;
import org.transdroid.daemon.task.PauseAllTask;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeAllTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetAlternativeModeTask;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.task.StartAllTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopAllTask;
import org.transdroid.daemon.task.StopTask;
import org.transdroid.daemon.util.DLog;
import org.transdroid.daemon.util.FileSizeConverter;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.gui.SetLabelDialog.ResultListener;
import org.transdroid.gui.TorrentViewSelectorWindow.LabelSelectionListener;
import org.transdroid.gui.TorrentViewSelectorWindow.MainViewTypeSelectionListener;
import org.transdroid.gui.rss.RssFeeds;
import org.transdroid.gui.search.Search;
import org.transdroid.gui.util.ActivityUtil;
import org.transdroid.gui.util.DialogWrapper;
import org.transdroid.gui.util.ErrorLogSender;
import org.transdroid.gui.util.InterfaceSettings;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.PreferencesMain;
import org.transdroid.search.barcode.GoogleWebSearchBarcodeResolver;
import org.transdroid.service.AlarmSettings;
import org.transdroid.service.BootReceiver;
import org.transdroid.util.TLog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ActionBar.OnNavigationListener;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.SubMenu;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The main screen for the Transdroid application and provides most on-the-surface functionality 
 * as well. Server daemon and search engine communication is wrapped in adapters.
 * 
 * @author erickok
 *
 */
public class TorrentsFragment extends Fragment implements IDaemonCallback, OnTouchListener {

	private static final String LOG_NAME = "Main";
	
	private static final int ACTIVITY_PREFERENCES = 0;
	private static final int ACTIVITY_DETAILS = 1;
	private static final int ACTIVITY_BARCODE = 0x0000c0de;
	
	private static final int DIALOG_SERVERS = 0;
	private static final int DIALOG_RATES = 1;
	private static final int DIALOG_CHANGELOG = 2;
	private static final int DIALOG_ASKREMOVE = 3;
	private static final int DIALOG_SETLABEL = 4;
	private static final int DIALOG_ADDFAILED = 5;
	private static final int DIALOG_SETDOWNLOADLOCATION = 6;
	private static final int DIALOG_REFRESH_INTERVAL = 7;
	private static final int DIALOG_INSTALLBARCODESCANNER = 8;
	private static final int DIALOG_FILTER = 9;

	private static final int MENU_ADD_ID = 1;
	private static final int MENU_BARCODE_ID = 2;
	private static final int MENU_RSS_ID = 3;
	private static final int MENU_FORALL_ID = 4;
	private static final int MENU_SORT_ID = 5;
	
	private static final int MENU_SETTINGS_ID = 6;
	private static final int MENU_CHANGELOG_ID = 7;
	private static final int MENU_SWITCH_ID = 8;
	private static final int MENU_RATES_ID = 9;
	private static final int MENU_ERRORREPORT_ID = 10;

	private static final int MENU_REFRESH_ID = 11;
	private static final int MENU_SEARCH_ID = 12;
	private static final int MENU_ALTMODE_ID = 13;

	private static final int MENU_FORALL_GROUP_ID = 20;
	private static final int MENU_FORALL_PAUSE_ID = 21;
	private static final int MENU_FORALL_RESUME_ID = 22;
	private static final int MENU_FORALL_STOP_ID = 23;
	private static final int MENU_FORALL_START_ID = 24;
	
	private static final int MENU_SORT_GROUP_ID = 30;
	private static final int MENU_SORT_ALPHA_ID = 31;
	private static final int MENU_SORT_STATUS_ID = 32;
	private static final int MENU_SORT_DONE_ID = 33;
	private static final int MENU_SORT_ADDED_ID = 34;
	private static final int MENU_SORT_UPSPEED_ID = 35;
	private static final int MENU_SORT_RATIO_ID = 36;
	private static final int MENU_SORT_GTZERO_ID = 37;

	private static final int MENU_PAUSE_ID = 41;
	private static final int MENU_RESUME_ID = 42;
	private static final int MENU_STOP_ID = 43;
	private static final int MENU_START_ID = 44;
	private static final int MENU_FORCESTART_ID = 45;
	private static final int MENU_REMOVE_ID = 46;
	private static final int MENU_REMOVE_DATA_ID = 47;
	private static final int MENU_SETLABEL_ID = 48;
	private static final int MENU_SETDOWNLOADLOCATION_ID = 49;
	
	private static final int MENU_FILTER_ID = 60;

	protected boolean useTabletInterface;
	private Handler handler;
	private Runnable refreshRunable;
	private GestureDetector gestureDetector;
	private TextView emptyText;
	private TableLayout statusBox;
	private TextView taskmessage, statusDown, statusUp, statusDownRate, statusUpRate;
	private LinearLayout startsettings;
	private ImageView viewtypeselector;
	private TextView viewtype;
	private LinearLayout controlbar;
	private TorrentViewSelectorWindow viewtypeselectorpopup;

    // Variables determining which view over the torrents to show
	private TorrentsSortBy sortSetting = TorrentsSortBy.Alphanumeric;
	private boolean sortReversed = false;
	private MainViewType activeMainView = MainViewType.ShowAll;
	private boolean onlyShowTransferring = false;
	private List<String> availableLabels;
	private String activeLabel = null;
	private boolean inAlternativeMode = false; // Whether the server is in alternative (speed) mode (i.e. Transmission's Turtle Mode)
	protected boolean ignoreFirstListNavigation = true;
	private String activeFilter = null;
		
	private List<Torrent> allTorrents;
	private List<Label> allLabels;
 	
	private TaskQueue queue;
	private boolean inProgress = false;

	// Settings-related variables
	private IDaemonAdapter daemon;
	private static List<DaemonSettings> allDaemonSettings;
	private static int lastUsedDaemonSettings;
	private static InterfaceSettings interfaceSettings;
	private static AlarmSettings alarmServiceSettings;
	
	// Variables to store data to use inside the dialogs that we pop up
	private Torrent selectedTorrent;
	private boolean selectedRemoveData;
	private String failedTorrentUri;
	private String failedTorrentTitle;

	public TorrentsFragment() {
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		ignoreFirstListNavigation = true;
		return inflater.inflate(R.layout.fragment_torrents, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Attach the Android TLog to the daemon logger
		DLog.setLogger(TLog.getInstance());
		
        // Set activity screen features
		useTabletInterface = Transdroid.isTablet(getResources());
        registerForContextMenu(getListView());
        getListView().setTextFilterEnabled(true);
        getListView().setOnItemClickListener(onTorrentClicked);
        
        // Access UI widgets and title
        emptyText = (TextView) findViewById(android.R.id.empty);
        startsettings = (LinearLayout) findViewById(R.id.startsettings);
        taskmessage = (TextView) findViewById(R.id.taskmessage);
        statusBox = (TableLayout) findViewById(R.id.st_box);
        statusDown = (TextView) findViewById(R.id.st_down);
        statusDownRate = (TextView) findViewById(R.id.st_downrate);
        statusUp = (TextView) findViewById(R.id.st_up);
        statusUpRate = (TextView) findViewById(R.id.st_uprate);
        setProgressBar(false);
        
        // Open settings button
        Button startsettingsButton = (Button) findViewById(R.id.startsettings_button);
        startsettingsButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				// Open settings screen
				startActivityForResult(new Intent(getActivity(), PreferencesMain.class), ACTIVITY_PREFERENCES);
			}
		});
        // Show a dialog allowing control over max. upload and download speeds
        statusBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_RATES);
			}
		});
        
        // The control bar and view type selector (quickaction popup)
        controlbar = (LinearLayout) findViewById(R.id.controlbar);
        viewtype = (TextView) findViewById(R.id.viewtype);
        viewtypeselector = (ImageView) findViewById(R.id.viewtypeselector);
		viewtypeselectorpopup = new TorrentViewSelectorWindow(viewtypeselector, new MainViewTypeSelectionListener() {
			@Override
			public void onMainViewTypeSelected(MainViewType newType) {
				if (activeMainView != newType) {
					// Show torrents for the new main view selection
					activeMainView = newType;
					updateTorrentsView(true);
				}
			}
		}, new LabelSelectionListener() {
			@Override
			public void onLabelSelected(int labelPosition) {
				String newLabel = availableLabels.get(labelPosition);
				if (activeLabel != newLabel) {
					// Show torrents for the new label selection
					activeLabel = newLabel;
					updateTorrentsView(true);
				}
			}
		});
        viewtypeselector.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewtypeselectorpopup.showLikePopDownMenu();
			}
		});
        
        // Swiping (flinging) between server configurations (or labels)
        gestureDetector = new GestureDetector(new MainScreenGestureListener());
        getListView().setOnTouchListener(this);
        emptyText.setOnTouchListener(this);

        // Attach auto refresh handler
        handler = new Handler();
        refreshRunable = new Runnable() {
            public void run() {
                updateTorrentList();
                setupRefreshTimer();
            }
        };

        // Read all the user preferences
    	readSettings();
    	
    	// Setup the default server daemon
    	setupDaemon();

    	// Set up a task queue
		queue = new TaskQueue(new TaskResultHandler(this));
    	queue.start();

    	// Start the alarm service, if needed
		BootReceiver.startAlarm(getActivity().getApplicationContext());
    	
		// Check for new app version, if needed
		BootReceiver.startUpdateCheck(getActivity().getApplicationContext());
		
		handleIntent(getActivity().getIntent());
		
    }
    
    public void handleIntent(final Intent startIntent) {
    	
    	// Handle new intents that come from either a regular application startup, a startup from a 
    	// new intent or a new intent being send with the application already started
    	if (startIntent != null && (startIntent.getData() != null || 
    			(startIntent.getAction() != null && startIntent.getAction().equals(Transdroid.INTENT_ADD_MULTIPLE))) && 
    			!isLaunchedFromHistory(startIntent)) {
            
    		if (startIntent.getAction() != null && startIntent.getAction().equals(Transdroid.INTENT_ADD_MULTIPLE)) {

    			// Intent should have some extras pointing to possibly multiple torrents
        		String[] urls = startIntent.getStringArrayExtra(Transdroid.INTENT_TORRENT_URLS);
        		String[] titles = startIntent.getStringArrayExtra(Transdroid.INTENT_TORRENT_TITLES);
        		if (urls != null) {
		    		for (int i = 0; i < urls.length; i++) {
		    			addTorrentByUrl(urls[i], (titles != null && titles.length >= i? titles[i]: "Torrent"));
		    		}
        		}
        		
    		} else {
    			
    			// Intent should have some Uri data pointing to a single torrent
	    		String data = startIntent.getDataString();
	    		if (data != null && startIntent.getData() != null && startIntent.getData().getScheme() != null) {
					if (startIntent.getData().getScheme().equals(HttpHelper.SCHEME_HTTP)
							|| startIntent.getData().getScheme().equals(HttpHelper.SCHEME_HTTPS)) {
			        	// From a global intent to add a .torrent file via URL (maybe form the browser)
		    			String title = data.substring(data.lastIndexOf("/"));
		    			if (startIntent.hasExtra(Transdroid.INTENT_TORRENT_TITLE)) {
		    				title = startIntent.getStringExtra(Transdroid.INTENT_TORRENT_TITLE);
		    			}
			            addTorrentByUrl(data, title);
		    		} else if (startIntent.getData().getScheme().equals(HttpHelper.SCHEME_MAGNET)) {
		    			// From a global intent to add a magnet link via URL (usually from the browser)
		    			addTorrentByMagnetUrl(data);
		    		} else if (startIntent.getData().getScheme().equals(HttpHelper.SCHEME_FILE)) {
		    			// From a global intent to add via the contents of a local .torrent file (maybe form a file manager)
		    			addTorrentByFile(data);
		    		}
	    		}
	    		
    		}
            
        }

    	// Possibly switch to a specific daemon (other than the last used)
    	boolean forceUpdate = false;
    	if (startIntent != null && !isLaunchedFromHistory(startIntent) && 
    			startIntent.hasExtra(Transdroid.INTENT_OPENDAEMON)) {
    		String openDaemon = startIntent.getStringExtra(Transdroid.INTENT_OPENDAEMON);
    		if (!daemon.getSettings().getIdString().equals(openDaemon)) {
        		int openDaemonI = (openDaemon == null || openDaemon.equals("")? 0: Integer.parseInt(openDaemon));
        		if (openDaemonI >= allDaemonSettings.size()) {
        			openDaemonI = 0;
        		}
				forceUpdate = true;
				switchDaemonConfig(openDaemonI);
    		}
	        
    	}

    	if (forceUpdate || allTorrents == null) {
    		// Not swithcing to another daemon and no known list of torrents: update
	        updateTorrentList();
    	} else {
    		// We retained the list of torrents form the fragment state: show again
    		updateStatusText(null);
    		updateTorrentsView(false);
    	}
    	
    }
    
    private boolean isLaunchedFromHistory(final Intent startIntent) {
        return (startIntent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
    }
    
    public void onResume() {
        super.onResume();
        queue.start();
    	setupRefreshTimer();
    }

    public void onPause() {
        super.onPause();
        queue.requestStop();
        handler.removeCallbacks(refreshRunable);
    }
    
    /**
     * Start a refresh timer, if the user enabled this in the settings
     */
    private void setupRefreshTimer() {
    	if (interfaceSettings != null && interfaceSettings.getRefreshTimerInterval() > 0) {
    		handler.postDelayed(refreshRunable, interfaceSettings.getRefreshTimerInterval() * 1000);
    	}
	}

    private void readSettings() {
    	
    	TLog.d(LOG_NAME, "Reading settings");

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	allDaemonSettings = Preferences.readAllDaemonSettings(prefs);
    	lastUsedDaemonSettings = Preferences.readLastUsedDaemonOrderNumber(prefs, allDaemonSettings);
    	interfaceSettings = Preferences.readInterfaceSettings(prefs);
    	sortSetting = TorrentsSortBy.getStatus(prefs.getInt(Preferences.KEY_PREF_LASTSORTBY, TorrentsSortBy.Alphanumeric.getCode()));
    	sortReversed = prefs.getBoolean(Preferences.KEY_PREF_LASTSORTORD, false);
    	onlyShowTransferring = prefs.getBoolean(Preferences.KEY_PREF_LASTSORTGTZERO, false);
    	alarmServiceSettings = Preferences.readAlarmSettings(prefs);

    	// Update the navigation list of the action bar
    	ignoreFirstListNavigation = true;
    	getSupportActivity().getSupportActionBar().setListNavigationCallbacks(buildServerListAdapter(), onServerChanged );
    	
    }
    
	private void setupDaemon() {

        // If preferences are set, create the daemon adapter
        if (allDaemonSettings != null && allDaemonSettings.size() > lastUsedDaemonSettings && 
        		allDaemonSettings.get(lastUsedDaemonSettings) != null && allDaemonSettings.get(lastUsedDaemonSettings).getType() != null) {
        	
        	// Instantiate the daemon (the connection isn't made until an actual request is done)
        	DaemonSettings settings = allDaemonSettings.get(lastUsedDaemonSettings);
        	daemon = settings.getType().createAdapter(settings);
        	emptyText.setText(R.string.connecting);
        	
    		// Show which server configuration is currently active
        	ignoreFirstListNavigation = true;
        	getSupportActivity().getSupportActionBar().setSelectedNavigationItem(lastUsedDaemonSettings);
        	
			// Show the control bar
        	startsettings.setVisibility(View.GONE);
        	controlbar.setVisibility(View.VISIBLE);
			statusBox.setVisibility(View.GONE);
			activeLabel = null;
			inAlternativeMode = false; // TODO: Actually this should be retrieved from the server's session
			getSupportActivity().invalidateOptionsMenu();

        } else {
        	
        	daemon = null;
        	
        	// The 'set preferences first' screen is shown
        	emptyText.setText(R.string.no_settings);
        	
			// Show that no server configuration is currently active
        	ignoreFirstListNavigation = true;
        	getSupportActivity().getSupportActionBar().setListNavigationCallbacks(null, onServerChanged);
        	statusBox.setVisibility(View.GONE);
			viewtype.setText("");

			// Hide the control bar
        	startsettings.setVisibility(View.VISIBLE);
        	controlbar.setVisibility(View.GONE);
			activeLabel = null;
			getSupportActivity().invalidateOptionsMenu();
			
        }

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		// Add the pause/resume/stop/start options
		Torrent selected = (Torrent) getTorrentListAdapter().getItem((int) ((AdapterContextMenuInfo)menuInfo).id);
		if (selected.canPause()) {
			menu.add(0, MENU_PAUSE_ID, 0, R.string.menu_pause);
		} else if (selected.canResume()) {
			menu.add(0, MENU_RESUME_ID, 0, R.string.menu_resume);
		}
		if (daemon != null && Daemon.supportsStoppingStarting(daemon.getType())) {
			if (selected.canStop()) {
				menu.add(0, MENU_STOP_ID, 0, R.string.menu_stop);
			} else if (selected.canStart()) {
				menu.add(0, MENU_START_ID, 0, R.string.menu_start);
			}
		}
		if (daemon != null && Daemon.supportsForcedStarting(daemon.getType()) && selected.canStart()) {
			menu.add(0, MENU_FORCESTART_ID, 0, R.string.menu_forcestart);
		}

		// Add the remove options
		menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
		if (daemon != null && Daemon.supportsRemoveWithData(daemon.getType())) {
			menu.add(0, MENU_REMOVE_DATA_ID, 0, R.string.menu_remove_data);
		}
		
		// Add the 'set label' option
		if (daemon != null && Daemon.supportsSetLabel(daemon.getType())) {
			menu.add(0, MENU_SETLABEL_ID, 0, R.string.menu_setlabel);
		}
		
		// Add the 'set download location' option
		if (daemon != null && Daemon.supportsSetDownloadLocation(daemon.getType())) {
			menu.add(0, MENU_SETDOWNLOADLOCATION_ID, 0, R.string.menu_setdownloadlocation);
		}
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		// Add title bar buttons
		MenuItem miRefresh = menu.add(0, MENU_REFRESH_ID, 0, R.string.refresh);
		miRefresh.setIcon(R.drawable.icon_refresh_title);
		miRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (inProgress) {
			// Show progress spinner instead of the option item
			View view = getActivity().getLayoutInflater().inflate(R.layout.part_actionbar_progressitem, null);
			miRefresh.setActionView(view);
		}
		MenuItem miSearch = menu.add(0, MENU_SEARCH_ID, 0, R.string.search);
		miSearch.setIcon(R.drawable.icon_search_title);
		miSearch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		MenuItem miAltMode = menu.add(0, MENU_ALTMODE_ID, 0, R.string.menu_altmode);
		miAltMode.setIcon(inAlternativeMode? R.drawable.icon_turtle_title: R.drawable.icon_turtle_title_off);
		miAltMode.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		miAltMode.setVisible(daemon != null && Daemon.supportsSetAlternativeMode(daemon.getType()));
		
		// Add the menu options row 1
		MenuItem miAdd = menu.add(0, MENU_ADD_ID, 0, R.string.menu_add);
		if (useTabletInterface) {
			miAdd.setIcon(R.drawable.icon_add);
		} else {
			miAdd.setIcon(android.R.drawable.ic_menu_add);
		}
		miAdd.setShortcut('1', 'a');
		miAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		MenuItem miBarcode = menu.add(0, MENU_BARCODE_ID, 0, R.string.menu_scanbarcode);
		miBarcode.setIcon(R.drawable.icon_barcode);
		miBarcode.setShortcut('2', 'f');
		miBarcode.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		MenuItem miRss = menu.add(0, MENU_RSS_ID, 0, R.string.menu_rss);
		miRss.setIcon(R.drawable.icon_rss);
		miRss.setShortcut('3', 'r');
		miRss.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		// Add the menu options row 2
		SubMenu miForAll = menu.addSubMenu(MENU_FORALL_GROUP_ID, MENU_FORALL_ID, 0, R.string.menu_forall);
		miForAll.setIcon(android.R.drawable.ic_menu_set_as);
		miForAll.add(MENU_FORALL_GROUP_ID, MENU_FORALL_PAUSE_ID, 0, R.string.menu_forall_pause).setShortcut('4', 'p');
		miForAll.add(MENU_FORALL_GROUP_ID, MENU_FORALL_RESUME_ID, 0, R.string.menu_forall_resume).setShortcut('5', 'u');
		miForAll.add(MENU_FORALL_GROUP_ID, MENU_FORALL_STOP_ID, 0, R.string.menu_forall_stop).setShortcut('7', 't');
		miForAll.add(MENU_FORALL_GROUP_ID, MENU_FORALL_START_ID, 0, R.string.menu_forall_start).setShortcut('8', 's');
		
		menu.add(0, MENU_FILTER_ID, 0, R.string.menu_filter); 
		
		SubMenu miSort = menu.addSubMenu(MENU_SORT_GROUP_ID, MENU_SORT_ID, 0, R.string.menu_sort);
		miSort.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
		//miSort.setHeaderTitle(R.string.menu_sort_reverse);
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_ALPHA_ID, 0, R.string.menu_sort_alpha).setAlphabeticShortcut('1').setChecked(true);
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_STATUS_ID, 0, R.string.menu_sort_status).setAlphabeticShortcut('2');
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_ADDED_ID, 0, R.string.menu_sort_added).setAlphabeticShortcut('3');
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_DONE_ID, 0, R.string.menu_sort_done).setAlphabeticShortcut('4');
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_UPSPEED_ID, 0, R.string.menu_sort_upspeed).setAlphabeticShortcut('5');
		miSort.add(MENU_SORT_GROUP_ID, MENU_SORT_RATIO_ID, 0, R.string.menu_sort_ratio).setAlphabeticShortcut('6');
		miSort.setGroupCheckable(MENU_SORT_GROUP_ID, true, true);
		miSort.add(MENU_SORT_GTZERO_ID, MENU_SORT_GTZERO_ID, 0, R.string.menu_sort_onlytransferring).setCheckable(true);

		// Add the extras menu options
		MenuItem miSpeeds = menu.add(0, MENU_RATES_ID, 0, R.string.menu_speeds);
		miSpeeds.setNumericShortcut('6');
		miSpeeds.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		MenuItem miSwitch = menu.add(0, MENU_SWITCH_ID, 0, R.string.menu_servers);
		miSwitch.setShortcut('9', 'h');
		miSwitch.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, MENU_ERRORREPORT_ID, 0, R.string.menu_errorreport);
		menu.add(0, MENU_CHANGELOG_ID, 0, R.string.menu_changelog);
		MenuItem miSettings = menu.add(0, MENU_SETTINGS_ID, 0, R.string.menu_settings);
		//miSettings.setIcon(android.R.drawable.ic_menu_preferences);
		miSettings.setNumericShortcut('0');
		miSettings.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ok = daemon != null;
		menu.findItem(MENU_ADD_ID).setEnabled(ok);
		menu.findItem(MENU_BARCODE_ID).setEnabled(ok);
		menu.findItem(MENU_RSS_ID).setEnabled(ok);
		menu.findItem(MENU_FORALL_ID).setEnabled(ok);
		menu.findItem(MENU_FILTER_ID).setEnabled(ok);
		menu.findItem(MENU_SORT_ID).setEnabled(ok);			
		if (ok) {
			menu.findItem(MENU_ALTMODE_ID).setVisible(Daemon.supportsSetAlternativeMode(daemon.getType()));
			/*menu.findItem(MENU_SORT_ADDED_ID).setVisible(Daemon.supportsDateAdded(daemon.getType()));
			menu.findItem(MENU_FORALL_STOP_ID).setVisible(Daemon.supportsStoppingStarting(daemon.getType()));
			menu.findItem(MENU_FORALL_START_ID).setVisible(Daemon.supportsStoppingStarting(daemon.getType()));
			
			// Show the currently selected sort by option
			SubMenu sortmenu = menu.findItem(MENU_SORT_ID).getSubMenu();
			for (int i = 0; i < sortmenu.size(); i++) {
				if (sortmenu.getItem(i).getItemId() == MENU_SORT_ALPHA_ID && sortSetting == TorrentsSortBy.Alphanumeric) {
					sortmenu.getItem(i).setChecked(true);
					break;
				} else if (sortmenu.getItem(i).getItemId() == MENU_SORT_STATUS_ID && sortSetting == TorrentsSortBy.Status) {
					sortmenu.getItem(i).setChecked(true);
					break;
				} else if (sortmenu.getItem(i).getItemId() == MENU_SORT_ADDED_ID && sortSetting == TorrentsSortBy.DateAdded) {
					sortmenu.getItem(i).setChecked(true);
					break;
				} else if (sortmenu.getItem(i).getItemId() == MENU_SORT_DONE_ID && sortSetting == TorrentsSortBy.DateDone) {
					sortmenu.getItem(i).setChecked(true);
					break;
				} else if (sortmenu.getItem(i).getItemId() == MENU_SORT_UPSPEED_ID && sortSetting == TorrentsSortBy.UploadSpeed) {
					sortmenu.getItem(i).setChecked(true);
					break;
				} else if (sortmenu.getItem(i).getItemId() == MENU_SORT_RATIO_ID && sortSetting == TorrentsSortBy.Ratio) {
					sortmenu.getItem(i).setChecked(true);
					break;
				}
			}

			// Show the checkbox status according to the current settings
			menu.findItem(MENU_SORT_GTZERO_ID).setChecked(onlyShowTransferring);*/
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return true;
		}
		
		// Get torrent item
		Torrent selection = (Torrent) getTorrentListAdapter().getItem((int)info.id);
		
		switch (item.getItemId()) {
		case MENU_PAUSE_ID:

			// Set an intermediate status first (for a response feel)
			selection.mimicPause();
			// Update adapter
			getTorrentListAdapter().notifyDataSetChanged();

			queue.enqueue(PauseTask.create(daemon, selection));
			return true;

		case MENU_RESUME_ID:
			
			// Set an intermediate status first (for a response feel)
			selection.mimicResume();
			// Update adapter
			getTorrentListAdapter().notifyDataSetChanged();

			queue.enqueue(ResumeTask.create(daemon, selection));
			queue.enqueue(RetrieveTask.create(daemon));
			return true;

		case MENU_STOP_ID:

			// Set an intermediate status first (for a response feel)
			selection.mimicStop();
			// Update adapter
			getTorrentListAdapter().notifyDataSetChanged();

			queue.enqueue(StopTask.create(daemon, selection));
			return true;

		case MENU_START_ID:
			
			// Set an intermediate status first (for a response feel)
			selection.mimicStart();
			// Update adapter
			getTorrentListAdapter().notifyDataSetChanged();

			queue.enqueue(StartTask.create(daemon, selection, false));
			queue.enqueue(RetrieveTask.create(daemon));
			return true;

		case MENU_FORCESTART_ID:
			
			// Set an intermediate status first (for a response feel)
			selection.mimicStart();
			// Update adapter
			getTorrentListAdapter().notifyDataSetChanged();

			queue.enqueue(StartTask.create(daemon, selection, true));
			queue.enqueue(RetrieveTask.create(daemon));
			return true;

		case MENU_REMOVE_ID:

			if (interfaceSettings.getAskBeforeRemove()) {
				// We store the torrent that we selected so the dialog can use this to make its calls
				selectedTorrent = selection;
				selectedRemoveData = false;
				showDialog(DIALOG_ASKREMOVE);
			} else {
				removeTorrentOnServer(selection, false);
			}
			return true;

		case MENU_REMOVE_DATA_ID:

			if (interfaceSettings.getAskBeforeRemove()) {
				// We store the torrent that we selected so the dialog can use this to make its calls
				selectedTorrent = selection;
				selectedRemoveData = true;
				showDialog(DIALOG_ASKREMOVE);
			} else {
				removeTorrentOnServer(selection, true);
			}
			return true;

		case MENU_SETLABEL_ID:

			// Store the torrent that we selected and open the set label dialog
			selectedTorrent = selection;
			showDialog(DIALOG_SETLABEL);
			return true;

		case MENU_SETDOWNLOADLOCATION_ID:

			// Store the torrent that we selected and open the set download location dialog
			selectedTorrent = selection;
			showDialog(DIALOG_SETDOWNLOADLOCATION);
			return true;

		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(android.support.v4.view.MenuItem item) {

		// Check connection first (when not opening the settings)
		if (item.getItemId() != MENU_SETTINGS_ID && 
				item.getItemId() != MENU_CHANGELOG_ID && daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return true;
		}
		
		switch (item.getItemId()) {
		/*case android.R.id.home:
			
			// Home button click in the action bar
			Intent i = new Intent(getActivity(), Torrents.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			break;*/
			
		case MENU_REFRESH_ID:
			
			refreshActivity();
			break;
			
		case MENU_SEARCH_ID:
			getSupportActivity().onSearchRequested();
			break;
			
		case MENU_ALTMODE_ID:
			
			switchAlternativeMode();
			break;
			
		case MENU_FORALL_PAUSE_ID:

			if (allTorrents != null && allTorrents.size() > 0) {
				
				// Set an intermediate status first on all items (for a response feel)
	    		for (Torrent torrent : allTorrents) {
					torrent.mimicPause();
				}
				// Update adapter
	    		getTorrentListAdapter().notifyDataSetChanged();

				queue.enqueue(PauseAllTask.create(daemon));
				
			}
			break;

		case MENU_FORALL_RESUME_ID:

			if (allTorrents != null && allTorrents.size() > 0) {

				// Set an intermediate status first on all items (for a response feel)
	    		for (Torrent torrent : allTorrents) {
					torrent.mimicResume();
				}
				// Update adapter
	    		getTorrentListAdapter().notifyDataSetChanged();

				queue.enqueue(ResumeAllTask.create(daemon));
				queue.enqueue(RetrieveTask.create(daemon));
				
			}
			break;

		case MENU_FORALL_STOP_ID:

			if (allTorrents != null && allTorrents.size() > 0 && Daemon.supportsStoppingStarting(daemon.getType())) {
				
				// Set an intermediate status first on all items (for a response feel)
	    		for (Torrent torrent : allTorrents) {
					torrent.mimicStop();
				}
				// Update adapter
	    		getTorrentListAdapter().notifyDataSetChanged();

				queue.enqueue(StopAllTask.create(daemon));
				
			}
			break;

		case MENU_FORALL_START_ID:

			if (allTorrents != null && allTorrents.size() > 0 && Daemon.supportsStoppingStarting(daemon.getType())) {
				
	    		// Set an intermediate status first on all items (for a response feel)
	    		for (Torrent torrent : allTorrents) {
					torrent.mimicStart();
				}
				// Update adapter
	    		getTorrentListAdapter().notifyDataSetChanged();
				
				queue.enqueue(StartAllTask.create(daemon, false));
				queue.enqueue(RetrieveTask.create(daemon));
				
			}
			break;

		case MENU_ADD_ID:

			// Show a torrent URL input screen;
			startActivity(new Intent(getActivity(), Add.class));
			break;

		case MENU_BARCODE_ID:

			startBarcodeScanner();
			break;

		case MENU_RSS_ID:

			// Show the RSS feeds screen
			startActivity(new Intent(getActivity(), RssFeeds.class));
			break;

		case MENU_SWITCH_ID:

			// Present a dialog with all available servers
			showDialog(DIALOG_SERVERS);
			break;

		case MENU_SETTINGS_ID:
			
			// Open settings menu
			startActivityForResult(new Intent(getActivity(), PreferencesMain.class), ACTIVITY_PREFERENCES);
			break;

		case MENU_RATES_ID:

			// Present a dialog that allows the setting of maxium transfer rates
			showDialog(DIALOG_RATES);
			break;

		case MENU_CHANGELOG_ID:

			// Present a dialog that shows version information and recent changes
			showDialog(DIALOG_CHANGELOG);
			break;

		case MENU_ERRORREPORT_ID:

			ErrorLogSender.collectAndSendLog(getActivity(), daemon, allDaemonSettings.get(lastUsedDaemonSettings));
			break;
			
		case MENU_FILTER_ID:
			// Present a dialog that allows filtering the list
			showDialog(DIALOG_FILTER);
			break;

		case MENU_SORT_ALPHA_ID:
			
			// Resort
			item.setChecked(true);
			newTorrentListSorting(TorrentsSortBy.Alphanumeric);
			break;

		case MENU_SORT_STATUS_ID:
			
			// Resort
			item.setChecked(true);
			newTorrentListSorting(TorrentsSortBy.Status);
			break;

		case MENU_SORT_DONE_ID:

			// Resort
			item.setChecked(true);
			newTorrentListSorting(TorrentsSortBy.DateDone);
			break;

		case MENU_SORT_UPSPEED_ID:

			// Resort
			item.setChecked(true);
			newTorrentListSorting(TorrentsSortBy.UploadSpeed);
			break;

		case MENU_SORT_RATIO_ID:

			// Resort
			item.setChecked(true);
			newTorrentListSorting(TorrentsSortBy.Ratio);
			break;

		case MENU_SORT_ADDED_ID:
			
			if (Daemon.supportsDateAdded(daemon.getType())) {
			
				// Resort
				item.setChecked(true);
				newTorrentListSorting(TorrentsSortBy.DateAdded);
				
			}
			break;

		case MENU_SORT_GTZERO_ID:
			
			// Set boolean to filter on only transferring (> 0 KB/s) torrents
			item.setChecked(!item.isChecked());
			onlyShowTransferring = item.isChecked();
			
			updateTorrentsView(true);
			
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private OnItemClickListener onTorrentClicked = new OnItemClickListener() {
	    @Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
	    	if (getTorrentListAdapter() != null) {
	    		
		    	Torrent tor = getTorrentListAdapter().getItem(position);
	    		
		    	// Show the details in the right of the screen (tablet interface) or separately
		    	if (useTabletInterface) {
		    		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		    		ft.replace(R.id.details, new DetailsFragment(TorrentsFragment.this, lastUsedDaemonSettings, tor, 
		    				buildLabelTexts(false)));
		    		ft.commit();
		    	} else {
		    		Intent i = new Intent(getActivity(), Details.class);
			    	i.putExtra(Details.STATE_DAEMON, lastUsedDaemonSettings);
			    	i.putExtra(Details.STATE_LABELS, buildLabelTexts(false));
			    	i.putExtra(Details.STATE_TORRENT, tor);
			    	startActivityForResult(i, ACTIVITY_DETAILS);
		    	}
		    	
	    	}
	    }
	};
	
	private void startBarcodeScanner() {

    	// Test to see if the ZXing barcode scanner is available that can handle the SCAN intent
	    Intent scan = new Intent(Transdroid.SCAN_INTENT);
	    scan.addCategory(Intent.CATEGORY_DEFAULT);
    	if (ActivityUtil.isIntentAvailable(getActivity(), scan)) {
    		// Ask the barcode scanner to allow the user to scan some code
    		startActivityForResult(scan, ACTIVITY_BARCODE);
    	} else {
    		// Show a message if the user should install the barcode scanner for this feature
    		showDialog(DIALOG_INSTALLBARCODESCANNER);
    	}
    	
	}
	
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_SERVERS:
			
			// Build a dialog with a radio box per daemon configuration
			AlertDialog.Builder serverDialog = new AlertDialog.Builder(getActivity());
			serverDialog.setTitle(R.string.menu_servers);
			serverDialog.setSingleChoiceItems(
					buildServerTextsForDialog(), // The strings of the available server configurations 
					(lastUsedDaemonSettings < allDaemonSettings.size()? lastUsedDaemonSettings: 0), // The current selection except when this suddenly doesn't exist any more 
					new DialogInterface.OnClickListener() {
				
						@Override
						// When the server is clicked (and it is different from the current active configuration), 
						// reload the daemon and update the torrent list
						public void onClick(DialogInterface dialog, int which) {
							if (which != lastUsedDaemonSettings) {
								switchDaemonConfig(which);
							}
							dismissDialog(DIALOG_SERVERS);
						}
			});
			return serverDialog.create();

		case DIALOG_REFRESH_INTERVAL:
			
			// Build a dialog with a radio boxes of different refresh intervals to choose from
			AlertDialog.Builder refintDialog = new AlertDialog.Builder(getActivity());
			refintDialog.setTitle(R.string.pref_uirefresh);
			refintDialog.setSingleChoiceItems(
					R.array.pref_uirefresh_types, 
					getCurrentRefreshIntervalID(), 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Store new refresh interval
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
							Preferences.storeNewRefreshInterval(prefs, 
									getResources().getStringArray(R.array.pref_uirefresh_values)[which]);
							interfaceSettings = Preferences.readInterfaceSettings(prefs);
							// Apply new refresh interval
					        handler.removeCallbacks(refreshRunable); // Stops refresh timer
							setupRefreshTimer();
							dismissDialog(DIALOG_REFRESH_INTERVAL);
						}
			});
			return refintDialog.create();

		case DIALOG_SETLABEL:

			// Build a dialog that asks for a new or selected an existing label to assign to the selected torrent
			String[] setLabelTexts = buildLabelTexts(false);
			SetLabelDialog setLabelDialog = new SetLabelDialog(getActivity(), new ResultListener() {
				@Override
				public void onLabelResult(String newLabel) {
					if (newLabel.equals(getString(R.string.labels_unlabeled).toString())) {
						// Setting a torrent to 'unlabeled' is actually setting the label to an empty string
						newLabel = "";
					}
					setNewLabel(newLabel);
				}
			}, setLabelTexts, selectedTorrent.getLabelName());
			setLabelDialog.setTitle(R.string.labels_newlabel);

			return setLabelDialog;

		case DIALOG_SETDOWNLOADLOCATION:
			
			// Build a dialog that asks for a new download location for the torrent
			final View setLocationLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_download_location, null);
			final EditText newLocation = (EditText) setLocationLayout.findViewById(R.id.download_location);
			AlertDialog.Builder setLocationDialog = new AlertDialog.Builder(getActivity());
			setLocationDialog.setTitle(R.string.menu_setdownloadlocation);
			setLocationDialog.setView(setLocationLayout);
			setLocationDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					
					setDownloadLocation(newLocation.getText().toString());
				}
			});
			setLocationDialog.setNegativeButton(android.R.string.cancel, null);
			return setLocationDialog.create();
			
		case DIALOG_RATES:
			
			// Build a dialog that allows for the input of maximum upload and download transfer rates
			final View ratesDialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_transfer_rates, null);
			final EditText rateDownload = (EditText) ratesDialogView.findViewById(R.id.rate_download);
			final EditText rateUpload = (EditText) ratesDialogView.findViewById(R.id.rate_upload);
			AlertDialog.Builder ratesDialog = new AlertDialog.Builder(getActivity());
			ratesDialog.setTitle(R.string.menu_speeds);
			ratesDialog.setView(ratesDialogView);
			ratesDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setTransferRates( 
							(rateUpload.getText().toString().equals("")? null: Integer.parseInt(rateUpload.getText().toString())),
							(rateDownload.getText().toString().equals("")? null: Integer.parseInt(rateDownload.getText().toString())));
				}
			});
			ratesDialog.setNeutralButton(R.string.rate_reset, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Reset the text boxes first
					rateUpload.setText("");
					rateDownload.setText("");
					setTransferRates(null, null);
				}
			});
			ratesDialog.setNegativeButton(android.R.string.cancel, null);
						
			return ratesDialog.create();

		case DIALOG_CHANGELOG:

			// Build a dialog listing the recent changes in Transdroid
			AlertDialog.Builder changesDialog = new AlertDialog.Builder(getActivity());
			changesDialog.setTitle(R.string.menu_changelog);
			View changes = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null);
			((TextView)changes.findViewById(R.id.transdroid)).setText("Transdroid " + ActivityUtil.getVersionNumber(getActivity()));
			changesDialog.setView(changes);
			return changesDialog.create();

		case DIALOG_ASKREMOVE:
			
			// Build a dialog that asks to confirm the deletions of a torrent
			AlertDialog.Builder askRemoveDialog = new AlertDialog.Builder(getActivity());
			askRemoveDialog.setTitle(R.string.askremove_title);
			askRemoveDialog.setMessage(R.string.askremove);
			askRemoveDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					removeTorrentOnServer(selectedTorrent, selectedRemoveData);
					dismissDialog(DIALOG_ASKREMOVE);
				}
			});
			askRemoveDialog.setNegativeButton(android.R.string.no, null);
			return askRemoveDialog.create();

		case DIALOG_ADDFAILED:
			
			// Build a dialog that asks to retry or store this torrent and add it later
			AlertDialog.Builder addFailedDialog = new AlertDialog.Builder(getActivity());
			addFailedDialog.setTitle(R.string.addfailed_title);
			if (alarmServiceSettings.isAlarmEnabled()) {
				addFailedDialog.setMessage(getString(R.string.addfailed_service2, failedTorrentTitle));
				addFailedDialog.setNeutralButton(R.string.addfailed_addlater, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Preferences.addToTorrentAddQueue(PreferenceManager.getDefaultSharedPreferences(getActivity()), 
								daemon.getSettings().getIdString(), failedTorrentUri);
						dismissDialog(DIALOG_ADDFAILED);
					}
				});
			} else {
				addFailedDialog.setMessage(getString(R.string.addfailed_noservice2, failedTorrentTitle));
			}
			addFailedDialog.setPositiveButton(R.string.addfailed_retry, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					if (Preferences.isQueuedTorrentToAddALocalFile(failedTorrentUri)) {
						addTorrentByFile(failedTorrentUri);
					} else if (Preferences.isQueuedTorrentToAddAMagnetUrl(failedTorrentUri)) {
						addTorrentByMagnetUrl(failedTorrentUri);
					} else {
						addTorrentByUrl(failedTorrentUri, failedTorrentTitle);
					}
					dismissDialog(DIALOG_ADDFAILED);
				}
			});
			addFailedDialog.setNegativeButton(android.R.string.no, null);
			return addFailedDialog.create();

		case DIALOG_INSTALLBARCODESCANNER:
			return ActivityUtil.buildInstallDialog(getActivity(), R.string.scanner_not_found, Transdroid.SCANNER_MARKET_URI);	
			
		
		case DIALOG_FILTER:
			// Build a dialog that asks for a new download location for the torrent
			final View setFilterLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_filter, null);
			final EditText newFilter = (EditText) setFilterLayout.findViewById(R.id.filter);
			if(activeFilter != null)
				newFilter.setText(activeFilter);
			AlertDialog.Builder setFilterDialog = new AlertDialog.Builder(getActivity());
			setFilterDialog.setTitle(R.string.menu_setfilter);
			setFilterDialog.setView(setFilterLayout);
			setFilterDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					
					setFilter(newFilter.getText().toString());
				}
			});
			setFilterDialog.setNegativeButton(android.R.string.cancel, null);
			setFilterDialog.setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {					
					setFilter("");
				}
			});
			return setFilterDialog.create();
		}
		return null;

	}

	/*@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		switch (id) {
		case DIALOG_SERVERS:
			
			// Re-populate the dialog adapter with possibly new/edited server configurations
			AlertDialog serverDialog = (AlertDialog) dialog;
			ListView serverRadios = serverDialog.getListView();
			ArrayAdapter<String> serverList = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, android.R.id.text1, buildServerTextsForDialog());
			serverRadios.setAdapter(serverList);
			
			// Also pre-select the active daemon
			int serverSelected = (lastUsedDaemonSettings < allDaemonSettings.size()? lastUsedDaemonSettings: 0); // Prevent going out of bounds
			serverRadios.clearChoices();
			serverRadios.setItemChecked(serverSelected, true);
			serverRadios.setSelection(serverSelected);
			break;

		case DIALOG_REFRESH_INTERVAL:
			
			// Pre-select the current refresh interval
			AlertDialog refintDialog = (AlertDialog) dialog;
			ListView refintRadios = refintDialog.getListView();
			refintRadios.clearChoices();
			refintRadios.setItemChecked(getCurrentRefreshIntervalID(), true);
			refintRadios.setSelection(getCurrentRefreshIntervalID());
			break;

		case DIALOG_SETLABEL:

			// Re-populate the dialog adapter with the available labels
			SetLabelDialog setLabelDialog = (SetLabelDialog) dialog;
			String[] setLabelTexts = buildLabelTexts(false);
			setLabelDialog.resetDialog(this, setLabelTexts, selectedTorrent.getLabelName());
			break;
			
		case DIALOG_SETDOWNLOADLOCATION:
			
			// Show the existing download location
			final EditText newLocation = (EditText) dialog.findViewById(R.id.download_location);
			newLocation.setText(selectedTorrent.getLocationDir());
			break;
			
		case DIALOG_ADDFAILED:

			AlertDialog addFailedDialog = (AlertDialog) dialog;
			if (alarmServiceSettings.isAlarmEnabled()) {
				addFailedDialog.setMessage(getString(R.string.addfailed_service2, failedTorrentTitle));
				addFailedDialog.setButton2(getText(R.string.addfailed_addlater), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Preferences.addToTorrentAddQueue(PreferenceManager.getDefaultSharedPreferences(TorrentsFragment.this), 
								daemon.getSettings().getIdString(), failedTorrentUri);
						removeDialog(DIALOG_ADDFAILED);
					}
				});
			} else {
				addFailedDialog.setMessage(getString(R.string.addfailed_noservice2, failedTorrentTitle));
				addFailedDialog.setButton2(null, (DialogInterface.OnClickListener)null);
			}
			break;
		}
	}*/

    private SpinnerAdapter buildServerListAdapter() {
    	ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), R.layout.abs__simple_spinner_item, buildServerTextsForDialog());
    	ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	return ad;
	}

	private String[] buildServerTextsForDialog() {

		// Build a textual list of servers available
		ArrayList<String> servers = new ArrayList<String>();
		for (DaemonSettings daemon : allDaemonSettings) {
			servers.add(daemon.getName());
		}
		return servers.toArray(new String[servers.size()]);
		
	}

	private int getCurrentRefreshIntervalID() {
		String[] values = getResources().getStringArray(R.array.pref_uirefresh_values);
		int current = interfaceSettings.getRefreshTimerInterval();
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(Integer.toString(current))) {
				return i;
			}
		}
		return 0;
	}

	private String[] buildLabelTexts(boolean addShowAll) {

		// Build a textual list of used torrent labels
		ArrayList<String> labelNames = new ArrayList<String>();
		if (availableLabels != null) {
			// The 'show all' label first ...
			String showAll = getText(R.string.labels_showall).toString();
			if (addShowAll) {
				labelNames.add(showAll);
			}
			// ... then all the normal labels (which will end up in alphabetical order)
			for (String name : availableLabels) {
				if (!name.equals(showAll)) {
					labelNames.add(name);
				}
			}
		}
		return labelNames.toArray(new String[labelNames.size()]);
		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_PREFERENCES:
			
			// Preference screen was called: use new preferences to connect
	    	readSettings();
			setupDaemon();
	        updateTorrentList();
			break;
			
		case ACTIVITY_DETAILS:
			
			// Details screen was shown; we may have an updated torrent (it can be started/removed/etc.)
			updateTorrentList();
			break;

    	case ACTIVITY_BARCODE:
    		
			if (resultCode == Activity.RESULT_OK) {
				
				// Get scan results code
				String contents = data.getStringExtra("SCAN_RESULT");
				String formatName = data.getStringExtra("SCAN_RESULT_FORMAT");
				
				if (formatName != null && formatName.equals(Transdroid.SCAN_FORMAT_QRCODE)) {
					// Scanned barcode was a QR code: assume the contents contain a URL to a .torrent file
					TLog.d(LOG_NAME, "Add torrent from QR code url '" + contents + "'");
					addTorrentByUrl(contents, "Torrent QR code"); // No real torrent title known
				} else {
					// Get a meaningful search query based on a Google Search product lookup
					TLog.d(LOG_NAME, "Starting barcode lookup for code '" + contents + "' " + (formatName == null? "(code type is unknown)": "(this should be a " + formatName + " code)"));
					setProgressBar(true);
					new GoogleWebSearchBarcodeResolver() {
						@Override
						protected void onBarcodeLookupComplete(String result) {

							setProgressBar(false);
							
							// No proper result?
					    	if (result == null || result.equals("")) {
					        	TLog.d(LOG_NAME, "Barcode not resolved (timout, connection error, no items, etc.)");
					    		Toast.makeText(getActivity(), R.string.no_results, Toast.LENGTH_SHORT).show();
					    		return;
					    	}

							// Open TransdroidSearch directly, mimicking a search query
					    	TLog.d(LOG_NAME, "Barcode resolved to '" + result + "'. Now starting search.");
							Intent search = new Intent(getActivity(), Search.class);
							search.setAction(Intent.ACTION_SEARCH);
							search.putExtra(SearchManager.QUERY, result);
							startActivity(search);
							
						}
					}.execute(contents);
				}
				
			}
    		break;
		}
	}

	private void refreshActivity() {
		updateTorrentList();
		if (getSupportFragmentManager().findFragmentById(R.id.details) != null) {
			// Marshal the refresh button click to the fragment
			((DetailsFragment)getSupportFragmentManager().findFragmentById(R.id.details)).refreshActivity();
		}
	}

	public void updateTorrentList() {
	    	if (daemon != null) {
			queue.enqueue(RetrieveTask.create(daemon));
			if (Daemon.supportsStats(daemon.getType())) {
				queue.enqueue(GetStatsTask.create(daemon));
			}
    		}
	}

	private void addTorrentByUrl(String url, String title) {

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}
		
		queue.enqueue(AddByUrlTask.create(daemon, url, title));
		queue.enqueue(RetrieveTask.create(daemon));
	}

	private void addTorrentByMagnetUrl(String url) {

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (!Daemon.supportsAddByMagnetUrl(daemon.getType())) {
			// Not supported by the torrent client
			Toast.makeText(getActivity(), R.string.no_magnet_links, Toast.LENGTH_SHORT).show();
			return;
		}
		
		queue.enqueue(AddByMagnetUrlTask.create(daemon, url));
		queue.enqueue(RetrieveTask.create(daemon));

	}

	private void addTorrentByFile(String fileUri) {

		if (daemon == null) {
			// No connection possible yet: give message
			if (getActivity() != null) {
				Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			}
			return;
		}
		
		if (!Daemon.supportsAddByFile(daemon.getType())) {
			// The daemon type does not support .torrent file uploads/metadata sending or this is not yet implemented
			if (getActivity() != null) {
				Toast.makeText(getActivity(), R.string.no_file_uploads, Toast.LENGTH_LONG).show();
			}
			return;
		}
		
		queue.enqueue(AddByFileTask.create(daemon, fileUri));
		queue.enqueue(RetrieveTask.create(daemon));

	}

	private void removeTorrentOnServer(Torrent torrent, boolean removeData) {
		
		// Remove the list item first (for a responsive feel) from views and labels
		getTorrentListAdapter().remove(torrent);
		updateEmptyListDescription();
		
		// Remove the torrent from the server
		queue.enqueue(RemoveTask.create(daemon, torrent, removeData));
		
	}

	private void setTransferRates(Integer uploadRate, Integer downloadRate) {

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!Daemon.supportsSetTransferRates(daemon.getType())) {
			// The daemon type does not support adjusting the maximum transfer rates
			Toast.makeText(getActivity(), R.string.rate_no_support, Toast.LENGTH_LONG).show();
			return;	
		}
		
		queue.enqueue(SetTransferRatesTask.create(daemon, uploadRate, downloadRate));

	}
	
	private void setNewLabel(String newLabel) {
		
		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!Daemon.supportsSetLabel(daemon.getType())) {
			// The daemon type does not support setting the label of a torrent
			Toast.makeText(getActivity(), R.string.labels_no_support, Toast.LENGTH_LONG).show();
			return;	
		}
		
		// Mimic that we have already set the label (for a response feel)
		selectedTorrent.mimicNewLabel(newLabel);
		
		queue.enqueue(SetLabelTask.create(daemon, selectedTorrent, newLabel));
		queue.enqueue(RetrieveTask.create(daemon));

	}
	
	private void setDownloadLocation(String newLocation) {

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!Daemon.supportsSetDownloadLocation(daemon.getType())) {
			return;	
		}
		
		queue.enqueue(SetDownloadLocationTask.create(daemon, selectedTorrent, newLocation));
		
	}

    protected void switchAlternativeMode() {

		if (daemon == null) {
			// No connection possible yet: give message
			Toast.makeText(getActivity(), R.string.no_settings_short, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!Daemon.supportsSetAlternativeMode(daemon.getType())) {
			return;	
		}

		inAlternativeMode = !inAlternativeMode;
		updateAlternativeModeIcon();
		queue.enqueue(SetAlternativeModeTask.create(daemon, inAlternativeMode));
		
	}

	@Override
	public void onQueuedTaskFinished(DaemonTask started) {
		
		// Show the daemon status (speeds) again
		updateStatusText(null);
		
	}

	@Override
	public void onQueuedTaskStarted(DaemonTask started) {

		// Started on a new task: turn on status indicator
		setProgressBar(true);
		// Show which task we are executing
		updateStatusText(getStatusFromTask(started).toString());
		
	}

	@Override
	public void onQueueEmpty() {

		// No active task: turn off status indicator
		setProgressBar(false);
		
	}

	private CharSequence getStatusFromTask(DaemonTask started) {
		switch (started.getMethod()) {
		case Retrieve:
			return getText(R.string.task_refreshing);
		case AddByUrl:
			return getText(R.string.task_addbyurl);
		case AddByMagnetUrl:
			return getText(R.string.task_addbyurl);
		case AddByFile:
			return getText(R.string.task_addbyfile);
		case Remove:
			return getText(R.string.task_removing);
		case Pause:
			return getText(R.string.task_pausing);
		case PauseAll:
			return getText(R.string.task_pausingall);
		case Resume:
			return getText(R.string.task_resuming);
		case ResumeAll:
			return getText(R.string.task_resumingall);
		case Stop:
			return getText(R.string.task_stopping);
		case StopAll:
			return getText(R.string.task_stoppingall);
		case Start:
			return getText(R.string.task_starting);
		case StartAll:
			return getText(R.string.task_startingall);
		case GetFileList:
			return getText(R.string.task_getfiles);
		case SetFilePriorities:
			return getText(R.string.task_setfileprop);
		case SetTransferRates:
			return getText(R.string.task_settransrates);
		case SetLabel:
			return getText(R.string.task_setlabel);
		case SetDownloadLocation:
			return getText(R.string.task_setlocation);
		case SetAlternativeMode:
			return getText(R.string.task_setalternativemode);
		}
		return "";
	}

	@Override
	public boolean isAttached() {
		return getActivity() != null;
	}

	@Override
	public void onTaskFailure(DaemonTaskFailureResult result) {

		if (result.getMethod() == DaemonMethod.Retrieve) {
			// For retrieve tasks: Only bother if we still are still looking at the same daemon since the task was queued
			if (result.getTask().getAdapterType() == daemon.getType()) {
				// Show error message
				Toast.makeText(getActivity(), LocalTorrent.getResourceForDaemonException(result.getException()), Toast.LENGTH_SHORT * 2).show();
			}
			
		} else if (result.getMethod() == DaemonMethod.AddByFile) {
			// Failed adding a torrent? Ask to add this torrent later
			failedTorrentUri = ((AddByFileTask)result.getTask()).getFile();
			failedTorrentTitle = new File(failedTorrentUri).getName(); // Use filename as title
			showDialog(DIALOG_ADDFAILED);

		} else if (result.getMethod() == DaemonMethod.AddByUrl) {
			// Failed adding a torrent? Ask to add this torrent later
			failedTorrentUri = ((AddByUrlTask)result.getTask()).getUrl();
			failedTorrentTitle = ((AddByUrlTask)result.getTask()).getTitle(); // Get title directly from task
			showDialog(DIALOG_ADDFAILED);

		} else if (result.getMethod() == DaemonMethod.AddByMagnetUrl) {
			// Failed adding a torrent? Ask to add this torrent later
			failedTorrentUri = ((AddByMagnetUrlTask)result.getTask()).getUrl();
			failedTorrentTitle = "Torrent"; // No way to know the title here
			showDialog(DIALOG_ADDFAILED);
			
		} else {
			// Show error message
			Toast.makeText(getActivity(), LocalTorrent.getResourceForDaemonException(result.getException()), Toast.LENGTH_SHORT * 2).show();			
		}
		
	}

	@Override
	public void onTaskSuccess(DaemonTaskSuccessResult result) {
		
		switch (result.getMethod()) {
		case AddByFile:
		case AddByUrl:

			// Show 'added' message
			Toast.makeText(getActivity(), R.string.torrent_added, Toast.LENGTH_SHORT).show();
			break;

		case Retrieve:

			// Only bother if we still are still looking at the same daemon since the task was queued
			if (result.getTask().getAdapterType() == daemon.getType()) {
				
				// Sort the new list of torrents
				allTorrents = ((RetrieveTaskSuccessResult) result).getTorrents();
				Collections.sort(allTorrents, new TorrentsComparator(daemon, sortSetting, sortReversed));

				// Sort the new list of labels
				allLabels = ((RetrieveTaskSuccessResult) result).getLabels();
				
				// Show refreshed totals for this daemon
				updateStatusText(null);
				updateTorrentsView(false);
				
				// Show 'refreshed' message unless we enabled the hide refresh message setting
				if (!interfaceSettings.shouldHideRefreshMessage()) {
					Toast.makeText(getActivity(), R.string.list_refreshed, Toast.LENGTH_SHORT).show();
				}
				
			}
			break;

		case GetStats:

			// Only bother if we still are still looking at the same daemon since the task was queued
			if (result.getTask().getAdapterType() == daemon.getType()) {
				
				GetStatsTaskSuccessResult stats = (GetStatsTaskSuccessResult) result;
				if (Daemon.supportsSetAlternativeMode(daemon.getType())) {
					// Update the alternative/tutle mode indicator
					inAlternativeMode = stats.isAlternativeModeEnabled();
					updateAlternativeModeIcon();
				}
					
			}
			break;

		case Remove:

			// Show 'removed' message
			boolean includingData = ((RemoveTask)result.getTask()).includingData();
			Toast.makeText(getActivity(), "'" + result.getTargetTorrent().getName() + "' " + getText(includingData? R.string.torrent_removed_with_data: R.string.torrent_removed), Toast.LENGTH_SHORT).show();
			break;

		case Resume:

			// Show 'resumed' message
			Toast.makeText(getActivity(), "'" + result.getTargetTorrent().getName() + "' " + getText(R.string.torrent_resumed), Toast.LENGTH_SHORT).show();
			break;

		case ResumeAll:

			// Show 'resumed all' message
			Toast.makeText(getActivity(), getText(R.string.torrent_resumed_all), Toast.LENGTH_SHORT).show();
			break;

		case Pause:

			// Show 'paused' message
			Toast.makeText(getActivity(), "'" + result.getTargetTorrent().getName() + "' " + getText(R.string.torrent_paused), Toast.LENGTH_SHORT).show();
			break;

		case PauseAll:

			// Show 'paused all' message
			Toast.makeText(getActivity(), getText(R.string.torrent_paused_all), Toast.LENGTH_SHORT).show();
			break;

		case Start:

			// Show 'started' message
			Toast.makeText(getActivity(), "'" + result.getTargetTorrent().getName() + "' " + getText(R.string.torrent_started), Toast.LENGTH_SHORT).show();
			break;

		case StartAll:

			// Show 'started all' message
			Toast.makeText(getActivity(), getText(R.string.torrent_started_all), Toast.LENGTH_SHORT).show();
			break;

		case Stop:

			// Show 'stopped' message
			Toast.makeText(getActivity(), "'" + result.getTargetTorrent().getName() + "' " + getText(R.string.torrent_stopped), Toast.LENGTH_SHORT).show();
			break;

		case StopAll:

			// Show 'stopped all' message
			Toast.makeText(getActivity(), getText(R.string.torrent_stopped_all), Toast.LENGTH_SHORT).show();
			break;
			
		case SetTransferRates:

			// Show 'rates updated' message
			Toast.makeText(getActivity(), R.string.rate_updated, Toast.LENGTH_SHORT).show();
			break;

		case SetDownloadLocation:
			Toast.makeText(getActivity(), getString(R.string.torrent_locationset, ((SetDownloadLocationTask)result.getTask()).getNewLocation()), Toast.LENGTH_SHORT).show();
			break;

		case SetAlternativeMode:
			
			// Updated the mode: now update the server stats to reflect the real new status (since the action might have been unsuccessful for some reason)
			queue.enqueue(GetStatsTask.create(daemon));
			break;
		}
		
	}

	private void updateAlternativeModeIcon() {
		// By invalidation the options menu it gets redrawn and the turtle icon gets updated
		getSupportActivity().invalidateOptionsMenu();
	}
	
	/**
	 * Shows the proper text when the torrent list is empty
	 */
	private void updateEmptyListDescription() {
		
		// Show empty text when no torrents exist any more
		if (getTorrentListAdapter() != null && getTorrentListAdapter().getAllItems().size() == 0) {
			if (activeMainView == MainViewType.OnlyDownloading) {
				emptyText.setText(R.string.no_downloading_torrents);
			} else if (activeMainView == MainViewType.OnlyUploading) {
				emptyText.setText(R.string.no_uploading_torrents);
			} else if (activeMainView == MainViewType.OnlyInactive) {
				emptyText.setText(R.string.no_inactive_torrents);
			} else if (activeMainView == MainViewType.OnlyActive) {
				emptyText.setText(R.string.no_active_torrents);
			} else {
				emptyText.setText(R.string.no_torrents);
			}
		}
		
	}

	private void updateStatusText(String taskmessageText) {

		if (taskmessageText != null || allTorrents == null) {
			taskmessage.setText(taskmessageText);
			taskmessage.setVisibility(View.VISIBLE);
			statusBox.setVisibility(View.GONE);
			if (allTorrents == null) {
				emptyText.setVisibility(View.VISIBLE);
			}
			return;
		}
		
		// Keep totals
    	int downloading = 0;
    	int downloadingD = 0;
    	int downloadingU = 0;
    	int eta = -1;
    	int seeding = 0;
    	int seedingU = 0;
    	//int other = 0;
    	for (Torrent tor : allTorrents) {
    		if (tor.getStatusCode() == TorrentStatus.Downloading && (!onlyShowTransferring || tor.getRateDownload() > 0)) {
    			downloading++;
    			downloadingD += tor.getRateDownload();
    			downloadingU += tor.getRateUpload();
    			eta = Math.max(eta, tor.getEta());
    		} else if (tor.getStatusCode() == TorrentStatus.Seeding && (!onlyShowTransferring || tor.getRateUpload() > 0)) {
    			seeding++;
    			seedingU += tor.getRateUpload();
    		//} else {
    		//	other++;
    		}
    	}
    	
    	// Set text views
		taskmessage.setVisibility(View.GONE);
		statusBox.setVisibility(View.VISIBLE);
		statusDown.setText(downloading + "\u2193");
		statusUp.setText(seeding + "\u2191");
		statusDownRate.setText(FileSizeConverter.getSize(downloadingD) + getString(R.string.status_persecond));
		statusUpRate.setText(FileSizeConverter.getSize(downloadingU + seedingU) + getString(R.string.status_persecond));
		emptyText.setVisibility(View.GONE);
		getListView().setVisibility(View.VISIBLE);
    	
	}
	
	private void newTorrentListSorting(TorrentsSortBy newSortBy) {

		this.sortReversed = (sortSetting == newSortBy? !sortReversed: false); // If the same is selected again, reverse the sort order
		this.sortSetting = newSortBy;

		if (!(getTorrentListAdapter() == null || getTorrentListAdapter().getCount() == 0)) {
			
			// Sort the shown list of torrents using the new sortBy criteria
			Collections.sort(allTorrents, new TorrentsComparator(daemon, sortSetting, sortReversed));
			updateTorrentsView(true);
			
		}

		// Remember the new option by saving it to the user preferences (for a new Transdroid startup)
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
		editor.putInt(Preferences.KEY_PREF_LASTSORTBY, sortSetting.getCode());
		editor.putBoolean(Preferences.KEY_PREF_LASTSORTORD, sortReversed);
		editor.putBoolean(Preferences.KEY_PREF_LASTSORTGTZERO, onlyShowTransferring);
		editor.commit();
		
	}

	private OnNavigationListener onServerChanged = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			if (!ignoreFirstListNavigation ) {
				switchDaemonConfig((int) itemId);
			}
			ignoreFirstListNavigation = false;
			return true;
		}
	};
	
	/**
	 * Clear the screen and set up the new daemon (just as if we were clicking on a daemon in the pop-up menu)
	 * @param which To which daemon settings configuration to switch to (which must exist)
	 */
	private void switchDaemonConfig(int which) {

		if (getActivity() == null) {
			return;
		}
		
		// Store this new 'last used server'
		Preferences.storeLastUsedDaemonSettings(getActivity().getApplicationContext(), which);
		lastUsedDaemonSettings = which;
		
		// Clear any old 'refresh' tasks from the queue
		queue.clear(DaemonMethod.Retrieve);
		
		// Hide the old torrents list and show that we are connecting again
		if (allTorrents != null) {
			allTorrents.clear();
		}
		if (getTorrentListAdapter() != null) {
			getTorrentListAdapter().clear();
		}
		emptyText.setText(R.string.connecting);
		
		// Clear the old details fragment
		if (useTabletInterface) {
			Fragment f = getSupportFragmentManager().findFragmentById(R.id.details);
			if (f != null) {
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.remove(f);
				ft.commit();
			}
		}
		
		// Reload server and update torrent list
		setupDaemon();
		updateTorrentList();
		
	}
	
	private void loadLabels() {

		availableLabels = new ArrayList<String>();

		if (Daemon.supportsLabels(getActiveDaemonType())) {
			
			// Add a label that includes all torrents called 'show all'
			String showAll = getText(R.string.labels_showall).toString();
			availableLabels.add(showAll);
			
			// Gather the used labels from the torrents
			if (allLabels!=null){
				for (Label lab : allLabels) {
					String name = lab.getName();
					// Start a new label if this name wasn't encountered yet
					if (!availableLabels.contains(name)) {
						availableLabels.add(name);
					}
					
				}				
			}
			for (Torrent tor : allTorrents) {
				
				// Force a label name (use 'unlabeled' if none is provided)
				String name = tor.getLabelName();
				if (name == null || name.equals("")) {
					name = getText(R.string.labels_unlabeled).toString();
				}
				tor.mimicNewLabel(name);
				
				// Start a new label if this name wasn't encountered yet
				if (!availableLabels.contains(name)) {
					availableLabels.add(name);
				}
				
			}
			Collections.sort(availableLabels);
			
		}
		
		viewtypeselectorpopup.updateLabels(availableLabels);

	}
	
	private void updateTorrentsView(boolean forceScrollToTop) {
		if (daemon != null && allTorrents != null) {

			// Load the labels
			String useLabel = null;
			String showAllLabelsText = getText(R.string.labels_showall).toString();
			loadLabels();
			if (Daemon.supportsLabels(getActiveDaemonType())) {
				useLabel = showAllLabelsText;
				if (activeLabel != null && availableLabels.contains(activeLabel)) {
					useLabel = activeLabel;
				}
			}
			
			// Build a list of torrents that should be shown
			List<Torrent> showTorrents = new ArrayList<Torrent>();
			for (Torrent torrent : allTorrents) {
				if (matchesLabel(torrent, useLabel, showAllLabelsText) && matchesStatus(torrent, activeMainView) && matchesFilter(torrent, activeFilter)) {
					showTorrents.add(torrent);
				}
			}
			
			if (getTorrentListAdapter() == null) {
				setListAdapter(new TorrentListAdapter(this, showTorrents));
			} else {
				getTorrentListAdapter().replace(showTorrents);
			}
			
			// Scroll to the top of the list as well?
			if (forceScrollToTop) {
				getListView().setSelection(0);
			}
			
			updateEmptyListDescription();

			// Update the torrents view type text
			int mainViewTextID = activeMainView == MainViewType.OnlyDownloading? R.string.view_showdl: 
				(activeMainView == MainViewType.OnlyUploading? R.string.view_showul: 
				(activeMainView == MainViewType.OnlyInactive? R.string.view_showinactive: 
					(activeMainView == MainViewType.OnlyActive? R.string.view_showactive: R.string.view_showall)));
			int mainViewDrawableID = activeMainView == MainViewType.OnlyDownloading? R.drawable.icon_showdl: 
				(activeMainView == MainViewType.OnlyUploading? R.drawable.icon_showup: 
					(activeMainView == MainViewType.OnlyInactive? R.drawable.icon_showinactive: 
						(activeMainView == MainViewType.OnlyActive? R.drawable.icon_showactive: R.drawable.icon_showall)));
			viewtype.setText(getText(mainViewTextID).toString() + (Daemon.supportsLabels(getActiveDaemonType())? "\n" + useLabel: ""));
			viewtypeselector.setImageResource(mainViewDrawableID);
			
		}
	}

	private boolean matchesLabel(Torrent torrent, String matchLabel, String showAllLabelsText) {
		if (Daemon.supportsLabels(getActiveDaemonType())) {
			return matchLabel == null || matchLabel.equals(showAllLabelsText) || torrent.getLabelName().equals(matchLabel);
		}
		return true;
	}
	
	private boolean matchesStatus(Torrent torrent, MainViewType matchViewType) {
		boolean isActivelyDownloading = torrent.getStatusCode() == TorrentStatus.Downloading && (!onlyShowTransferring || torrent.getRateDownload() > 0);
		boolean isActivelySeeding = torrent.getStatusCode() == TorrentStatus.Seeding && (!onlyShowTransferring || torrent.getRateUpload() > 0);
		return (matchViewType == MainViewType.ShowAll || 
				(matchViewType == MainViewType.OnlyDownloading && isActivelyDownloading) || 
				(matchViewType == MainViewType.OnlyUploading && isActivelySeeding)||  
				(matchViewType == MainViewType.OnlyActive && (isActivelySeeding || isActivelyDownloading))|| 
				(matchViewType == MainViewType.OnlyInactive && !isActivelyDownloading && !isActivelySeeding));
	}
	
	private boolean matchesFilter(Torrent torrent, String matchSearchString) {
		return ((matchSearchString == null? true : false) || (torrent.getName().toLowerCase().indexOf(matchSearchString.toLowerCase()) > -1));
	}

	private void setProgressBar(boolean b) {
		inProgress  = b;
		if (getSupportActivity() != null)
			getSupportActivity().invalidateOptionsMenu();
	}

	protected View findViewById(int id) {
		return getView().findViewById(id);
	}

	protected ListView getListView() {
		return (ListView) findViewById(R.id.torrents);
	}

	private TorrentListAdapter getTorrentListAdapter() {
		return (TorrentListAdapter) getListView().getAdapter();
	}
	
	private void setListAdapter(TorrentListAdapter torrentListAdapter) {
		getListView().setAdapter(torrentListAdapter);
		if (torrentListAdapter == null || torrentListAdapter.getCount() <= 0) {
			emptyText.setVisibility(View.VISIBLE);
			getListView().setVisibility(View.GONE);
		} else {
			emptyText.setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		}
	}
	
	public void setFilter(String search_text){
		if(search_text.equals(""))
			activeFilter = null;
		else
			activeFilter = search_text.trim();
		updateTorrentsView(true);
	}

	public void showDialog(int id) {
		new DialogWrapper(onCreateDialog(id)).show(getSupportActivity().getSupportFragmentManager(), DialogWrapper.TAG + id);
	}

	protected void dismissDialog(int id) {
		// Remove the dialog wrapper fragment for the dialog's ID
		getSupportActivity().getSupportFragmentManager().beginTransaction().remove(
			getSupportActivity().getSupportFragmentManager().findFragmentByTag(DialogWrapper.TAG + id)).commit();
	}

    /**
     * Returns the type of the currently active (shown) daemon
     * @return The enum type of the active daemon
     */
	public Daemon getActiveDaemonType() {
		return daemon.getType();
	}

    /*@Override
    public boolean onTouchEvent(MotionEvent me) {
    	return gestureDetector.onTouchEvent(me);
    }*/

	@Override
	public boolean onTouch(View v, MotionEvent event) {
    	return gestureDetector.onTouchEvent(event);
	}
	
	/**
	 * Internal class that handles gestures from the Transdroid main screen (a 'swipe' or 'fling').
	 * 
	 * More at http://stackoverflow.com/questions/937313/android-basic-gesture-detection
	 */
	class MainScreenGestureListener extends SimpleOnGestureListener {
		
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
	            
	            if (daemon != null && interfaceSettings != null) {
		            if (Daemon.supportsLabels(daemon.getType()) && interfaceSettings.shouldSwipeLabels()) {
		            	
		            	// Determine to which label to switch
		            	String[] allLabels = buildLabelTexts(true);
		            	int newLabel = 0;
		            	if (activeLabel != null) {
		            		for (int i = 0; i < allLabels.length; i++) {
			            		if (activeLabel.equals(allLabels[i])) {
			            			newLabel = i;
			            			break;
			            		}
		            		}
		            	}
			            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			            	newLabel++;
			            	if (newLabel >= allLabels.length) {
			            		newLabel = 0;
			            	}
			            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			            	newLabel--;
			            	if (newLabel < 0) {
			            		newLabel = allLabels.length - 1;
			            	}
			            }
			            
			            // Make the switch, if possible and needed
			            if (newLabel >=0 && newLabel < allLabels.length && (activeLabel == null || activeLabel != allLabels[newLabel])) {
			            	activeLabel = allLabels[newLabel];
				            updateTorrentsView(true);
			            }
		            	
		            } else {
		            	
			            // Determine to which daemon we are now switching
			            int newDaemon = lastUsedDaemonSettings;
			            // right to left swipe
			            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			            	newDaemon = lastUsedDaemonSettings + 1;
			                if (newDaemon >= allDaemonSettings.size()) {
			                	newDaemon = 0;
			                }
			            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			            	newDaemon = lastUsedDaemonSettings - 1;
			                if (newDaemon < 0) {
			                	newDaemon = allDaemonSettings.size() - 1;
			                }
			            }
			            
			            // Make the switch, if needed
			            if (lastUsedDaemonSettings != newDaemon) {
			            	switchDaemonConfig(newDaemon);
			            }
		            
		            }
	            }
			}
            
	        return false;			
		}
		
	}

}
