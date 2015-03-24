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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.util.FileSizeConverter;

/**
 * Represents a group of views that show torrent status, sizes, speeds and other details.
 * @author Eric Kok
 */
@EViewGroup(R.layout.fragment_details_header)
public class TorrentDetailsView extends RelativeLayout {

	@ViewById
	protected TextView labelText, dateaddedText, uploadedText, uploadedunitText, ratioText, upspeedText, seedersText, downloadedunitText,
			downloadedText, totalsizeText, downspeedText, leechersText, statusText;
	@ViewById
	protected TorrentStatusLayout statusLayout;

	public TorrentDetailsView(Context context) {
		super(context);
	}

	/**
	 * Update the text fields with new/updated torrent details
	 * @param torrent The torrent for which to show details
	 */
	public void update(Torrent torrent) {

		if (torrent == null) {
			return;
		}

		LocalTorrent local = LocalTorrent.fromTorrent(torrent);

		// Set label text
		if (Daemon.supportsLabels(torrent.getDaemon())) {
			if (TextUtils.isEmpty(torrent.getLabelName())) {
				labelText.setText(getResources().getString(R.string.labels_unlabeled));
			} else {
				labelText.setText(torrent.getLabelName());
			}
			labelText.setVisibility(View.VISIBLE);
		} else {
			labelText.setVisibility(View.INVISIBLE);
		}

		// Set status texts
		if (torrent.getDateAdded() != null) {
			dateaddedText.setText(getResources().getString(R.string.status_sincedate, DateUtils
							.getRelativeDateTimeString(getContext(), torrent.getDateAdded().getTime(), DateUtils.SECOND_IN_MILLIS,
									DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_MONTH)));
			dateaddedText.setVisibility(View.VISIBLE);
		} else {
			dateaddedText.setVisibility(View.INVISIBLE);
		}

		statusLayout.setStatus(torrent.getStatusCode());
		statusText.setText(getResources().getString(R.string.status_status, local.getProgressStatusEta(getResources())));
		ratioText.setText(getResources().getString(R.string.status_ratio, local.getRatioString()));
		seedersText.setText(getResources().getString(R.string.status_seeders, torrent.getSeedersConnected(), torrent.getSeedersKnown()));
		leechersText.setText(getResources().getString(R.string.status_leechers, torrent.getLeechersConnected(), torrent.getLeechersKnown()));
		// TODO: Add field that displays torrent errors (as opposed to tracker errors)
		// TODO: Add field that displays availability

		// Sizes and speeds texts
		totalsizeText.setText(getResources().getString(R.string.status_ofsize, FileSizeConverter.getSize(torrent.getTotalSize())));
		downloadedText.setText(FileSizeConverter.getSize(torrent.getDownloadedEver(), false));
		downloadedunitText.setText(FileSizeConverter.getSizeUnit(torrent.getDownloadedEver()).toString());
		uploadedText.setText(FileSizeConverter.getSize(torrent.getUploadedEver(), false));
		uploadedunitText.setText(FileSizeConverter.getSizeUnit(torrent.getUploadedEver()).toString());
		downspeedText
				.setText(getResources().getString(R.string.status_speed_down_details, FileSizeConverter.getSize(torrent.getRateDownload()) + "/s"));
		upspeedText.setText(getResources().getString(R.string.status_speed_up, FileSizeConverter.getSize(torrent.getRateUpload()) + "/s"));

	}

}
