/*
 * Copyright 2010-2024 Eric Kok et al.
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

import android.annotation.SuppressLint;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.app.settings.SystemSettings;
import org.transdroid.core.gui.lists.DetailsPagerAdapter;
import org.transdroid.core.gui.lists.GeoIpHelper;
import org.transdroid.core.gui.lists.PiecesMapView;
import org.transdroid.core.gui.lists.TorrentDetailsView;
import org.transdroid.core.gui.lists.TorrentDetailsView_;
import org.transdroid.core.gui.navigation.Label;
import org.transdroid.core.gui.navigation.NavigationHelper_;
import org.transdroid.core.gui.navigation.RefreshableActivity;
import org.transdroid.core.gui.navigation.SelectionManagerMode;
import org.transdroid.core.gui.navigation.SetLabelDialog;
import org.transdroid.core.gui.navigation.SetLabelDialog.OnLabelPickedListener;
import org.transdroid.core.gui.navigation.SetStorageLocationDialog;
import org.transdroid.core.gui.navigation.SetStorageLocationDialog.OnStorageLocationUpdatedListener;
import org.transdroid.core.gui.navigation.SetTrackersDialog;
import org.transdroid.core.gui.navigation.SetTrackersDialog.OnTrackersUpdatedListener;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Peer;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.Tracker;
import org.transdroid.daemon.TrackerStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment that shows detailed statistics about some torrent. These come from some already fetched {@link Torrent} object, but it also retrieves
 * further detailed statistics. The actual execution of tasks is performed by the activity that contains this fragment, as per the {@link
 * TorrentTasksExecutor} interface.
 *
 * @author Eric Kok
 */
