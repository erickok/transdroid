package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.gui.lists.TorrentsAdapter;
import org.transdroid.core.gui.lists.TorrentsAdapter_;
import org.transdroid.core.gui.navigation.NavigationFilter;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentsComparator;
import org.transdroid.daemon.TorrentsSortBy;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

@EFragment(resName = "fragment_torrents")
public class TorrentsFragment extends SherlockFragment {

	// Local data
	@Bean
	protected ApplicationSettings applicationSettings;
	@InstanceState
	protected ArrayList<Torrent> torrents = null;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected TorrentsSortBy currentSortOrder = TorrentsSortBy.Alphanumeric;
	@InstanceState
	protected boolean currentSortDescending = false;
	@InstanceState
	protected boolean hasAConnection = false;
	@InstanceState
	protected boolean isLoading = true;
	@InstanceState
	protected String connectionErrorMessage = null;

	// Views
	@ViewById(resName = "torrent_list")
	protected SherlockListView torrentsList;
	@ViewById
	protected TextView emptyText;
	@ViewById
	protected TextView nosettingsText;
	@ViewById
	protected TextView errorText;
	@ViewById
	protected ProgressBar loadingProgress;

	@AfterViews
	protected void init() {
		this.currentSortOrder = applicationSettings.getLastUsedSortOrder();
		this.currentSortDescending = applicationSettings.getLastUsedSortDescending();
		torrentsList.setAdapter(TorrentsAdapter_.getInstance_(getActivity()));
		torrentsList.setMultiChoiceModeListener(onTorrentsSelected);
		if (torrents != null)
			updateTorrents(torrents);
	}

	/**
	 * Updates the list adapter to show a new list of torrent objects, replacing the old torrents completely
	 * @param newTorrents The new, updated list of torrents
	 */
	public void updateTorrents(ArrayList<Torrent> newTorrents) {
		torrents = newTorrents;
		applyFilter(null); // Resets the filter and shown list of torrents
	}

	/**
	 * Clears the currently visible list of torrents.
	 */
	public void clear() {
		this.connectionErrorMessage = null;
		updateTorrents(null);
	}

	/**
	 * Stores the new sort order (for future refreshes) and sorts the current visible list. If the given new sort
	 * property equals the existing property, the list sort order is reversed instead.
	 * @param newSortOrder The sort order that the user selected.
	 */
	public void sortBy(TorrentsSortBy newSortOrder) {
		// Update the sort order property and direction and store this last used setting
		if (this.currentSortOrder == newSortOrder) {
			this.currentSortDescending = !this.currentSortDescending;
		} else {
			this.currentSortOrder = newSortOrder;
			this.currentSortDescending = false;
		}
		applicationSettings.setLastUsedSortOrder(this.currentSortOrder, this.currentSortDescending);
		// Get the server daemon type directly form the local list of torrents, if it's not empty
		Daemon serverType = (this.torrents != null && this.torrents.size() > 0 ? this.torrents.get(0).getDaemon()
				: Daemon.Transmission);
		Collections.sort(this.torrents, new TorrentsComparator(serverType, this.currentSortOrder,
				this.currentSortDescending));
		// Show the new resorted list
		applyFilter(this.currentFilter);
	}
	
	/**
	 * Apply a filter on the current list of all torrents, showing the appropriate sublist of torrents only
	 * @param newFilter The new filter to apply to the local list of torrents
	 */
	public void applyFilter(NavigationFilter newFilter) {
		this.currentFilter = newFilter;
		if (torrents != null && newFilter != null) {
			// Build a local list of torrents that match the selected navigation filter
			ArrayList<Torrent> filteredTorrents = new ArrayList<Torrent>();
			for (Torrent torrent : torrents) {
				if (newFilter.matches(torrent))
					filteredTorrents.add(torrent);
			}
			((TorrentsAdapter) torrentsList.getAdapter()).update(filteredTorrents);
		} else if (torrents != null) {
			// No need to filter any torrents; directly show the full list
			((TorrentsAdapter) torrentsList.getAdapter()).update(torrents);
		}
		updateViewVisibility();
	}

