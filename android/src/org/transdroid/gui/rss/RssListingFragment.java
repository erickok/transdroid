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
 package org.transdroid.gui.rss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.R;
import org.transdroid.gui.Torrents;
import org.transdroid.gui.Transdroid;
import org.transdroid.gui.search.Search;
import org.transdroid.gui.util.DialogWrapper;
import org.transdroid.gui.util.SelectableArrayAdapter.OnSelectedChangedListener;
import org.transdroid.preferences.Preferences;
import org.transdroid.rss.RssFeedSettings;
import org.transdroid.util.TLog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

@SuppressLint("ValidFragment")
public class RssListingFragment extends SherlockFragment implements OnTouchListener, OnSelectedChangedListener {

	private static final String LOG_NAME = "RSS listing";

	private static final int MENU_REFRESH_ID = 1;
	private static final int ITEMMENU_ADD_ID = 10;
	private static final int ITEMMENU_BROWSE_ID = 11;
	private static final int ITEMMENU_DETAILS_ID = 12;
	private static final int ITEMMENU_USEASSEARCH_ID = 13;

	private static final int DIALOG_ITEMDETAILS = 20;
	
	private TextView empty;
	private LinearLayout addSelected;
	private Button addSelectedButton;
	private GestureDetector gestureDetector;
	
	private boolean inProgress = false;
	private List<RssFeedSettings> allFeeds = null;
	protected List<Item> lastLoadedItems = null;
	private RssFeedSettings feedSettings;
	private boolean ignoreFirstListNavigation = true;
	private String detailsDialogText;


	public RssListingFragment(RssFeedSettings feedSettings) {
		this.feedSettings = feedSettings;
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_rsslisting, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        
        registerForContextMenu(getListView());
        getListView().setOnItemClickListener(onItemClicked);
        empty = (TextView) getView().findViewById(android.R.id.empty);
        addSelected = (LinearLayout) getView().findViewById(R.id.add_selected);
        addSelectedButton = (Button) getView().findViewById(R.id.add_selectedbutton);
        addSelectedButton.setOnClickListener(addSelectedClicked);
        // Swiping or flinging between server configurations
        gestureDetector = new GestureDetector(new RssScreenGestureListener());
        getSherlockActivity().getSupportActionBar().setTitle(R.string.rss);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        allFeeds = Preferences.readAllRssFeedSettings(prefs);
        
    	ignoreFirstListNavigation = true;
        if (getActivity() instanceof RssListing) {
        	getSherlockActivity().getSupportActionBar().setListNavigationCallbacks(buildFeedsAdapter(), onFeedSelected);
        }
        if (lastLoadedItems == null || feedSettings == null) {
        	// Start loading the items
        	loadItems();
        } else {
        	// Set items from the retained instance state
        	if (getActivity() instanceof RssListing) {
        		getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(feedSettingsIndex(feedSettings));
        		setListAdapter(new RssItemListAdapter(getActivity(), RssListingFragment.this, lastLoadedItems, true, feedSettings.getLastNew()));
        	}
        }
        
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.add(0, ITEMMENU_ADD_ID, 0, R.string.searchmenu_add);
		menu.add(0, ITEMMENU_BROWSE_ID, 0, R.string.searchmenu_details);
		menu.add(0, ITEMMENU_DETAILS_ID, 0, R.string.details_details);
		menu.add(0, ITEMMENU_USEASSEARCH_ID, 0, R.string.searchmenu_useassearch);
		
	}
	
