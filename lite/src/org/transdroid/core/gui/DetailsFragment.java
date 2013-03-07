package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.DetailsAdapter;
import org.transdroid.core.gui.lists.SimpleListItemAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;

import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

/**
 * Fragment that shown detailed statistics about some torrent. These come from some already fetched {@link Torrent}
 * object, but it also retrieves further detailed statistics.
 * @author Eric Kok
 */
@EFragment(R.layout.fragment_details)
public class DetailsFragment extends SherlockFragment {

	// Local data
	@InstanceState
	protected Torrent torrent = null;
	@InstanceState
	protected TorrentDetails torrentDetails = null;
	@InstanceState
	protected ArrayList<TorrentFile> torrentFiles = null;

	// Views
	@ViewById(R.id.details_list)
	protected SherlockListView detailsList;
	@ViewById
	protected TextView emptyText;

	@AfterViews
	protected void init() {

		detailsList.setAdapter(new DetailsAdapter(getActivity()));
		detailsList.setEmptyView(emptyText);
		if (torrent != null)
			updateTorrent(torrent);
		if (torrentDetails != null)
			updateTorrentDetails(torrentDetails);
		if (torrentFiles != null)
			updateTorrentFiles(torrentFiles);

	}

	/**
	 * Updates the details adapter header to show the new torrent data
	 * @param newTorrent The new torrent object
	 */
	public void updateTorrent(Torrent newTorrent) {
		this.torrent = newTorrent;
		((DetailsAdapter) detailsList.getAdapter()).updateTorrent(newTorrent);
	}

	/**
	 * Updates the details adapter to show the list of trackers and tracker errors
	 * @param newTorrentDetails The new fine details object of some torrent
	 */
	public void updateTorrentDetails(TorrentDetails newTorrentDetails) {
		this.torrentDetails = newTorrentDetails;
		((DetailsAdapter) detailsList.getAdapter()).updateTrackers(SimpleListItemAdapter.SimpleStringItem
				.wrapStringsList(newTorrentDetails.getTrackers()));
		((DetailsAdapter) detailsList.getAdapter()).updateErrors(SimpleListItemAdapter.SimpleStringItem
				.wrapStringsList(newTorrentDetails.getErrors()));
	}

	/**
	 * Updates the list adapter to show a new list of torrent files, replacing the old files list
	 * @param newTorrents The new, updated list of torrent file objects
	 */
	public void updateTorrentFiles(ArrayList<TorrentFile> newTorrentFiles) {
		this.torrentFiles = newTorrentFiles;
		((DetailsAdapter) detailsList.getAdapter()).updateTorrentFiles(newTorrentFiles);
	}

	/**
	 * Can be called if some outside activity returned new torrents, so we can perhaps piggyback on this by update our 
	 * data as well
	 * @param torrents The last of retrieved torrents
	 */
	public void perhapsUpdateTorrent(List<Torrent> torrents) {
		for (Torrent newTorrent : torrents) {
			if (newTorrent.getUniqueID().equals(this.torrent.getUniqueID())) {
				// Found, so we can update our data as well
				updateTorrent(newTorrent);
				break;
			}
		}
	}

	/**
	 * Clear the screen by fully clearing the internal merge list (with header and other lists)
	 */
	public void clear() {
		((DetailsAdapter)detailsList.getAdapter()).clear();
		torrent = null;
		torrentDetails = null;
		torrentFiles = null;
	}
	
}
