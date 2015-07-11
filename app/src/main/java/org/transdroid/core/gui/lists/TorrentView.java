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
import android.widget.ImageView;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;

/**
 * View that represents some {@link Torrent} object and displays progress, status, speeds, etc.
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_torrent)
public class TorrentView extends TorrentStatusLayout {

	@ViewById
	protected ImageView priorityImage;
	@ViewById
	protected TextView nameText, ratioText, progressText, speedText, peersText;
	@ViewById
	protected TorrentProgressBar torrentProgressbar;

	public TorrentView(Context context) {
		super(context);
	}

	public void bind(Torrent torrent) {
		LocalTorrent local = LocalTorrent.fromTorrent(torrent);
		setStatus(torrent.getStatusCode());
		nameText.setText(torrent.getName());
		progressText.setText(local.getProgressSizeText(getResources(), false));
		ratioText.setText(local.getProgressEtaRatioText(getResources()));
		// TODO: Implement per-torrent priority and set priorityImage
		priorityImage.setVisibility(View.INVISIBLE);

		// Only show status bar, peers and speed fields if relevant, i.e. when downloading or actively seeding
		if (torrent.getStatusCode() == TorrentStatus.Downloading ||
				(torrent.getStatusCode() == TorrentStatus.Seeding && torrent.getRateUpload() > 0)) {
			torrentProgressbar.setVisibility(View.VISIBLE);
			torrentProgressbar.setProgress((int) (torrent.getDownloadedPercentage() * 100));
			torrentProgressbar.setActive(torrent.canPause());
			torrentProgressbar.setError(torrent.getStatusCode() == TorrentStatus.Error);
			peersText.setVisibility(View.VISIBLE);
			peersText.setText(local.getProgressConnectionText(getResources()));
			speedText.setVisibility(View.VISIBLE);
			speedText.setText(local.getProgressSpeedText(getResources()));
		} else if (torrent.getPartDone() < 1) {
			// Not active, but also not complete, so show the status bar
			torrentProgressbar.setVisibility(View.VISIBLE);
			torrentProgressbar.setProgress((int) (torrent.getDownloadedPercentage() * 100));
			torrentProgressbar.setActive(torrent.canPause());
			torrentProgressbar.setError(torrent.getStatusCode() == TorrentStatus.Error);
			peersText.setVisibility(View.GONE);
			speedText.setVisibility(View.GONE);
		} else {
			torrentProgressbar.setVisibility(View.GONE);
			peersText.setVisibility(View.GONE);
			speedText.setVisibility(View.GONE);
		}
	}

}
