package org.transdroid.core.gui;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;

import java.util.List;

public class ConfirmRemoveDialog {

	/**
	 * Opens a dialog that confirms the removal of a torrent, along with an option for deleting downloaded files
	 * @param activity The torrents activity from which the dialog is started (and which received the callback)
     * @param torrents List of torrents to be removed
	 */
	public static void startConfirmRemove(final TorrentsActivity activity, final List<Torrent> torrents) {
        final CharSequence checkboxItems[] = {activity.getString(R.string.navigation_confirmdataremoval)};
        final boolean[] isRemoveDataChecked = {false};

		new DialogFragment() {
			public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
				return new AlertDialog.Builder(activity).setTitle(R.string.navigation_confirmremove)
                        .setMultiChoiceItems(checkboxItems, null, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                isRemoveDataChecked[0] = isChecked;
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (Torrent torrent : torrents) {
									boolean shouldRemoveData = isRemoveDataChecked[0] &&
											Daemon.supportsRemoveWithData(torrent.getDaemon());
                                    activity.removeTorrent(torrent, shouldRemoveData);
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
			};
		}.show(activity.getFragmentManager(), "confirmremoval");
	}

}
