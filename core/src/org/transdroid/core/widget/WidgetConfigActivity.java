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
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.lists.SimpleListItemSpinnerAdapter;
import org.transdroid.core.gui.lists.SortByListItem;
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

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
	protected ListView torrentsList;
	private List<Torrent> previewTorrents = null;

	// Settings and helpers
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected ApplicationSettings applicationSettings;
	private int appWidgetId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (getIntent() == null || getIntent().getExtras() == null
				|| !getIntent().hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
			// Invalid configuration; return canceled result
			setResult(RESULT_CANCELED,
					new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
			finish();
		}

		// Get the appwidget ID we are configuring
		appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		// Set preliminary canceled result and continue with the initialisation
		setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));

	}

	@AfterViews
	protected void init() {

		// Populate the selection spinners with custom array adapters
		List<SortByListItem> sortOrders = new ArrayList<SortByListItem>();
		for (TorrentsSortBy order : TorrentsSortBy.values()) {
			sortOrders.add(new SortByListItem(this, order));
		}
		serverSpinner.setAdapter(new SimpleListItemSpinnerAdapter<ServerSetting>(this, 0, applicationSettings
				.getServerSettings()));
		filterSpinner.setAdapter(new SimpleListItemSpinnerAdapter<StatusTypeFilter>(this, 0, StatusType
				.getAllStatusTypes(this)));
		sortSpinner.setAdapter(new SimpleListItemSpinnerAdapter<SortByListItem>(this, 0, sortOrders));
		// TODO: Update to AndroidAnnotations 3.0 and use @CheckedChanged
		reverseorderCheckBox.setOnCheckedChangeListener(reverseorderCheckedChanged);
		torrentsList.setEmptyView(errorText);

		// Set up action bar with a done button
		// Inspired by NoNonsenseNotes's ListWidgetConfig.java (Apache License, Version 2.0)
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		View doneButtonFrame = getLayoutInflater().inflate(R.layout.actionbar_donebutton, null);
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
		torrentsList.setAdapter(new WidgetPreviewAdapter(this, 0, filteredTorrents));
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
			WidgetConfig config = new WidgetConfig(server, statusType, sortBy, reverseSort, useDarkTheme);
			applicationSettings.setWidgetConfig(appWidgetId, config);

			// Return the widget configuration result
			AppWidgetManager manager = AppWidgetManager.getInstance(WidgetConfigActivity.this);
			manager.updateAppWidget(appWidgetId,
					WidgetProvider.buildRemoteViews(getApplicationContext(), appWidgetId, config));
			manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.torrents_list);
			setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
			finish();

		}
	};

}
