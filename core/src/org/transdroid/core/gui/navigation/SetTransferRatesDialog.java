package org.transdroid.core.gui.navigation;

import java.security.InvalidParameterException;

import org.transdroid.core.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
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
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.status_maxspeed).setView(transferRatesContent)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
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
					}
				}).setNeutralButton(R.string.status_maxspeed_reset, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onRatesPickedListener.resetRates();
					}
				}).setNegativeButton(android.R.string.cancel, null).create();
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
			if (numberView.getText().equals("-"))
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
