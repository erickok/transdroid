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

import java.util.ArrayList;
import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.gui.search.SiteSettings;
import org.transdroid.rss.RssFeedSettings;

import ca.seedstuff.transdroid.preferences.SeedstuffSettings;

import com.seedm8.transdroid.preferences.SeedM8Settings;
import com.xirvik.transdroid.preferences.XirvikSettings;

import android.app.ListActivity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A hybrid adapter that can show Transdroid prefences in a list screen.
 *  
 * @author erickok
 *
 */
public class PreferencesAdapter extends BaseAdapter {

	public static final String ADD_NEW_XSERVER = "add_new_xserver";
	public static final String ADD_NEW_8SERVER = "add_new_8server";
	public static final String ADD_NEW_SSERVER = "add_new_sserver";
	public static final String ADD_NEW_DAEMON = "add_new_daemon";
	public static final String ADD_NEW_WEBSITE = "add_new_website";
	public static final String ADD_NEW_RSSFEED = "add_new_rssfeed";
	public static final String ADD_EZRSS_FEED = "add_ezrss_feed";
	public static final String RSS_SETTINGS = "rss_settings";
	public static final String INTERFACE_SETTINGS = "interface_settings";
	public static final String CLEAN_SEARCH_HISTORY = "clear_search_history";
	public static final String ALARM_SETTINGS = "alarm_settings";
	public static final String SET_DEFAULT_SITE = "set_default_site";
	public static final String EXPORT_SETTINGS = "export_settings";
	public static final String IMPORT_SETTINGS = "import_settings";
	
	private Context context;
	private List<Object> items;
	
	/**
	 * Convenience constructor for a simple listing of the given servers (for server selection, for example)
	 * @param context The preferences screen
	 * @param daemons List of existing servers
	 */
	public PreferencesAdapter(Context context, List<DaemonSettings> daemons) {
		this(context, null, null, null, null, daemons, null, null, true, false, false);
	}

	/**
	 * Convenience constructor for a the RSS preferences screen
	 * @param context The preferences screen
	 * @param feeds List of existing RSS feeds
	 * @param foo Dummy (unused) parameter to make this constructor's signature unique
	 */
	public PreferencesAdapter(Context context, List<RssFeedSettings> feeds, int foo) {
		this(context, null, null, null, null, null, feeds, null, false, false, true);
	}

	/**
	 * Convenience constructor for the main preferences screen
	 * @param preferencesActivity The preferences screen
	 * @param xservers All Xirvik server settings
	 * @param s8servers All SeedM8 server settings
	 * @param daemons All regular server settings
	 * @param websites All web-search site settings
	 */
	public PreferencesAdapter(ListActivity preferencesActivity, List<XirvikSettings> xservers, List<SeedM8Settings> s8servers, List<SeedstuffSettings> sservers, List<DaemonSettings> daemons, List<SiteSettings> websites) {
		this(preferencesActivity, preferencesActivity, xservers, s8servers, sservers, daemons, null, websites, true, true, false);
	}
	
