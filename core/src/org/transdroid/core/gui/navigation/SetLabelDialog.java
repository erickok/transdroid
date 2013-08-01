package org.transdroid.core.gui.navigation;

import java.security.InvalidParameterException;
import java.util.List;

import org.transdroid.core.R;
import org.transdroid.core.gui.lists.SimpleListItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * A dialog fragment that allows picking a label or entering a new label to set this new label to the torrent.
 * @author Eric Kok
 */
public class SetLabelDialog extends DialogFragment {

	private OnLabelPickedListener onLabelPickedListener = null;
	private List<? extends SimpleListItem> currentLabels = null;

	public SetLabelDialog() {
		setRetainInstance(true);
	}

	/**
	 * Sets the callback for when the user is has picked a label for the target torrent.
	 * @param onLabelPickedListener The event listener to this dialog
	 * @return This dialog, for method chaining
	 */
	public SetLabelDialog setOnLabelPickedListener(OnLabelPickedListener onLabelPickedListener) {
		this.onLabelPickedListener = onLabelPickedListener;
		return this;
	}

	/**
	 * Sets the list of currently known labels as are active on the server. These are offered to the user to pick a new
	 * label for the target torrents.
	 * @param currentLabels The list of torrent labels
	 * @return This dialog, for method chaining
	 */
	public SetLabelDialog setCurrentLabels(List<? extends SimpleListItem> currentLabels) {
		this.currentLabels = currentLabels;
		return this;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (onLabelPickedListener == null)
			throw new InvalidParameterException(
					"Please first set the callback listener using setOnLabelPickedListener before opening the dialog.");
		if (currentLabels == null)
			throw new InvalidParameterException(
					"Please first set the list of currently known labels before opening the dialog, even if the list is empty.");
		final View setlabelFrame = getActivity().getLayoutInflater().inflate(R.layout.dialog_setlabel, null, false);
		final ListView labelsList = (ListView) setlabelFrame.findViewById(R.id.labels_list);
		final EditText newlabelEdit = (EditText) setlabelFrame.findViewById(R.id.newlabel_edit);
		if (currentLabels.size() == 0) {
			// Hide the list (and its label) if there are no labels yet
			setlabelFrame.findViewById(R.id.pick_label).setVisibility(View.GONE);
			setlabelFrame.findViewById(R.id.line1).setVisibility(View.GONE);
			setlabelFrame.findViewById(R.id.line2).setVisibility(View.GONE);
			labelsList.setVisibility(View.GONE);
		} else {
			labelsList.setAdapter(new FilterListItemAdapter(getActivity(), currentLabels));
			labelsList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					onLabelPickedListener.onLabelPicked(((Label) labelsList.getItemAtPosition(position)).getName());
					dismiss();
				}
			});
		}
		return new AlertDialog.Builder(getActivity()).setView(setlabelFrame)
				.setPositiveButton(R.string.status_update, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// User should have provided a new label
						if (newlabelEdit.getText().toString().equals("")) {
							Crouton.showText(getActivity(), R.string.error_notalabel,
									NavigationHelper.CROUTON_ERROR_STYLE);
						}
						onLabelPickedListener.onLabelPicked(newlabelEdit.getText().toString());
					}
				}).setNeutralButton(R.string.status_label_remove, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onLabelPickedListener.onLabelPicked(null);
					}
				}).setNegativeButton(android.R.string.cancel, null).show();
	}

	public interface OnLabelPickedListener {
		public void onLabelPicked(String newLabel);
	}

}