	private void loadItems() {

		setListAdapter(null);
		if (feedSettings == null) {
			empty.setText(R.string.rss_no_items);
			return;
		}
		
		// Show the 'loading' icon (rotating indeterminate progress bar)
		setProgressBar(true);
		empty.setText(R.string.rss_loading_feed);

		// Show the (newly) selected feed
		if (getActivity() instanceof RssListing && feedSettings.getName() != null && !feedSettings.getName().equals("")) {
	    	ignoreFirstListNavigation = true;
	    	getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(feedSettingsIndex(feedSettings));
		}
		
		// Read the RSS items asynchronously
		final Handler retrieveHandler = new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {

					// Not loading any more, turn off status indicator
					setProgressBar(false);
					
				   // Error?
				   if (msg.what == -1) {
					    String errorMessage = ((Exception)msg.obj).getMessage();
						Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
						empty.setText(errorMessage);
						return;
				   }
				   
				   // The list of items is contained in the message obj
				   List<Item> items = (List<Item>) msg.obj;
				   lastLoadedItems  = items;
				   if (items == null || items.size() == 0) {
					   
					   empty.setText(R.string.rss_no_items);
					   
				   } else {
					   
					   setListAdapter(new RssItemListAdapter(getActivity(), RssListingFragment.this, items, true, feedSettings.getLastNew()));

					   // Also store the 'last url' for the newest item that we now viewed
					   // (The most recent is always the first item, we assume)
					   Editor manager = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
					   manager.putString(Preferences.KEY_PREF_RSSLASTNEW + feedSettings.getKey(), items.get(0).getTheLink());
					   manager.commit();
					   feedSettings.setLastNew(items.get(0).getTheLink());
					   
				   }
				   
			}
		};

		// Asynchronous getting of the RSS items
		new Thread() {
			@Override
			public void run() {
				try {

					// Load RSS items
					RssParser parser = new RssParser(feedSettings.getUrl());
					parser.parse();
					if (parser.getChannel() == null) {
						throw new Exception(getResources().getString(R.string.error_norssfeed));
					}
					List<Item> items = parser.getChannel().getItems();
					Collections.sort(items, Collections.reverseOrder());
					
					// Return the list of items
					TLog.d(LOG_NAME, "RSS feed " + feedSettings.getName() + " has " + items.size() + " messages.");
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

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Item result = (Item) getListAdapter().getItem((int) info.id);
		
		switch (item.getItemId()) {
		case ITEMMENU_ADD_ID:
			
			// Directly add this torrent
			addTorrent(result);
			break;
			
		case ITEMMENU_BROWSE_ID:
			
			// Open the browser to show the website contained in the item's link tag
			if (result.getLink() != null && !result.getLink().equals("")) {
				Uri uri = Uri.parse(result.getLink());
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} else {
	    		// No URL was specified in the RSS feed item link tag (or no link tag was present)
	    		Toast.makeText(getActivity(), R.string.error_no_link, Toast.LENGTH_LONG).show();
			}
			break;

		case ITEMMENU_DETAILS_ID:
			
			// Show a dialog box with the RSS item description text
			detailsDialogText = result.getDescription();
			showDialog(DIALOG_ITEMDETAILS);
			break;

		case ITEMMENU_USEASSEARCH_ID:
			
			// Use the title of this torrent as a new search query
			Intent search = new Intent(getActivity(), Search.class);
			search.setAction(Intent.ACTION_SEARCH);
			search.putExtra(SearchManager.QUERY, result.getTitle());
			startActivity(search);
			break;
		}
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		// Add title bar buttons
		if (getActivity() instanceof RssListing) {
			MenuItem miRefresh = menu.add(0, MENU_REFRESH_ID, 0, R.string.refresh);
			miRefresh.setIcon(R.drawable.icon_refresh_title);
			miRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			if (inProgress ) {
				// Show progress spinner instead of the option item
				View view = getActivity().getLayoutInflater().inflate(R.layout.part_actionbar_progressitem, null);
				miRefresh.setActionView(view);
			}
		}

	}

	private OnItemClickListener onItemClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Item item = (Item) getListAdapter().getItem(position);
			// If something was already selected before
			if (!getRssItemListAdapter().getSelected().isEmpty()) {
				getRssItemListAdapter().itemChecked(item, !getRssItemListAdapter().isItemChecked(item));
				getListView().invalidateViews();
			} else {
				// No selection: add directly
				addTorrent(item);
			}
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH_ID:
			loadItems();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ITEMDETAILS:

			// Build a dialog with a description text
			AlertDialog.Builder detailsDialog = new AlertDialog.Builder(getActivity());
			detailsDialog.setTitle(R.string.details_details);
			detailsDialog.setMessage(Html.fromHtml((detailsDialogText == null? "": detailsDialogText)));
			detailsDialog.setNeutralButton(android.R.string.ok, null);
			return detailsDialog.create();

		/*case DIALOG_FEEDS:
			
			// Build a dialog with a radio box per RSS feed
			AlertDialog.Builder feedsDialog = new AlertDialog.Builder(this);
			feedsDialog.setTitle(R.string.pref_rss_info);
			feedsDialog.setSingleChoiceItems(
					buildFeedTextsForDialog(), // The strings of the available RSS feeds 
					feedSettingsIndex(feedSettings), 
					new DialogInterface.OnClickListener() {
						@Override
						// When the feed is clicked (and it is different from the current shown feed), 
						// change the feed to show
						public void onClick(DialogInterface dialog, int which) {
							RssFeedSettings selected = allFeeds.get(which);
							if (selected.getKey() != feedSettings.getKey()) {

								feedSettings = selected;
								loadItems();
								
							}
							removeDialog(DIALOG_FEEDS);
						}
			});
			return feedsDialog.create();*/
			
		}
		return null;

	}

