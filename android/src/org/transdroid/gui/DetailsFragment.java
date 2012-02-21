package org.transdroid.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.IDaemonCallback;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.TaskQueue;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentFilesComparator;
import org.transdroid.daemon.TorrentFilesSortBy;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopTask;
import org.transdroid.gui.SetLabelDialog.ResultListener;
import org.transdroid.gui.util.ActivityUtil;
import org.transdroid.gui.util.DialogWrapper;
import org.transdroid.gui.util.SelectableArrayAdapter.OnSelectedChangedListener;
import org.transdroid.preferences.Preferences;
import org.transdroid.util.TLog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class DetailsFragment extends Fragment implements IDaemonCallback, OnSelectedChangedListener {

	private static final String LOG_NAME = "Details fragment";

	private static final int FILEMENU_SETPRIORITY_ID = 0;
	private static final int FILEMENU_SETOFF_ID = 1;
	private static final int FILEMENU_SETLOW_ID = 2;
	private static final int FILEMENU_SETNORMAL_ID = 3;
	private static final int FILEMENU_SETHIGH_ID = 4;
	private static final int FILEMENU_REMOTESTART_ID = 5;
	private static final int FILEMENU_FTPDOWNLOAD_ID = 6;

	private static final int MENU_FORCESTART_ID = 50;
	private static final int MENU_SETLOCATION_ID = 51;
	private static final int MENU_EDITTRACKERS_ID = 52;
	private static final int MENU_INVERTSELECTION_ID = 53;
	private static final int MENU_REFRESH_ID = 54;

	static final int DIALOG_ASKREMOVE = 11;
	private static final int DIALOG_INSTALLVLC = 12;
	private static final int DIALOG_INSTALLFTPCLIENT = 13;
	static final int DIALOG_SETLABEL = 14;
	private static final int DIALOG_SETLOCATION = 15;
	private static final int DIALOG_EDITTRACKERS = 16;

	TorrentFilesSortBy sortSetting = TorrentFilesSortBy.Alphanumeric;
	boolean sortReversed = false;

	private final TorrentsFragment torrentsFragment;
	private final int daemonNumber;
	private Torrent torrent;
	private TorrentDetails fineDetails = null;
	private String[] existingLabels;
	private IDaemonAdapter daemon;
	private TaskQueue queue;

	private LinearLayout prioBar;
	private Button prioOff, prioLow, prioNormal, prioHigh;

	/**
	 * Public empty constructor for use with fragment retainment (setRetainInstance(true);)
	 */
	public DetailsFragment() {
		this.torrentsFragment  = null;
		this.daemonNumber = -1;
	}
	
	public DetailsFragment(TorrentsFragment torrentsFragment, int daemonNumber, Torrent torrent, String[] existingLabels) {
		this.torrentsFragment  = torrentsFragment;
		this.daemonNumber = daemonNumber;
		this.torrent = torrent;
		this.existingLabels = existingLabels;
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_details, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		getListView().setTextFilterEnabled(true);
		getListView().setOnItemClickListener(onFileClicked);

		prioBar = (LinearLayout) findViewById(R.id.setprio);
		prioOff = (Button) findViewById(R.id.setprio_off);
		prioLow = (Button) findViewById(R.id.setprio_low);
		prioNormal = (Button) findViewById(R.id.setprio_normal);
		prioHigh = (Button) findViewById(R.id.setprio_high);

		prioOff.setOnClickListener(setPriorityOffClicked);
		prioLow.setOnClickListener(setPriorityLowClicked);
		prioNormal.setOnClickListener(setPriorityNormalClicked);
		prioHigh.setOnClickListener(setPriorityHighClicked);

		// Set up a task queue
		queue = new TaskQueue(new TaskResultHandler(this));
		queue.start();

		loadData(true);
	}

	private void loadData(boolean clearOldData) {

		if (torrent == null) {
			TLog.d(LOG_NAME, "No torrent was provided in either the Intent or savedInstanceState.");
			return;
		}

		// Setup the daemon
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		DaemonSettings daemonSettings = Preferences.readAllDaemonSettings(prefs).get(daemonNumber);
		daemon = daemonSettings.getType().createAdapter(daemonSettings);

		// Show the torrent details
		getListView().setAdapter(new DetailsListAdapter(this, torrent, fineDetails));
		getSupportActivity().setTitle(torrent.getName());

		if (Daemon.supportsFileListing(daemon.getType())) {

			// Remove possibly old data and start loading the new file list
			if (clearOldData) {
				getDetailsListAdapter().getTorrentFileAdapter().clear();
				queue.enqueue(GetFileListTask.create(daemon, torrent));
			}

		} else {
			// Show that details are not (yet) supported by this adapter
			// TODO: Show this in a textview rather than as a toast pop-up
			Toast.makeText(getActivity(), R.string.details_notsupported, Toast.LENGTH_LONG).show();
		}

		if (Daemon.supportsFineDetails(daemon.getType()) && clearOldData) {
			queue.enqueue(GetTorrentDetailsTask.create(daemon, torrent));
		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		TorrentFile file = getDetailsListAdapter().getTorrentFileAdapter().getItem(
			(int) ((AdapterContextMenuInfo) menuInfo).id);

		if (Daemon.supportsFilePrioritySetting(daemon.getType())) {
			menu.add(FILEMENU_SETPRIORITY_ID, FILEMENU_SETOFF_ID, 0, R.string.file_off);
			menu.add(FILEMENU_SETPRIORITY_ID, FILEMENU_SETLOW_ID, 0, R.string.file_low);
			menu.add(FILEMENU_SETPRIORITY_ID, FILEMENU_SETNORMAL_ID, 0, R.string.file_normal);
			menu.add(FILEMENU_SETPRIORITY_ID, FILEMENU_SETHIGH_ID, 0, R.string.file_high);
		}
		// Show a remote play option if the server supports file paths and a mime type for this file can be
		// inferred
		if (Daemon.supportsFilePaths(daemon.getType()) && torrent.getLocationDir() != null
			&& file.getMimeType() != null) {
			menu.add(FILEMENU_REMOTESTART_ID, FILEMENU_REMOTESTART_ID, 0, R.string.file_remotestart);
		}
		if (Daemon.supportsFilePaths(daemon.getType()) && daemon.getSettings().getFtpUrl() != null && 
			!daemon.getSettings().getFtpUrl().equals("") && file.getRelativePath() != null) {
			menu.add(FILEMENU_FTPDOWNLOAD_ID, FILEMENU_FTPDOWNLOAD_ID, 0, R.string.file_ftpdownload);
		}

	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {

		// Get the selected file
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		TorrentFile file = getDetailsListAdapter().getTorrentFileAdapter().getItem((int) info.id);

		if (item.getItemId() >= FILEMENU_SETOFF_ID && item.getItemId() <= FILEMENU_SETHIGH_ID) {
			// Update the priority for this file
			Priority newPriority = file.getPriority();
			switch (item.getItemId()) {
			case FILEMENU_SETOFF_ID:
				newPriority = Priority.Off;
				break;
			case FILEMENU_SETLOW_ID:
				newPriority = Priority.Low;
				break;
			case FILEMENU_SETNORMAL_ID:
				newPriority = Priority.Normal;
				break;
			case FILEMENU_SETHIGH_ID:
				newPriority = Priority.High;
				break;
			}

			// Schedule a task to update this file's priority
			queue.enqueue(SetFilePriorityTask.create(daemon, torrent, newPriority, file));
		}

		if (item.getItemId() == FILEMENU_REMOTESTART_ID) {
			// Set up an intent to remotely play this file (in VLC)
			Intent remote = new Intent(Transdroid.REMOTEINTENT);
			remote.addCategory(Intent.CATEGORY_DEFAULT);
			// TODO: See if this still works
			remote.setDataAndType(Uri.parse(file.getFullPathUri()), file.getMimeType());
			remote.putExtra(Transdroid.REMOTEINTENT_HOST, daemon.getSettings().getAddress());

			TLog.d(LOG_NAME, "Remote start requested for " + remote.getData() + " (" + remote.getType() + ")");
			if (ActivityUtil.isIntentAvailable(getActivity(), remote)) {
				startActivity(remote);
			} else {
				showDialog(DIALOG_INSTALLVLC);
			}
		}

		if (item.getItemId() == FILEMENU_FTPDOWNLOAD_ID) {
			// Set up an intent to download this file using the partial user-specified FTP URL
			Uri ftpUri = Uri.parse(daemon.getSettings().getFtpUrl() + file.getRelativePath());
			Intent dl = new Intent(Intent.ACTION_PICK);
			dl.setDataAndType(Uri.parse(ftpUri.getScheme() + "://" + ftpUri.getHost()), Transdroid.ANDFTP_INTENT_TYPE);
			dl.putExtra(Transdroid.ANDFTP_INTENT_USER, (ftpUri.getEncodedUserInfo() == null ? daemon.getSettings()
				.getUsername() : ftpUri.getEncodedUserInfo()));
			dl.putExtra(Transdroid.ANDFTP_INTENT_PASS, daemon.getSettings().getFtpPassword());
			dl.putExtra(Transdroid.ANDFTP_INTENT_PASV, "true");
			dl.putExtra(Transdroid.ANDFTP_INTENT_CMD, "download");
			dl.putExtra(Transdroid.ANDFTP_INTENT_FILE, ftpUri.getEncodedPath());
			dl.putExtra(Transdroid.ANDFTP_INTENT_LOCAL, "/sdcard/download");

			TLog.d(LOG_NAME, "Requesting FTP transfer for " + dl.getStringExtra(Transdroid.ANDFTP_INTENT_FILE)
				+ " from " + dl.getDataString());
			if (ActivityUtil.isIntentAvailable(getActivity(), dl)) {
				startActivity(dl);
			} else {
				showDialog(DIALOG_INSTALLFTPCLIENT);
			}
		}

		return true;

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (getActivity() instanceof Details) {
			// Add title bar buttons
			MenuItem miRefresh = menu.add(0, MENU_REFRESH_ID, 0, R.string.refresh);
			miRefresh.setIcon(R.drawable.icon_refresh_title);
			miRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}
		if (Daemon.supportsForcedStarting(daemon.getType())) {
			MenuItem forced = menu.add(0, MENU_FORCESTART_ID, MENU_FORCESTART_ID, R.string.menu_forcestart);
			forced.setIcon(R.drawable.icon_start_menu);
		}
		if (Daemon.supportsSetDownloadLocation(daemon.getType())) {
			MenuItem location = menu
				.add(0, MENU_SETLOCATION_ID, MENU_SETLOCATION_ID, R.string.menu_setdownloadlocation);
			location.setIcon(android.R.drawable.ic_menu_upload);
		}
		if (Daemon.supportsSetTrackers(daemon.getType()) && fineDetails != null) {
			MenuItem trackers = menu.add(0, MENU_EDITTRACKERS_ID, MENU_EDITTRACKERS_ID, R.string.menu_edittrackers);
			trackers.setIcon(R.drawable.icon_trackers);
		}
		MenuItem invert = menu.add(0, MENU_INVERTSELECTION_ID, MENU_INVERTSELECTION_ID, R.string.menu_invertselection);
		invert.setIcon(R.drawable.icon_mark);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH_ID:
			refreshActivity();
			break;
		case MENU_FORCESTART_ID:
			queue.enqueue(StartTask.create(daemon, torrent, true));
			return true;

		case MENU_SETLOCATION_ID:
			showDialog(DIALOG_SETLOCATION);
			return true;

		case MENU_EDITTRACKERS_ID:
			showDialog(DIALOG_EDITTRACKERS);
			return true;

		case MENU_INVERTSELECTION_ID:
			// Invert the current file selection
			getDetailsListAdapter().getTorrentFileAdapter().invertSelection();
			getListView().invalidateViews();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onTorrentFilesLoaded(List<TorrentFile> allFiles) {
		if (allFiles != null && getView() != null) {
			Collections.sort(allFiles, new TorrentFilesComparator(TorrentFilesSortBy.Alphanumeric, false));
			getDetailsListAdapter().getTorrentFileAdapter().replace(allFiles);
		}
	}

	OnClickListener onResumePause = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (torrent.canPause()) {
				queue.enqueue(PauseTask.create(daemon, torrent));
			} else {
				queue.enqueue(ResumeTask.create(daemon, torrent));
			}
		}
	};

	OnClickListener onStartStop = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (torrent.canStop()) {
				queue.enqueue(StopTask.create(daemon, torrent));
			} else {
				queue.enqueue(StartTask.create(daemon, torrent, false));
			}
		}
	};

	OnClickListener onRemove = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_ASKREMOVE);
		}
	};

	OnClickListener onSetLabel = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showDialog(DIALOG_SETLABEL);
		}
	};

	private void setNewLabel(String newLabel) {

		if (!Daemon.supportsSetLabel(daemon.getType())) {
			// The daemon type does not support setting the label of a torrent
			Toast.makeText(getActivity(), R.string.labels_no_support, Toast.LENGTH_LONG).show();
			return;
		}

		// Mimic that we have already set the label (for a response feel)
		torrent.mimicNewLabel(newLabel);
		getDetailsListAdapter().updateViewsAndButtonStates();

		String saveLabel = newLabel;
		if (newLabel.equals(getString(R.string.labels_unlabeled).toString())) {
			// Setting a torrent to 'unlabeled' is actually setting the label to an empty string
			saveLabel = "";
		}
		queue.enqueue(SetLabelTask.create(daemon, torrent, saveLabel));
		queue.enqueue(RetrieveTask.create(daemon));

	}

	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_ASKREMOVE:

			// Build a dialog that asks to confirm the deletions of a torrent
			AlertDialog.Builder askRemoveDialog = new AlertDialog.Builder(getActivity());
			askRemoveDialog.setTitle(R.string.askremove_title);
			askRemoveDialog.setMessage(R.string.askremove);
			askRemoveDialog.setPositiveButton(R.string.menu_remove, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// Starts the remove task; we won't close this details dialog until its result is returned
					queue.enqueue(RemoveTask.create(daemon, torrent, false));
					dismissDialog(DIALOG_ASKREMOVE);
				}
			});
			askRemoveDialog.setNeutralButton(R.string.menu_also_data, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// Starts the remove task; we won't close this details dialog until its result is returned
					queue.enqueue(RemoveTask.create(daemon, torrent, true));
					dismissDialog(DIALOG_ASKREMOVE);
				}
			});
			askRemoveDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					dismissDialog(DIALOG_ASKREMOVE);
				}
			});
			return askRemoveDialog.create();

		case DIALOG_SETLABEL:

			// Build a dialog that asks for a new or selected an existing label to assign to the selected
			// torrent
			SetLabelDialog setLabelDialog = new SetLabelDialog(getActivity(), new ResultListener() {
				@Override
				public void onLabelResult(String newLabel) {
					if (newLabel.equals(getString(R.string.labels_unlabeled).toString())) {
						// Setting a torrent to 'unlabeled' is actually setting the label to an empty string
						newLabel = "";
					}
					setNewLabel(newLabel);
				}
			}, existingLabels, torrent.getLabelName());
			setLabelDialog.setTitle(R.string.labels_newlabel);

			return setLabelDialog;

		case DIALOG_SETLOCATION:

			// Build a dialog that asks for a new download location for the torrent
			final View setLocationLayout = LayoutInflater.from(getActivity()).inflate(
				R.layout.dialog_set_download_location, null);
			final EditText newLocation = (EditText) setLocationLayout.findViewById(R.id.download_location);
			newLocation.setText(torrent.getLocationDir());
			AlertDialog.Builder setLocationDialog = new AlertDialog.Builder(getActivity());
			setLocationDialog.setTitle(R.string.menu_setdownloadlocation);
			setLocationDialog.setView(setLocationLayout);
			setLocationDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					queue.enqueue(SetDownloadLocationTask.create(daemon, torrent, newLocation.getText().toString()));
				}
			});
			setLocationDialog.setNegativeButton(android.R.string.cancel, null);
			return setLocationDialog.create();

		case DIALOG_EDITTRACKERS:

			// Build a dialog that allows for the editing of the trackers
			final View editTrackersLayout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edittrackers,
				null);
			final EditText trackersText = (EditText) editTrackersLayout.findViewById(R.id.trackers);
			AlertDialog.Builder editTrackersDialog = new AlertDialog.Builder(getActivity());
			editTrackersDialog.setTitle(R.string.menu_edittrackers);
			editTrackersDialog.setView(editTrackersLayout);
			editTrackersDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					queue.enqueue(SetTrackersTask.create(daemon, torrent, Arrays.asList(trackersText.getText()
						.toString().split("\n"))));
				}
			});
			editTrackersDialog.setNegativeButton(android.R.string.cancel, null);
			return editTrackersDialog.create();

		case DIALOG_INSTALLVLC:
			return ActivityUtil.buildInstallDialog(getActivity(), R.string.vlcremote_not_found,
				Transdroid.VLCREMOTE_MARKET_URI);
		case DIALOG_INSTALLFTPCLIENT:
			return ActivityUtil.buildInstallDialog(getActivity(), R.string.ftpclient_not_found,
				Transdroid.ANDFTP_MARKET_URI);
		}
		return null;

	}

	/*
	 * @Override protected void onPrepareDialog(int id, Dialog dialog) { super.onPrepareDialog(id, dialog);
	 * 
	 * switch (id) { case DIALOG_SETLABEL:
	 * 
	 * // Re-populate the dialog adapter with the available labels SetLabelDialog setLabelDialog =
	 * (SetLabelDialog) dialog; setLabelDialog.resetDialog(this, existingLabels, torrent.getLabelName());
	 * break;
	 * 
	 * case DIALOG_SETLOCATION:
	 * 
	 * // Show the existing download location final EditText newLocation = (EditText)
	 * dialog.findViewById(R.id.download_location); newLocation.setText(torrent.getLocationDir()); break;
	 * 
	 * case DIALOG_EDITTRACKERS:
	 * 
	 * // Show the existing trackers final EditText trackersText = (EditText)
	 * dialog.findViewById(R.id.trackers); trackersText.setText(fineDetails.getTrackersText()); break;
	 * 
	 * } }
	 */

	@Override
	public boolean isAttached() {
		return getActivity() != null;
	}

	@Override
	public void onQueueEmpty() {
		// No active task: turn off status indicator
		// ((TransdroidListActivity)getActivity()).setProgressBar(false);
	}

	@Override
	public void onQueuedTaskFinished(DaemonTask finished) {
	}

	@Override
	public void onQueuedTaskStarted(DaemonTask started) {
		// Started on a new task: turn on status indicator
		// ((TransdroidListActivity)getActivity()).setProgressBar(true);
	}

	@Override
	public void onTaskFailure(DaemonTaskFailureResult result) {

		if (getActivity() == null) {
			// No longer visible
			return;
		}
		// Show error message
		Toast.makeText(getActivity(), LocalTorrent.getResourceForDaemonException(result.getException()),
			Toast.LENGTH_SHORT * 2).show();

	}

	@Override
	public void onTaskSuccess(DaemonTaskSuccessResult result) {

		if (getView() == null) {
			// We are no longer visible: discard the result
			return;
		}
		
		switch (result.getMethod()) {
		case Retrieve:
			// In the full updated list of torrents, look for the one we are showing
			// (Of course ideally we would only request info on this torrent, but there is no such
			// DaemonMethod for that at the moment)
			List<Torrent> list = ((RetrieveTaskSuccessResult) result).getTorrents();
			if (list != null) {
				for (Torrent t : list) {
					if (torrent.getUniqueID().equals(t.getUniqueID())) {

						// This is the updated torrent data for the torrent we are showing
						torrent = t;
						getDetailsListAdapter().setTorrent(torrent);
						getDetailsListAdapter().updateViewsAndButtonStates();

						// Force a label name (use 'unlabeled' if none is provided)
						if (torrent.getLabelName() == null || torrent.getLabelName().equals("")) {
							torrent.mimicNewLabel(getText(R.string.labels_unlabeled).toString());
						}

						break;
					}
				}
			}
			break;

		case GetTorrentDetails:
			fineDetails = ((GetTorrentDetailsTaskSuccessResult) result).getTorrentDetails();
			getDetailsListAdapter().setTorrentDetails(fineDetails);
			getDetailsListAdapter().updateViewsAndButtonStates();
			break;

		case GetFileList:
			onTorrentFilesLoaded(((GetFileListTaskSuccessResult) result).getFiles());
			break;

		case SetFilePriorities:
			// Queue a new task to update the file listing
			Toast.makeText(getActivity(), R.string.details_priorities_updated, Toast.LENGTH_SHORT).show();
			queue.enqueue(GetFileListTask.create(daemon, torrent));
			break;

		case Pause:
			torrent.mimicPause();
			getDetailsListAdapter().updateViewsAndButtonStates();
			// Also call back to the main torrents list to update its view
			if (torrentsFragment != null) {
				torrentsFragment.updateTorrentList();
			}
			break;

		case Resume:
			torrent.mimicResume();
			getDetailsListAdapter().updateViewsAndButtonStates();
			queue.enqueue(RetrieveTask.create(daemon));
			queue.enqueue(GetFileListTask.create(daemon, torrent));
			// Also call back to the main torrents list to update its view
			if (torrentsFragment != null) {
				torrentsFragment.updateTorrentList();
			}
			break;

		case Stop:
			torrent.mimicStop();
			getDetailsListAdapter().updateViewsAndButtonStates();
			// Also call back to the main torrents list to update its view
			if (torrentsFragment != null) {
				torrentsFragment.updateTorrentList();
			}
			break;

		case Start:
			torrent.mimicStart();
			getDetailsListAdapter().updateViewsAndButtonStates();
			queue.enqueue(RetrieveTask.create(daemon));
			queue.enqueue(GetFileListTask.create(daemon, torrent));
			// Also call back to the main torrents list to update its view
			if (torrentsFragment != null) {
				torrentsFragment.updateTorrentList();
			}
			break;

		case Remove:
			boolean includingData = ((RemoveTask) result.getTask()).includingData();
			Toast.makeText(
				getActivity(),
				"'" + result.getTargetTorrent().getName() + "' "
					+ getText(includingData ? R.string.torrent_removed_with_data : R.string.torrent_removed),
				Toast.LENGTH_SHORT).show();
			// Also call back to the main torrents list to update its view
			if (torrentsFragment != null) {
				torrentsFragment.updateTorrentList();
			}
			// Close this details fragment
			if (torrentsFragment != null) {
				FragmentTransaction ft = getSupportActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
			} else {
				getSupportActivity().getSupportFragmentManager().popBackStack();
			}
			break;

		case SetDownloadLocation:
			Toast.makeText(getActivity(),
				getString(R.string.torrent_locationset, ((SetDownloadLocationTask) result.getTask()).getNewLocation()),
				Toast.LENGTH_SHORT).show();
			// TODO: Show the download location in the details
			break;

		case SetTrackers:
			Toast.makeText(getActivity(), R.string.torrent_trackersupdated, Toast.LENGTH_SHORT).show();
			break;

		}

	}

	public void onSelectedResultsChanged() {
		if (getDetailsListAdapter().getTorrentFileAdapter().getSelected().size() == 0) {
			// Hide the bar with priority setting buttons
			prioBar.setVisibility(View.GONE);
		} else if (Daemon.supportsFilePrioritySetting(daemon.getType())) {
			prioBar.setVisibility(View.VISIBLE);
		}
	}

	private OnItemClickListener onFileClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
			// If something was already selected before, use an item click as selection click
			if (!getDetailsListAdapter().getTorrentFileAdapter().getSelected().isEmpty()) {
				TorrentFile file = getDetailsListAdapter().getTorrentFileAdapter().getItem(position - 1);
				getDetailsListAdapter().getTorrentFileAdapter().itemChecked(file,
					!getDetailsListAdapter().getTorrentFileAdapter().isItemChecked(file));
				getListView().invalidateViews();
			}
		}
	};

	private OnClickListener setPriorityOffClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Queue a task to set the priority of all selected files
			queueSetFilePrioritiesTask(Priority.Off, getDetailsListAdapter().getTorrentFileAdapter().getSelected());
		}
	};

	private OnClickListener setPriorityLowClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Queue a task to set the priority of all selected files
			queueSetFilePrioritiesTask(Priority.Low, getDetailsListAdapter().getTorrentFileAdapter().getSelected());
		}
	};

	private OnClickListener setPriorityNormalClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Queue a task to set the priority of all selected files
			queueSetFilePrioritiesTask(Priority.Normal, getDetailsListAdapter().getTorrentFileAdapter().getSelected());
		}
	};

	private OnClickListener setPriorityHighClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Queue a task to set the priority of all selected files
			queueSetFilePrioritiesTask(Priority.High, getDetailsListAdapter().getTorrentFileAdapter().getSelected());
		}
	};

	private void queueSetFilePrioritiesTask(Priority newPriority, List<TorrentFile> selected) {
		// Queue a task to set the priority of all selected files
		queue.enqueue(SetFilePriorityTask.create(daemon, torrent, newPriority, (ArrayList<TorrentFile>) selected));
		// Clear the selection
		// TorrentFileListAdapter adapter = (TorrentFileListAdapter) fileslist.getAdapter();
		// adapter.clearSelection();
	}

	public void pauseTorrent() {
		queue.enqueue(PauseTask.create(daemon, torrent));
	}

	public void resumeTorrent() {
		queue.enqueue(ResumeTask.create(daemon, torrent));
	}

	public void stopTorrent() {
		queue.enqueue(StopTask.create(daemon, torrent));
	}

	public void startTorrent(boolean forced) {
		queue.enqueue(StartTask.create(daemon, torrent, forced));
	}

	protected void refreshActivity() {
		queue.enqueue(RetrieveTask.create(daemon));
		if (Daemon.supportsFileListing(daemon.getType())) {
			queue.enqueue(GetFileListTask.create(daemon, torrent));
		}
		if (Daemon.supportsFineDetails(daemon.getType())) {
			queue.enqueue(GetTorrentDetailsTask.create(daemon, torrent));
		}
	}


	protected View findViewById(int id) {
		return getView().findViewById(id);
	}

	protected ListView getListView() {
		return (ListView) findViewById(android.R.id.list);
	}

	private DetailsListAdapter getDetailsListAdapter() {
		return (DetailsListAdapter) getListView().getAdapter();
	}

	public Daemon getActiveDaemonType() {
		return daemon.getType();
	}

	public void showDialog(int id) {
		new DialogWrapper(onCreateDialog(id)).show(getSupportActivity().getSupportFragmentManager(), DialogWrapper.TAG
			+ id);
	}

	protected void dismissDialog(int id) {
		// Remove the dialog wrapper fragment for the dialog's ID
		getSupportActivity().getSupportFragmentManager().beginTransaction().remove(
			getSupportActivity().getSupportFragmentManager().findFragmentByTag(DialogWrapper.TAG + id)).commit();
	}

}
