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

import android.app.DialogFragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;

import org.transdroid.R;
import org.transdroid.core.app.settings.SystemSettings_;

import java.util.Arrays;
import java.util.List;

public class SetTrackersDialog extends DialogFragment {

	/**
	 * A dialog fragment that allows changing the trackers of a torrent by editing the text directly.
	 * @param context The activity context that opens (and owns) this dialog
	 * @param onTrackersUpdatedListener The callback for when the user is done updating the trackers list
	 * @param currentTrackers The current trackers text/list that will be available to the user to edit
	 */
	public static void show(final Context context, final OnTrackersUpdatedListener onTrackersUpdatedListener, String currentTrackers) {
		View trackersLayout = LayoutInflater.from(context).inflate(R.layout.dialog_trackers, null);
		final EditText trackersText = (EditText) trackersLayout.findViewById(R.id.trackers_edit);
		trackersText.setText(currentTrackers);
		new MaterialDialog.Builder(context).customView(trackersLayout, false).positiveText(R.string.status_update)
				.negativeText(android.R.string.cancel).callback(new MaterialDialog.ButtonCallback() {
			@Override
			public void onPositive(MaterialDialog dialog) {
				// User is done editing and requested to update given the text input
				onTrackersUpdatedListener.onTrackersUpdated(Arrays.asList(trackersText.getText().toString().split("\n")));
			}
		}).theme(SystemSettings_.getInstance_(context).getMaterialDialogtheme()).show();
	}

	public interface OnTrackersUpdatedListener {
		void onTrackersUpdated(List<String> updatedTrackers);
	}

}
