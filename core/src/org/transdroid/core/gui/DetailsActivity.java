package org.transdroid.core.gui;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
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
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.task.StopTask;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.keyboardsurfer.android.widget.crouton.Crouton;

@EActivity(resName = "activity_details")
@OptionsMenu(resName = "activity_details")
public class DetailsActivity extends SherlockFragmentActivity implements TorrentTasksExecutor {

	@Extra
	@InstanceState
	protected Torrent torrent;

	// Settings
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ApplicationSettings applicationSettings;
	private IDaemonAdapter currentConnection = null;

	// Details view components
	@FragmentById(resName = "torrent_details")
	protected DetailsFragment fragmentDetails;

	@AfterViews
	protected void init() {

		// We require a torrent to be specified; otherwise close the activity
		if (torrent == null) {
			finish();
			return;
		}

		// Simple action bar with up, torrent name as title and refresh button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(torrent.getName());

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		currentConnection = lastUsed.createServerAdapter();

		// Show details and load fine stats and torrent files
		fragmentDetails.updateTorrent(torrent);

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(resName = "action_refresh")
	protected void refreshScreen() {
		fragmentDetails.updateIsLoading(true);
		refreshTorrent();
	}

	@Background
	protected void refreshTorrent() {
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute();
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(),
					((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	public void refreshTorrentDetails(Torrent torrent) {
		if (!Daemon.supportsFineDetails(torrent.getDaemon()))
			return;
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute();
		if (result instanceof GetTorrentDetailsTaskSuccessResult) {
			onTorrentDetailsRetrieved(torrent, ((GetTorrentDetailsTaskSuccessResult) result).getTorrentDetails());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	public void refreshTorrentFiles(Torrent torrent) {
		if (!Daemon.supportsFileListing(torrent.getDaemon()))
			return;
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute();
		if (result instanceof GetFileListTaskSuccessResult) {
			onTorrentFilesRetrieved(torrent, ((GetFileListTaskSuccessResult) result).getFiles());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_resumed);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void pauseTorrent(Torrent torrent) {
		torrent.mimicPause();
		DaemonTaskResult result = PauseTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_paused);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void startTorrent(Torrent torrent, boolean forced) {
		torrent.mimicStart();
		DaemonTaskResult result = StartTask.create(currentConnection, torrent, forced).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_started);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void stopTorrent(Torrent torrent) {
		torrent.mimicStop();
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_stopped);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, withData ? R.string.result_removed_with_data
					: R.string.result_removed);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateLabel(Torrent torrent, String newLabel) {
		torrent.mimicNewLabel(newLabel);
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_labelset, newLabel);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_trackersupdated);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute();
		if (result instanceof DaemonTaskResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, R.string.result_locationset, newLocation);
		} else {
			onCommunicationError((DaemonTaskFailureResult) result);
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, int successMessageId, String... messageParams) {
		Crouton.showText(this, getString(successMessageId, (Object[]) messageParams),
				navigationHelper.CROUTON_INFO_STYLE);
	}

	@UiThread
	protected void onTorrentDetailsRetrieved(Torrent torrent, TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		fragmentDetails.updateTorrentDetails(torrent, torrentDetails);
	}

	@UiThread
	protected void onTorrentFilesRetrieved(Torrent torrent, List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		fragmentDetails.updateTorrentFiles(torrent, new ArrayList<TorrentFile>(torrentFiles));
	}

	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result) {
		Log.i(this, result.getException().toString());
		fragmentDetails.updateIsLoading(false);
		Crouton.showText(this, getString(LocalTorrent.getResourceForDaemonException(result.getException())),
				navigationHelper.CROUTON_ERROR_STYLE);
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {
		// Update the details fragment
		fragmentDetails.updateIsLoading(false);
		fragmentDetails.perhapsUpdateTorrent(torrents);
	}

}
