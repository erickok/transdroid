/*
 * Copyright 2010-2018 Eric Kok et al.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import org.transdroid.R;

import java.lang.ref.WeakReference;

public class FilePickerHelper {

    public static final int ACTIVITY_FILEPICKER = 0x0000c0df; // A 'random' ID to identify file picker intents
    public static final Uri FILEMANAGER_MARKET_URI = Uri.parse("market://search?q=pname:org.openintents.filemanager");

    /**
     * Call this to start a file picker intent. The calling activity will receive an Intent result with ID
     * {@link #ACTIVITY_FILEPICKER} with an Intent that contains the selected local file as data Intent.
     *
     * @param activity The calling activity, to which the result is returned or a dialog is bound that asks to install
     *                 the file picker
     */
    @SuppressLint("ValidFragment")
    public static void startFilePicker(final Activity activity) {
        try {
            // Start a file manager that can handle the file/* file/* intents
            activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("application/x-bittorrent"),
                    ACTIVITY_FILEPICKER);
        } catch (Exception e1) {
            try {
                // Start a file manager that can handle the PICK_FILE intent (specifically IO File Manager)
                activity.startActivityForResult(new Intent("org.openintents.action.PICK_FILE"), ACTIVITY_FILEPICKER);
            } catch (Exception e2) {
                // Can't start the file manager, for example with a SecurityException or when IO File Manager is not present
                final WeakReference<Context> intentStartContext = new WeakReference<>(activity);
                new AlertDialog.Builder(activity)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(activity.getString(R.string.search_filemanagernotfound))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (intentStartContext.get() != null)
                                intentStartContext.get().startActivity(new Intent(Intent.ACTION_VIEW, FILEMANAGER_MARKET_URI));
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        }
    }

}
