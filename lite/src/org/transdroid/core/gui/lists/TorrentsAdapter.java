package org.transdroid.core.gui.lists;

import java.util.ArrayList;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.gui.lists.TorrentView_;
import org.transdroid.daemon.Torrent;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Adapter that contains a list of torrent objects to show.
 * @author Eric Kok
 */
@EBean
public class TorrentsAdapter extends BaseAdapter {
	
	private ArrayList<Torrent> torrents = null;
	
	@RootContext
	protected Context context;

	/**
	 * Allows updating the full internal list of torrents at once, replacing the old list
	 * @param newTorrents The new list of torrent objects
	 */
	public void update(ArrayList<Torrent> newTorrents) {
		this.torrents = newTorrents;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (torrents == null)
			return 0;
		return torrents.size();
	}

	@Override
	public Torrent getItem(int position) {
		if (torrents == null)
			return null;
		return torrents.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TorrentView torrentView;
		if (convertView == null) {
			torrentView = TorrentView_.build(context);
		} else {
			torrentView = (TorrentView) convertView;
		}
		torrentView.bind(getItem(position));
		return torrentView;
	}

}
