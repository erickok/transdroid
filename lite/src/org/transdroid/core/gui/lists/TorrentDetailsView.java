package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.util.FileSizeConverter;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Represents a group of views that show torrent status, sizes, speeds and other details.
 * @author Eric Kok
 */
@EViewGroup(R.layout.fragment_details_header)
public class TorrentDetailsView extends RelativeLayout {

	private boolean isShowingData = false;
	
	@ViewById
	protected TextView labelText, dateaddedText, uploadedText, uploadedunitText, ratioText, upspeedText, seedersText,
			downloadedunitText, downloadedText, totalsizeText, downspeedText, leechersText, statusText;

	public TorrentDetailsView(Context context) {
		super(context);
	}

	/**
	 * Update the text fields with new/updated torrent details
	 * @param torrent The torrent for which to show details
	 */
	public void update(Torrent torrent) {

		if (torrent == null) {
			isShowingData = false;
			return;
		}

		isShowingData = true;
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
			dateaddedText.setText(getResources().getString(
					R.string.status_sincedate,
					DateUtils.getRelativeDateTimeString(getContext(), torrent.getDateAdded().getTime(),
							DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_MONTH)));
			dateaddedText.setVisibility(View.VISIBLE);
		} else {
			dateaddedText.setVisibility(View.INVISIBLE);
		}
		statusText.setText(local.getProgressStatusEta(getResources()));
		ratioText.setText(getResources().getString(R.string.status_ratio, local.getRatioString()));
		// TODO: Implement separate numbers of seeders and leechers
		seedersText.setText(getResources().getString(R.string.status_peers, torrent.getPeersSendingToUs(),
				torrent.getPeersConnected()));
		leechersText.setText(getResources().getString(R.string.status_peers, torrent.getPeersSendingToUs(),
				torrent.getPeersConnected()));
		// TODO: Add field that displays torrent errors (as opposed to tracker errors)
		// TODO: Add field that displays availability

		// Sizes and speeds texts
		totalsizeText.setText(FileSizeConverter.getSize(torrent.getTotalSize()));
		downloadedText.setText(FileSizeConverter.getSize(torrent.getDownloadedEver(), false));
		downloadedunitText.setText(FileSizeConverter.getSizeUnit(torrent.getDownloadedEver()).toString());
		uploadedText.setText(FileSizeConverter.getSize(torrent.getUploadedEver(), false));
		uploadedunitText.setText(FileSizeConverter.getSizeUnit(torrent.getUploadedEver()).toString());
		downspeedText.setText(getResources().getString(R.string.status_speed_down,
				FileSizeConverter.getSize(torrent.getRateDownload()) + "/s"));
		upspeedText.setText(getResources().getString(R.string.status_speed_up,
				FileSizeConverter.getSize(torrent.getRateUpload()) + "/s"));

	}

	public boolean isBound() {
		return isShowingData ;
	}

}
