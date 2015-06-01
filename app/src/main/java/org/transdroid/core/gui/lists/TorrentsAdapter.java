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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.daemon.Torrent;

import java.util.ArrayList;

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
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (torrents == null) {
			return 0;
		}
		return torrents.size();
	}

	@Override
	public Torrent getItem(int position) {
		if (torrents == null) {
			return null;
		}
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
