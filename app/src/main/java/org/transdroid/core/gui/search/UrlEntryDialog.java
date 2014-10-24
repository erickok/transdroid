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
package org.transdroid.core.gui.search;

import org.transdroid.core.gui.TorrentsActivity;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Build;
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
	@SuppressLint("ValidFragment")
	public static void startUrlEntry(final TorrentsActivity activity) {
		new DialogFragment() {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			public android.app.Dialog onCreateDialog(android.os.Bundle savedInstanceState) {
				final EditText urlInput = new EditText(activity);
				urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					ClipboardManager clipboard = (ClipboardManager) activity
							.getSystemService(Context.CLIPBOARD_SERVICE);
					if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
						CharSequence content = clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity);
						urlInput.setText(content);
					}
				}
				((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(
						InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
				return new AlertDialog.Builder(activity).setView(urlInput)
						.setPositiveButton(android.R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Assume text entry box input as URL and treat the filename (after the last /) as title
								String url = urlInput.getText().toString();
								Uri uri = Uri.parse(url);
								if (activity != null && !TextUtils.isEmpty(url)) {
									String title = NavigationHelper.extractNameFromUri(uri);
									if (uri.getScheme() != null && uri.getScheme().equals("magnet")) {
										activity.addTorrentByMagnetUrl(url, title);
									} else {
										activity.addTorrentByUrl(url, title);
									}
								}
							}
						}).setNegativeButton(android.R.string.cancel, null).create();
			};
		}.show(activity.getFragmentManager(), "urlentry");
	}

}
