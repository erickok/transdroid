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
package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.*;
import org.transdroid.core.gui.lists.DetailsAdapter;
import org.transdroid.core.gui.lists.SimpleListItemAdapter;
import org.transdroid.core.gui.navigation.*;
import org.transdroid.core.gui.navigation.SetLabelDialog.OnLabelPickedListener;
import org.transdroid.core.gui.navigation.SetStorageLocationDialog;
import org.transdroid.core.gui.navigation.SetStorageLocationDialog.OnStorageLocationUpdatedListener;
import org.transdroid.core.gui.navigation.SetTrackersDialog;
import org.transdroid.core.gui.navigation.SetTrackersDialog.OnTrackersUpdatedListener;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Fragment that shows detailed statistics about some torrent. These come from some already fetched {@link Torrent}
 * object, but it also retrieves further detailed statistics. The actual execution of tasks is performed by the activity
 * that contains this fragment, as per the {@link TorrentTasksExecutor} interface.
 * @author Eric Kok
 */
@EFragment(resName = "fragment_details")
@OptionsMenu(resName = "fragment_details")
public class DetailsFragment extends Fragment implements OnTrackersUpdatedListener, OnLabelPickedListener,
		OnStorageLocationUpdatedListener {

	// Local data
	@InstanceState
	protected Torrent torrent = null;
	@InstanceState
	protected String torrentId = null;
	@InstanceState
	protected TorrentDetails torrentDetails = null;
	@InstanceState
	protected ArrayList<TorrentFile> torrentFiles = null;
	@InstanceState
	protected ArrayList<Label> currentLabels = null;
	@InstanceState
	protected boolean isLoadingTorrent = false;
	@InstanceState
	protected boolean hasCriticalError = false;
	private ServerSetting currentServerSettings = null;

	// Views
	@ViewById(resName = "details_container")
	protected View detailsContainer;
	@ViewById(resName = "details_list")
	protected ListView detailsList;
	@ViewById
	protected TextView emptyText, errorText;
	@ViewById
	protected ProgressBar loadingProgress;

	@AfterViews
	protected void init() {

		// On large screens where this fragment is shown next to the torrents list, we show a continues grey vertical
		// line to separate the lists visually
		if (!NavigationHelper_.getInstance_(getActivity()).isSmallScreen()) {
			if (SystemSettings_.getInstance_(getActivity()).useDarkTheme()) {
				detailsContainer.setBackgroundResource(R.drawable.details_list_background_dark);
			} else {
				detailsContainer.setBackgroundResource(R.drawable.details_list_background_light);
			}
		}

		// Set up details adapter (itself containing the actual lists to show), which allows multi-select and fast
		// scrolling
		detailsList.setAdapter(new DetailsAdapter(getActivity()));
		detailsList.setMultiChoiceModeListener(onDetailsSelected);
		detailsList.setFastScrollEnabled(true);
		if (getActivity() != null && getActivity() instanceof RefreshableActivity) {
			((RefreshableActivity) getActivity()).addRefreshableView(detailsList);
		}

		// Restore the fragment state (on orientation changes et al.)
		if (torrent != null)
			updateTorrent(torrent);
		if (torrentDetails != null)
			updateTorrentDetails(torrent, torrentDetails);
		if (torrentFiles != null)
			updateTorrentFiles(torrent, torrentFiles);

	}

	public void setCurrentServerSettings(ServerSetting serverSettings) {
		currentServerSettings = serverSettings;
	}

	/**
	 * Updates the details adapter header to show the new torrent data.
	 * @param newTorrent The new, non-null torrent object
	 */
	public void updateTorrent(Torrent newTorrent) {
		this.torrent = newTorrent;
		this.torrentId = newTorrent.getUniqueID();
		this.hasCriticalError = false;
		((DetailsAdapter) detailsList.getAdapter()).updateTorrent(newTorrent);
		// Make the list (with details header) visible
		detailsList.setVisibility(View.VISIBLE);
		emptyText.setVisibility(View.GONE);
		errorText.setVisibility(View.GONE);
		loadingProgress.setVisibility(View.GONE);
		// Also update the available actions in the action bar
		getActivity().invalidateOptionsMenu();
		// Refresh the detailed statistics (errors) and list of files
		torrentDetails = null;
		torrentFiles = null;
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
		if (torrentId == null || !torrentId.equals(checkTorrent.getUniqueID()))
			return;
		this.torrentDetails = newTorrentDetails;
		((DetailsAdapter) detailsList.getAdapter()).updateTrackers(
				SimpleListItemAdapter.SimpleStringItem.wrapStringsList(newTorrentDetails.getTrackers()));
		((DetailsAdapter) detailsList.getAdapter()).updateErrors(
				SimpleListItemAdapter.SimpleStringItem.wrapStringsList(newTorrentDetails.getErrors()));
	}

	/**
	 * Updates the list adapter to show a new list of torrent files, replacing the old files list.
	 * @param checkTorrent The torrent for which the details were retrieved
	 * @param newTorrentFiles The new, updated list of torrent file objects
	 */
	public void updateTorrentFiles(Torrent checkTorrent, ArrayList<TorrentFile> newTorrentFiles) {
		// Check if these are actually the details of the torrent we are now showing
		if (torrentId == null || !torrentId.equals(checkTorrent.getUniqueID()))
			return;
		Collections.sort(newTorrentFiles);
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
		if (this.torrentId == null || torrents == null)
			return;
		for (Torrent newTorrent : torrents) {
			if (newTorrent.getUniqueID().equals(torrentId)) {
				// Found, so we can update our data as well
				updateTorrent(newTorrent);
				break;
			}
		}
	}

	/**
	 * Updates the locally maintained list of labels that are active on the server. Used in the label picking dialog and
	 * should be updated every time after the list of torrents was retrieved to keep it updated.
	 * @param currentLabels The list of known server labels
	 */
	public void updateLabels(ArrayList<Label> currentLabels) {
		this.currentLabels = currentLabels == null ? null : new ArrayList<Label>(currentLabels);
	}

	/**
	 * Clear the screen by fully clearing the internal merge list (with header and other lists)
	 */
	public void clear() {
		detailsList.setAdapter(new DetailsAdapter(getActivity()));
		detailsList.setVisibility(View.GONE);
		emptyText.setVisibility(!isLoadingTorrent && !hasCriticalError ? View.VISIBLE : View.GONE);
		errorText.setVisibility(!isLoadingTorrent && hasCriticalError ? View.VISIBLE : View.GONE);
		loadingProgress.setVisibility(isLoadingTorrent ? View.VISIBLE : View.GONE);
		torrent = null;
		torrentDetails = null;
		torrentFiles = null;
	}

	/**
	 * Updates the shown screen depending on whether the torrent is loading
	 * @param isLoading True if the torrent is (re)loading, false otherwise
	 * @param connectionErrorMessage The error message text to show to the user, or null if there was no error
	 */
	public void updateIsLoading(boolean isLoading, String connectionErrorMessage) {
		this.isLoadingTorrent = isLoading;
		this.hasCriticalError = connectionErrorMessage != null;
		errorText.setText(connectionErrorMessage);
		if (isLoading || hasCriticalError)
			clear();
	}

	@ItemClick(resName = "details_list")
	protected void detailsListClicked(int position) {
		detailsList.setItemChecked(position, false);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		if (torrent == null) {
			menu.findItem(R.id.action_resume).setVisible(false);
			menu.findItem(R.id.action_pause).setVisible(false);
			menu.findItem(R.id.action_start).setVisible(false);
			menu.findItem(R.id.action_start_direct).setVisible(false);
			menu.findItem(R.id.action_stop).setVisible(false);
			menu.findItem(R.id.action_remove).setVisible(false);
			menu.findItem(R.id.action_remove_withdata).setVisible(false);
			menu.findItem(R.id.action_setlabel).setVisible(false);
			menu.findItem(R.id.action_forcerecheck).setVisible(false);
			menu.findItem(R.id.action_updatetrackers).setVisible(false);
			menu.findItem(R.id.action_changelocation).setVisible(false);
			return;
		}
		// Update action availability
		boolean startStop = Daemon.supportsStoppingStarting(torrent.getDaemon());
		menu.findItem(R.id.action_resume).setVisible(torrent.canResume());
		menu.findItem(R.id.action_pause).setVisible(torrent.canPause());
		boolean forcedStart = Daemon.supportsForcedStarting(torrent.getDaemon());
		menu.findItem(R.id.action_start).setVisible(startStop && forcedStart && torrent.canStart());
		menu.findItem(R.id.action_start_direct).setVisible(startStop && !forcedStart && torrent.canStart());
		menu.findItem(R.id.action_stop).setVisible(startStop && torrent.canStop());
		menu.findItem(R.id.action_remove).setVisible(true);
		boolean removeWithData = Daemon.supportsRemoveWithData(torrent.getDaemon());
		menu.findItem(R.id.action_remove_withdata).setVisible(removeWithData);
		boolean setLabel = Daemon.supportsSetLabel(torrent.getDaemon());
		menu.findItem(R.id.action_setlabel).setVisible(setLabel);
		boolean forceRecheck = Daemon.supportsForceRecheck(torrent.getDaemon());
		menu.findItem(R.id.action_forcerecheck).setVisible(forceRecheck);
		boolean setTrackers = Daemon.supportsSetTrackers(torrent.getDaemon());
		menu.findItem(R.id.action_updatetrackers).setVisible(setTrackers);
		boolean setLocation = Daemon.supportsSetDownloadLocation(torrent.getDaemon());
		menu.findItem(R.id.action_changelocation).setVisible(setLocation);

	}

	@OptionsItem(resName = "action_resume")
	protected void resumeTorrent() {
		getTasksExecutor().resumeTorrent(torrent);
	}

	@OptionsItem(resName = "action_pause")
	protected void pauseTorrent() {
		getTasksExecutor().pauseTorrent(torrent);
	}

	@OptionsItem(resName = "action_start_direct")
	protected void startTorrentDirect() {
		getTasksExecutor().startTorrent(torrent, false);
	}

	@OptionsItem(resName = "action_start_default")
	protected void startTorrentDefault() {
		getTasksExecutor().startTorrent(torrent, false);
	}

	@OptionsItem(resName = "action_start_forced")
	protected void startTorrentForced() {
		getTasksExecutor().startTorrent(torrent, true);
	}

	@OptionsItem(resName = "action_stop")
	protected void stopTorrent() {
		getTasksExecutor().stopTorrent(torrent);
	}

	@OptionsItem(resName = "action_remove_default")
	protected void removeTorrentDefault() {
		getTasksExecutor().removeTorrent(torrent, false);
	}

	@OptionsItem(resName = "action_remove_withdata")
	protected void removeTorrentWithData() {
		getTasksExecutor().removeTorrent(torrent, true);
	}

	@OptionsItem(resName = "action_setlabel")
	protected void setLabel() {
		if (currentLabels != null)
			new SetLabelDialog().setOnLabelPickedListener(this).setCurrentLabels(currentLabels)
					.show(getFragmentManager(), "SetLabelDialog");
	}

	@OptionsItem(resName = "action_forcerecheck")
	protected void setForceRecheck() {
		getTasksExecutor().forceRecheckTorrent(torrent);
	}

	@OptionsItem(resName = "action_updatetrackers")
	protected void updateTrackers() {
		if (torrentDetails == null) {
			Crouton.showText(getActivity(), R.string.error_stillloadingdetails, NavigationHelper.CROUTON_INFO_STYLE);
			return;
		}
		new SetTrackersDialog().setOnTrackersUpdated(this).setCurrentTrackers(torrentDetails.getTrackersText())
				.show(getFragmentManager(), "SetTrackersDialog");
	}

	@OptionsItem(resName = "action_changelocation")
	protected void changeStorageLocation() {
		new SetStorageLocationDialog().setOnStorageLocationUpdated(this).setCurrentLocation(torrent.getLocationDir())
				.show(getFragmentManager(), "SetStorageLocationDialog");
	}

	@Override
	public void onLabelPicked(String newLabel) {
		if (torrent == null)
			return;
		getTasksExecutor().updateLabel(torrent, newLabel);
	}

	@Override
	public void onTrackersUpdated(List<String> updatedTrackers) {
		if (torrent == null)
			return;
		getTasksExecutor().updateTrackers(torrent, updatedTrackers);
	}

	@Override
	public void onStorageLocationUpdated(String newLocation) {
		if (torrent == null)
			return;
		getTasksExecutor().updateLocation(torrent, newLocation);
	}

	@Click
	protected void emptyTextClicked() {
		// Refresh the activity (that contains this fragment) when the empty view gear is clicked
		if (getActivity() != null && getActivity() instanceof RefreshableActivity) {
			((RefreshableActivity) getActivity()).refreshScreen();
		}
	}

	@Click
	protected void errorTextClicked() {
		// Refresh the activity (that contains this fragment) when the error view gear is clicked
		if (getActivity() != null && getActivity() instanceof RefreshableActivity) {
			((RefreshableActivity) getActivity()).refreshScreen();
		}
	}

	private MultiChoiceModeListener onDetailsSelected = new MultiChoiceModeListener() {

		SelectionManagerMode selectionManagerMode;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Show contextual action bar to start/stop/remove/etc. torrents in batch mode
			mode.getMenuInflater().inflate(R.menu.fragment_details_cab, menu);
			selectionManagerMode = new SelectionManagerMode(detailsList, R.plurals.navigation_filesselected);
			selectionManagerMode.setOnlyCheckClass(TorrentFile.class);
			selectionManagerMode.onCreateActionMode(mode, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// Pause autorefresh
			if (getActivity() != null && getActivity() instanceof TorrentsActivity) {
				((TorrentsActivity) getActivity()).stopRefresh = true;
				((TorrentsActivity) getActivity()).stopAutoRefresh();
			}
			boolean filePaths =
					currentServerSettings != null && Daemon.supportsFilePaths(currentServerSettings.getType());
			menu.findItem(R.id.action_download).setVisible(filePaths);
			boolean filePriorities = currentServerSettings != null &&
					Daemon.supportsFilePrioritySetting(currentServerSettings.getType());
			menu.findItem(R.id.action_priority_off).setVisible(filePriorities);
			menu.findItem(R.id.action_priority_low).setVisible(filePriorities);
			menu.findItem(R.id.action_priority_normal).setVisible(filePriorities);
			menu.findItem(R.id.action_priority_high).setVisible(filePriorities);
			return selectionManagerMode.onPrepareActionMode(mode, menu);
		}

		@SuppressLint("SdCardPath")
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			// Get checked torrents
			List<TorrentFile> checked = new ArrayList<TorrentFile>();
			for (int i = 0; i < detailsList.getCheckedItemPositions().size(); i++) {
				if (detailsList.getCheckedItemPositions().valueAt(i)
						&& i < detailsList.getAdapter().getCount()
						&& detailsList.getAdapter().getItem(detailsList.getCheckedItemPositions().keyAt(i)) instanceof TorrentFile)
					checked.add((TorrentFile) detailsList.getAdapter().getItem(
							detailsList.getCheckedItemPositions().keyAt(i)));
			}

			int itemId = item.getItemId();
			if (itemId == R.id.action_download) {

				if (checked.size() < 1 || currentServerSettings == null)
					return true;
				String urlBase = currentServerSettings.getFtpUrl();
				if (urlBase == null || urlBase.equals(""))
					urlBase = "ftp://" + currentServerSettings.getAddress();
				if (!urlBase.endsWith("/"))
					urlBase += "/";
				Uri urlBaseUri = Uri.parse(urlBase);
				urlBaseUri = urlBaseUri.normalizeScheme();
				String basePath = urlBaseUri.getPath();

				// Try using AndFTP intents
				Intent andftpStart = new Intent(Intent.ACTION_PICK);
				andftpStart.setDataAndType(Uri.fromParts(urlBaseUri.getScheme(), urlBaseUri.getAuthority(), null),
					"vnd.android.cursor.dir/lysesoft.andftp.uri");
				andftpStart.putExtra("command_type", "download");
				andftpStart.putExtra("ftp_pasv", "true");
				if (urlBaseUri.getUserInfo() != null)
					andftpStart.putExtra("ftp_username", urlBaseUri.getUserInfo());
				else
					andftpStart.putExtra("ftp_username", currentServerSettings.getUsername());
				if (currentServerSettings.getFtpPassword() != null
						&& !currentServerSettings.getFtpPassword().equals("")) {
					andftpStart.putExtra("ftp_password", currentServerSettings.getFtpPassword());
				} else {
					andftpStart.putExtra("ftp_password", currentServerSettings.getPassword());
				}
				// Note: AndFTP doesn't understand the directory that Environment.getExternalStoragePublicDirectory()
				// uses :(
				// Todo: Let user choose the download directory / make it configurable
				andftpStart.putExtra("local_folder", "/sdcard/Download");
				for (int f = 0; f < checked.size(); f++) {
					String file = checked.get(f).getRelativePath();
					if (file != null) {
						if (file.startsWith("/"))
							file = file.substring(1);
						andftpStart.putExtra("remote_file" + (f + 1), basePath + file);
					}
				}
				if (andftpStart.resolveActivity(getActivity().getPackageManager()) != null) {
					startActivity(andftpStart);
					mode.finish();
					return true;
				}

				// Try using a VIEW intent given an ftp:// scheme URI
				String file = checked.get(0).getRelativePath();
				if (file != null && file.startsWith("/"))
					file = file.substring(1);
				String url = urlBase + file;
				Intent simpleStart = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				if (simpleStart.resolveActivity(getActivity().getPackageManager()) != null) {
					startActivity(simpleStart);
					mode.finish();
					return true;
				}

				// No app is available that can handle FTP downloads
				Crouton.showText(getActivity(), getString(R.string.error_noftpapp, url),
						NavigationHelper.CROUTON_ERROR_STYLE);
				mode.finish();
				return true;

			} else if (itemId == R.id.action_copytoclipboard) {

				StringBuilder names = new StringBuilder();
				for (int f = 0; f < checked.size(); f++) {
					if (f != 0)
						names.append("\n");
					names.append(checked.get(f).getName());
				}
				ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(
						Context.CLIPBOARD_SERVICE);
				clipboardManager.setPrimaryClip(ClipData.newPlainText("Transdroid", names.toString()));
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
			selectionManagerMode.onItemCheckedStateChanged(mode, position, id, checked);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Resume autorefresh
			if (getActivity() != null && getActivity() instanceof TorrentsActivity) {
				((TorrentsActivity) getActivity()).stopRefresh = false;
				((TorrentsActivity) getActivity()).startAutoRefresh();
			}
			selectionManagerMode.onDestroyActionMode(mode);
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
