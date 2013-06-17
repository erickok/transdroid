package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

/**
 * Fragment that shows a list of torrents that are active on the server. It supports sorting and filtering and can show
 * connection progress and issues. However, actual task starting and execution and overall navigation elements are part
 * of the containing activity, not this fragment.
 * @author Eric Kok
 */
@EFragment(resName = "fragment_torrents")
public class TorrentsFragment extends SherlockFragment {

	// Local data
	@Bean
	protected ApplicationSettings applicationSettings;
	@InstanceState
	protected ArrayList<Torrent> torrents = null;
	@InstanceState
	protected NavigationFilter currentNavigationFilter = null;
	@InstanceState
	protected TorrentsSortBy currentSortOrder = TorrentsSortBy.Alphanumeric;
	@InstanceState
	protected boolean currentSortDescending = false;
	@InstanceState
	protected String currentTextFilter = null;
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

		// Load the requested sort order from the user settings
		this.currentSortOrder = applicationSettings.getLastUsedSortOrder();
		this.currentSortDescending = applicationSettings.getLastUsedSortDescending();

		// Set up the list adapter, which allows multi-select and fast scrolling
		torrentsList.setAdapter(TorrentsAdapter_.getInstance_(getActivity()));
		torrentsList.setMultiChoiceModeListener(onTorrentsSelected);
		torrentsList.setFastScrollEnabled(true);
		if (torrents != null)
			updateTorrents(torrents);

	}

	/**
	 * Updates the list adapter to show a new list of torrent objects, replacing the old torrents completely
	 * @param newTorrents The new, updated list of torrents
	 */
	public void updateTorrents(ArrayList<Torrent> newTorrents) {
		torrents = newTorrents;
		applyNavigationFilter(null); // Resets the filter and shown list of torrents
	}

	/**
	 * Clears the currently visible list of torrents.
	 * @param b
	 */
	public void clear(boolean clearError) {
		this.torrents = null;
		if (clearError)
			this.connectionErrorMessage = null;
		this.currentTextFilter = null;
		this.currentNavigationFilter = null;
		applyAllFilters();
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
		applyAllFilters();
	}

	public void applyTextFilter(String newTextFilter) {
		this.currentTextFilter = newTextFilter;
		// Show the new filtered list
		applyAllFilters();
	}

	/**
	 * Apply a filter on the current list of all torrents, showing the appropriate sublist of torrents only
	 * @param newFilter The new filter to apply to the local list of torrents
	 */
	public void applyNavigationFilter(NavigationFilter newFilter) {
		this.currentNavigationFilter = newFilter;
		applyAllFilters();
	}

	private void applyAllFilters() {

		// No torrents? Directly update views accordingly
		if (torrents == null) {
			updateViewVisibility();
			return;
		}

		// Get the server daemon type directly form the local list of torrents, if it's not empty
		Daemon serverType = (this.torrents.size() > 0 ? this.torrents.get(0).getDaemon() : Daemon.Transmission);

		// Filter the list of torrents to show according to navigation and text filters
		ArrayList<Torrent> filteredTorrents = new ArrayList<Torrent>(torrents);
		if (filteredTorrents != null && currentNavigationFilter != null) {
			// Remove torrents that do not match the selected navigation filter
			for (Iterator<Torrent> torrentIter = filteredTorrents.iterator(); torrentIter.hasNext();) {
				if (!currentNavigationFilter.matches(torrentIter.next()))
					torrentIter.remove();
			}
		}
		if (filteredTorrents != null && currentTextFilter != null) {
			// Remove torrent that do not contain the text filter string
			for (Iterator<Torrent> torrentIter = filteredTorrents.iterator(); torrentIter.hasNext();) {
				if (!torrentIter.next().getName().toLowerCase(Locale.getDefault())
						.contains(currentTextFilter.toLowerCase(Locale.getDefault())))
					torrentIter.remove();
			}
		}

		// Sort the list of filtered torrents
		Collections.sort(filteredTorrents, new TorrentsComparator(serverType, this.currentSortOrder,
				this.currentSortDescending));

		((TorrentsAdapter) torrentsList.getAdapter()).update(filteredTorrents);
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
				if (torrentsList.getCheckedItemPositions().valueAt(i))
					checked.add((Torrent) torrentsList.getAdapter().getItem(
							torrentsList.getCheckedItemPositions().keyAt(i)));
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
			// Show the number of selected torrents in the CAB
			// torrentsList.getCheckedItemPositions().size() ?
			int checkedCount = 0;
			for (int i = 0; i < torrentsList.getCheckedItemPositions().size(); i++) {
				if (torrentsList.getCheckedItemPositions().valueAt(i))
					checkedCount++;
			}
			mode.setTitle(getResources().getQuantityString(R.plurals.navigation_torrentsselected, checkedCount,
					checkedCount));
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
		((TorrentsActivity) getActivity()).openDetails(torrent);
	}

	/**
	 * Updates the shown screen depending on whether we have a connection (so torrents can be shown) or not (in case we
	 * need to show a message suggesting help). This should only ever be called on the UI thread.
	 * @param hasAConnection True if the user has servers configured and therefore has a connection that can be used
	 */
	public void updateConnectionStatus(boolean hasAConnection) {
		this.hasAConnection = hasAConnection;
		if (!hasAConnection) {
			clear(true); // Indirectly also calls updateViewVisibility()
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
			clear(true); // Indirectly also calls updateViewVisibility()
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
			clear(false); // Indirectly also calls updateViewVisibility()
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
