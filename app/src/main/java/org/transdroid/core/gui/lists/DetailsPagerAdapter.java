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
package org.transdroid.core.gui.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.transdroid.R;
import org.transdroid.daemon.Peer;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.Tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ViewPager2} adapter that hosts the three swipeable detail pages (Files, Peers and
 * Trackers), each a {@link ListView} wrapped in a {@link SwipeRefreshLayout}. The page views are
 * built once and reused, so the hosting fragment can keep stable references to push data and attach
 * the multi-select contextual action bar to the Files list.
 *
 * @author Eric Kok
 */
public class DetailsPagerAdapter extends RecyclerView.Adapter<DetailsPagerAdapter.PageViewHolder> {

    public static final int PAGE_FILES = 0;
    public static final int PAGE_PEERS = 1;
    public static final int PAGE_TRACKERS = 2;
    private static final int PAGE_COUNT = 3;

    private final View[] pages = new View[PAGE_COUNT];
    private final ListView filesList;
    private final ListView peersList;
    private final ListView trackersList;
    private final TextView peersEmpty;
    private final FilesAdapter filesAdapter;
    private final PeersAdapter peersAdapter;
    private final TrackersAdapter trackersAdapter;

    public DetailsPagerAdapter(Context context, GeoIpHelper geoIpHelper, Runnable refreshCallback) {

        LayoutInflater inflater = LayoutInflater.from(context);

        // Files page (the only multi-selectable one, to preserve the priority CAB)
        pages[PAGE_FILES] = inflater.inflate(R.layout.page_details_list, null);
        filesList = pages[PAGE_FILES].findViewById(R.id.page_list);
        TextView filesEmpty = pages[PAGE_FILES].findViewById(R.id.page_empty);
        filesEmpty.setText(R.string.status_nofiles);
        filesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        filesAdapter = new FilesAdapter(context, new ArrayList<>());
        filesList.setAdapter(filesAdapter);
        filesList.setEmptyView(filesEmpty);
        bindRefresh(pages[PAGE_FILES], refreshCallback);

        // Peers page
        pages[PAGE_PEERS] = inflater.inflate(R.layout.page_details_list, null);
        peersList = pages[PAGE_PEERS].findViewById(R.id.page_list);
        peersEmpty = pages[PAGE_PEERS].findViewById(R.id.page_empty);
        peersEmpty.setText(R.string.status_nopeers);
        peersAdapter = new PeersAdapter(context, geoIpHelper, new ArrayList<>());
        peersList.setAdapter(peersAdapter);
        peersList.setEmptyView(peersEmpty);
        peersList.setFastScrollEnabled(true);
        bindRefresh(pages[PAGE_PEERS], refreshCallback);

        // Trackers page
        pages[PAGE_TRACKERS] = inflater.inflate(R.layout.page_details_list, null);
        trackersList = pages[PAGE_TRACKERS].findViewById(R.id.page_list);
        TextView trackersEmpty = pages[PAGE_TRACKERS].findViewById(R.id.page_empty);
        trackersEmpty.setText(R.string.status_notrackers);
        trackersAdapter = new TrackersAdapter(context, new ArrayList<>());
        trackersList.setAdapter(trackersAdapter);
        trackersList.setEmptyView(trackersEmpty);
        trackersList.setFastScrollEnabled(true);
        bindRefresh(pages[PAGE_TRACKERS], refreshCallback);
    }

    private void bindRefresh(View page, Runnable refreshCallback) {
        SwipeRefreshLayout swipe = page.findViewById(R.id.page_swipe);
        swipe.setOnRefreshListener(() -> {
            if (refreshCallback != null) {
                refreshCallback.run();
            }
            swipe.setRefreshing(false); // We use our own loading indicator
        });
    }

    /**
     * The Files list view, exposed so the fragment can attach its multi-choice contextual action bar.
     */
    public ListView getFilesList() {
        return filesList;
    }

    public void setFiles(List<TorrentFile> files) {
        filesAdapter.update(files);
    }

    public void setPeers(List<Peer> peers, boolean supported) {
        peersEmpty.setText(supported ? R.string.status_nopeers : R.string.status_peers_unsupported);
        peersAdapter.update(supported ? peers : new ArrayList<>());
    }

    public void setTrackers(List<Tracker> trackers) {
        trackersAdapter.update(trackers);
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View page = pages[viewType];
        if (page.getParent() instanceof ViewGroup) {
            ((ViewGroup) page.getParent()).removeView(page);
        }
        page.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new PageViewHolder(page);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        // Nothing to bind; pages are static and updated through the setX methods
    }

    @Override
    public int getItemViewType(int position) {
        // One distinct type per page so each pre-built page maps to a single holder
        return position;
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        PageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * Simple adapter that renders {@link TorrentFile} rows; mirrors the former inner adapter of
     * {@link DetailsAdapter} so the Files page keeps the same look and multi-select behaviour.
     */
    private static class FilesAdapter extends BaseAdapter {

        private final Context context;
        private List<TorrentFile> items;

        FilesAdapter(Context context, List<TorrentFile> items) {
            this.context = context;
            this.items = items;
        }

        void update(List<TorrentFile> newItems) {
            this.items = newItems == null ? new ArrayList<>() : newItems;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public TorrentFile getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TorrentFileView torrentFileView;
            if (convertView == null) {
                torrentFileView = TorrentFileView_.build(context);
            } else {
                torrentFileView = (TorrentFileView) convertView;
            }
            torrentFileView.bind(getItem(position));
            return torrentFileView;
        }

    }

}
