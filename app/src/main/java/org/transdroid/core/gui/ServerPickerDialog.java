/*
 * Copyright 2010-2024 Eric Kok et al.
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

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.transdroid.R;
import org.transdroid.core.app.settings.ServerSetting;

import java.util.List;

public class ServerPickerDialog extends DialogFragment {

    /**
     * Opens a dialog that allows the selection of a configured server (manual or seedbox). The calling activity will
     * receive a callback on its switchServerAndAddFromIntent(int) method.
     *
     * @param activity       The torrents activity from which the picker is started (and which received the callback)
     * @param serverSettings The list of all available servers, of which their names will be offered to the user to pick
     *                       from (and its position in the list is returned to the activity)
     */
    public static void startServerPicker(final TorrentsActivity activity, List<ServerSetting> serverSettings) {
        final String[] serverNames = new String[serverSettings.size()];
        for (int i = 0; i < serverSettings.size(); i++) {
            serverNames[i] = serverSettings.get(i).getName();
        }
        ServerPickerDialog dialog = new ServerPickerDialog();
        Bundle arguments = new Bundle();
        arguments.putStringArray("serverNames", serverNames);
        dialog.setArguments(arguments);
        dialog.show(activity.getSupportFragmentManager(), "serverpicker");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] serverNames = getArguments().getStringArray("serverNames");
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.navigation_pickserver)
                .setItems(serverNames, (dialog, which) -> {
                    if (getActivity() != null && getActivity() instanceof TorrentsActivity)
                        ((TorrentsActivity) getActivity()).switchServerAndAddFromIntent(which);
                })
                .create();
    }

}
