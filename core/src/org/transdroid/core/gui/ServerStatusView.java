package org.transdroid.core.gui;

import java.util.List;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.navigation.SetTransferRatesDialog;
import org.transdroid.core.gui.navigation.SetTransferRatesDialog.OnRatesPickedListener;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.util.FileSizeConverter;

import de.keyboardsurfer.android.widget.crouton.Crouton;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

@EViewGroup(resName = "actionbar_serverstatus")
public class ServerStatusView extends RelativeLayout implements OnRatesPickedListener {

	@ViewById
	protected TextView downcountText, upcountText, downcountSign, upcountSign, downspeedText, upspeedText;
	private TorrentsActivity activity;

	public ServerStatusView(Context context) {
		super(context);
	}

	public ServerStatusView(TorrentsActivity activity) {
		super(activity);
		this.activity = activity;
	}

	/**
	 * Updates the statistics as shown in the action bar through this server status view.
	 * @param torrents The most recently received list of torrents
	 */
	public void update(List<Torrent> torrents) {

		if (torrents == null) {
			downcountText.setText(null);
			upcountText.setText(null);
			downspeedText.setText(null);
			upspeedText.setText(null);
			downcountSign.setVisibility(View.INVISIBLE);
			upcountSign.setVisibility(View.INVISIBLE);
			setClickListener(null);
		}

		int downcount = 0, upcount = 0, downspeed = 0, upspeed = 0;
		for (Torrent torrent : torrents) {

			// Downloading torrents count towards downloads and uploads, seeding torrents towards uploads
			if (torrent.getStatusCode() == TorrentStatus.Downloading) {
				downcount++;
				upcount++;
			} else if (torrent.getStatusCode() == TorrentStatus.Seeding) {
				upcount++;
			}
			downspeed += torrent.getRateDownload();
			upspeed += torrent.getRateUpload();

		}

		downcountText.setText(Integer.toString(downcount));
		upcountText.setText(Integer.toString(upcount));
		downspeedText.setText(FileSizeConverter.getSize(downspeed));
		upspeedText.setText(FileSizeConverter.getSize(upspeed));
		downcountSign.setVisibility(View.VISIBLE);
		upcountSign.setVisibility(View.VISIBLE);
		setClickListener(onStartDownPickerClicked);

	}

	private void setClickListener(OnClickListener onClick) {
		downcountText.setOnClickListener(onClick);
		upcountText.setOnClickListener(onClick);
		downspeedText.setOnClickListener(onClick);
		upspeedText.setOnClickListener(onClick);
		downcountSign.setOnClickListener(onClick);
		upcountSign.setOnClickListener(onClick);
	}

	private OnClickListener onStartDownPickerClicked = new OnClickListener() {
		public void onClick(View v) {
			new SetTransferRatesDialog().setOnRatesPickedListener(ServerStatusView.this).show(
					activity.getSupportFragmentManager(), "SetTransferRatesDialog");
		}
	};

	@Override
	public void onRatesPicked(int maxDownloadSpeed, int maxUploadSpeed) {
		activity.updateMaxSpeeds(maxDownloadSpeed, maxUploadSpeed);
	}

	@Override
	public void resetRates() {
		activity.updateMaxSpeeds(null, null);
	}

	@Override
	public void onInvalidNumber() {
		Crouton.showText(activity, R.string.error_notanumber, NavigationHelper.CROUTON_ERROR_STYLE);
	}

}