	private PreferencesAdapter(Context context, ListActivity preferencesActivity, List<XirvikSettings> xservers, List<SeedM8Settings> s8servers, List<SeedstuffSettings> sservers, List<DaemonSettings> daemons, List<RssFeedSettings> feeds, List<SiteSettings> websites, boolean withDaemons, boolean withOthers, boolean withRssFeeds) {

		this.context = context;
		
		// Put all 'real' items in a generic list together with the needed buttons and separators
		this.items = new ArrayList<Object>();
		if (withDaemons) {
			this.items.addAll(daemons);
		}
		if (withOthers) {
			this.items.addAll(xservers);
			this.items.addAll(s8servers);
			this.items.addAll(sservers);
			this.items.add(new PreferencesListButton(context, ADD_NEW_DAEMON, R.string.add_new_server));
			this.items.add(new XirvikListButton(preferencesActivity, ADD_NEW_XSERVER, R.string.xirvik_add_new_xserver));
			this.items.add(new SeedM8ListButton(preferencesActivity, ADD_NEW_8SERVER, R.string.seedm8_add_new_xserver));
			this.items.add(new SeedstuffListButton(preferencesActivity, ADD_NEW_SSERVER, R.string.seedstuff_add_new_xserver));
			this.items.add(new Divider(context, R.string.pref_search));
			this.items.add(new PreferencesListButton(context, SET_DEFAULT_SITE, R.string.pref_setdefault));
			this.items.addAll(websites);
			this.items.add(new PreferencesListButton(context, ADD_NEW_WEBSITE, R.string.add_new_website));
			this.items.add(new Divider(context, R.string.other_settings));
			this.items.add(new PreferencesListButton(context, RSS_SETTINGS, R.string.pref_rss_info));
			this.items.add(new PreferencesListButton(context, INTERFACE_SETTINGS, R.string.pref_interface));
			this.items.add(new PreferencesListButton(context, ALARM_SETTINGS, R.string.pref_alarm));
			this.items.add(new PreferencesListButton(context, CLEAN_SEARCH_HISTORY, R.string.pref_clear_search_history));
			this.items.add(new PreferencesListButton(context, EXPORT_SETTINGS, R.string.pref_export_settings));
			this.items.add(new PreferencesListButton(context, IMPORT_SETTINGS, R.string.pref_import_settings));
		}
		if (withRssFeeds) {
			this.items.addAll(feeds);
			this.items.add(new PreferencesListButton(context, ADD_NEW_RSSFEED, R.string.add_new_rssfeed));
			this.items.add(new PreferencesListButton(context, ADD_EZRSS_FEED, R.string.pref_ezrss));
		}
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		// Always enabled, except when the item is a Divider
		return !(getItem(position) instanceof Divider);
	}

	@Override
	/**
	 * Returns the view for this list item, which is 
	 * a xirvik server,
	 * a daemon, 
	 * a web search site, 
	 * an RSS feed,
	 * or one of the buttons or dividers
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		Object item = getItem(position);
		if (item instanceof XirvikSettings) {
			
			// return a Xirvik list view
			XirvikSettings xserver = (XirvikSettings) item;
			if (convertView == null || !(convertView instanceof XirvikSettingsView)) {
				return new XirvikSettingsView(context, xserver);
			}
			// Reuse view
			XirvikSettingsView setView = (XirvikSettingsView) convertView;
			setView.SetData(xserver);
			return setView;

		} else if (item instanceof SeedM8Settings) {
			
			// return a SeedM8 list view
			SeedM8Settings s8server = (SeedM8Settings) item;
			if (convertView == null || !(convertView instanceof SeedM8SettingsView)) {
				return new SeedM8SettingsView(context, s8server);
			}
			// Reuse view
			SeedM8SettingsView setView = (SeedM8SettingsView) convertView;
			setView.SetData(s8server);
			return setView;

		} else if (item instanceof SeedstuffSettings) {
			
			// return a Seedstuff list view
			SeedstuffSettings sserver = (SeedstuffSettings) item;
			if (convertView == null || !(convertView instanceof SeedstuffSettingsView)) {
				return new SeedstuffSettingsView(context, sserver);
			}
			// Reuse view
			SeedstuffSettingsView setView = (SeedstuffSettingsView) convertView;
			setView.SetData(sserver);
			return setView;

		} else if (item instanceof DaemonSettings) {
			
			// return a DaemonSettings list view
			DaemonSettings daemon = (DaemonSettings) item;
			if (convertView == null || !(convertView instanceof DaemonSettingsView)) {
				return new DaemonSettingsView(context, daemon);
			}
			// Reuse view
			DaemonSettingsView setView = (DaemonSettingsView) convertView;
			setView.SetData(daemon);
			return setView;

		} else if (item instanceof SiteSettings) {

			// return a SiteSettings list view
			SiteSettings site = (SiteSettings) item;
			if (convertView == null || !(convertView instanceof SiteSettingsView)) {
				return new SiteSettingsView(context, site);
			}
			// Reuse view
			SiteSettingsView setView = (SiteSettingsView) convertView;
			setView.SetData(site);
			return setView;

		} else if (item instanceof RssFeedSettings) {

			// return a RssSettings list view
			RssFeedSettings rssFeed = (RssFeedSettings) item; 
			if (convertView == null || !(convertView instanceof RssFeedSettingsView)) {
				return new RssFeedSettingsView(context, rssFeed);
			}
			// Reuse view
			RssFeedSettingsView setView = (RssFeedSettingsView) convertView;
			setView.SetData(rssFeed);
			return setView;

		} else if (item instanceof LinearLayout){

			// Directly return the button/divider view
			return (LinearLayout) item;

		}
		return null;
		
	}

	/**
	 * A list item representing a Xirvik settings object (by showing its name and an identifier text)
	 */
	public class XirvikSettingsView extends LinearLayout {

