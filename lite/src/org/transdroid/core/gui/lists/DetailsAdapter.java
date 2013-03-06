package org.transdroid.core.gui.lists;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;
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
@EBean
public class DetailsAdapter extends MergeAdapter {

	@RootContext
	protected Context context;
	private TorrentDetailsView torrentDetailsView = null;
	private TorrentFilesAdapter torrentFilesAdapter = null;
	private SimpleListItemAdapter trackersAdapter = null;
	private SimpleListItemAdapter errorsAdapter = null;

	/**
	 * Update the torrent data in the details header of this merge adapter
	 * @param torrent The torrent for which detailed data is shown
	 */
	public void updateTorrent(Torrent torrent) {
		if (this.torrentDetailsView == null) {
			torrentDetailsView = TorrentDetailsView_.build(context);
			addView(torrentDetailsView, false);
		}
		torrentDetailsView.update(torrent);
	}
	
	/**
	 * Update the list of files contained in this torrent
	 * @param torrentFiles The new list of files
	 */
	public void updateTorrentFiles(List<TorrentFile> torrentFiles) {
		if (this.torrentFilesAdapter == null && torrentFiles != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.status_files)), false);
			this.torrentFilesAdapter = new TorrentFilesAdapter(context, torrentFiles);
			addAdapter(torrentFilesAdapter);
		} else if (this.torrentFilesAdapter != null && torrentFiles != null) {
			this.torrentFilesAdapter.update(torrentFiles);
		} else {
			this.torrentFilesAdapter = null;
		}
	}

	/**
	 * Update the list of trackers
	 * @param trackers The new list of trackers known for this torrent
	 */
	public void updateTrackers(List<? extends SimpleListItem> trackers) {
		if (this.trackersAdapter == null && trackers != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.status_trackers)), false);
			this.trackersAdapter = new SimpleListItemAdapter(context, trackers);
			addAdapter(trackersAdapter);
		} else if (this.trackersAdapter != null && trackers != null) {
			this.trackersAdapter.update(trackers);
		} else {
			this.trackersAdapter = null;
		}
	}

	/**
	 * Update the list of errors
	 * @param errors The new list of errors known for this torrent
	 */
	public void updateErrors(List<? extends SimpleListItem> errors) {
		if (this.errorsAdapter == null && errors != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.status_errors)), false);
			this.errorsAdapter = new SimpleListItemAdapter(context, errors);
			addAdapter(errorsAdapter);
		} else if (this.errorsAdapter != null && errors != null) {
			this.errorsAdapter.update(errors);
		} else {
			this.errorsAdapter = null;
		}
	}

	protected class TorrentFilesAdapter extends BaseAdapter {

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
