package org.transdroid.core.gui.search;

import org.transdroid.core.gui.TorrentsActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class UrlEntryDialog {

	/**
	 * Opens a dialog that allows entry of a single URL string, which (on confirmation) will be supplied to the calling
	 * activity's {@link TorrentsActivity#addTorrentByUrl(String, String) method}.
	 * @param activity The activity that opens (and owns) this dialog
	 */
	public static void startUrlEntry(final TorrentsActivity activity) {
		new DialogFragment() {
			public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
				final EditText urlInput = new EditText(activity);
				urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
				((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
						InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				return new AlertDialog.Builder(activity).setView(urlInput)
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Assume text entry box input as URL and treat the filename (after the last /) as title
								String url = urlInput.getText().toString();
								if (activity != null && !TextUtils.isEmpty(url))
									activity.addTorrentByUrl(url, url.substring(url.lastIndexOf("/")));
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
			};
		}.show(activity.getSupportFragmentManager(), "urlentry");
	}

}
