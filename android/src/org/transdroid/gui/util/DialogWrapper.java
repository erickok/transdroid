package org.transdroid.gui.util;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A wrapper used to create dialog fragments out of old-style activity dialogs
 */
public class DialogWrapper extends DialogFragment {
	
    public static final String TAG = "DialogWrapper";
    
	private final Dialog dialog;
	
	public DialogWrapper(Dialog dialog) {
		this.dialog = dialog;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	return dialog;
	}
	
}