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
package org.transdroid.core.gui.navigation;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.transdroid.R;
import org.transdroid.core.app.settings.SystemSettings_;

import java.util.Iterator;
import java.util.List;

public class SetLabelDialog {

	/**
	 * A dialog fragment that allows picking a label or entering a new label to set this new label to the torrent.
	 * @param context The activity context that opens (and owns) this dialog
	 * @param onLabelPickedListener The callback when a new label has been entered or picked by the user
	 * @param currentLabels The list of labels as currently exist on the server, to present as list for easy selection
	 */
	public static void show(final Context context, final OnLabelPickedListener onLabelPickedListener, List<Label> currentLabels) {

		// Discard the empty label in this list before storing it locally
		for (Iterator<Label> iter = currentLabels.iterator(); iter.hasNext(); ) {
			if (iter.next().isEmptyLabel()) {
				iter.remove();
			}
		}

		final View setLabelLayout = LayoutInflater.from(context).inflate(R.layout.dialog_setlabel, null);
		final ListView labelsList = (ListView) setLabelLayout.findViewById(R.id.labels_list);
		final EditText newLabelEdit = (EditText) setLabelLayout.findViewById(R.id.newlabel_edit);

		final MaterialDialog dialog = new MaterialDialog.Builder(context).customView(setLabelLayout, false).positiveText(R.string.status_update)
				.neutralText(R.string.status_label_remove).negativeText(android.R.string.cancel).callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						// User should have provided a new label
						if (TextUtils.isEmpty(newLabelEdit.getText())) {
							SnackbarManager.show(Snackbar.with(context).text(R.string.error_notalabel).colorResource(R.color.red));
							return;
						}
						onLabelPickedListener.onLabelPicked(newLabelEdit.getText().toString());
					}

					@Override
					public void onNeutral(MaterialDialog dialog) {
						onLabelPickedListener.onLabelPicked(null);
					}
				}).theme(SystemSettings_.getInstance_(context).getMaterialDialogtheme()).build();

		if (currentLabels.size() == 0) {
			// Hide the list (and its label) if there are no labels yet
			setLabelLayout.findViewById(R.id.pick_label).setVisibility(View.GONE);
			labelsList.setVisibility(View.GONE);
		} else {
			labelsList.setAdapter(new FilterListItemAdapter(context, currentLabels));
			labelsList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					onLabelPickedListener.onLabelPicked(((Label) labelsList.getItemAtPosition(position)).getName());
					dialog.dismiss();
				}
			});
		}

		dialog.show();

	}

	public interface OnLabelPickedListener {
		void onLabelPicked(String newLabel);
	}

}
