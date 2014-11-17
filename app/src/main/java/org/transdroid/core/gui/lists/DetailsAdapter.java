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
package org.transdroid.core.gui.lists;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.R;
import org.transdroid.core.gui.navigation.*;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;

import android.content.Context;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * List adapter that holds a header view showing torrent details and show the list list contained by the torrent.
 * @author Eric Kok
 */
public class DetailsAdapter extends MergeAdapter {

	private ViewHolderAdapter torrentDetailsViewAdapter = null;
	private TorrentDetailsView torrentDetailsView = null;
	private ViewHolderAdapter trackersSeparatorAdapter = null;
	private SimpleListItemAdapter trackersAdapter = null;
	private ViewHolderAdapter errorsSeparatorAdapter = null;
	private SimpleListItemAdapter errorsAdapter = null;
	private ViewHolderAdapter torrentFilesSeparatorAdapter = null;
	private TorrentFilesAdapter torrentFilesAdapter = null;

	public DetailsAdapter(Context context) {
		// Immediately bind the adapters, or the MergeAdapter will not be able to determine the view types and instead
		// display nothing at all

		// Torrent details header
		torrentDetailsView = TorrentDetailsView_.build(context);
		torrentDetailsViewAdapter = new ViewHolderAdapter(torrentDetailsView);
		torrentDetailsViewAdapter.setViewEnabled(false);
		torrentDetailsViewAdapter.setViewVisibility(View.GONE);
		addAdapter(torrentDetailsViewAdapter);

		// Tracker errors
		errorsSeparatorAdapter = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
				context.getString(R.string.status_errors)));
		errorsSeparatorAdapter.setViewEnabled(false);
		errorsSeparatorAdapter.setViewVisibility(View.GONE);
		addAdapter(errorsSeparatorAdapter);
		this.errorsAdapter = new SimpleListItemAdapter(context, new ArrayList<SimpleListItem>());
		this.errorsAdapter.setAutoLinkMask(Linkify.WEB_URLS);
		addAdapter(errorsAdapter);

		// Trackers
		trackersSeparatorAdapter = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
				context.getString(R.string.status_trackers)));
		trackersSeparatorAdapter.setViewEnabled(false);
		trackersSeparatorAdapter.setViewVisibility(View.GONE);
		addAdapter(trackersSeparatorAdapter);
		this.trackersAdapter = new SimpleListItemAdapter(context, new ArrayList<SimpleListItem>());
		addAdapter(trackersAdapter);

		// Torrent files
		torrentFilesSeparatorAdapter = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
				context.getString(R.string.status_files)));
		torrentFilesSeparatorAdapter.setViewEnabled(false);
		torrentFilesSeparatorAdapter.setViewVisibility(View.GONE);
		addAdapter(torrentFilesSeparatorAdapter);
		this.torrentFilesAdapter = new TorrentFilesAdapter(context, new ArrayList<TorrentFile>());
		addAdapter(torrentFilesAdapter);

	}

	/**
	 * Update the torrent data in the details header of this merge adapter
	 * @param torrent The torrent for which detailed data is shown
	 */
	public void updateTorrent(Torrent torrent) {
		torrentDetailsView.update(torrent);
		torrentDetailsViewAdapter.setViewVisibility(torrent == null ? View.GONE : View.VISIBLE);
	}

	/**
	 * Update the list of files contained in this torrent
	 * @param torrentFiles The new list of files, or null if the list and header should be hidden
	 */
	public void updateTorrentFiles(List<TorrentFile> torrentFiles) {
		if (torrentFiles == null) {
			torrentFilesAdapter.update(new ArrayList<TorrentFile>());
			torrentFilesSeparatorAdapter.setViewVisibility(View.GONE);
		} else {
			torrentFilesAdapter.update(torrentFiles);
			torrentFilesSeparatorAdapter.setViewVisibility(View.VISIBLE);
		}
	}

	/**
	 * Update the list of trackers
	 * @param trackers The new list of trackers known for this torrent, or null if the list and header should be hidden
	 */
	public void updateTrackers(List<? extends SimpleListItem> trackers) {
		if (trackers == null || trackers.isEmpty()) {
			trackersAdapter.update(new ArrayList<SimpleListItemAdapter.SimpleStringItem>());
			trackersSeparatorAdapter.setViewVisibility(View.GONE);
		} else {
			trackersAdapter.update(trackers);
			trackersSeparatorAdapter.setViewVisibility(View.VISIBLE);
		}
	}

	/**
	 * Update the list of errors
	 * @param errors The new list of errors known for this torrent, or null if the list and header should be hidden
	 */
	public void updateErrors(List<? extends SimpleListItem> errors) {
		if (errors == null || errors.isEmpty()) {
			errorsAdapter.update(new ArrayList<SimpleListItemAdapter.SimpleStringItem>());
			errorsSeparatorAdapter.setViewVisibility(View.GONE);
		} else {
			errorsAdapter.update(errors);
			errorsSeparatorAdapter.setViewVisibility(View.VISIBLE);
		}
	}

	/**
	 * Clear currently visible torrent, including header and shown lists
	 */
	public void clear() {
		updateTorrent(null);
		updateTorrentFiles(null);
		updateErrors(null);
		updateTrackers(null);
	}

	protected static class TorrentFilesAdapter extends BaseAdapter {

		private final Context context;
		private List<TorrentFile> items;

		public TorrentFilesAdapter(Context context, List<TorrentFile> items) {
			this.context = context;
			this.items = items;
		}

		/**
		 * Allows updating of the full data list underlying this adapter, replacing all items
		 * @param newItems The new list of files to display
		 */
		public void update(List<TorrentFile> newItems) {
			this.items = newItems;
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