	private MultiChoiceModeListenerCompat onTorrentsSelected = new MultiChoiceModeListenerCompat() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Show contextual action bar to start/stop/remove/etc. torrents in batch mode
			mode.getMenuInflater().inflate(R.menu.fragment_torrents_cab, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			// Get checked torrents
			List<Torrent> checked = new ArrayList<Torrent>();
			for (int i = 0; i < torrentsList.getCheckedItemPositions().size(); i++) {
				if (torrentsList.getCheckedItemPositions().get(i))
					checked.add((Torrent) torrentsList.getAdapter().getItem(i));
			}

			int itemId = item.getItemId();
			if (itemId == R.id.action_resume) {
				for (Torrent torrent : checked) {
					getTasksExecutor().resumeTorrent(torrent);
				}
				mode.finish();
				return true;
			} else if (itemId == R.id.action_pause) {
				for (Torrent torrent : checked) {
					getTasksExecutor().pauseTorrent(torrent);
				}
				mode.finish();
				return true;
			} else if (itemId == R.id.action_remove_default) {
				for (Torrent torrent : checked) {
					getTasksExecutor().removeTorrent(torrent, false);
				}
				mode.finish();
				return true;
			} else if (itemId == R.id.action_remove_withdata) {
				for (Torrent torrent : checked) {
					getTasksExecutor().removeTorrent(torrent, true);
				}
				mode.finish();
				return true;
			} else if (itemId == R.id.action_setlabel) {
				// TODO: Open label selection dialogue
				mode.finish();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			// TODO: Update title or otherwise show number of selected torrents?
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

	};

	@ItemClick(resName = "torrent_list")
	protected void torrentsListClicked(Torrent torrent) {
		((TorrentsActivity)getActivity()).openDetails(torrent);
	}

	/**
	 * Updates the shown screen depending on whether we have a connection (so torrents can be shown) or not (in case we
	 * need to show a message suggesting help). This should only ever be called on the UI thread.
	 * @param hasAConnection True if the user has servers configured and therefore has a connection that can be used
	 */
	public void updateConnectionStatus(boolean hasAConnection) {
		this.hasAConnection = hasAConnection;
		if (!hasAConnection) {
			clear(); // Indirectly also calls updateViewVisibility()
		} else {
			updateViewVisibility();
		}
	}

	/**
	 * Updates the shown screen depending on whether the torrents are loading. This should only ever be called on the UI
	 * thread.
	 * @param isLoading True if the list of torrents is (re)loading, false otherwise
	 */
	public void updateIsLoading(boolean isLoading) {
		this.isLoading = isLoading;
		if (isLoading) {
			clear(); // Indirectly also calls updateViewVisibility()
		} else {
			updateViewVisibility();
		}
	}

	/**
	 * Updates the shown screen depending on whether a connection error occurred. This should only ever be called on the
	 * UI thread.
	 * @param connectionErrorMessage The error message from the last failed connection attempt, or null to clear the
	 *            visible error text
	 */
	public void updateError(String connectionErrorMessage) {
		this.connectionErrorMessage = connectionErrorMessage;
		errorText.setText(connectionErrorMessage);
		if (connectionErrorMessage != null) {
			clear(); // Indirectly also calls updateViewVisibility()
		} else {
			updateViewVisibility();
		}
	}

	private void updateViewVisibility() {
		if (!hasAConnection) {
			torrentsList.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			loadingProgress.setVisibility(View.GONE);
			errorText.setVisibility(View.GONE);
			nosettingsText.setVisibility(View.VISIBLE);
			return;
		}
		boolean isEmpty = torrents == null || torrentsList.getAdapter().isEmpty();
		boolean hasError = connectionErrorMessage != null;
		nosettingsText.setVisibility(View.GONE);
		errorText.setVisibility(hasError ? View.VISIBLE : View.GONE);
		torrentsList.setVisibility(!hasError && !isLoading && !isEmpty ? View.VISIBLE : View.GONE);
		loadingProgress.setVisibility(!hasError && isLoading ? View.VISIBLE : View.GONE);
		emptyText.setVisibility(!hasError && !isLoading && isEmpty ? View.VISIBLE : View.GONE);
	}

	/**
	 * Returns the object responsible for executing torrent tasks against a connected server
	 * @return The executor for tasks on some torrent
	 */
	private TorrentTasksExecutor getTasksExecutor() {
		// NOTE: Assumes the activity implements all the required torrent tasks
		return (TorrentTasksExecutor) getActivity();
	}

}
