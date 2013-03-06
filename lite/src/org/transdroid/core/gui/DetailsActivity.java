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
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;

import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

@EActivity(R.layout.activity_details)
@OptionsMenu(R.menu.activity_details)
public class DetailsActivity extends SherlockFragmentActivity {

	@Extra
	@InstanceState
	protected Torrent torrent;

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	private IDaemonAdapter currentConnection = null;
	
	// Details view components
	@FragmentById(R.id.torrent_details)
	protected DetailsFagment fragmentDetails;

	@AfterViews
	protected void init() {

		// Simple action bar with up, torrent name as title and refresh button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(torrent.getName());

		// Connect to the last used server
		ServerSetting lastUsed = applicationSettings.getLastUsedServer();
		currentConnection = lastUsed.createServerAdapter();

		// Load fine details and torrent files
		refreshTorrentDetails();
		
	}
	
	@OptionsItem(R.id.action_refresh)
	protected void refreshScreen() {
		refreshTorrent();
		refreshTorrentDetails();
		refreshTorrentFiles();
	}
	
	@Background
	protected void refreshTorrent() {
		DaemonTaskResult result = RetrieveTask.create(currentConnection).execute();
		if (result instanceof RetrieveTaskSuccessResult) {
			onTorrentsRetrieved(((RetrieveTaskSuccessResult) result).getTorrents(), ((RetrieveTaskSuccessResult) result).getLabels());
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}

	@UiThread
	protected void onTorrentsRetrieved(List<Torrent> torrents, List<org.transdroid.daemon.Label> labels) {
		// Update the details fragment
		fragmentDetails.perhapsUpdateTorrent(torrents);
	}

	@Background
	protected void refreshTorrentDetails() {
		DaemonTaskResult result = GetTorrentDetailsTask.create(currentConnection, torrent).execute();
		if (result instanceof GetTorrentDetailsTaskSuccessResult) {
			onTorrentDetailsRetrieved(((GetTorrentDetailsTaskSuccessResult) result).getTorrentDetails());
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}

	@UiThread
	protected void onTorrentDetailsRetrieved(TorrentDetails torrentDetails) {
		// Update the details fragment with the new fine details for the shown torrent
		fragmentDetails.updateTorrentDetails(torrentDetails);
	}

	@Background
	protected void refreshTorrentFiles() {
		DaemonTaskResult result = GetFileListTask.create(currentConnection, torrent).execute();
		if (result instanceof GetFileListTaskSuccessResult) {
			onTorrentFilesRetrieved(((GetFileListTaskSuccessResult) result).getFiles());
		} else {
			onCommunicationError((DaemonTaskFailureResult)result);
		}
	}

	@UiThread
	protected void onTorrentFilesRetrieved(List<TorrentFile> torrentFiles) {
		// Update the details fragment with the newly retrieved list of files
		fragmentDetails.updateTorrentFiles(new ArrayList<TorrentFile>(torrentFiles));
	}
	
	@UiThread
	protected void onCommunicationError(DaemonTaskFailureResult result) {
		// TODO: Properly report this error
		Toast.makeText(this, getString(LocalTorrent.getResourceForDaemonException(result.getException())),
				Toast.LENGTH_LONG).show();
	}

}