	/*@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_ITEMDETAILS:
			
			AlertDialog detailsDialog = (AlertDialog) dialog;
			detailsDialog.setMessage(Html.fromHtml(detailsDialogText == null? "": detailsDialogText));
			break;
			
		}
		super.onPrepareDialog(id, dialog);
	}*/

	private int feedSettingsIndex(RssFeedSettings afeed) {
		int i = 0;
		for (RssFeedSettings feed : allFeeds) {
			if (feed.getKey().equals(afeed.getKey())) {
				return i;
			}
			i++;
		}
		return -1;
	}
	
	private SpinnerAdapter buildFeedsAdapter() {
    	ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), R.layout.abs__simple_spinner_item, buildFeedTextsForDialog());
    	ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	return ad;
	}

	private String[] buildFeedTextsForDialog() {

		// Build a textual list of RSS feeds available
		ArrayList<String> feeds = new ArrayList<String>();
		for (RssFeedSettings feed : allFeeds) {
			feeds.add(feed.getName());
		}
		return feeds.toArray(new String[feeds.size()]);
		
	}

	private OnNavigationListener onFeedSelected = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int which, long itemId) {
			if (!ignoreFirstListNavigation) {
				RssFeedSettings selected = allFeeds.get(which);
				if (selected.getKey() != feedSettings.getKey()) {
	
					feedSettings = selected;
					loadItems();
					
				}
			}
			ignoreFirstListNavigation = false;
			return true;
		}
	};
    
    private RssItemListAdapter getRssItemListAdapter() {
		return (RssItemListAdapter) getListAdapter();
	}

	private void addTorrent(Item item) {

    	// The actual torrent URL is usually contained an 'enclosure' or the link tag
    	if (item.getTheLink() != null && !item.getTheLink().equals("")) {

    		Intent i;    		
    		if (feedSettings.needsAuthentication()) {
    			// Redirect links to the Android browser instead  of directly adding it via URL
    			i = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getTheLink()));
    		} else {
	    		// Build new intent that Transdroid can pick up again
	    		i = new Intent(getActivity(), Torrents.class);
	    		i.setData(Uri.parse(item.getTheLink()));
    		}

    		// Create a result for the calling activity
    		//setResult(RESULT_OK);
    		startActivity(i);
    		getSherlockActivity().getSupportFragmentManager().popBackStack();

    	} else {
    		
    		// No URL was specified in the RSS feed item
    		Toast.makeText(getActivity(), R.string.error_no_url_enclosure, Toast.LENGTH_LONG).show();
    		
    	}
    	
    }
    
    private void addTorrents(List<Item> items) {

    	// The actual torrent URL is usually contained an 'enclosure' or the link tag
    	// We will test the first selected item, since it is very likely that if this has a link, the others will as well
    	if (items != null && items.size() > 0) {
	    	if (items.get(0).getTheLink() != null && !items.get(0).getTheLink().equals("")) {
		
				// Build new intent that Transdroid can pick up again
				// This sets the data to "<url>|<url>|..."
				Intent i = new Intent(getActivity(), Torrents.class);
				String[] urls = new String[items.size()];
				for (int j = 0; j < items.size(); j++) {
					urls[j] = items.get(j).getTheLink();
				}
				i.setAction(Transdroid.INTENT_ADD_MULTIPLE);
				i.putExtra(Transdroid.INTENT_TORRENT_URLS, urls);
		
				// Create a result for the calling activity
				//setResult(RESULT_OK);
				startActivity(i);
				getSherlockActivity().getSupportFragmentManager().popBackStack();
	
	    	} else {
	    		
	    		// No URL was specified in the RSS feed item
	    		Toast.makeText(getActivity(), R.string.error_no_url_enclosure, Toast.LENGTH_LONG).show();
	    		
	    	}
    	}
    	
    }

	private void setProgressBar(boolean b) {
		inProgress = b;
		if (getSherlockActivity() != null) {
			getSherlockActivity().supportInvalidateOptionsMenu();
		}
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
	 * Internal class that handles gestures from the search screen (a 'swipe' or 'fling').
	 * 
	 * More at http://stackoverflow.com/questions/937313/android-basic-gesture-detection
	 */
	class RssScreenGestureListener extends SimpleOnGestureListener {
		
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
	            
	            // Determine to which feed we are now switching
	            int newFeed = feedSettingsIndex(feedSettings);
	            // right to left swipe
	            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	newFeed += 1;
	                if (newFeed >= allFeeds.size()) {
	                	newFeed = 0;
	                }
	            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	            	newFeed -= 1;
	                if (newFeed < 0) {
	                	newFeed = allFeeds.size() - 1;
	                }
	            }
	            
	            // Make the switch, if needed
	            RssFeedSettings newFeedSettings = allFeeds.get(newFeed);
	            if (!newFeedSettings.getKey().equals(feedSettings.getKey())) {
	            	feedSettings = newFeedSettings;
					loadItems();
	            }
			}
            
	        return false;			
		}

	}

	/**
	 * Called by the SelectableArrayAdapter when the set of selected items changed
	 */
	public void onSelectedResultsChanged() {
		RssItemListAdapter adapter = (RssItemListAdapter) getListAdapter();
		if (adapter.getSelected().size() == 0) {
			// Hide the 'add selected' button
			addSelected.setVisibility(View.GONE);
		} else {
			addSelected.setVisibility(View.VISIBLE);
		}
	}
	
	private OnClickListener addSelectedClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Send the urls of all selected search result back to Transdroid
			RssItemListAdapter adapter = (RssItemListAdapter) getListAdapter();
			addTorrents(adapter.getSelected());
		}
	};

	public void showDialog(int id) {
		new DialogWrapper(onCreateDialog(id)).show(getSherlockActivity().getSupportFragmentManager(), DialogWrapper.TAG + id);
	}

	protected void dismissDialog(int id) {
		// Remove the dialog wrapper fragment for the dialog's ID
		getSherlockActivity().getSupportFragmentManager().beginTransaction().remove(
				getSherlockActivity().getSupportFragmentManager().findFragmentByTag(DialogWrapper.TAG + id)).commit();
	}

	protected ListView getListView() {
		return (ListView) getView().findViewById(android.R.id.list);
	}

	protected RssItemListAdapter getListAdapter() {
		return (RssItemListAdapter) getListView().getAdapter();
	}
	
	private View getEmptyText() {
		return getView().findViewById(android.R.id.empty);
	}

	private void setListAdapter(RssItemListAdapter adapter) {
		getListView().setAdapter(adapter);
		if (adapter == null || adapter.getCount() <= 0) {
			getListView().setVisibility(View.GONE);
			getEmptyText().setVisibility(View.VISIBLE);
		} else {
			getListView().setVisibility(View.VISIBLE);
			getEmptyText().setVisibility(View.GONE);
		}
	}

}
