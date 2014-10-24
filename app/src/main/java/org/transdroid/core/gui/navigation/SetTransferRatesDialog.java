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

import java.security.InvalidParameterException;

import org.transdroid.R;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * A dialog fragment that allow picking of maximum download and upload transfer rates as well as the resetting of these
 * values.
 * @author Eric Kok
 */
public class SetTransferRatesDialog extends DialogFragment {

	private OnRatesPickedListener onRatesPickedListener = null;
	private TextView maxSpeedDown, maxSpeedUp;

	public SetTransferRatesDialog() {
		setRetainInstance(true);
	}

	/**
	 * Sets the callback for results in this dialog (with newly selected values or a reset).
	 * @param onRatesPickedListener The event listener to this dialog
	 * @return This dialog, for method chaining
	 */
	public SetTransferRatesDialog setOnRatesPickedListener(OnRatesPickedListener onRatesPickedListener) {
		this.onRatesPickedListener = onRatesPickedListener;
		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (onRatesPickedListener == null)
			throw new InvalidParameterException(
					"Please first set the callback listener using setOnRatesPickedListener before opening the dialog.");
		final View transferRatesContent = getActivity().getLayoutInflater().inflate(R.layout.dialog_transferrates,
				null, false);
		maxSpeedDown = (TextView) transferRatesContent.findViewById(R.id.maxspeeddown_text);
		maxSpeedUp = (TextView) transferRatesContent.findViewById(R.id.maxspeedup_text);
		bindButtons(transferRatesContent, maxSpeedDown, R.id.down1Button, R.id.down2Button, R.id.down3Button,
				R.id.down4Button, R.id.down5Button, R.id.down6Button, R.id.down7Button, R.id.down8Button,
				R.id.down9Button, R.id.down0Button);
		bindButtons(transferRatesContent, maxSpeedUp, R.id.up1Button, R.id.up2Button, R.id.up3Button, R.id.up4Button,
				R.id.up5Button, R.id.up6Button, R.id.up7Button, R.id.up8Button, R.id.up9Button, R.id.up0Button);
		((Button) transferRatesContent.findViewById(R.id.ok_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int maxDown = -1, maxUp = -1;
				try {
					maxDown = Integer.parseInt(maxSpeedDown.getText().toString());
					maxUp = Integer.parseInt(maxSpeedUp.getText().toString());
				} catch (NumberFormatException e) {
				}
				if (maxDown <= 0 || maxUp <= 0) {
					onRatesPickedListener.onInvalidNumber();
				}
				onRatesPickedListener.onRatesPicked(maxDown, maxUp);
				dismiss();
			}
		});
		((Button) transferRatesContent.findViewById(R.id.reset_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onRatesPickedListener.resetRates();
				dismiss();
			}
		});
		((Button) transferRatesContent.findViewById(R.id.cancel_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		Dialog dialog = new Dialog(getActivity());
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(transferRatesContent);
		return dialog;
	}

	private void bindButtons(View transferRatesContent, View numberView, int... buttonResource) {
		for (int i : buttonResource) {
			// Keep the relevant number as reference in the view tag and bind the click listerner
			transferRatesContent.findViewById(i).setTag(numberView);
			transferRatesContent.findViewById(i).setOnClickListener(onNumberClicked);
		}
	}

	private android.view.View.OnClickListener onNumberClicked = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// Append the text contents of the button itself as text to the current number (as reference in the view's
			// tag)
			TextView numberView = (TextView) v.getTag();
			if (numberView.getText().toString().equals(getString(R.string.status_maxspeed_novalue)))
				numberView.setText("");
			numberView.setText(numberView.getText().toString() + ((Button) v).getText().toString());
		}
	};

	/**
	 * Listener interface to the user having picked or wanting to resets the current maximum transfer speeds;
	 */
	public interface OnRatesPickedListener {
		public void onRatesPicked(int maxDownloadSpeed, int maxUploadSpeed);

		public void resetRates();

		public void onInvalidNumber();
	}

}
