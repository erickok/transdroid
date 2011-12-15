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

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.R;
import org.transdroid.gui.rss.RssItemListAdapter;
import org.transdroid.util.TLog;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class EzRssFeedBuilder extends ListActivity implements Runnable {

	private static final String LOG_NAME = "RSS listing";

	private static final String EZRSS_URL = "http://ezrss.it/search/index.php?show_name=%s%s&date=&quality=%s%s&release_group=%s&mode=rss";
	private static final String EZRSS_URL_SHOWNAME_EXACT = "&show_name_exact=true";
	private static final String EZRSS_URL_QUALITY_EXACT = "&quality_exact=true";
	private static final long TIMER_DELAY = 750;
	
	private TextView empty;
	private EditText showname, quality, group;
	private CheckBox shownameExact, qualityExact;
	private Button save, dismiss;
	
	private Thread timer; 
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_ezrss_feedbuilder);
        registerForContextMenu(findViewById(android.R.id.list));
        
        empty = (TextView) findViewById(android.R.id.empty);
        showname = (EditText) findViewById(R.id.ezrss_showname);
        quality = (EditText) findViewById(R.id.ezrss_quality);
        group = (EditText) findViewById(R.id.ezrss_group);
        shownameExact = (CheckBox) findViewById(R.id.ezrss_showname_exact);
        qualityExact = (CheckBox) findViewById(R.id.ezrss_quality_exact);
        save = (Button) findViewById(R.id.ezrss_save);
        dismiss = (Button) findViewById(R.id.ezrss_dismiss);

        showname.addTextChangedListener(queryChangedListener);
        quality.addTextChangedListener(queryChangedListener);
        group.addTextChangedListener(queryChangedListener);
        shownameExact.setOnCheckedChangeListener(queryChangedListener2);
        qualityExact.setOnCheckedChangeListener(queryChangedListener2);
        save.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				
				// Look for the last RSS feed setting
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				int i = 0;
		        String nextUrl = Preferences.KEY_PREF_RSSURL + Integer.toString(i);
		        while (prefs.contains(nextUrl)) {
		        	i++;
		        	nextUrl = Preferences.KEY_PREF_RSSURL + Integer.toString(i);
		        }
		        
				// Store an RSS feed setting for this ezRSS feed
		        Editor editor = prefs.edit();
		        editor.putString(Preferences.KEY_PREF_RSSNAME + Integer.toString(i), showname.getText().toString());
		        editor.putString(nextUrl, getUrlForQuery());
		        editor.commit();

		    	finish();
			}
		});
        dismiss.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
		    	finish();
			}
		});
        
    }

    /**
     * Refreshes the example feed on text changes (with delay)
     */
	private TextWatcher queryChangedListener = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			timer = new Thread(EzRssFeedBuilder.this);
			timer.start();
			updateWidgets(false, getText(R.string.pref_ezrss_enter).toString(), showname.getText().toString().trim().equals(""));
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			if (timer != null && timer.isAlive()) {
				timer.interrupt();
			}
		}
		@Override
		public void afterTextChanged(Editable s) {}
	};
	
	private OnCheckedChangeListener queryChangedListener2 = new OnCheckedChangeListener() {		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateQuery();
		}
	};

	/**
	 * Implements a small timer to delay the loading of the example RSS feed
	 */
	@Override
	public void run() {
		try {
			Thread.sleep(TIMER_DELAY);
			// If not interrupted...
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Update the example results
					updateQuery();
				}
			});
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Returns the ezRSS feed URL for the given query, consisting of name, quality and group
	 * @return The URL address of the feed as String
	 */
	private String getUrlForQuery() {
		return String.format(EZRSS_URL, 
				URLEncoder.encode(showname.getText().toString()),
				(shownameExact.isChecked()? EZRSS_URL_SHOWNAME_EXACT: ""), 
				URLEncoder.encode(quality.getText().toString()), 
				(qualityExact.isChecked()? EZRSS_URL_QUALITY_EXACT: ""), 
				URLEncoder.encode(group.getText().toString()));
	}
	
	private void updateQuery() {
		
		if (showname.getText().toString().trim().equals("")) {
			// Not even a show name given
			updateWidgets(false, getText(R.string.pref_ezrss_enter).toString(), false);
			return;
		}
		
		final String url = getUrlForQuery();
		
		// Set the user message to Loading...
		setProgressBarIndeterminate(true);
		updateWidgets(false, getText(R.string.pref_ezrss_loading).toString(), true);
		
		final Handler retrieveHandler = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {

					// Not loading any more, turn off status indicator
				setProgressBarIndeterminate(false);
					
				   // Error?
				   if (msg.what == -1) {
					    String errorMessage = ((Exception)msg.obj).getMessage();
						Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
						updateWidgets(false, errorMessage, true);
						return;
				   }
				   
				   // The list of items is contained in the message obj
				   List<Item> items = (List<Item>) msg.obj;
				   if (items == null || items.size() == 0) {
					   updateWidgets(false, getText(R.string.pref_ezrss_noresults).toString(), true);
				   } else {
					   setListAdapter(new RssItemListAdapter(EzRssFeedBuilder.this, null, items, false, null));
					   updateWidgets(true, "", true);
				   }
				   
			}
		};

		// Asynchronous getting of the RSS items
		new Thread() {
			@Override
			public void run() {
				try {

					// Load RSS items
					RssParser parser = new RssParser(url);
					parser.parse();
					if (parser.getChannel() == null) {
						throw new Exception(getResources().getString(R.string.error_norssfeed));
					}
					List<Item> items = parser.getChannel().getItems();
					Collections.sort(items, Collections.reverseOrder());
					
					// Return the list of items
					TLog.d(LOG_NAME, "ezRSS feed for '" + showname.getText() + "' has " + items.size() + " messages");
					Message msg = Message.obtain();
					msg.what = 1;
					msg.obj = items;
					retrieveHandler.sendMessage(msg);
					
				} catch (Exception e) {
					
					// Return the error to the callback
					TLog.d(LOG_NAME, e.toString());
					Message msg = Message.obtain();
					msg.what = -1;
					msg.obj = e;
					retrieveHandler.sendMessage(msg);
				}
				
			}
		}.start();

	}

	private void updateWidgets(boolean hasResults, String message, boolean hasQuery) {
		if (!hasResults) {
			// No results (yet), show the message to the user
			empty.setText(message);
			setListAdapter(null);
		}
		save.setEnabled(hasQuery);
	}

}
