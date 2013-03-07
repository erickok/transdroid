package org.transdroid.core.gui.lists;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.core.R;
import org.transdroid.core.gui.navigation.FilterSeparatorView;
import org.transdroid.core.gui.navigation.FilterSeparatorView_;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.commonsware.cwac.merge.MergeAdapter;

/**
 * List adapter that holds a header view showing torrent details and show the list list contained by the torrent.
 * @author Eric Kok
 */
public class DetailsAdapter extends MergeAdapter {

	private TorrentDetailsView torrentDetailsView = null;
	private FilterSeparatorView trackersSeparatorView = null;
	private SimpleListItemAdapter trackersAdapter = null;
	private FilterSeparatorView errorsSeparatorView = null;
	private SimpleListItemAdapter errorsAdapter = null;
	private FilterSeparatorView torrentFilesSeparatorView = null;
	private TorrentFilesAdapter torrentFilesAdapter = null;

	public DetailsAdapter(Context context) {
		// Immediately bind the adapters, or the MergeAdapter will not be able to determine the view types and instead
		// display nothing at all

		// Torrent details header
		torrentDetailsView = TorrentDetailsView_.build(context);
		torrentDetailsView.setVisibility(View.GONE);
		addView(torrentDetailsView, true);
		
		// Trackers
		trackersSeparatorView = FilterSeparatorView_.build(context).setText(context.getString(R.string.status_trackers));
		trackersSeparatorView.setVisibility(View.GONE);
		addView(trackersSeparatorView, true);
		this.trackersAdapter = new SimpleListItemAdapter(context, new ArrayList<SimpleListItem>());
		addAdapter(trackersAdapter);
		
		// Tracker errors
		errorsSeparatorView = FilterSeparatorView_.build(context).setText(context.getString(R.string.status_errors));
		errorsSeparatorView.setVisibility(View.GONE);
		addView(errorsSeparatorView, true);
		this.errorsAdapter = new SimpleListItemAdapter(context, new ArrayList<SimpleListItem>());
		addAdapter(errorsAdapter);
		
		// Torrent files
		torrentFilesSeparatorView = FilterSeparatorView_.build(context).setText(context.getString(R.string.status_files));
		torrentFilesSeparatorView.setVisibility(View.GONE);
		addView(torrentFilesSeparatorView, true);
		this.torrentFilesAdapter = new TorrentFilesAdapter(context, new ArrayList<TorrentFile>());
		addAdapter(torrentFilesAdapter);
		
	}
	
	/**
	 * Update the torrent data in the details header of this merge adapter
	 * @param torrent The torrent for which detailed data is shown
	 */
	public void updateTorrent(Torrent torrent) {
		torrentDetailsView.update(torrent);
		torrentDetailsView.setVisibility(torrent == null? View.GONE: View.VISIBLE);
	}
	
	/**
	 * Update the list of files contained in this torrent
	 * @param torrentFiles The new list of files, or null if the list and header should be hidden
	 */
	public void updateTorrentFiles(List<TorrentFile> torrentFiles) {
		if (torrentFiles == null) {
			torrentFilesAdapter.update(new ArrayList<TorrentFile>());
			torrentFilesSeparatorView.setVisibility(View.GONE);
		} else {
			torrentFilesAdapter.update(torrentFiles);
			torrentFilesSeparatorView.setVisibility(View.GONE);
		}
	}

	/**
	 * Update the list of trackers
	 * @param trackers The new list of trackers known for this torrent, or null if the list and header should be hidden
	 */
	public void updateTrackers(List<? extends SimpleListItem> trackers) {
		if (trackers == null) {
			trackersAdapter.update(new ArrayList<SimpleListItemAdapter.SimpleStringItem>());
			trackersSeparatorView.setVisibility(View.GONE);
		} else {
			trackersAdapter.update(trackers);
			trackersSeparatorView.setVisibility(View.GONE);
		}
	}

	/**
	 * Update the list of errors
	 * @param errors The new list of errors known for this torrent, or null if the list and header should be hidden
	 */
	public void updateErrors(List<? extends SimpleListItem> errors) {
		if (errors == null) {
			errorsAdapter.update(new ArrayList<SimpleListItemAdapter.SimpleStringItem>());
			errorsSeparatorView.setVisibility(View.GONE);
		} else {
			errorsAdapter.update(errors);
			errorsSeparatorView.setVisibility(View.GONE);
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
