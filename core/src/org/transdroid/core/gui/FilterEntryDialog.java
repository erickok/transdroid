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
package org.transdroid.core.gui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class FilterEntryDialog {

	/**
	 * Opens a dialog that allows entry of a filter string, which (on confirmation) will be used to filter the list of 
	 * torrents.
	 * @param activity The activity that opens (and owns) this dialog
	 */
	@SuppressLint("ValidFragment")
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
		}.show(activity.getFragmentManager(), "filterentry");
	}

}