		XirvikSettings settings;
		
		public XirvikSettingsView(Context context, XirvikSettings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_seedbox_settings, null));
			ImageView icon = (ImageView) findViewById(R.id.icon);
			icon.setImageResource(R.drawable.xirvik_icon);
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual texts and images to the visible widgets (fields)
		 */
		public void SetData(XirvikSettings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setText(settings.getHumanReadableIdentifier());
		}

	}

	/**
	 * A list item representing a SeedM8 settings object (by showing its name and an identifier text)
	 */
	public class SeedM8SettingsView extends LinearLayout {

		SeedM8Settings settings;
		
		public SeedM8SettingsView(Context context, SeedM8Settings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_seedbox_settings, null));
			ImageView icon = (ImageView) findViewById(R.id.icon);
			icon.setImageResource(R.drawable.seedm8_icon2);
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual texts and images to the visible widgets (fields)
		 */
		public void SetData(SeedM8Settings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setText(settings.getHumanReadableIdentifier());
		}

	}

	/**
	 * A list item representing a Seedstuff settings object (by showing its name and an identifier text)
	 */
	public class SeedstuffSettingsView extends LinearLayout {

		SeedstuffSettings settings;
		
		public SeedstuffSettingsView(Context context, SeedstuffSettings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_seedbox_settings, null));
			ImageView icon = (ImageView) findViewById(R.id.icon);
			icon.setImageResource(R.drawable.seedstuff_icon);
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual texts and images to the visible widgets (fields)
		 */
		public void SetData(SeedstuffSettings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setText(settings.getHumanReadableIdentifier());
		}

	}

	/**
	 * A list item representing a daemon settings object (by showing its name and an identifier text)
	 */
	public class DaemonSettingsView extends LinearLayout {

		DaemonSettings settings;
		
		public DaemonSettingsView(Context context, DaemonSettings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_daemon_settings, null));
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual texts and images to the visible widgets (fields)
		 */
		public void SetData(DaemonSettings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setText(settings.getHumanReadableIdentifier());
		}

	}

	/**
	 * A list item representing a daemon settings object (by showing its name and an identifier text)
	 */
	public class SiteSettingsView extends LinearLayout {

		SiteSettings settings;
		
		public SiteSettingsView(Context context, SiteSettings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_daemon_settings, null));
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual texts and images to the visible widgets (fields)
		 */
		public void SetData(SiteSettings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setText(settings.getSearchTypeTextResource());
		}

	}

	/**
	 * A list item representing an RSS feed settings object (by showing its name)
	 */
	public class RssFeedSettingsView extends LinearLayout {

		RssFeedSettings settings;
		
		public RssFeedSettingsView(Context context, RssFeedSettings settings) {
			super(context);
			addView(inflate(context, R.layout.list_item_daemon_settings, null));
			
			this.settings = settings;
			SetData(settings);
		}

		/**
		 * Sets the actual text to the visible widget
		 */
		public void SetData(RssFeedSettings settings) {
			((TextView)findViewById(R.id.title)).setText(settings.getName());
			((TextView)findViewById(R.id.summary)).setVisibility(GONE);
		}

	}

	/**
	 * An action button that can be shown inside the list
	 */
	public class PreferencesListButton extends LinearLayout {

		private String key;
		
		/**
		 * Create a static action button instance, that can be shown in the list screen
		 * @param context The application context
		 * @param key The button-unique string to identify clicks
		 * @param textResourceID The resource of the text to show as the buttons title text
		 */
		public PreferencesListButton(Context context, String key, int textResourceID) {
			super(context);
			addView(inflate(context, android.R.layout.simple_list_item_1, null));

			this.key = key;
			((TextView)findViewById(android.R.id.text1)).setText(textResourceID);
		}
		
		/**
		 * Returns the string identifier that can be used on clicks
		 * @return The identifier key
		 */
		public String getKey() {
			return key;
		}
	}

	/**
	 * An button to show inside the list, that allows adding of a new xirvik server as well as to click a '?' button
	 */
	public class XirvikListButton extends LinearLayout {

		private String key;
		
		/**
		 * Create a static action button instance, that can be shown in the list screen
		 * @param context The application context
		 * @param key The button-unique string to identify clicks
		 * @param textResourceID The resource of the text to show as the buttons title text
		 */
		public XirvikListButton(final ListActivity context, String key, int textResourceID) {
			super(context);
			addView(inflate(context, R.layout.list_item_seedbox_pref, null));

			this.key = key;
			((TextView)findViewById(R.id.add_server)).setText(textResourceID);
			ImageButton helpButton = (ImageButton)findViewById(R.id.info);
			helpButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					context.showDialog(PreferencesMain.DIALOG_XIRVIK_INFO);
				}
			});
			helpButton.setFocusable(false);
		}
		
		/**
		 * Returns the string identifier that can be used on clicks
		 * @return The identifier key
		 */
		public String getKey() {
			return key;
		}
	}

	/**
	 * An button to show inside the list, that allows adding of a new seedm8 server as well as to click a '?' button
	 */
	public class SeedM8ListButton extends LinearLayout {

		private String key;
		
		/**
		 * Create a static action button instance, that can be shown in the list screen
		 * @param preferencesActivity The application context
		 * @param key The button-unique string to identify clicks
		 * @param textResourceID The resource of the text to show as the buttons title text
		 */
		public SeedM8ListButton(final ListActivity preferencesActivity, String key, int textResourceID) {
			super(preferencesActivity);
			addView(inflate(preferencesActivity, R.layout.list_item_seedbox_pref, null));

			this.key = key;
			((TextView)findViewById(R.id.add_server)).setText(textResourceID);
			ImageButton helpButton = (ImageButton)findViewById(R.id.info);
			helpButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					preferencesActivity.showDialog(PreferencesMain.DIALOG_SEEDM8_INFO);
				}
			});
			helpButton.setFocusable(false);
		}
		
		/**
		 * Returns the string identifier that can be used on clicks
		 * @return The identifier key
		 */
		public String getKey() {
			return key;
		}
	}

	/**
	 * An button to show inside the list, that allows adding of a new seedstuff server as well as to click a '?' button
	 */
	public class SeedstuffListButton extends LinearLayout {

		private String key;
		
		/**
		 * Create a static action button instance, that can be shown in the list screen
		 * @param preferencesActivity The application context
		 * @param key The button-unique string to identify clicks
		 * @param textResourceID The resource of the text to show as the buttons title text
		 */
		public SeedstuffListButton(final ListActivity preferencesActivity, String key, int textResourceID) {
			super(preferencesActivity);
			addView(inflate(preferencesActivity, R.layout.list_item_seedbox_pref, null));

			this.key = key;
			((TextView)findViewById(R.id.add_server)).setText(textResourceID);
			ImageButton helpButton = (ImageButton)findViewById(R.id.info);
			helpButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					preferencesActivity.showDialog(PreferencesMain.DIALOG_SEEDSTUFF_INFO);
				}
			});
			helpButton.setFocusable(false);
		}
		
		/**
		 * Returns the string identifier that can be used on clicks
		 * @return The identifier key
		 */
		public String getKey() {
			return key;
		}
	}
	
	/**
	 * A list divider (with the same look as a PreferenceCategory), showing a simple text	 *
	 */
	public class Divider extends LinearLayout {

		public Divider(Context context, int textResourceID) {
			super(context);
			addView(inflate(context, R.layout.list_item_preferences_divider, null));
			((TextView)findViewById(R.id.title)).setText(textResourceID);
		}

	}
    
}
