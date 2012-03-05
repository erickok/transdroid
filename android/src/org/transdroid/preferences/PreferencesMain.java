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
 package org.transdroid.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.gui.search.SiteSettings;
import org.transdroid.gui.search.TorrentSearchHistoryProvider;
import org.transdroid.gui.util.ActivityUtil;
import org.transdroid.preferences.PreferencesAdapter.PreferencesListButton;
import org.transdroid.preferences.PreferencesAdapter.SeedM8ListButton;
import org.transdroid.preferences.PreferencesAdapter.SeedstuffListButton;
import org.transdroid.preferences.PreferencesAdapter.XirvikListButton;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import ca.seedstuff.transdroid.preferences.PreferencesSeedstuffServer;
import ca.seedstuff.transdroid.preferences.SeedstuffSettings;

import com.seedm8.transdroid.preferences.PreferencesSeedM8Server;
import com.seedm8.transdroid.preferences.SeedM8Settings;
import com.xirvik.transdroid.preferences.PreferencesXirvikServer;
import com.xirvik.transdroid.preferences.XirvikSettings;

/**
 * Provides an activity to edit and store the user preferences of the Transdroid application.
 * 
 * @author erickok
 *
 */
public class PreferencesMain extends ListActivity {

	private static final int MENU_SET_DEFAULT_ID = 0;
	private static final int MENU_REMOVE_ID = 1;
	static final int DIALOG_XIRVIK_INFO = 0;
	static final int DIALOG_SEEDM8_INFO = 1;
	private static final int DIALOG_SET_DEFAULT_SITE = 2;
	private static final int DIALOG_IMPORT_SETTINGS = 3;
	private static final int DIALOG_EXPORT_SETTINGS = 4;
	private static final int DIALOG_INSTALL_FILE_MANAGER = 5;
	static final int DIALOG_SEEDSTUFF_INFO = 6;
	private final static String PICK_DIRECTORY_INTENT = "org.openintents.action.PICK_DIRECTORY";
	private final static String PICK_FILE_INTENT = "org.openintents.action.PICK_FILE";
	private final static Uri OIFM_MARKET_URI = Uri.parse("market://search?q=pname:org.openintents.filemanager");
	public static final int DIRECTORY_REQUEST_CODE = 1;
	public static final int FILE_REQUEST_CODE = 2;
	
	private PreferencesAdapter adapter;
	private SharedPreferences prefs;
	
	private SiteSettings currentdefaultsite;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
        // Make sure a context menu is created on long-presses
        registerForContextMenu(getListView());

