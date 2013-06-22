package org.transdroid.core.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class FilterEntryDialog {

	/**
	 * Opens a dialog that allows entry of a filter string, which (on confirmation) will be used to filter the list of 
	 * torrents.
	 * @param activity The activity that opens (and owns) this dialog
	 */
	public static void startFilterEntry(final TorrentsActivity activity) {
		new DialogFragment() {
			public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
				final EditText filterInput = new EditText(activity);
				filterInput.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
				((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
						InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				return new AlertDialog.Builder(activity).setView(filterInput)
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String filterText = filterInput.getText().toString();
								if (activity != null)
									activity.filterTorrents(filterText);
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
			};
		}.show(activity.getSupportFragmentManager(), "filterentry");
	}

}
