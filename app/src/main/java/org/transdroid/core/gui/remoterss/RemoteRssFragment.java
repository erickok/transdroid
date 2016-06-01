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
package org.transdroid.core.gui.remoterss;


import android.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.ActionMenuView;
import android.view.View;
import android.widget.ListView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows detailed statistics about some torrent. These come from some already fetched {@link Torrent} object, but it also retrieves
 * further detailed statistics. The actual execution of tasks is performed by the activity that contains this fragment, as per the {@link
 * TorrentTasksExecutor} interface.
 * @author Eric Kok
 */
@EFragment(R.layout.fragment_remoterss)
public class RemoteRssFragment extends Fragment {
	@Bean
	protected Log log;

	// Local data
	@InstanceState
	protected ArrayList<RemoteRssItem> torrentFiles;

	// Views
	@ViewById
	protected View detailsContainer;
	@ViewById(R.id.contextual_menu)
	protected ActionMenuView contextualMenu;
	@ViewById
	protected SwipeRefreshLayout swipeRefreshLayout;
	@ViewById
	protected ListView torrentsList;

	protected RemoteRssFilesAdapter adapter;

	@AfterViews
	protected void init() {

		// Inject menu options in the actions toolbar
		setHasOptionsMenu(true);

//		// On large screens where this fragment is shown next to the torrents list, we show a continues grey vertical
//		// line to separate the lists visually
//		if (!NavigationHelper_.getInstance_(getActivity()).isSmallScreen()) {
//			if (SystemSettings_.getInstance_(getActivity()).useDarkTheme()) {
//				detailsContainer.setBackgroundResource(R.drawable.details_list_background_dark);
//			} else {
//				detailsContainer.setBackgroundResource(R.drawable.details_list_background_light);
//			}
//		}

		// Set up details adapter (itself containing the actual lists to show), which allows multi-select and fast
		// scrolling
		adapter = new RemoteRssFilesAdapter(getActivity());
		torrentsList.setAdapter(adapter);
		torrentsList.setFastScrollEnabled(true);

		// Restore the fragment state (on orientation changes et al.)
		if (torrentFiles != null) {
			updateTorrentFiles(torrentFiles);
		}
	}

	/**
	 * Updates the list adapter to show a new list of torrent files, replacing the old files list.
	 * @param checkTorrent The torrent for which the details were retrieved
	 * @param newTorrentFiles The new, updated list of torrent file objects
	 */
	public void updateTorrentFiles(List<RemoteRssItem> remoteRssFiles) {
		torrentFiles = new ArrayList<>(remoteRssFiles);
		adapter.updateFiles(torrentFiles);
	}

	@ItemClick(resName = "torrents_list")
	protected void detailsListClicked(int position) {
		RemoteRssActivity activity = (RemoteRssActivity) getActivity();
		RemoteRssItem item = (RemoteRssItem) adapter.getItem(position);
		IDaemonAdapter currentConnection = activity.getCurrentConnection();
		DaemonTaskResult result;

		if (item.isMagnetLink()) {
			// Check if it's supported
			if (!Daemon.supportsAddByMagnetUrl(currentConnection.getType())) {
				// TODO:
				return;
			}

			AddByMagnetUrlTask addByMagnetUrlTask = AddByMagnetUrlTask.create(currentConnection, item.getLink());
			result = addByMagnetUrlTask.execute(log);
		}
		else {
			result = AddByUrlTask.create(currentConnection, item.getLink(), item.getTitle()).execute(log);
		}

		if (result instanceof DaemonTaskSuccessResult) {
			onTaskSucceeded((DaemonTaskSuccessResult) result, getString(R.string.result_added, item.getTitle()));
		} else {
//				onCommunicationError((DaemonTaskFailureResult) result, false);
		}
	}

	@UiThread
	protected void onTaskSucceeded(DaemonTaskSuccessResult result, String successMessage) {
		SnackbarManager.show(Snackbar.with(getActivity()).text(successMessage));
	}
}
