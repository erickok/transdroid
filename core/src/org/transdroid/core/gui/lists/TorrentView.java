package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.marvinlabs.widget.CheckableRelativeLayout;

/**
 * View that represents some {@link Torrent} object and displays progress, status, speeds, etc.
 * @author Eric Kok
 */
@EViewGroup(resName="list_item_torrent")
public class TorrentView extends CheckableRelativeLayout {

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
		nameText.setText(torrent.getName());
		ratioText.setText(local.getProgressEtaRatioText(getResources()));
		progressText.setText(local.getProgressSizeText(getResources(), false));
		speedText.setText(local.getProgressSpeedText(getResources()));
		peersText.setText(local.getProgressConnectionText(getResources()));
		torrentProgressbar.setProgress((int) (torrent.getDownloadedPercentage() * 100));
		torrentProgressbar.setActive(torrent.canPause());;
		torrentProgressbar.setError(torrent.getStatusCode() == TorrentStatus.Error);
		// TODO: Implement per-torrent priority and set priorityImage
		priorityImage.setVisibility(View.INVISIBLE);
	}

}
