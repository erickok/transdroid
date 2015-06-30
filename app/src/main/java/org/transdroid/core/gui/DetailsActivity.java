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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

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
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings_;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.navigation.Label;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.navigation.RefreshableActivity;
import org.transdroid.core.service.ConnectivityHelper;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.ForceRecheckTask;
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

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that holds a single torrents details fragment. It is used on devices (i.e. phones) where there is no room to show details in the {@link
 * TorrentsActivity} directly. Task execution, such as loading of more details and updating file priorities, is performed in this activity via
 * background methods.
 * @author Eric Kok
 */
@EActivity(R.layout.activity_details)
@OptionsMenu(R.menu.activity_details)
public class DetailsActivity extends AppCompatActivity implements TorrentTasksExecutor, RefreshableActivity {

	@Extra
	@InstanceState
	protected Torrent torrent;
	@Extra
	@InstanceState
	protected ArrayList<Label> currentLabels;

	// Settings
	@Bean
	protected Log log;
	@Bean
	protected NavigationHelper navigationHelper;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected ApplicationSettings applicationSettings;
	private IDaemonAdapter currentConnection = null;

	// Details view components
	@ViewById
	protected Toolbar selectionToolbar;
	@FragmentById(R.id.torrentdetails_fragment)
	protected DetailsFragment fragmentDetails;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the theme according to the user preference
		if (SystemSettings_.getInstance_(this).useDarkTheme()) {
			setTheme(R.style.TransdroidTheme_Dark);
		}
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	protected void init() {

		// We require a torrent to be specified; otherwise close the activity
		if (torrent == null) {
			finish();
			return;
		}

		// Simple action bar with up, torrent name as title and refresh button
		setSupportActionBar(selectionToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(NavigationHelper.buildCondensedFontString(torrent.getName()));

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		fragmentDetails.setCurrentServerSettings(lastUsed);
		currentConnection = lastUsed.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);

		// Show details and load fine stats and torrent files
		fragmentDetails.updateTorrent(torrent);
		fragmentDetails.updateLabels(currentLabels);

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@OptionsItem(android.R.id.home)
	protected void navigateUp() {
		TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@OptionsItem(R.id.action_refresh)
	public void refreshScreen() {
		fragmentDetails.updateIsLoading(true, null);
		refreshTorrent();
		refreshTorrentDetails(torrent);
		refreshTorrentFiles(torrent);
	}

	@Background
	protected void refreshTorrent() {
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute(log);
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(), ((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, true);
		}
	}

	@Background
	public void refreshTorrentDetails(Torrent torrent) {
		if (!Daemon.supportsFineDetails(torrent.getDaemon())) {
			return;
		}
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute(log);
		if (result instanceof GetTorrentDetailsTaskSuccessResult) {
			onTorrentDetailsRetrieved(torrent, ((GetTorrentDetailsTaskSuccessResult) result).getTorrentDetails());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	public void refreshTorrentFiles(Torrent torrent) {
		if (!Daemon.supportsFileListing(torrent.getDaemon())) {
			return;
		}
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute(log);
		if (result instanceof GetFileListTaskSuccessResult) {
			onTorrentFilesRetrieved(torrent, ((GetFileListTaskSuccessResult) result).getFiles());
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void resumeTorrent(Torrent torrent) {
		torrent.mimicResume();
		DaemonTaskResult result = ResumeTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_resumed, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void pauseTorrent(Torrent torrent) {
		torrent.mimicPause();
		DaemonTaskResult result = PauseTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_paused, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void startTorrent(Torrent torrent, boolean forced) {
		torrent.mimicStart();
		DaemonTaskResult result = StartTask.create(currentConnection, torrent, forced).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_started, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void stopTorrent(Torrent torrent) {
		torrent.mimicStop();
		DaemonTaskResult result = StopTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_stopped, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void removeTorrent(Torrent torrent, boolean withData) {
		DaemonTaskResult result = RemoveTask.create(currentConnection, torrent, withData).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			// Close the details activity (as the torrent is now removed)
			closeActivity(getString(withData ? R.string.result_removed_with_data : R.string.result_removed, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@UiThread
	protected void closeActivity(String closeText) {
		setResult(RESULT_OK, new Intent().putExtra("torrent_removed", true).putExtra("affected_torrent", torrent));
		finish();
		if (closeText != null) {
			SnackbarManager.show(Snackbar.with(this).text(closeText));
		}
	}

	@Background
	@Override
	public void updateLabel(Torrent torrent, String newLabel) {
		torrent.mimicNewLabel(newLabel);
		DaemonTaskResult result = SetLabelTask.create(currentConnection, torrent, newLabel == null ? "" : newLabel).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_labelset, newLabel));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void forceRecheckTorrent(Torrent torrent) {
		torrent.mimicCheckingStatus();
		DaemonTaskResult result = ForceRecheckTask.create(currentConnection, torrent).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_recheckedstarted, torrent.getName()));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateTrackers(Torrent torrent, List<String> newTrackers) {
		DaemonTaskResult result = SetTrackersTask.create(currentConnection, torrent, newTrackers).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_trackersupdated));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updateLocation(Torrent torrent, String newLocation) {
		DaemonTaskResult result = SetDownloadLocationTask.create(currentConnection, torrent, newLocation).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_locationset, newLocation));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@Background
	@Override
	public void updatePriority(Torrent torrent, List<TorrentFile> files, Priority priority) {
		DaemonTaskResult result = SetFilePriorityTask.create(currentConnection, torrent, priority, new ArrayList<>(files)).execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_priotitiesset));
		} else {
			onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, String successMessage) {
		// Set the activity result so the calling activity knows it needs to update its view
		setResult(RESULT_OK, new Intent().putExtra("torrent_updated", true).putExtra("affected_torrent", torrent));
		// Refresh the screen as well
		refreshTorrent();
		refreshTorrentDetails(torrent);
		SnackbarManager.show(Snackbar.with(this).text(successMessage));
	}

	@UiThread
	protected void onTorrentDetailsRetrieved(Torrent torrent, TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		fragmentDetails.updateTorrentDetails(torrent, torrentDetails);
	}

	@UiThread
	protected void onTorrentFilesRetrieved(Torrent torrent, List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		fragmentDetails.updateTorrentFiles(torrent, new ArrayList<>(torrentFiles));
	}

	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result, boolean isCritical) {
		log.i(this, result.getException().toString());
		String error = getString(LocalTorrent.getResourceForDaemonException(result.getException()));
		fragmentDetails.updateIsLoading(false, isCritical ? error : null);
		SnackbarManager.show(Snackbar.with(this).text(getString(LocalTorrent.getResourceForDaemonException(result.getException())))
				.colorResource(R.color.red));
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {
		// Update the details fragment accordingly
		fragmentDetails.updateIsLoading(false, null);
		fragmentDetails.perhapsUpdateTorrent(torrents);
		fragmentDetails.updateLabels(Label.convertToNavigationLabels(labels, getResources().getString(R.string.labels_unlabeled)));
	}

}
