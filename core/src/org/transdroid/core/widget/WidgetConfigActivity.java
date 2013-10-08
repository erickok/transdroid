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
package org.transdroid.core.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemSelect;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.lists.SortByListItem;
import org.transdroid.core.gui.lists.TorrentsAdapter;
import org.transdroid.core.gui.navigation.FilterListItemAdapter;
import org.transdroid.core.gui.navigation.StatusType;
import org.transdroid.core.gui.navigation.StatusType.StatusTypeFilter;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Label;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentsComparator;
import org.transdroid.daemon.TorrentsSortBy;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.SherlockListView;

@EActivity(resName = "activity_widgetconfig")
public class WidgetConfigActivity extends SherlockActivity {

	// Views and adapters
	@ViewById
	protected Spinner serverSpinner, filterSpinner, sortSpinner;
	@ViewById
	protected CheckBox reverseorderCheckBox, darkthemeCheckBox;
	@ViewById
	protected TextView filterText, serverText, errorText;
	@ViewById
	protected SherlockListView torrentsList;
	@Bean
	protected TorrentsAdapter previewTorrentsAdapter;
	private List<Torrent> previewTorrents = null;

	// Settings and helpers
	@SystemService
	protected AppWidgetManager appWidgetManager;
	@SystemService
	protected LayoutInflater layoutInflater;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected ApplicationSettings applicationSettings;
	private int appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
			getSupportActionBar().setIcon(R.drawable.ic_activity_torrents);
		}
		super.onCreate(savedInstanceState);

		if (getIntent() != null && getIntent().getExtras() != null) {
			// Get the appwidget ID we are configuring
			appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			// Set preliminary canceled result and continue with the initialisation
			setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
		}

		// Invalid configuration; return canceled result
		setResult(RESULT_CANCELED,
				new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		finish();

	}

	@AfterViews
	protected void init() {

		// Populate the selection spinners
		List<SimpleListItem> sortOrders = new ArrayList<SimpleListItem>();
		for (TorrentsSortBy order : TorrentsSortBy.values()) {
			sortOrders.add(new SortByListItem(order));
		}
		serverSpinner.setAdapter(new FilterListItemAdapter(this, applicationSettings.getServerSettings()));
		filterSpinner.setAdapter(new FilterListItemAdapter(this, StatusType.getAllStatusTypes(this)));
		sortSpinner.setAdapter(new FilterListItemAdapter(this, sortOrders));
		// TODO: Update to AndroidAnnotations 3.0 and use @CheckedChanged
		reverseorderCheckBox.setOnCheckedChangeListener(reverseorderCheckedChanged);
		torrentsList.setAdapter(previewTorrentsAdapter);

		// Set up action bar with a done button
		// Inspired by NoNonsenseNotes's ListWidgetConfig.java (Apache License, Version 2.0)
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		View doneButtonFrame = layoutInflater.inflate(R.layout.actionbar_donebutton, null);
		doneButtonFrame.findViewById(R.id.actionbar_done).setOnClickListener(doneClicked);
		getSupportActionBar().setCustomView(doneButtonFrame);

	}

	@ItemSelect
	protected void serverSpinnerItemSelected(boolean selected, ServerSetting server) {
		serverText.setText(server.getName());
		loadTorrents();
	}

	@ItemSelect
	protected void filterSpinnerItemSelected(boolean selected, StatusTypeFilter statusTypeFilter) {
		filterText.setText(statusTypeFilter.getName());
		filterTorrents();
	}

	@ItemSelect
	protected void sortSpinnerItemSelected(boolean selected, SortByListItem sortByListItem) {
		filterTorrents();
	}

	protected OnCheckedChangeListener reverseorderCheckedChanged = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			filterTorrents();
		}
	};

	@Background
	protected void loadTorrents() {

		if (serverSpinner.getSelectedItem() == null)
			return;

		// Create a connection object and retrieve the live torrents
		IDaemonAdapter connection = ((ServerSetting) serverSpinner.getSelectedItem())
				.createServerAdapter(connectivityHelper.getConnectedNetworkName());
		DaemonTaskResult result = RetrieveTask.create(connection).execute();
		if (result instanceof RetrieveTaskSuccessResult) {
			// Success; show the active torrents in the widget preview
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(),
					((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			// Can't connect right now; provide a nice error message
			showError(false);
		}

	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<Label> labels) {
		previewTorrents = torrents;
		filterTorrents();
	}

	protected void filterTorrents() {

		// All spinners have to be initialised already
		if (serverSpinner.getSelectedItem() == null)
			return;
		if (filterSpinner.getSelectedItem() == null)
			return;
		if (sortSpinner.getSelectedItem() == null)
			return;
		if (previewTorrents == null)
			return;

		// Get the already loaded torrents and filter and sort them
		ArrayList<Torrent> filteredTorrents = new ArrayList<Torrent>(previewTorrents.size());
		StatusTypeFilter statusTypeFilter = (StatusTypeFilter) filterSpinner.getSelectedItem();
		for (Torrent torrent : previewTorrents) {
			if (statusTypeFilter.matches(torrent))
				filteredTorrents.add(torrent);
		}
		if (filteredTorrents.size() == 0) {
			showError(true);
			return;
		}
		TorrentsSortBy sortBy = ((SortByListItem) sortSpinner.getSelectedItem()).getSortBy();
		Daemon serverType = filteredTorrents.get(0).getDaemon();
		Collections
				.sort(filteredTorrents, new TorrentsComparator(serverType, sortBy, reverseorderCheckBox.isChecked()));

		// Finally update the widget preview with the live, filtered and sorted torrents list
		previewTorrentsAdapter.update(filteredTorrents);
		torrentsList.setVisibility(View.VISIBLE);
		errorText.setVisibility(View.GONE);
	}

	@UiThread
	protected void showError(boolean emptyResults) {
		torrentsList.setVisibility(View.GONE);
		errorText.setVisibility(View.VISIBLE);
		errorText.setText(emptyResults ? R.string.navigation_emptytorrents : R.string.error_httperror);
	}

	private OnClickListener doneClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {

			// All spinner have to be initialised already
			if (serverSpinner.getSelectedItem() == null)
				return;
			if (filterSpinner.getSelectedItem() == null)
				return;
			if (sortSpinner.getSelectedItem() == null)
				return;

			// Store these user preferences
			int server = ((ServerSetting) serverSpinner.getSelectedItem()).getOrder();
			StatusType statusType = ((StatusTypeFilter) filterSpinner.getSelectedItem()).getStatusType();
			TorrentsSortBy sortBy = ((SortByListItem) sortSpinner.getSelectedItem()).getSortBy();
			boolean reverseSort = reverseorderCheckBox.isChecked();
			boolean useDarkTheme = darkthemeCheckBox.isChecked();
			applicationSettings.setWidgetConfig(appWidgetId, new WidgetSettings(server, statusType, sortBy,
					reverseSort, useDarkTheme));

			// Return the widget configuration result
			appWidgetManager.updateAppWidget(appWidgetId, null);
			setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
			finish();

		}
	};
}
