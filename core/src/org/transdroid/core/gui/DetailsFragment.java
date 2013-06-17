package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.DetailsAdapter;
import org.transdroid.core.gui.lists.SimpleListItemAdapter;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Fragment that shows detailed statistics about some torrent. These come from some already fetched {@link Torrent}
 * object, but it also retrieves further detailed statistics.
 * @author Eric Kok
 */
@EFragment(resName="fragment_details")
@OptionsMenu(resName="fragment_details")
public class DetailsFragment extends SherlockFragment {

	// Local data
	@InstanceState
	@FragmentArg
	protected Torrent torrent = null;
	@InstanceState
	protected TorrentDetails torrentDetails = null;
	@InstanceState
	protected ArrayList<TorrentFile> torrentFiles = null;
	@InstanceState
	protected boolean isLoadingTorrent = false;

	// Views
	@ViewById(resName="details_list")
	protected SherlockListView detailsList;
	@ViewById
	protected TextView emptyText;
	@ViewById
	protected ProgressBar loadingProgress;

	@AfterViews
	protected void init() {

		// Set up details adapter (itself containing the actual lists to show), which allows multi-select and fast
		// scrolling
		detailsList.setAdapter(new DetailsAdapter(getActivity()));
		detailsList.setMultiChoiceModeListener(onDetailsSelected);
		detailsList.setFastScrollEnabled(true);
		if (torrent != null)
			updateTorrent(torrent);
		if (torrentDetails != null)
			updateTorrentDetails(torrent, torrentDetails);
		if (torrentFiles != null)
			updateTorrentFiles(torrent, torrentFiles);

	}

	/**
	 * Updates the details adapter header to show the new torrent data.
	 * @param newTorrent The new torrent object
	 */
	public void updateTorrent(Torrent newTorrent) {
		clear();
		this.torrent = newTorrent;
		((DetailsAdapter) detailsList.getAdapter()).updateTorrent(newTorrent);
		// Make the list (with details header) visible
		detailsList.setVisibility(View.VISIBLE);
		emptyText.setVisibility(View.GONE);
		loadingProgress.setVisibility(View.GONE);
		// Also update the available actions in the action bar
		getActivity().supportInvalidateOptionsMenu();
		// Refresh the detailed statistics (errors) and list of files
		getTasksExecutor().refreshTorrentDetails(torrent);
		getTasksExecutor().refreshTorrentFiles(torrent);
	}

	/**
	 * Updates the details adapter to show the list of trackers and tracker errors.
	 * @param checkTorrent The torrent for which the details were retrieved
	 * @param newTorrentDetails The new fine details object of some torrent
	 */
	public void updateTorrentDetails(Torrent checkTorrent, TorrentDetails newTorrentDetails) {
		// Check if these are actually the details of the torrent we are now showing
		if (!torrent.getUniqueID().equals(checkTorrent.getUniqueID()))
			return;
		this.torrentDetails = newTorrentDetails;
		((DetailsAdapter) detailsList.getAdapter()).updateTrackers(SimpleListItemAdapter.SimpleStringItem
				.wrapStringsList(newTorrentDetails.getTrackers()));
		((DetailsAdapter) detailsList.getAdapter()).updateErrors(SimpleListItemAdapter.SimpleStringItem
				.wrapStringsList(newTorrentDetails.getErrors()));
	}

	/**
	 * Updates the list adapter to show a new list of torrent files, replacing the old files list.
	 * @param checkTorrent The torrent for which the details were retrieved
	 * @param newTorrents The new, updated list of torrent file objects
	 */
	public void updateTorrentFiles(Torrent checkTorrent, ArrayList<TorrentFile> newTorrentFiles) {
		// Check if these are actually the details of the torrent we are now showing
		if (!torrent.getUniqueID().equals(checkTorrent.getUniqueID()))
			return;
		this.torrentFiles = newTorrentFiles;
		((DetailsAdapter) detailsList.getAdapter()).updateTorrentFiles(newTorrentFiles);
	}

	/**
	 * Can be called if some outside activity returned new torrents, so we can perhaps piggyback on this by update our 
	 * data as well.
	 * @param torrents The last of retrieved torrents
	 */
	public void perhapsUpdateTorrent(List<Torrent> torrents) {
		// Only try to update if we actually were showing a torrent
		if (this.torrent == null || torrents == null)
			return;
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
		detailsList.setAdapter(new DetailsAdapter(getActivity()));
		detailsList.setVisibility(View.GONE);
		emptyText.setVisibility(!isLoadingTorrent? View.VISIBLE: View.GONE);
		loadingProgress.setVisibility(isLoadingTorrent? View.VISIBLE: View.GONE);
		// Note: this.torrent is not cleared as we need to know later what the fragment was originally bound to
		torrentDetails = null;
		torrentFiles = null;
	}

