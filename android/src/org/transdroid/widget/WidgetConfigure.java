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
package org.transdroid.widget;

import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.preferences.Preferences;
import org.transdroid.preferences.TransdroidButtonPreference;
import org.transdroid.preferences.TransdroidCheckBoxPreference;
import org.transdroid.preferences.TransdroidEditTextPreference;
import org.transdroid.preferences.TransdroidListPreference;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An activity to set up some preferences for a new-to-add home screen widget.
 * 
 * @author erickok
 */
public class WidgetConfigure extends PreferenceActivity {

	private int widget = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean isSmall;
	private List<DaemonSettings> allDaemons;

	private TransdroidListPreference daemon;
	private TransdroidListPreference refresh;
	private TransdroidListPreference layout;
	private TransdroidButtonPreference addButton;

	private String daemonValue = null;
	private String refreshValue;
	private String layoutValue;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        
        // For which widget?
        if (getIntent() != null && getIntent().hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
        	widget = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        if (widget == AppWidgetManager.INVALID_APPWIDGET_ID) {
        	finish();
        	return;
        }
        
        // Determine if this is a configuration session for a small widget
        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
        isSmall = mgr.getAppWidgetInfo(widget).provider.getClassName().equals("org.transdroid.widget.WidgetSmall");
        
        // Create the preferences screen here: this takes care of saving/loading, but also contains the ListView adapter, etc.
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load the user preferences (with the available daemons)
        allDaemons = Preferences.readAllDaemonSettings(prefs);
        String[] allDaemonEntries = new String[allDaemons.size()];
        String[] allDaemonValues = new String[allDaemons.size()];
        int i = 0;
        for (DaemonSettings daemon : allDaemons) {
        	allDaemonEntries[i] = daemon.getName();
        	allDaemonValues[i] = daemon.getIdString();
        	i++;
        }
        
        // Are there any configured servers that can be attached to this widget?
        if (allDaemons.size() == 0) {
        	Toast.makeText(this, R.string.no_servers, Toast.LENGTH_SHORT).show();
        	finish();
        	return;
        }
        
        // Set default values
        DaemonSettings lastUsedDaemon = Preferences.readLastUsedDaemonSettings(prefs, allDaemons);
        if (lastUsedDaemon != null) {
        	daemonValue = lastUsedDaemon.getIdString();
        }
        refreshValue = "86400";
        if (isSmall) {
        	layoutValue = "style_small";	
        } else {
        	layoutValue = "style_black";
        }
        // Save this settings, so that if the user doesn't make any changes, the widget still has the default values being assigned
        Editor editor = prefs.edit();
        editor.putString(Preferences.KEY_WIDGET_DAEMON + widget, daemonValue);
        editor.putString(Preferences.KEY_WIDGET_REFRESH + widget, refreshValue);
        editor.putString(Preferences.KEY_WIDGET_LAYOUT + widget, layoutValue);
        editor.commit();
        
        // Create preference objects
        getPreferenceScreen().setTitle(R.string.pref_search);
        // Daemon
        daemon = new TransdroidListPreference(this);
        daemon.setTitle(R.string.widget_pref_server);
        daemon.setKey(Preferences.KEY_WIDGET_DAEMON + widget);
        daemon.setEntries(allDaemonEntries);
        daemon.setEntryValues(allDaemonValues);
        daemon.setDialogTitle(R.string.widget_pref_server);
        if (daemonValue != null) {
        	daemon.setValue(daemonValue);
        	int daemonId = (daemonValue == ""? 0: Integer.parseInt(daemonValue));
        	daemon.setSummary(allDaemons.get(daemonId).getName());
        }
        daemon.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(daemon);
        // Refresh
        refresh = new TransdroidListPreference(this);
        refresh.setTitle(R.string.widget_pref_refresh);
        refresh.setKey(Preferences.KEY_WIDGET_REFRESH + widget);
        refresh.setEntries(R.array.pref_alarminterval_types);
        refresh.setEntryValues(R.array.pref_alarminterval_values);
        refresh.setDialogTitle(R.string.widget_pref_refresh);
        refresh.setValue(refreshValue);
        refresh.setOnPreferenceChangeListener(updateHandler);
        getPreferenceScreen().addItemFromInflater(refresh);
        if (!isSmall) {
	        // Layout
	        layout = new TransdroidListPreference(this);
	        layout.setTitle(R.string.widget_pref_layout);
	        layout.setKey(Preferences.KEY_WIDGET_LAYOUT + widget);
	        layout.setEntries(R.array.pref_widget_types);
	        layout.setEntryValues(R.array.pref_widget_values);
	        layout.setDialogTitle(R.string.widget_pref_layout);
	        layout.setValue(layoutValue);
	        layout.setOnPreferenceChangeListener(updateHandler);
	        getPreferenceScreen().addItemFromInflater(layout);
        }
        addButton = new TransdroidButtonPreference(this);
        addButton.setTitle(R.string.widget_pref_addwidget);
        addButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startWidget();
				return true;
			}
		});
        getPreferenceScreen().addItemFromInflater(addButton);
        
        updateDescriptionTexts();
        
    }

	private OnPreferenceChangeListener updateHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference == daemon) {
				daemonValue = (String) newValue;
			} else if (preference == refresh) {
				refreshValue = (String) newValue;
			} else if (preference == layout) {
				layoutValue = (String) newValue;
			}
			updateDescriptionTexts();
			// Set the value as usual
			return true;
		}
    };
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Perform click action, which always is a Preference
    	Preference item = (Preference) getListAdapter().getItem(position);
    	
		// Let the Preference open the right dialog
		if (item instanceof TransdroidListPreference) {
			((TransdroidListPreference)item).click();
		} else if (item instanceof TransdroidCheckBoxPreference) {
    		((TransdroidCheckBoxPreference)item).click();
		} else if (item instanceof TransdroidEditTextPreference) {
    		((TransdroidEditTextPreference)item).click();
		}
		
    }

    private void updateDescriptionTexts() {
    	int daemonId = (daemonValue == ""? 0: Integer.parseInt(daemonValue));
    	daemon.setSummary(daemonValue == null? "": allDaemons.get(daemonId).getName());
    	refresh.setSummary(Preferences.parseArrayEntryFromValue(this, R.array.pref_alarminterval_types, R.array.pref_alarminterval_values, refreshValue));
    	if (layout != null) {
    		layout.setSummary(Preferences.parseArrayEntryFromValue(this, R.array.pref_widget_types, R.array.pref_widget_values, layoutValue));
    	}
    }

    protected void startWidget() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	WidgetSettings settings = Preferences.readWidgetSettings(prefs, widget, allDaemons);
    	
    	// Perform a first widget update
    	/*AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
    	RemoteViews views = new RemoteViews(getPackageName(), settings.getLayoutResourceId());
    	appWidgetManager.updateAppWidget(settings.getId(), views);*/
    	
    	WidgetService.scheduleUpdates(getApplicationContext(), settings.getId());
		
    	setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, settings.getId()));
    	finish();

	}

}
