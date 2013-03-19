package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.*;
import org.transdroid.core.gui.navigation.NavigationFilter;
import org.transdroid.daemon.Torrent;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

@EFragment(resName="fragment_torrents")
public class TorrentsFragment extends SherlockFragment {

	// Local data
	@InstanceState
	protected ArrayList<Torrent> torrents = null;
	@InstanceState
	protected NavigationFilter currentFilter = null;
	@InstanceState
	protected boolean hasAConnection = false;
	@InstanceState
	protected boolean isLoading = false;

	// Views
	@ViewById(resName="torrent_list")
	protected SherlockListView torrentsList;
	@ViewById
	protected TextView emptyText;
	@ViewById
	protected TextView nosettingsText;
	@ViewById
	protected ProgressBar loadingProgress;

	@AfterViews
	protected void init() {
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
	 * Clear currently visible list of torrents
	 */
	public void clear() {
		updateTorrents(null);
	}

	/**
	 * Apply a filter on the current list of all torrents, showing the appropriate sublist of torrents only
	 * @param currentFilter
	 */
	public void applyFilter(NavigationFilter currentFilter) {
		this.currentFilter = currentFilter;
		if (torrents != null) {
			// Build a local list of torrents that match the selected navigation filter
			ArrayList<Torrent> filteredTorrents = new ArrayList<Torrent>();
			for (Torrent torrent : torrents) {
				if (currentFilter.matches(torrent))
					filteredTorrents.add(torrent);
			}
			((TorrentsAdapter) torrentsList.getAdapter()).update(filteredTorrents);
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

	@ItemClick(resName="torrent_list")
	protected void torrentsListClicked(Torrent torrent) {
		DetailsActivity_.intent(getActivity()).torrent(torrent).start();
	}

	/**
	 * Updates the shown screen depending on whether we have a connection (so torrents can be shown) or not (in case we
	 * need to show a message suggesting help)
	 * @param hasAConnection True if the user has servers configured and therefore has a connection that can be used
	 */
	public void updateConnectionStatus(boolean hasAConnection) {
		this.hasAConnection = hasAConnection;
		if (!hasAConnection)
			clear();
		updateViewVisibility();
	}
	
	private void updateViewVisibility() {
		if (!hasAConnection) {
			torrentsList.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			loadingProgress.setVisibility(View.GONE);
			nosettingsText.setVisibility(View.VISIBLE);
			return;
		}
		boolean isEmpty = torrents == null || torrentsList.getAdapter().isEmpty();
		nosettingsText.setVisibility(View.GONE);
		torrentsList.setVisibility(!isLoading && !isEmpty? View.GONE: View.VISIBLE);
		loadingProgress.setVisibility(isLoading? View.VISIBLE: View.GONE);
		emptyText.setVisibility(!isLoading && isEmpty? View.VISIBLE: View.GONE);
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
