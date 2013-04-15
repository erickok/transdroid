package org.transdroid.core.gui.settings;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.transdroid.core.R;
import org.transdroid.core.app.search.SearchHelper;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.WebsearchSetting;
import org.transdroid.core.gui.settings.RssfeedPreference.OnRssfeedClickedListener;
import org.transdroid.core.gui.settings.ServerPreference.OnServerClickedListener;
import org.transdroid.core.gui.settings.WebsearchPreference.OnWebsearchClickedListener;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

/**
 * The main activity that provides access to all application settings. It shows the configured serves, web search sites
 * and RSS feeds along with other general settings.
 * @author Eric Kok
 */
@EActivity
public class MainSettingsActivity extends SherlockPreferenceActivity {

	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected SearchHelper searchHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Note: Settings are loaded in onResume()
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		
		if (getPreferenceScreen() != null)
			getPreferenceScreen().removeAll();
		
		// Load the preference menu and attach actions
		addPreferencesFromResource(R.xml.pref_main);
		findPreference("header_addserver").setOnPreferenceClickListener(onAddServer);
		findPreference("header_addwebsearch").setOnPreferenceClickListener(onAddWebsearch);
		findPreference("header_addrssfeed").setOnPreferenceClickListener(onAddRssfeed);
		findPreference("header_background").setOnPreferenceClickListener(onBackgroundSettings);
		findPreference("header_system").setOnPreferenceClickListener(onSystemSettings);

		// Add existing servers
		List<ServerSetting> servers = applicationSettings.getServerSettings();
		for (ServerSetting serverSetting : servers) {
			getPreferenceScreen().addPreference(
					new ServerPreference(this).setServerSetting(serverSetting).setOnServerClickedListener(
							onServerClicked));
		}

		// Add existing websearch sites
		List<WebsearchSetting> websearches = applicationSettings.getWebsearchSettings();
		for (WebsearchSetting websearchSetting : websearches) {
			getPreferenceScreen().addPreference(
					new WebsearchPreference(this).setWebsearchSetting(websearchSetting).setOnWebsearchClickedListener(
							onWebsearchClicked));
		}

		// Add existing RSS feeds
		List<RssfeedSetting> rssfeeds = applicationSettings.getRssfeedSettings();
		for (RssfeedSetting rssfeedSetting : rssfeeds) {
			getPreferenceScreen().addPreference(
					new RssfeedPreference(this).setRssfeedSetting(rssfeedSetting).setOnRssfeedClickedListener(
							onRssfeedClicked));
		}

		// Construct list of all available search sites, in-app and web
		ListPreference setSite = (ListPreference) findPreference("header_setsearchsite");
		// Retrieve the available in-app search sites (using the Torrent Search package)
		List<SearchSite> searchsites = searchHelper.getAvailableSites();
		if (searchsites == null)
			searchsites = new ArrayList<SearchSite>();
		List<String> siteNames = new ArrayList<String>(websearches.size() + searchsites.size());
		List<String> siteValues = new ArrayList<String>(websearches.size() + searchsites.size());
		for (SearchSite searchSite : searchsites) {
			siteNames.add(searchSite.getName());
			siteValues.add(searchSite.getKey());
		}
		for (WebsearchSetting websearch : websearches) {
			siteNames.add(websearch.getName());
			siteValues.add(websearch.getKey());
		}
		// Supply the Preference list names and values
		setSite.setEntries(siteNames.toArray(new String[siteNames.size()]));
		setSite.setEntryValues(siteValues.toArray(new String[siteValues.size()]));
		
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		// TODO: Add two-pane support in settings
		super.onBuildHeaders(target);
	}

	private OnPreferenceClickListener onAddServer = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			ServerSettingsActivity_.intent(MainSettingsActivity.this).start();
			return true;
		}
	};

	private OnPreferenceClickListener onAddWebsearch = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			WebsearchSettingsActivity_.intent(MainSettingsActivity.this).start();
			return true;
		}
	};

	private OnPreferenceClickListener onAddRssfeed = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			RssfeedSettingsActivity_.intent(MainSettingsActivity.this).start();
			return true;
		}
	};

	private OnPreferenceClickListener onBackgroundSettings = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			NotificationSettingsActivity_.intent(MainSettingsActivity.this).start();
			return true;
		}
	};

	private OnPreferenceClickListener onSystemSettings = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			SystemSettingsActivity_.intent(MainSettingsActivity.this).start();
			return true;
		}
	};

	private OnServerClickedListener onServerClicked = new OnServerClickedListener() {
		@Override
		public void onServerClicked(ServerSetting serverSetting) {
			ServerSettingsActivity_.intent(MainSettingsActivity.this).key(serverSetting.getOrder()).start();
		}
	};

	private OnWebsearchClickedListener onWebsearchClicked = new OnWebsearchClickedListener() {
		@Override
		public void onWebsearchClicked(WebsearchSetting websearchSetting) {
			WebsearchSettingsActivity_.intent(MainSettingsActivity.this).key(websearchSetting.getOrder()).start();
		}
	};

	private OnRssfeedClickedListener onRssfeedClicked = new OnRssfeedClickedListener() {
		@Override
		public void onRssfeedClicked(RssfeedSetting rssfeedSetting) {
			RssfeedSettingsActivity_.intent(MainSettingsActivity.this).key(rssfeedSetting.getOrder()).start();
		}
	};

}
