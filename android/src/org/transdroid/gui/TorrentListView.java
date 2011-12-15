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

import org.transdroid.R;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A view that shows the torrent data as a list item.
 * 
 * @author erickok
 *
 */
public class TorrentListView extends LinearLayout {

	private ViewHolder views;
	
	/**
	 * Constructs a view that can display torrent data (to use in a list) and sets the display data
	 * @param context The activity context
	 * @param torrent The torrent info to show the data for
	 */
	public TorrentListView(Context context, Torrent torrent, boolean withAvailability) {
		super(context);
		addView(inflate(context, R.layout.list_item_torrent, null));
		
		setData(torrent, withAvailability);
	}

	/**
	 * Sets the actual texts and images to the visible widgets (fields)
	 */
	public void setData(Torrent tor, boolean withAvailability) {
		LocalTorrent torrent = LocalTorrent.fromTorrent(tor);
		if (views == null) {
			views = new ViewHolder();
			views.name = (TextView) findViewById(R.id.name);
			views.progressSize = (TextView) findViewById(R.id.progress_size);
			views.progressEtaRatio = (TextView) findViewById(R.id.progress_eta_ratio);
			views.pb = (TorrentProgressBar) findViewById(R.id.progressbar);
			views.progressPeers = (TextView) findViewById(R.id.progress_peers);
			views.progressSpeed = (TextView) findViewById(R.id.progress_speed);
		}
		views.name.setText(tor.getName());
		views.progressSize.setText(Html.fromHtml(torrent.getProgressSizeText(getResources(), false)), TextView.BufferType.SPANNABLE);
		views.progressEtaRatio.setText(Html.fromHtml(torrent.getProgressEtaRatioText(getResources())), TextView.BufferType.SPANNABLE);
		views.pb.setProgress((int)(tor.getDownloadedPercentage() * 100));
		views.pb.setActive(tor.canPause());
		views.pb.setError(tor.getStatusCode() == TorrentStatus.Error);
		views.progressPeers.setText(Html.fromHtml(torrent.getProgressConnectionText(getResources())), TextView.BufferType.SPANNABLE);
		views.progressSpeed.setText(Html.fromHtml(torrent.getProgressSpeedText(getResources())), TextView.BufferType.SPANNABLE);

	}
	
	/**
	 * Used to further optimize the getting of Views
	 */
	private static class ViewHolder {
		TextView name;
		TextView progressSize;
		TextView progressEtaRatio;
		TorrentProgressBar pb;
		TextView progressPeers;
		TextView progressSpeed;
	}

}
