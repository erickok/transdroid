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
import java.util.Arrays;
import java.util.List;

import org.transdroid.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * A dialog fragment that allows changing the trackers of a torrent by editing the text directly.
 * @author Eric Kok
 */
public class SetTrackersDialog extends DialogFragment {

	private OnTrackersUpdatedListener onTrackersUpdatedListener = null;
	private String currentTrackers = null;

	public SetTrackersDialog() {
		setRetainInstance(true);
	}

	/**
	 * Sets the callback for when the user is done updating the trackers list.
	 * @param onTrackersUpdatedListener The event listener to this dialog
	 * @return This dialog, for method chaining
	 */
	public SetTrackersDialog setOnTrackersUpdated(OnTrackersUpdatedListener onTrackersUpdatedListener) {
		this.onTrackersUpdatedListener = onTrackersUpdatedListener;
		return this;
	}

	/**
	 * Sets the current trackers text/list that will be available to the user to edit
	 * @param currentTrackers The current trackers for the target torrent
	 * @return This dialog, for method chaining
	 */
	public SetTrackersDialog setCurrentTrackers(String currentTrackers) {
		this.currentTrackers = currentTrackers;
		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (currentTrackers == null)
			throw new InvalidParameterException(
					"Please first set the current trackers text using setCurrentTrackers before opening the dialog.");
		if (onTrackersUpdatedListener == null)
			throw new InvalidParameterException(
					"Please first set the callback listener using setOnTrackersUpdated before opening the dialog.");
		final View trackersFrame = getActivity().getLayoutInflater().inflate(R.layout.dialog_trackers, null, false);
		final EditText trackersText = (EditText) trackersFrame.findViewById(R.id.trackers_edit);
		trackersText.setText(currentTrackers);
		return new AlertDialog.Builder(getActivity()).setView(trackersFrame)
				.setPositiveButton(R.string.status_update, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// User is done editing and requested to update given the text input
						onTrackersUpdatedListener.onTrackersUpdated(Arrays.asList(trackersText.getText().toString()
								.split("\n")));
					}
				}).setNegativeButton(android.R.string.cancel, null).show();
	}

	public interface OnTrackersUpdatedListener {
		public void onTrackersUpdated(List<String> updatedTrackers);
	}

}
