package org.transdroid.core.gui.settings;

import org.transdroid.core.R;
import org.transdroid.core.gui.navigation.DialogHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * Fragment that shows info about the application developer and used open source libraries.
 * @author Eric Kok
 */
public class AboutDialog implements DialogHelper.DialogSpecification {

	private static final long serialVersionUID = -4711432869714292985L;

	@Override
	public int getDialogLayoutId() {
		return R.layout.dialog_about;
	}

	@Override
	public int getDialogMenuId() {
		return R.menu.dialog_about;
	}

	@Override
	public boolean onMenuItemSelected(Activity ownerActivity, int selectedItemId) {
		if (selectedItemId == R.id.action_visitwebsite) {
			ownerActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://transdroid.org")));
			return true;
		}
		return false;
	}

}
