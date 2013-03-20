package org.transdroid.core.gui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Fragment that shows info about the application developer and used open source libraries.
 * @author Eric Kok
 */
@EFragment(resName="fragment_about")
@OptionsMenu(resName="fragment_about")
public class AboutFragment extends SherlockDialogFragment {

	@AfterViews
	protected void init() {
		// TODO: Add list of used open source libraries
	}

	@OptionsItem(resName="action_visitwebsite")
	protected void visitWebsite() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://transdroid.org")));
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Allow presenting of this fragment as a dialog
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}
	
}
