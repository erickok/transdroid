package org.transdroid.core.gui;

import java.util.ArrayList;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.TorrentsAdapter;
import org.transdroid.daemon.Torrent;

import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

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
	
	@AfterViews
	protected void init() {
		torrentsList.setAdapter(new TorrentsAdapter());
		torrentsList.setEmptyView(emptyText);
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
			((TorrentsAdapter)torrentsList.getAdapter()).update(newTorrents);
			// NOTE: This will also make visible again the list or empty view
		}
	}

	/**
	 * Clear currently visible list of torrents
	 */
	public void clear() {
		updateTorrents(null);
	}

	@ItemClick(R.id.torrent_list)
	protected void torrentsListClicked(Torrent torrent) {
		
	}
	
}
