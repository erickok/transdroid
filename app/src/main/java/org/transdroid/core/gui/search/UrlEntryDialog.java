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

import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;

import org.transdroid.R;
import org.transdroid.core.gui.TorrentsActivity;
import org.transdroid.core.gui.navigation.NavigationHelper;

public class UrlEntryDialog {

	/**
	 * Opens a dialog that allows entry of a single URL string, which (on confirmation) will be supplied to the calling activity's {@link
	 * TorrentsActivity#addTorrentByUrl(String, String) method}.
	 * @param activity The activity that opens (and owns) this dialog
	 */
	public static void show(final TorrentsActivity activity) {
		View inputLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_url, null);
		final EditText urlEdit = (EditText) inputLayout.findViewById(R.id.url_edit);
		ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
			CharSequence content = clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity);
			urlEdit.setText(content);
		}
		new MaterialDialog.Builder(activity).customView(inputLayout, false).positiveText(android.R.string.ok).negativeText(android.R.string.cancel)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						String url = urlEdit.getText().toString();
						Uri uri = Uri.parse(url);
						if (!TextUtils.isEmpty(url)) {
							String title = NavigationHelper.extractNameFromUri(uri);
							if (uri.getScheme() != null && uri.getScheme().equals("magnet")) {
								activity.addTorrentByMagnetUrl(url, title);
							} else {
								activity.addTorrentByUrl(url, title);
							}
						}
					}
				}).show();
	}

}
