package org.transdroid.core.gui;

import java.util.List;

import org.transdroid.R;
import org.transdroid.core.app.settings.ServerSetting;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class ServerPickerDialog {

	/**
	 * Opens a dialog that allows the selection of a configured server (manual or seedbox). The calling activity will
	 * receive a callback on its switchServerAndAddFromIntent(int) method.
	 * @param activity The torrents activity from which the picker is started (and which received the callback)
	 * @param serverSettings The list of all available servers, of which their names will be offered to the user to pick
	 *            from (and its position in the list is returned to the activity)
	 */
	public static void startServerPicker(final TorrentsActivity activity, List<ServerSetting> serverSettings) {
		final String[] serverNames = new String[serverSettings.size()];
		for (int i = 0; i < serverSettings.size(); i++) {
			serverNames[i] = serverSettings.get(i).getName();
		}
		new DialogFragment() {
			public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
				return new AlertDialog.Builder(activity).setTitle(R.string.navigation_pickserver)
						.setItems(serverNames, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (activity != null)
									activity.switchServerAndAddFromIntent(which);
							}
						}).create();
			};
		}.show(activity.getFragmentManager(), "serverpicker");
	}

}
