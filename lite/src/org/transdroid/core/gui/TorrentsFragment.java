package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.TorrentsAdapter;
import org.transdroid.core.gui.lists.TorrentsAdapter_;
import org.transdroid.daemon.Torrent;

import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

@EFragment(R.layout.fragment_torrents)
public class TorrentsFragment extends SherlockFragment {

	// Local data
	@InstanceState
	protected ArrayList<Torrent> torrents = null;

	// Views
	@ViewById(R.id.torrent_list)
	protected SherlockListView torrentsList;
	@ViewById
	protected TextView emptyText;
	@ViewById
	protected TextView nosettingsText;

	@AfterViews
	protected void init() {
		torrentsList.setAdapter(TorrentsAdapter_.getInstance_(getActivity()));
		torrentsList.setEmptyView(emptyText);
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
		if (newTorrents == null) {
			// Hide list adapter as well as empty text
			torrentsList.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
		} else {
			((TorrentsAdapter) torrentsList.getAdapter()).update(newTorrents);
			// NOTE: This will also make visible again the list or empty view
		}
	}

	/**
	 * Clear currently visible list of torrents
	 */
	public void clear() {
		updateTorrents(null);
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
			
			// Execute the requested action
			// TODO: Add the other actions
			switch (item.getItemId()) {
			case R.id.action_start:
				startTorrents(checked);
				mode.finish();
				return true;
			default:
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

	@ItemClick(R.id.torrent_list)
	protected void torrentsListClicked(Torrent torrent) {
		DetailsActivity_.intent(getActivity()).torrent(torrent).start();
	}

	/**
	 * Updates the shown screen depending on whether we have a connection (so torrents can be shown) or not (in case we
	 * need to show a message suggesting help)
	 * @param hasAConnection True if the user has servers configured and therefor has a conenction that can be used
	 */
	public void updateConnectionStatus(boolean hasAConnection) {
		if (!hasAConnection) {
			clear();
			torrentsList.setVisibility(View.GONE);
			emptyText.setVisibility(View.GONE);
			nosettingsText.setVisibility(View.VISIBLE);
		} else {
			nosettingsText.setVisibility(View.GONE);
			torrentsList.setVisibility(torrentsList.getAdapter().isEmpty()? View.GONE: View.VISIBLE);
			emptyText.setVisibility(torrentsList.getAdapter().isEmpty()? View.VISIBLE: View.GONE);
		}
	}

	@Background
	protected void startTorrents(List<Torrent> torrents) {
		// TODO: Implement action
	}

}
