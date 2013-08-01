package org.transdroid.core.gui.search;

import org.transdroid.core.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.DialogFragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class FilePickerHelper {

	public static final int ACTIVITY_FILEPICKER = 0x0000c0df; // A 'random' ID to identify file picker intents
	public static final Uri FILEMANAGER_MARKET_URI = Uri.parse("market://search?q=pname:org.openintents.filemanager");

	/**
	 * Call this to start a file picker intent. The calling activity will receive an Intent result with ID
	 * {@link #ACTIVITY_FILEPICKER} with an Intent that contains the selected local file as data Intent.
	 * @param activity The calling activity, to which the result is returned or a dialog is bound that asks to install
	 *            the file picker
	 */
	public static void startFilePicker(final SherlockFragmentActivity activity) {
		try {
			// Start a file manager that can handle the PICK_FILE intent (specifically IO File Manager)
			activity.startActivityForResult(new Intent("org.openintents.action.PICK_FILE"), ACTIVITY_FILEPICKER);
		} catch (Exception e) {
			// Can't start the file manager, for example with a SecurityException or when IO File Manager is not present
			new DialogFragment() {
				public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
					return new AlertDialog.Builder(activity).setIcon(android.R.drawable.ic_dialog_alert)
							.setMessage(activity.getString(R.string.search_filemanagernotfound))
							.setPositiveButton(android.R.string.yes, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (activity != null)
										activity.startActivity(new Intent(Intent.ACTION_VIEW, FILEMANAGER_MARKET_URI));
								}
							}).setNegativeButton(android.R.string.no, null).create();
				};
			}.show(activity.getSupportFragmentManager(), "installfilemanager");
		}
	}

}
