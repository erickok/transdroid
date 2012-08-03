package org.transdroid.gui;

import org.transdroid.R;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * A dialog that shows a list of (existing) labels to choose from as well as
 * give a free text input to assign a new label. If the chosen label is 
 * different form the current (already assigned) label, onLabelResult is 
 * called with the new-to-be-assigned label. 
 * 
 * @author erickok
 */
public class SetLabelDialog extends Dialog {

	private ResultListener callback;
	private String currentLabel;
	private ListView existingLabelsList;
	private EditText newLabelText;
	private Button okButton;
	
	/**
	 * Callback listener for when a label is either selected or entered by the user
	 */
	public interface ResultListener {
		/**
		 * Called when the label result is known and different form the current (already assigned) label
		 * @param label The chosen or newly entered label (to be assigned to a torrent)
		 */
		public void onLabelResult(String label);
	}

	/**
	 * Constructor for the labels dialog
	 * @param context The activity context
	 * @param callback The activity that will handle the dialog result (being the to be assigned label string)
	 * @param existingLabels The labels to list as existing
	 * @param currentLabel The currently assigned label to the torrent
	 */
	public SetLabelDialog(Context context, ResultListener callback, String[] existingLabels, String currentLabel) {
		super(context);
		this.callback = callback;
		this.currentLabel = currentLabel;
		
		// Custom layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_new_label);
		existingLabelsList = (ListView) findViewById(R.id.labels);
		newLabelText = (EditText) findViewById(R.id.new_label);
		okButton = (Button) findViewById(R.id.set_button);
		newLabelText.setText(currentLabel);
		
		// Set content and attach listeners
		existingLabelsList.setAdapter(new ArrayAdapter<String>(context, R.layout.list_item_label, existingLabels));
		existingLabelsList.setOnItemClickListener(onLabelSelected);
		okButton.setOnClickListener(onNewLabelClick);
	}

	public void resetDialog(Context context, String[] existingLabels, String currentLabel) {
		// Update the available existing labels and empty the text box
		this.currentLabel = currentLabel;
		existingLabelsList.setAdapter(new ArrayAdapter<String>(context, R.layout.list_item_label, existingLabels));
		newLabelText.setText(currentLabel);
	}

	private OnItemClickListener onLabelSelected = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Set the result to be the selected item in the list
			returnResult((String) existingLabelsList.getItemAtPosition(position));
		}
	};

	private View.OnClickListener onNewLabelClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// Set the result to be the current EditText input
			returnResult(newLabelText.getText().toString());
		}
	}; 

	private void returnResult(String label) {
		// Return result (if needed) and close the dialog
		if (currentLabel == null || !currentLabel.equals(label)) {
			callback.onLabelResult(label);
		}
		dismiss();
	}
	
}
