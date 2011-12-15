/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.gui;

import java.util.List;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;
import org.transdroid.gui.util.ArrayAdapter;

import android.view.View;
import android.view.ViewGroup;

/**
 * An adapter that can be mapped to a list of torrents.
 * @author erickok
 *
 */
public class TorrentListAdapter extends ArrayAdapter<Torrent> {

	TorrentsFragment mainScreen;
	
	public TorrentListAdapter(TorrentsFragment mainScreen, List<Torrent> torrents) {
		super(mainScreen.getActivity(), torrents);
		this.mainScreen = mainScreen;
	}
	
	public View getView(int position, View convertView, ViewGroup paret) {
		if (convertView == null) {
			// Create a new view
			return new TorrentListView(getContext(), getItem(position), Daemon.supportsAvailability(mainScreen.getActiveDaemonType()));
		} else {
			// Reuse view
			TorrentListView setView = (TorrentListView) convertView;
			setView.setData(getItem(position), Daemon.supportsAvailability(mainScreen.getActiveDaemonType()));
			return setView;
		}
	}

}