        buildAdapter();
        
    }

	private void buildAdapter() {
		
        // Build a list of server and search site settings objects to show
		List<XirvikSettings> xservers = Preferences.readAllXirvikSettings(prefs);
		List<SeedM8Settings> s8servers = Preferences.readAllSeedM8Settings(prefs);
		List<SeedstuffSettings> sservers = Preferences.readAllSeedstuffSettings(prefs);
		List<DaemonSettings> daemons = Preferences.readAllNormalDaemonSettings(prefs);
		List<SiteSettings> websites = Preferences.readAllWebSearchSiteSettings(prefs);
		
		currentdefaultsite = Preferences.readDefaultSearchSiteSettings(prefs);
        
        // Set the list items
        adapter = new PreferencesAdapter(this, xservers, s8servers, sservers, daemons, websites);
        setListAdapter(adapter);
        
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Perform click action depending on the clicked list item (note that dividers are ignored)
    	Object item = getListAdapter().getItem(position);

    	// Handle button clicks first
    	if (item instanceof XirvikListButton) {

			// What is the max current xirvik settings ID number?
			int max = 0;
			while (prefs.contains(Preferences.KEY_PREF_XSERVER + (max == 0? "": Integer.toString(max)))) {
				max++;
			}
			
			// Start a new xirvik server settings screen
    		Intent i = new Intent(this, PreferencesXirvikServer.class);
    		i.putExtra(PreferencesXirvikServer.PREFERENCES_XSERVER_KEY, (max == 0? "": Integer.toString(max)));
    		startActivityForResult(i, 0);

    	} else if (item instanceof SeedM8ListButton) {

			// What is the max current seedm8 settings ID number?
			int max = 0;
			while (prefs.contains(Preferences.KEY_PREF_8SERVER + (max == 0? "": Integer.toString(max)))) {
				max++;
			}
			
			// Start a new seedm8 server settings screen
    		Intent i = new Intent(this, PreferencesSeedM8Server.class);
    		i.putExtra(PreferencesSeedM8Server.PREFERENCES_8SERVER_KEY, (max == 0? "": Integer.toString(max)));
    		startActivityForResult(i, 0);

    	} else if (item instanceof SeedstuffListButton) {

			// What is the max current seedstuff settings ID number?
			int max = 0;
			while (prefs.contains(Preferences.KEY_PREF_SUSER + (max == 0? "": Integer.toString(max)))) {
				max++;
			}
			
			// Start a new seedstuff server settings screen
    		Intent i = new Intent(this, PreferencesSeedstuffServer.class);
    		i.putExtra(PreferencesSeedstuffServer.PREFERENCES_SSERVER_KEY, (max == 0? "": Integer.toString(max)));
    		startActivityForResult(i, 0);

    	} else if (item instanceof PreferencesListButton) {

    		PreferencesListButton button = (PreferencesListButton) item;
    		if (button.getKey().equals(PreferencesAdapter.ADD_NEW_DAEMON)) {
    			
    			// What is the max current server ID number?
    			int max = 0;
    			while (prefs.contains(Preferences.KEY_PREF_ADDRESS + (max == 0? "": Integer.toString(max)))) {
    				max++;
    			}
    			
    			// Start a new server daemon settings screen
        		Intent i = new Intent(this, PreferencesServer.class);
        		i.putExtra(PreferencesServer.PREFERENCES_SERVER_KEY, (max == 0? "": Integer.toString(max)));
        		startActivityForResult(i, 0);

			} else if (button.getKey().equals(PreferencesAdapter.ADD_NEW_WEBSITE)) {
				
				// What is the max current site ID number?
				int max = 0;
				while (prefs.contains(Preferences.KEY_PREF_WEBURL + Integer.toString(max))) {
					max++;
				}
				
				// Start a new web site settings screen
	    		Intent i = new Intent(this, PreferencesWebSearch.class);
	    		i.putExtra(PreferencesWebSearch.PREFERENCES_WEBSITE_KEY, Integer.toString(max));
	    		startActivityForResult(i, 0);
	
			} else if (button.getKey().equals(PreferencesAdapter.RSS_SETTINGS)) {
	    		startActivity(new Intent(this, PreferencesRss.class));
	
			} else if (button.getKey().equals(PreferencesAdapter.INTERFACE_SETTINGS)) {
	    		startActivity(new Intent(this, PreferencesInterface.class));
	
			} else if (button.getKey().equals(PreferencesAdapter.CLEAN_SEARCH_HISTORY)) {
				
				// Clear all previous search terms from the search history provider
				TorrentSearchHistoryProvider.clearHistory(this);
				Toast.makeText(this, R.string.pref_history_cleared, Toast.LENGTH_SHORT).show();

			} else if (button.getKey().equals(PreferencesAdapter.SET_DEFAULT_SITE)) {
				showDialog(DIALOG_SET_DEFAULT_SITE);
	
			} else if (button.getKey().equals(PreferencesAdapter.ALARM_SETTINGS)) {
	    		startActivity(new Intent(this, PreferencesAlarm.class));

			} else if (button.getKey().equals(PreferencesAdapter.IMPORT_SETTINGS)) {
				showDialog(DIALOG_IMPORT_SETTINGS);

			} else if (button.getKey().equals(PreferencesAdapter.EXPORT_SETTINGS)) {
				showDialog(DIALOG_EXPORT_SETTINGS);
			}

    	} else if (item instanceof XirvikSettings) { 
    		
    		// Open the xirvik server settings edit activity for the clicked server
    		Intent i = new Intent(this, PreferencesXirvikServer.class);
    		XirvikSettings xserver = (XirvikSettings) item;
    		i.putExtra(PreferencesXirvikServer.PREFERENCES_XSERVER_KEY, xserver.getIdString());
    		startActivityForResult(i, 0);

    	} else if (item instanceof SeedM8Settings) { 
    		
    		// Open the seedm8 server settings edit activity for the clicked server
    		Intent i = new Intent(this, PreferencesSeedM8Server.class);
    		SeedM8Settings s8server = (SeedM8Settings) item;
    		i.putExtra(PreferencesSeedM8Server.PREFERENCES_8SERVER_KEY, s8server.getIdString());
    		startActivityForResult(i, 0);

    	} else if (item instanceof SeedstuffSettings) { 
    		
    		// Open the seedstuff server settings edit activity for the clicked server
    		Intent i = new Intent(this, PreferencesSeedstuffServer.class);
    		SeedstuffSettings sserver = (SeedstuffSettings) item;
    		i.putExtra(PreferencesSeedstuffServer.PREFERENCES_SSERVER_KEY, sserver.getIdString());
    		startActivityForResult(i, 0);

    	} else if (item instanceof DaemonSettings) { 
    		
    		// Open the daemon settings edit activity for the clicked server
    		Intent i = new Intent(this, PreferencesServer.class);
    		DaemonSettings daemon = (DaemonSettings) item;
    		i.putExtra(PreferencesServer.PREFERENCES_SERVER_KEY, daemon.getIdString());
    		startActivityForResult(i, 0);

    	} else if (item instanceof SiteSettings) { 
    		
    		SiteSettings website = (SiteSettings) item;
    		if (website.isWebSearch()) {
	    		// Open the site settings edit activity for the clicked website
	    		Intent i = new Intent(this, PreferencesWebSearch.class);
	    		i.putExtra(PreferencesWebSearch.PREFERENCES_WEBSITE_KEY, website.getKey());
	    		startActivityForResult(i, 0);
    		}
    		
    	}
		
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    	switch (requestCode) {
    	case DIRECTORY_REQUEST_CODE:
	    	// Did we receive a directory name?
	    	if (data != null && data.getData() != null && data.getData().toString() != "") {
				if (!canReadWriteToExternalStorage(false)) {
					Toast.makeText(PreferencesMain.this, R.string.error_media_not_available, Toast.LENGTH_LONG).show();
					return;
				}
				String path = data.getData().toString().substring("file://".length()) + ImportExport.DEFAULT_SETTINGS_FILENAME;
	    		doExport(new File(path));
	    	}
	    	break;

    	case FILE_REQUEST_CODE:
	    	// Did we receive a file name?
	    	if (data != null && data.getData() != null && data.getData().toString() != "") {
				if (!canReadWriteToExternalStorage(false)) {
					Toast.makeText(PreferencesMain.this, R.string.error_media_not_available, Toast.LENGTH_LONG).show();
					return;
				}
				String file = data.getData().toString().substring("file://".length());
	    		doImport(new File(file));
	    	}
	    	break;

    	default:
    		// One of the server settings has been updated: refresh the list
    		buildAdapter();
    		break;
    		
    	}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		Object selected = adapter.getItem((int) ((AdapterContextMenuInfo) item.getMenuInfo()).id);
		if (selected instanceof DaemonSettings) {
			
			if (item.getItemId() == MENU_REMOVE_ID) {

				// Remove this daemon configuration and reload this screen
				Preferences.removeDaemonSettings(prefs, (DaemonSettings)selected);
				buildAdapter();
				return true;
			}
		}
		if (selected instanceof XirvikSettings) {
			
			if (item.getItemId() == MENU_REMOVE_ID) {

				// Remove this xirvik server configuration and reload this screen
				Preferences.removeXirvikSettings(prefs, (XirvikSettings)selected);
				buildAdapter();
				return true;
			}
		}
		if (selected instanceof SeedM8Settings) {
			
			if (item.getItemId() == MENU_REMOVE_ID) {

				// Remove this SeedM8 server configuration and reload this screen
				Preferences.removeSeedM8Settings(prefs, (SeedM8Settings)selected);
				buildAdapter();
				return true;
			}
		}
		if (selected instanceof SeedstuffSettings) {
			
			if (item.getItemId() == MENU_REMOVE_ID) {

				// Remove this Seedstuff server configuration and reload this screen
				Preferences.removeSeedstuffSettings(prefs, (SeedstuffSettings)selected);
				buildAdapter();
				return true;
			}
		}
		if (selected instanceof SiteSettings) {

			if (item.getItemId() == MENU_REMOVE_ID) {
				
				// Remove this site configuration and reload this screen
				Preferences.removeSiteSettings(prefs, (SiteSettings)selected);
				buildAdapter();
				return true;
				
			} else if (item.getItemId() == MENU_SET_DEFAULT_ID) {
				
				// Set this site as the default search
				Preferences.storeLastUsedSearchSiteSettings(this, ((SiteSettings)selected).getKey());
				Toast.makeText(this, getResources().getText(R.string.menu_default_site_set_to) + " " + ((SiteSettings)selected).getName(), Toast.LENGTH_SHORT).show();
				return true;
				
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// Allow removing of daemon and site settings
		Object item = adapter.getItem((int) ((AdapterContextMenuInfo)menuInfo).id);

		// For XirvikSettings, allow removing of the config
		if (item instanceof XirvikSettings) {
			menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
		}

		// For SeedM8Settings, allow removing of the config
		if (item instanceof SeedM8Settings) {
			menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
		}

		// For SeedstuffSettings, allow removing of the config
		if (item instanceof SeedstuffSettings) {
			menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
		}

		// For DeamonSettings, allow removing of the config
		if (item instanceof DaemonSettings) {
			menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
		}
		
		// For SiteSettings, show a context menu
		if (item instanceof SiteSettings) {
			menu.add(0, MENU_SET_DEFAULT_ID, 0, R.string.menu_set_default);
			// Allow removal if it is a web search engine (in-app sites cannot be removed)
			if (((SiteSettings)item).isWebSearch()) {
				menu.add(0, MENU_REMOVE_ID, 0, R.string.menu_remove);
			}
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_XIRVIK_INFO:

			// Build a dialog with the xirvik info message (with logo and link)
			AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
			infoDialog.setView(getLayoutInflater().inflate(R.layout.dialog_xirvik_info, null));
			return infoDialog.create();

		case DIALOG_SEEDM8_INFO:

			// Build a dialog with the seedm8 info message (with logo and link)
			AlertDialog.Builder s8infoDialog = new AlertDialog.Builder(this);
			s8infoDialog.setView(getLayoutInflater().inflate(R.layout.dialog_seedm8_info, null));
			return s8infoDialog.create();

		case DIALOG_SEEDSTUFF_INFO:

			// Build a dialog with the seedstuff info message (with logo and link)
			AlertDialog.Builder sinfoDialog = new AlertDialog.Builder(this);
			sinfoDialog.setView(getLayoutInflater().inflate(R.layout.dialog_seedstuff_info, null));
			return sinfoDialog.create();

		case DIALOG_SET_DEFAULT_SITE:

			// Build a dialog with a radio box per available search site
			List<SiteSettings> allsites = Preferences.readAllSiteSettings(prefs);
			AlertDialog.Builder sitesDialog = new AlertDialog.Builder(this);
			sitesDialog.setTitle(R.string.pref_setdefault);
			// Determine the ID of the current default site
			final String[] sitesTexts = buildSiteListForDialog(allsites);
			int i = 0;
			for (String siteName : sitesTexts) {
				if (currentdefaultsite == null || siteName.equals(currentdefaultsite.getName())) {
					break;
				}
				i++;
			}
			int activeItem = i;
			sitesDialog.setSingleChoiceItems(
					sitesTexts, // The strings of different sites
					(allsites == null? 0: (activeItem < allsites.size()? activeItem: 0)), // The current selection except when this suddenly doesn't exist any more 
					new DialogInterface.OnClickListener() {
				
						@Override
						// When the site name is clicked (and it is different from current default), set the new default site
						public void onClick(DialogInterface dialog, int which) {
							List<SiteSettings> allsites = Preferences.readAllSiteSettings(prefs);
							currentdefaultsite = allsites.get(which);
							Preferences.storeLastUsedSearchSiteSettings(getApplicationContext(), currentdefaultsite.getKey());
							removeDialog(DIALOG_SET_DEFAULT_SITE);
						}
			});
			return sitesDialog.create();

		case DIALOG_IMPORT_SETTINGS:
			Builder importDialog = new AlertDialog.Builder(this);
			importDialog.setTitle(R.string.pref_import_settings);
			importDialog.setMessage(getText(R.string.pref_import_settings_info) + " " + ImportExport.DEFAULT_SETTINGS_FILE);
			importDialog.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					dismissDialog(DIALOG_IMPORT_SETTINGS);
					if (!canReadWriteToExternalStorage(false)) {
						Toast.makeText(PreferencesMain.this, R.string.error_media_not_available, Toast.LENGTH_LONG).show();
						return;
					}
					doImport(ImportExport.DEFAULT_SETTINGS_FILE);
				}
			});
			importDialog.setNeutralButton(R.string.pref_import_settings_pick, new OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Test to see if a file manager is available that can handle the PICK_FILE intent, such as IO File Manager
					Intent pick = new Intent(PICK_FILE_INTENT);
			    	if (ActivityUtil.isIntentAvailable(PreferencesMain.this, pick)) {
			    		// Ask the file manager to allow the user to pick a directory
			    		startActivityForResult(pick, FILE_REQUEST_CODE);
			    	} else {
			    		// Show a message if the user should install OI File Manager for this feature
			    		showDialog(DIALOG_INSTALL_FILE_MANAGER);
			    	}
				}
			});
			importDialog.setNegativeButton(android.R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					dismissDialog(DIALOG_IMPORT_SETTINGS);
				}
			});
			return importDialog.create();

		case DIALOG_EXPORT_SETTINGS:
			Builder exportDialog = new AlertDialog.Builder(this);
			exportDialog.setTitle(R.string.pref_export_settings);
			exportDialog.setMessage(getText(R.string.pref_export_settings_info) + " " + ImportExport.DEFAULT_SETTINGS_FILE);
			exportDialog.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					dismissDialog(DIALOG_EXPORT_SETTINGS);
					if (!canReadWriteToExternalStorage(false)) {
						Toast.makeText(PreferencesMain.this, R.string.error_media_not_available, Toast.LENGTH_LONG).show();
						return;
					}
					doExport(ImportExport.DEFAULT_SETTINGS_FILE);
				}
			});
			exportDialog.setNeutralButton(R.string.pref_export_settings_pick, new OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
			    	// Test to see if a file manager is available that can handle the PICK_DIRECTORY intent, such as IO File Manager
					Intent pick = new Intent(PICK_DIRECTORY_INTENT);
			    	if (ActivityUtil.isIntentAvailable(PreferencesMain.this, pick)) {
			    		// Ask the file manager to allow the user to pick a directory
			    		startActivityForResult(pick, DIRECTORY_REQUEST_CODE);
			    	} else {
			    		// Show a message if the user should install OI File Manager for this feature
			    		showDialog(DIALOG_INSTALL_FILE_MANAGER);
			    	}
				}
			});
			exportDialog.setNegativeButton(android.R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					dismissDialog(DIALOG_EXPORT_SETTINGS);
				}
			});
			return exportDialog.create();

		case DIALOG_INSTALL_FILE_MANAGER:
			return buildInstallDialog(R.string.oifm_not_found, OIFM_MARKET_URI);
			
		}
		return super.onCreateDialog(id);

	}

    /**
     * Builds a (reusable) dialog that asks to install some application from the Android market
     * @param messageResourceID The message to show to the user
     * @param marketUri The application's URI on the Android Market
     * @return
     */
	private Dialog buildInstallDialog(int messageResourceID, final Uri marketUri) {
		AlertDialog.Builder fbuilder = new AlertDialog.Builder(this);
		fbuilder.setMessage(messageResourceID);
		fbuilder.setCancelable(true);
		fbuilder.setPositiveButton(R.string.oifm_install, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent install = new Intent(Intent.ACTION_VIEW, marketUri);
				if (ActivityUtil.isIntentAvailable(getApplicationContext(), install)) {
					startActivity(install);
				} else {
					Toast.makeText(getApplicationContext(), R.string.oifm_nomarket, Toast.LENGTH_LONG).show();
				}
				dialog.dismiss();
			}
		});
		fbuilder.setNegativeButton(android.R.string.cancel, new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		return fbuilder.create();
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		switch (id) {
		case DIALOG_SET_DEFAULT_SITE:
			
			// Re-populate the dialog adapter with the available sites
			List<SiteSettings> allsites = Preferences.readAllSiteSettings(prefs);
			AlertDialog sitesDialog = (AlertDialog) dialog;
			ListView sitesRadios = sitesDialog.getListView();
			String[] sitesTexts = buildSiteListForDialog(allsites);
			ArrayAdapter<String> sitesList = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, android.R.id.text1, sitesTexts);
			sitesRadios.setAdapter(sitesList);
			
			// Determine the ID of the current default
			int i = 0;
			for (String siteName : sitesTexts) {
				if (currentdefaultsite == null || siteName.equals(currentdefaultsite.getName())) {
					break;
				}
				i++;
			}
			// Also pre-select the current default site
			int labelSelected = (allsites == null? 0: (i < allsites.size()? i: 0)); // Prevent going out of bounds
			sitesRadios.clearChoices();
			sitesRadios.setItemChecked(labelSelected, true);
			sitesRadios.setSelection(labelSelected);
			break;

		}
	}

	private String[] buildSiteListForDialog(List<SiteSettings> allsites) {
		String[] sites = new String[allsites.size()];
		for (int i = 0; i < allsites.size(); i++) {
			sites[i] = allsites.get(i).getName();
		}
		return sites;
	}

	private boolean canReadWriteToExternalStorage(boolean needsWriteAccess) {
        // Check access to SD card (for import/export)
        if (needsWriteAccess) {
        	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        } else {
        	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY) ||
        		Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        }
	}

	private void doImport(File settingsFile) {
		try {
			ImportExport.importSettings(PreferenceManager.getDefaultSharedPreferences(PreferencesMain.this), settingsFile);
			buildAdapter();
			Toast.makeText(PreferencesMain.this, R.string.pref_import_settings_success, Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			Toast.makeText(PreferencesMain.this, R.string.error_no_valid_settings_file, Toast.LENGTH_SHORT).show();
		} catch (FileNotFoundException e) {
			Toast.makeText(PreferencesMain.this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
		}
	}

	private void doExport(File settingsFile) {
		try {
			Toast.makeText(PreferencesMain.this, R.string.pref_export_settings_success, Toast.LENGTH_SHORT).show();
			ImportExport.exportSettings(PreferenceManager.getDefaultSharedPreferences(PreferencesMain.this), settingsFile);
		} catch (JSONException e) {
			Toast.makeText(PreferencesMain.this, R.string.error_no_valid_settings_file, Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(PreferencesMain.this, R.string.error_media_not_available, Toast.LENGTH_SHORT).show();
		}
	}
	
}
