package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * View that represents some {@link Torrent} object and displays progress, status, speeds, etc.
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_torrent2")
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
		if (torrent.getStatusCode() == TorrentStatus.Downloading
				|| (torrent.getStatusCode() == TorrentStatus.Seeding && torrent.getRateUpload() > 0)) {
			torrentProgressbar.setVisibility(View.VISIBLE);
			torrentProgressbar.setProgress((int) (torrent.getDownloadedPercentage() * 100));
			torrentProgressbar.setActive(torrent.canPause());
			torrentProgressbar.setError(torrent.getStatusCode() == TorrentStatus.Error);
			peersText.setVisibility(View.VISIBLE);
			peersText.setText(local.getProgressConnectionText(getResources()));
			speedText.setVisibility(View.VISIBLE);
			speedText.setText(local.getProgressSpeedText(getResources()));
		} else {
			torrentProgressbar.setVisibility(View.GONE);
			peersText.setVisibility(View.GONE);
			speedText.setVisibility(View.GONE);
		}
	}

}