@EFragment(R.layout.fragment_details)
public class DetailsFragment extends Fragment implements OnTrackersUpdatedListener, OnLabelPickedListener, OnStorageLocationUpdatedListener {

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
    protected ArrayList<Peer> torrentPeers = null;
    @InstanceState
    protected ArrayList<Label> currentLabels = null;
    @InstanceState
    protected boolean isLoadingTorrent = false;
    @InstanceState
    protected boolean hasCriticalError = false;
    // Views
    @ViewById
    protected View detailsContainer;
    @ViewById(R.id.details_menu)
    protected ActionMenuView detailsMenu;
    @ViewById(R.id.contextual_menu)
    protected ActionMenuView contextualMenu;
    @ViewById
    protected View detailsContent;
    @ViewById
    protected LinearLayout detailsHeader;
    @ViewById
    protected TabLayout detailsTabs;
    @ViewById
    protected ViewPager2 detailsPager;
    @ViewById
    protected TextView emptyText, errorText;
    @ViewById
    protected ProgressBar loadingProgress;
    @Bean
    protected GeoIpHelper geoIpHelper;
    @Bean
    protected SystemSettings systemSettings;
    private DetailsPagerAdapter pagerAdapter = null;
    private ListView filesList = null;
    private TorrentDetailsView torrentDetailsView = null;
    private PiecesMapView piecesMapView = null;
    private ServerSetting currentServerSettings = null;
    private MultiChoiceModeListener onDetailsSelected = new MultiChoiceModeListener() {

        SelectionManagerMode selectionManagerMode;

        @Override
        public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
            // Show contextual action bar to start/stop/remove/etc. torrents in batch mode
            detailsMenu.setVisibility(View.INVISIBLE);
            contextualMenu.setVisibility(View.VISIBLE);
            contextualMenu.setOnMenuItemClickListener(menuItem -> onActionItemClicked(mode, menuItem));
            contextualMenu.getMenu().clear();
            getActivity().getMenuInflater().inflate(R.menu.fragment_details_cab_main, contextualMenu.getMenu());
            Context themedContext = ((AppCompatActivity) getActivity()).getSupportActionBar().getThemedContext();
            mode.getMenuInflater().inflate(R.menu.fragment_details_cab_secondary, menu);
            selectionManagerMode = new SelectionManagerMode(themedContext, filesList, R.plurals.navigation_filesselected);
            selectionManagerMode.setOnlyCheckClass(TorrentFile.class);
            selectionManagerMode.onCreateActionMode(mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            selectionManagerMode.onPrepareActionMode(mode, menu);
            // Pause autorefresh
            if (getActivity() != null && getActivity() instanceof TorrentsActivity) {
                ((TorrentsActivity) getActivity()).stopRefresh = true;
                ((TorrentsActivity) getActivity()).stopAutoRefresh();
            }
            if (getActivity() instanceof DetailsActivity) {
                ((DetailsActivity) getActivity()).stopRefresh = true;
                ((DetailsActivity) getActivity()).stopAutoRefresh();
            }
            boolean filePaths = currentServerSettings != null && Daemon.supportsFilePaths(currentServerSettings.getType());
            contextualMenu.getMenu().findItem(R.id.action_download).setVisible(filePaths);
            boolean filePriorities = currentServerSettings != null && Daemon.supportsFilePrioritySetting(currentServerSettings.getType());
            contextualMenu.getMenu().findItem(R.id.action_priority_off).setVisible(filePriorities);
            contextualMenu.getMenu().findItem(R.id.action_priority_low).setVisible(filePriorities);
            contextualMenu.getMenu().findItem(R.id.action_priority_normal).setVisible(filePriorities);
            contextualMenu.getMenu().findItem(R.id.action_priority_high).setVisible(filePriorities);
            return true;
        }

        @SuppressLint("SdCardPath")
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            // Get checked torrents
            List<TorrentFile> checked = new ArrayList<>();
            for (int i = 0; i < filesList.getCheckedItemPositions().size(); i++) {
                if (filesList.getCheckedItemPositions().valueAt(i) && i < filesList.getAdapter().getCount() &&
                        filesList.getAdapter().getItem(filesList.getCheckedItemPositions().keyAt(i)) instanceof TorrentFile) {
                    checked.add((TorrentFile) filesList.getAdapter().getItem(filesList.getCheckedItemPositions().keyAt(i)));
                }
            }

            int itemId = item.getItemId();
            if (itemId == R.id.action_download) {

                if (checked.size() < 1 || currentServerSettings == null) {
                    return true;
                }
                String urlBase = currentServerSettings.getFtpUrl();
                if (urlBase == null || urlBase.equals("")) {
                    urlBase = "ftp://" + currentServerSettings.getAddress() + "/";
                }

                // Try using AndFTP intents
                Intent andftpStart = new Intent(Intent.ACTION_PICK);
                andftpStart.setDataAndType(Uri.parse(urlBase), "vnd.android.cursor.dir/lysesoft.andftp.uri");
                andftpStart.putExtra("command_type", "download");
                andftpStart.putExtra("ftp_pasv", "true");
                if (Uri.parse(urlBase).getUserInfo() != null) {
                    andftpStart.putExtra("ftp_username", Uri.parse(urlBase).getUserInfo());
                } else {
                    andftpStart.putExtra("ftp_username", currentServerSettings.getUsername());
                }
                if (currentServerSettings.getFtpPassword() != null && !currentServerSettings.getFtpPassword().equals("")) {
                    andftpStart.putExtra("ftp_password", currentServerSettings.getFtpPassword());
                } else {
                    andftpStart.putExtra("ftp_password", currentServerSettings.getPassword());
                }
                // Note: AndFTP doesn't understand the directory that Environment.getExternalStoragePublicDirectory()
                // uses :(
                andftpStart.putExtra("local_folder", "/sdcard/Download");
                for (int f = 0; f < checked.size(); f++) {
                    String file = checked.get(f).getRelativePath();
                    if (file != null) {
                        // If the file is directly in the root, AndFTP fails if we supply the proper path (like
                        // /file.pdf)
                        // Work around this bug by removing the leading / if no further directories are used in the path
                        if (file.startsWith("/") && file.indexOf("/", 1) < 0) {
                            file = file.substring(1);
                        }
                        andftpStart.putExtra("remote_file" + (f + 1), file);
                    }
                }
                if (andftpStart.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(andftpStart);
                    mode.finish();
                    return true;
                }

                // Try using a VIEW intent given an ftp:// scheme URI
                String url = urlBase + checked.get(0).getRelativePath();
                Intent simpleStart = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (simpleStart.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(simpleStart);
                    mode.finish();
                    return true;
                }

                // No app is available that can handle FTP downloads
                SnackbarManager.show(Snackbar.with(getActivity()).text(getString(R.string.error_noftpapp, url)).type(SnackbarType.MULTI_LINE)
                        .colorResource(R.color.red));
                mode.finish();
                return true;

            } else if (itemId == R.id.action_copytoclipboard) {

                StringBuilder names = new StringBuilder();
                for (int f = 0; f < checked.size(); f++) {
                    if (f != 0) {
                        names.append("\n");
                    }
                    names.append(checked.get(f).getName());
                }
                ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText("Transdroid", names.toString()));
                mode.finish();
                return true;

            } else {
                Priority priority = Priority.Off;
                if (itemId == R.id.action_priority_low) {
                    priority = Priority.Low;
                }
                if (itemId == R.id.action_priority_normal) {
                    priority = Priority.Normal;
                }
                if (itemId == R.id.action_priority_high) {
                    priority = Priority.High;
                }
                if (getTasksExecutor() != null)
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
            if (getActivity() instanceof DetailsActivity) {
                ((DetailsActivity) getActivity()).stopRefresh = false;
                ((DetailsActivity) getActivity()).startAutoRefresh();
            }
            selectionManagerMode.onDestroyActionMode(mode);
            contextualMenu.setVisibility(View.GONE);
            detailsMenu.setVisibility(View.VISIBLE);
        }

    };

    @AfterViews
    protected void init() {

        // Inject menu options in the actions toolbar
        setHasOptionsMenu(true);

        // On large screens where this fragment is shown next to the torrents list, we show a continues grey vertical
        // line to separate the lists visually
        if (!NavigationHelper_.getInstance_(getActivity()).isSmallScreen()) {
            detailsContainer.setBackgroundResource(R.drawable.details_list_background);
        }

        createMenuOptions();

        // Build the fixed summary header (torrent stats and an optional pieces map)
        torrentDetailsView = TorrentDetailsView_.build(getActivity());
        torrentDetailsView.setVisibility(View.GONE);
        detailsHeader.addView(torrentDetailsView);
        piecesMapView = new PiecesMapView(getActivity());
        piecesMapView.setVisibility(View.GONE);
        detailsHeader.addView(piecesMapView);

        // Set up the swipeable Files / Peers / Trackers pager below the header
        Runnable refresh = () -> {
            if (getActivity() instanceof RefreshableActivity) {
                ((RefreshableActivity) getActivity()).refreshScreen();
            }
        };
        pagerAdapter = new DetailsPagerAdapter(getActivity(), geoIpHelper, refresh);
        detailsPager.setAdapter(pagerAdapter);
        detailsPager.setOffscreenPageLimit(2); // Keep all three pages alive
        new TabLayoutMediator(detailsTabs, detailsPager, (tab, position) -> {
            switch (position) {
                case DetailsPagerAdapter.PAGE_PEERS:
                    tab.setText(R.string.status_peers);
                    break;
                case DetailsPagerAdapter.PAGE_TRACKERS:
                    tab.setText(R.string.status_trackers);
                    break;
                case DetailsPagerAdapter.PAGE_FILES:
                default:
                    tab.setText(R.string.status_files);
                    break;
            }
        }).attach();

        // The Files list keeps the multi-select priority contextual action bar
        filesList = pagerAdapter.getFilesList();
        filesList.setMultiChoiceModeListener(onDetailsSelected);
        filesList.setFastScrollEnabled(true);
        filesList.setOnItemClickListener((parent, view, position, id) -> filesList.setItemChecked(position, false));

        // Restore the fragment state (on orientation changes et al.)
        if (torrent != null) {
            updateTorrent(torrent);
        }
        if (torrent != null && torrentDetails != null) {
            updateTorrentDetails(torrent, torrentDetails);
        }
        if (torrent != null && torrentFiles != null) {
            updateTorrentFiles(torrent, torrentFiles);
        }
        if (torrent != null && torrentPeers != null) {
            updateTorrentPeers(torrent, torrentPeers);
        }

    }

    public void setCurrentServerSettings(ServerSetting serverSettings) {
        currentServerSettings = serverSettings;
    }

    /**
     * Updates the details adapter header to show the new torrent data.
     *
     * @param newTorrent The new, non-null torrent object
     */
    public void updateTorrent(Torrent newTorrent) {
        this.torrent = newTorrent;
        this.torrentId = newTorrent.getUniqueID();
        this.hasCriticalError = false;
        torrentDetailsView.update(newTorrent);
        torrentDetailsView.setVisibility(newTorrent == null ? View.GONE : View.VISIBLE);
        // Make the content (header + swipeable pages) visible
        detailsContent.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.GONE);
        // Also update the available actions in the action bar
        updateMenuOptions();
        // Refresh the detailed statistics (trackers), peers and list of files
        torrentDetails = null;
        torrentFiles = null;
        torrentPeers = null;
        if (getTasksExecutor() != null) {
            getTasksExecutor().refreshTorrentDetails(torrent);
            getTasksExecutor().refreshTorrentFiles(torrent);
            if (Daemon.supportsExtraPeers(torrent.getDaemon())) {
                getTasksExecutor().refreshTorrentPeers(torrent);
            } else {
                updateTorrentPeers(torrent, new ArrayList<>());
            }
        }
    }

    /**
     * Updates the details adapter to show the list of trackers and tracker errors.
     *
     * @param checkTorrent      The torrent for which the details were retrieved
     * @param newTorrentDetails The new fine details object of some torrent
     */
    public void updateTorrentDetails(Torrent checkTorrent, TorrentDetails newTorrentDetails) {
        // Check if these are actually the details of the torrent we are now showing
        if (torrentId == null || !torrentId.equals(checkTorrent.getUniqueID())) {
            return;
        }
        this.torrentDetails = newTorrentDetails;

        // Prefer the structured tracker details; fall back to the plain tracker URLs (status unknown)
        List<Tracker> trackers = newTorrentDetails.getTrackerDetails();
        if ((trackers == null || trackers.isEmpty()) && newTorrentDetails.getTrackers() != null) {
            trackers = new ArrayList<>();
            for (String url : newTorrentDetails.getTrackers()) {
                trackers.add(new Tracker(url, TrackerStatus.UNKNOWN, null));
            }
        }
        pagerAdapter.setTrackers(trackers);

        // Update the optional pieces map in the fixed header
        List<Integer> pieces = newTorrentDetails.getPieces();
        if (pieces == null || pieces.isEmpty()) {
            piecesMapView.setVisibility(View.GONE);
        } else {
            piecesMapView.setPieces(pieces);
            piecesMapView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the list adapter to show a new list of torrent files, replacing the old files list.
     *
     * @param checkTorrent    The torrent for which the details were retrieved
     * @param newTorrentFiles The new, updated list of torrent file objects
     */
    public void updateTorrentFiles(Torrent checkTorrent, ArrayList<TorrentFile> newTorrentFiles) {
        // Check if these are actually the details of the torrent we are now showing
        if (torrentId == null || !torrentId.equals(checkTorrent.getUniqueID())) {
            return;
        }
        Collections.sort(newTorrentFiles);
        this.torrentFiles = newTorrentFiles;
        pagerAdapter.setFiles(newTorrentFiles);
    }

    /**
     * Updates the list adapter to show a new list of connected peers, replacing the old peers list.
     *
     * @param checkTorrent    The torrent for which the peers were retrieved
     * @param newTorrentPeers The new, updated list of peer objects
     */
    public void updateTorrentPeers(Torrent checkTorrent, ArrayList<Peer> newTorrentPeers) {
        // Check if these are actually the peers of the torrent we are now showing
        if (torrentId == null || !torrentId.equals(checkTorrent.getUniqueID())) {
            return;
        }
        Collections.sort(newTorrentPeers, systemSettings.getPeerSortBy().comparator());
        this.torrentPeers = newTorrentPeers;
        boolean supported = currentServerSettings == null || Daemon.supportsExtraPeers(currentServerSettings.getType());
        pagerAdapter.setPeers(newTorrentPeers, supported);
    }

    /**
     * Can be called if some outside activity returned new torrents, so we can perhaps piggyback on this by update our data as well.
     *
     * @param torrents The last of retrieved torrents
     */
    public void perhapsUpdateTorrent(List<Torrent> torrents) {
        // Only try to update if we actually were showing a torrent
        if (this.torrentId == null || torrents == null) {
            return;
        }
        for (Torrent newTorrent : torrents) {
            if (newTorrent.getUniqueID().equals(torrentId)) {
                // Found, so we can update our data as well
                updateTorrent(newTorrent);
                break;
            }
        }
    }

    /**
     * Updates the locally maintained list of labels that are active on the server. Used in the label picking dialog and should be updated every time
     * after the list of torrents was retrieved to keep it updated.
     *
     * @param currentLabels The list of known server labels
     */
    public void updateLabels(ArrayList<Label> currentLabels) {
        this.currentLabels = currentLabels == null ? null : new ArrayList<>(currentLabels);
    }

    /**
     * Clear the screen by fully clearing the internal merge list (with header and other lists)
     */
    public void clear() {
        if (pagerAdapter != null) {
            pagerAdapter.setFiles(new ArrayList<>());
            pagerAdapter.setPeers(new ArrayList<>(), true);
            pagerAdapter.setTrackers(new ArrayList<>());
        }
        if (torrentDetailsView != null) {
            torrentDetailsView.setVisibility(View.GONE);
        }
        if (piecesMapView != null) {
            piecesMapView.setVisibility(View.GONE);
        }
        detailsContent.setVisibility(View.GONE);
        emptyText.setVisibility(!isLoadingTorrent && !hasCriticalError ? View.VISIBLE : View.GONE);
        errorText.setVisibility(!isLoadingTorrent && hasCriticalError ? View.VISIBLE : View.GONE);
        loadingProgress.setVisibility(isLoadingTorrent ? View.VISIBLE : View.GONE);
        torrent = null;
        torrentDetails = null;
        torrentFiles = null;
        torrentPeers = null;
    }

    /**
     * Updates the shown screen depending on whether the torrent is loading
     *
     * @param isLoading              True if the torrent is (re)loading, false otherwise
     * @param connectionErrorMessage The error message text to show to the user, or null if there was no error
     */
    public void updateIsLoading(boolean isLoading, String connectionErrorMessage) {
        this.isLoadingTorrent = isLoading;
        this.hasCriticalError = connectionErrorMessage != null;
        errorText.setText(connectionErrorMessage);
        if (isLoading || hasCriticalError) {
            clear();
        }
    }

    public void createMenuOptions() {
        getActivity().getMenuInflater().inflate(R.menu.fragment_details, detailsMenu.getMenu());
        detailsMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.action_pause:
                    pauseTorrent();
                    return true;
                case R.id.action_updatetrackers:
                    updateTrackers();
                    return true;
                case R.id.action_start_forced:
                    startTorrentForced();
                    return true;
                case R.id.action_remove_withdata:
                    removeTorrentWithData();
                    return true;
                case R.id.action_stop:
                    stopTorrent();
                    return true;
                case R.id.action_toggle_sequential:
                    toggleSequentialDownload(menuItem);
                    return true;
                case R.id.action_toggle_firstlastpiece:
                    toggleFirstLastPieceDownload(menuItem);
                    return true;
                case R.id.action_forcerecheck:
                    setForceRecheck();
                    return true;
                case R.id.action_changelocation:
                    changeStorageLocation();
                    return true;
                case R.id.action_start_default:
                    startTorrentDefault();
                    return true;
                case R.id.action_remove_default:
                    removeTorrentDefault();
                    return true;
                case R.id.action_start_direct:
                    startTorrentDirect();
                    return true;
                case R.id.action_setlabel:
                    setLabel();
                    return true;
                case R.id.action_resume:
                    resumeTorrent();
                    return true;
            }
            return false;
        });
    }

    private void updateMenuOptions() {

        if (torrent == null) {
            detailsMenu.getMenu().findItem(R.id.action_resume).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_pause).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_start).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_start_direct).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_stop).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_remove).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_remove_withdata).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_setlabel).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_forcerecheck).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_updatetrackers).setVisible(false);
            detailsMenu.getMenu().findItem(R.id.action_changelocation).setVisible(false);
            return;
        }
        // Update action availability
        boolean startStop = Daemon.supportsStoppingStarting(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_resume).setVisible(torrent.canResume());
        detailsMenu.getMenu().findItem(R.id.action_pause).setVisible(torrent.canPause());
        boolean forcedStart = Daemon.supportsForcedStarting(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_start).setVisible(startStop && forcedStart && torrent.canStart());
        detailsMenu.getMenu().findItem(R.id.action_start_direct).setVisible(startStop && !forcedStart && torrent.canStart());
        detailsMenu.getMenu().findItem(R.id.action_stop).setVisible(startStop && torrent.canStop());
        detailsMenu.getMenu().findItem(R.id.action_remove).setVisible(true);
        boolean removeWithData = Daemon.supportsRemoveWithData(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_remove_withdata).setVisible(removeWithData);
        boolean setLabel = Daemon.supportsSetLabel(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_setlabel).setVisible(setLabel);
        boolean forceRecheck = Daemon.supportsForceRecheck(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_forcerecheck).setVisible(forceRecheck);
        boolean sequentialdl = Daemon.supportsSequentialDownload(torrent.getDaemon());
        MenuItem seqMenuItem = detailsMenu.getMenu().findItem(R.id.action_toggle_sequential);
        seqMenuItem.setVisible(sequentialdl);
        seqMenuItem.setChecked(torrent.isSequentiallyDownloading());
        boolean firstlastpiecedl = Daemon.supportsFirstLastPiece(torrent.getDaemon());
        MenuItem flpMenuItem = detailsMenu.getMenu().findItem(R.id.action_toggle_firstlastpiece);
        flpMenuItem.setVisible(firstlastpiecedl);
        flpMenuItem.setChecked(torrent.isDownloadingFirstLastPieceFirst());
        detailsMenu.getMenu().findItem(R.id.action_download_mode).setVisible(!torrent.isFinished() && (firstlastpiecedl || sequentialdl));
        boolean setTrackers = Daemon.supportsSetTrackers(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_updatetrackers).setVisible(setTrackers);
        boolean setLocation = Daemon.supportsSetDownloadLocation(torrent.getDaemon());
        detailsMenu.getMenu().findItem(R.id.action_changelocation).setVisible(setLocation);

    }

    @OptionsItem(R.id.action_resume)
    protected void resumeTorrent() {
        if (getTasksExecutor() != null)
            getTasksExecutor().resumeTorrent(torrent);
    }

    @OptionsItem(R.id.action_pause)
    protected void pauseTorrent() {
        if (getTasksExecutor() != null)
            getTasksExecutor().pauseTorrent(torrent);
    }

    @OptionsItem(R.id.action_start_direct)
    protected void startTorrentDirect() {
        if (getTasksExecutor() != null)
            getTasksExecutor().startTorrent(torrent, false);
    }

    @OptionsItem(R.id.action_start_default)
    protected void startTorrentDefault() {
        if (getTasksExecutor() != null)
            getTasksExecutor().startTorrent(torrent, false);
    }

    @OptionsItem(R.id.action_start_forced)
    protected void startTorrentForced() {
        if (getTasksExecutor() != null)
            getTasksExecutor().startTorrent(torrent, true);
    }

    @OptionsItem(R.id.action_stop)
    protected void stopTorrent() {
        if (getTasksExecutor() != null)
            getTasksExecutor().stopTorrent(torrent);
    }

    @OptionsItem(R.id.action_remove_default)
    protected void removeTorrentDefault() {
        if (getTasksExecutor() != null)
            getTasksExecutor().removeTorrent(torrent, false);
    }

    @OptionsItem(R.id.action_remove_withdata)
    protected void removeTorrentWithData() {
        if (getTasksExecutor() != null)
            getTasksExecutor().removeTorrent(torrent, true);
    }

    @OptionsItem(R.id.action_setlabel)
    protected void setLabel() {
        if (currentLabels != null) {
            SetLabelDialog.show(getActivity(), this, currentLabels);
        }
    }

    @OptionsItem(R.id.action_toggle_sequential)
    protected void toggleSequentialDownload(MenuItem menuItem) {
        if (getTasksExecutor() != null)
            getTasksExecutor().toggleSequentialDownload(torrent, !menuItem.isChecked());
    }

    @OptionsItem(R.id.action_toggle_firstlastpiece)
    protected void toggleFirstLastPieceDownload(MenuItem menuItem) {
        if (getTasksExecutor() != null)
            getTasksExecutor().toggleFirstLastPieceDownload(torrent, !menuItem.isChecked());
    }

    @OptionsItem(R.id.action_forcerecheck)
    protected void setForceRecheck() {
        if (getTasksExecutor() != null)
            getTasksExecutor().forceRecheckTorrent(torrent);
    }

    @OptionsItem(R.id.action_updatetrackers)
    protected void updateTrackers() {
        if (torrentDetails == null) {
            SnackbarManager.show(Snackbar.with(getActivity()).text(R.string.error_stillloadingdetails));
            return;
        }
        SetTrackersDialog.show(getActivity(), this, torrentDetails.getTrackersText());
    }

    @OptionsItem(R.id.action_changelocation)
    protected void changeStorageLocation() {
        SetStorageLocationDialog.show(getActivity(), this, torrent.getLocationDir());
    }

    @Override
    public void onLabelPicked(String newLabel) {
        if (torrent == null) {
            return;
        }
        if (getTasksExecutor() != null)
            getTasksExecutor().updateLabel(torrent, newLabel);
    }

    @Override
    public void onTrackersUpdated(List<String> updatedTrackers) {
        if (torrent == null) {
            return;
        }
        if (getTasksExecutor() != null)
            getTasksExecutor().updateTrackers(torrent, updatedTrackers);
    }

    @Override
    public void onStorageLocationUpdated(String newLocation) {
        if (torrent == null) {
            return;
        }
        if (getTasksExecutor() != null)
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

    /**
     * Returns the object responsible for executing torrent tasks against a connected server
     *
     * @return The executor for tasks on some torrent
     */
    private TorrentTasksExecutor getTasksExecutor() {
        // NOTE: Assumes the activity implements all the required torrent tasks
        return (TorrentTasksExecutor) getActivity();
    }

}
