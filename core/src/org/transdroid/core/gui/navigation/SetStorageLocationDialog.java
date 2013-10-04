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

import org.transdroid.core.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;

/**
 * A dialog fragment that allows changing of the storage location by editing the path text directly.
 * @author Eric Kok
 */
public class SetStorageLocationDialog extends DialogFragment {

	private OnStorageLocationUpdatedListener onStorageLocationUpdatedListener = null;
	private String currentLocation = null;

	public SetStorageLocationDialog() {
		setRetainInstance(true);
	}

	/**
	 * Sets the callback for when the user is done updating the storage location.
	 * @param onStorageLocationUpdatedListener The event listener to this dialog
	 * @return This dialog, for method chaining
	 */
	public SetStorageLocationDialog setOnStorageLocationUpdated(
			OnStorageLocationUpdatedListener onStorageLocationUpdatedListener) {
		this.onStorageLocationUpdatedListener = onStorageLocationUpdatedListener;
		return this;
	}

	/**
	 * Sets the current storage location that will be available to the user to edit
	 * @param currentLocation The current storage location path as text
	 * @return This dialog, for method chaining
	 */
	public SetStorageLocationDialog setCurrentLocation(String currentLocation) {
		this.currentLocation = currentLocation;
		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (currentLocation == null)
			throw new InvalidParameterException(
					"Please first set the current trackers text using setCurrentLocation before opening the dialog.");
		if (onStorageLocationUpdatedListener == null)
			throw new InvalidParameterException(
					"Please first set the callback listener using setOnStorageLocationUpdated before opening the dialog.");
		final View locationFrame = getActivity().getLayoutInflater().inflate(R.layout.dialog_storagelocation, null,
				false);
		final EditText locationText = (EditText) locationFrame.findViewById(R.id.location_edit);
		locationText.setText(currentLocation);
		return new AlertDialog.Builder(getActivity()).setView(locationFrame)
				.setPositiveButton(R.string.status_update, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// User is done editing and requested to update given the text input
						onStorageLocationUpdatedListener.onStorageLocationUpdated(locationText.getText().toString());
					}
				}).setNegativeButton(android.R.string.cancel, null).show();
	}

	public interface OnStorageLocationUpdatedListener {
		public void onStorageLocationUpdated(String newLocation);
	}

}
