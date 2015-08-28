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
package org.transdroid.core.gui.navigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.transdroid.R;
import org.transdroid.core.app.settings.SystemSettings_;

public class SetTransferRatesDialog {

	/**
	 * A dialog fragment that allow picking of maximum download and upload transfer rates as well as the resetting of these values.
	 * @param context The activity context that opens (and owns) this dialog
	 * @param onRatesPickedListener The callback for results in this dialog (with newly selected values or a reset)
	 */
	public static void show(final Context context, final OnRatesPickedListener onRatesPickedListener) {

		View transferRatesLayout = LayoutInflater.from(context).inflate(R.layout.dialog_transferrates, null);
		final TextView maxSpeedDown = (TextView) transferRatesLayout.findViewById(R.id.maxspeeddown_text);
		final TextView maxSpeedUp = (TextView) transferRatesLayout.findViewById(R.id.maxspeedup_text);

		MaterialDialog dialog = new MaterialDialog.Builder(context).customView(transferRatesLayout, false).positiveText(R.string.status_update)
				.neutralText(R.string.status_maxspeed_reset).negativeText(android.R.string.cancel).callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						int maxDown = -1, maxUp = -1;
						try {
							maxDown = Integer.parseInt(maxSpeedDown.getText().toString());
							maxUp = Integer.parseInt(maxSpeedUp.getText().toString());
						} catch (NumberFormatException e) {
							// Impossible as we only input via the number buttons
						}
						if (maxDown <= 0 || maxUp <= 0) {
							onRatesPickedListener.onInvalidNumber();
							return;
						}
						onRatesPickedListener.onRatesPicked(maxDown, maxUp);
					}

					@Override
					public void onNeutral(MaterialDialog dialog) {
						onRatesPickedListener.resetRates();
					}
				}).theme(SystemSettings_.getInstance_(context).getMaterialDialogtheme()).build();

		bindButtons(dialog.getCustomView(), maxSpeedDown, R.id.down1Button, R.id.down2Button, R.id.down3Button, R.id.down4Button, R.id.down5Button,
				R.id.down6Button, R.id.down7Button, R.id.down8Button, R.id.down9Button, R.id.down0Button);
		bindButtons(dialog.getCustomView(), maxSpeedUp, R.id.up1Button, R.id.up2Button, R.id.up3Button, R.id.up4Button, R.id.up5Button,
				R.id.up6Button, R.id.up7Button, R.id.up8Button, R.id.up9Button, R.id.up0Button);

		dialog.show();

	}

	private static void bindButtons(View transferRatesContent, View numberView, int... buttonResource) {
		for (int i : buttonResource) {
			// Keep the relevant number as reference in the view tag and bind the click listerner
			transferRatesContent.findViewById(i).setTag(numberView);
			transferRatesContent.findViewById(i).setOnClickListener(onNumberClicked);
		}
	}

	private static OnClickListener onNumberClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Append the text contents of the button itself as text to the current number (as reference in the view's
			// tag)
			TextView numberView = (TextView) v.getTag();
			if (numberView.getText().toString().equals(v.getContext().getString(R.string.status_maxspeed_novalue))) {
				numberView.setText("");
			}
			numberView.setText(numberView.getText().toString() + ((Button) v).getText().toString());
		}
	};

	/**
	 * Listener interface to the user having picked or wanting to resets the current maximum transfer speeds;
	 */
	public interface OnRatesPickedListener {
		void onRatesPicked(int maxDownloadSpeed, int maxUploadSpeed);

		void resetRates();

		void onInvalidNumber();
	}

}
