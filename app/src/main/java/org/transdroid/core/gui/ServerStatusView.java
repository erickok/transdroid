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
package org.transdroid.core.gui;

import java.util.List;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.navigation.SetTransferRatesDialog;
import org.transdroid.core.gui.navigation.SetTransferRatesDialog.OnRatesPickedListener;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.util.FileSizeConverter;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.keyboardsurfer.android.widget.crouton.Crouton;

@EViewGroup(resName = "actionbar_serverstatus")
public class ServerStatusView extends RelativeLayout implements OnRatesPickedListener {

	@ViewById
	protected TextView downcountText, upcountText, downcountSign, upcountSign, downspeedText, upspeedText;
	@ViewById
	protected View speedswrapperLayout;
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
	 * @param dormantAsInactive 
	 */
	public void update(List<Torrent> torrents, boolean dormantAsInactive) {

		if (torrents == null) {
			downcountText.setText(null);
			upcountText.setText(null);
			downspeedText.setText(null);
			upspeedText.setText(null);
			downcountSign.setVisibility(View.INVISIBLE);
			upcountSign.setVisibility(View.INVISIBLE);
			speedswrapperLayout.setOnClickListener(null);
		}

		int downcount = 0, upcount = 0, downspeed = 0, upspeed = 0;
		for (Torrent torrent : torrents) {

			// Downloading torrents count towards downloads and uploads, seeding torrents towards uploads
			if (torrent.isDownloading(dormantAsInactive)) {
				downcount++;
				upcount++;
			} else if (torrent.isSeeding(dormantAsInactive)) {
				upcount++;
			}
			downspeed += torrent.getRateDownload();
			upspeed += torrent.getRateUpload();

		}

		downcountText.setText(Integer.toString(downcount));
		upcountText.setText(Integer.toString(upcount));
		downspeedText.setText(FileSizeConverter.getSize(downspeed) + "/s");
		upspeedText.setText(FileSizeConverter.getSize(upspeed) + "/s");
		downcountSign.setVisibility(View.VISIBLE);
		upcountSign.setVisibility(View.VISIBLE);
		speedswrapperLayout.setOnClickListener(onStartDownPickerClicked);

	}

	private OnClickListener onStartDownPickerClicked = new OnClickListener() {
		public void onClick(View v) {
			new SetTransferRatesDialog().setOnRatesPickedListener(ServerStatusView.this).show(
					activity.getFragmentManager(), "SetTransferRatesDialog");
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