	/**
	 * Updates the shown screen depending on whether the torrent is loading
	 * @param isLoading True if the torrent is (re)loading, false otherwise
	 */
	public void updateIsLoading(boolean isLoading) {
		this.isLoadingTorrent = isLoading;
		if (isLoadingTorrent)
			clear();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if (torrent == null) {
			menu.findItem(R.id.action_resume).setVisible(false);
			menu.findItem(R.id.action_pause).setVisible(false);
			menu.findItem(R.id.action_start).setVisible(false);
			menu.findItem(R.id.action_stop).setVisible(false);
			menu.findItem(R.id.action_remove).setVisible(false);
			menu.findItem(R.id.action_remove_withdata).setVisible(false);
			menu.findItem(R.id.action_setlabel).setVisible(false);
			menu.findItem(R.id.action_updatetrackers).setVisible(false);
			return;
		}
		// Update action availability
		boolean startStop = Daemon.supportsStoppingStarting(torrent.getDaemon());
		menu.findItem(R.id.action_resume).setVisible(torrent.canResume());
		menu.findItem(R.id.action_pause).setVisible(torrent.canPause());
		menu.findItem(R.id.action_start).setVisible(startStop && torrent.canStart());
		menu.findItem(R.id.action_stop).setVisible(startStop && torrent.canStop());
		menu.findItem(R.id.action_remove).setVisible(true);
		boolean removeWithData = Daemon.supportsRemoveWithData(torrent.getDaemon());
		menu.findItem(R.id.action_remove_withdata).setVisible(removeWithData);
		boolean setLabel = Daemon.supportsSetLabel(torrent.getDaemon());
		menu.findItem(R.id.action_setlabel).setVisible(setLabel);
		boolean setTrackers = Daemon.supportsSetTrackers(torrent.getDaemon());
		menu.findItem(R.id.action_updatetrackers).setVisible(setTrackers);
		
	}

	@OptionsItem(resName="action_resume")
	protected void resumeTorrent() {
		getTasksExecutor().resumeTorrent(torrent);
	}

	@OptionsItem(resName="action_pause")
	protected void pauseTorrent() {
		getTasksExecutor().pauseTorrent(torrent);
	}

	@OptionsItem(resName="action_start_default")
	protected void startTorrentDefault() {
		getTasksExecutor().startTorrent(torrent, false);
	}

	@OptionsItem(resName="action_start_forced")
	protected void startTorrentForced() {
		getTasksExecutor().startTorrent(torrent, true);
	}

	@OptionsItem(resName="action_stop")
	protected void stopTorrent() {
		getTasksExecutor().stopTorrent(torrent);
	}

	@OptionsItem(resName="action_remove_default")
	protected void removeTorrentDefault() {
		getTasksExecutor().removeTorrent(torrent, false);
	}

	@OptionsItem(resName="action_remove_withdata")
	protected void removeTorrentWithData() {
		getTasksExecutor().removeTorrent(torrent, true);
	}

	@OptionsItem(resName="action_setlabel")
	protected void setLabel() {
		// TODO: Show label selection dialog
	}

	@OptionsItem(resName="action_updatetrackers")
	protected void updateTrackers() {
		// TODO: Show trackers edit dialog
	}

	private MultiChoiceModeListenerCompat onDetailsSelected = new MultiChoiceModeListenerCompat() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Show contextual action bar to start/stop/remove/etc. torrents in batch mode
			mode.getMenuInflater().inflate(R.menu.fragment_details_file, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			// Get checked torrents
			List<TorrentFile> checked = new ArrayList<TorrentFile>();
			for (int i = 0; i < detailsList.getCheckedItemPositions().size(); i++) {
				if (detailsList.getCheckedItemPositions().valueAt(i)
						&& detailsList.getAdapter().getItem(detailsList.getCheckedItemPositions().keyAt(i)) instanceof TorrentFile)
					checked.add((TorrentFile) detailsList.getAdapter().getItem(
							detailsList.getCheckedItemPositions().keyAt(i)));
			}

			int itemId = item.getItemId();
			if (itemId == R.id.action_download) {
				// TODO: Start FTP download command for the selected torrents
				Crouton.showText(getActivity(), "TODO: Start FTP download command for the selected torrents",
						NavigationHelper.CROUTON_INFO_STYLE);
				//for (TorrentFile file : checked) {
				//}
				mode.finish();
				return true;
			} else {
				Priority priority = Priority.Off;
				if (itemId == R.id.action_priority_low)
					priority = Priority.Low;
				if (itemId == R.id.action_priority_normal)
					priority = Priority.Normal;
				if (itemId == R.id.action_priority_high)
					priority = Priority.High;
				getTasksExecutor().updatePriority(torrent, checked, priority);
				mode.finish();
				return true;
			}
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			// Show the number of selected files in the CAB
			// torrentsList.getCheckedItemPositions().size() ?
			int checkedCount = 0;
			for (int i = 0; i < detailsList.getCheckedItemPositions().size(); i++) {
				if (detailsList.getCheckedItemPositions().valueAt(i)
						&& detailsList.getAdapter().getItem(detailsList.getCheckedItemPositions().keyAt(i)) instanceof TorrentFile)
					checkedCount++;
			}
			mode.setTitle(getResources().getQuantityString(R.plurals.navigation_filesselected, checkedCount, checkedCount));
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

	};

	/**
	 * Returns the object responsible for executing torrent tasks against a connected server
	 * @return The executor for tasks on some torrent
	 */
	private TorrentTasksExecutor getTasksExecutor() {
		// NOTE: Assumes the activity implements all the required torrent tasks
		return (TorrentTasksExecutor) getActivity();
	}
	
}
