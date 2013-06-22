package org.transdroid.core.gui.settings;

import org.transdroid.core.R;
import org.transdroid.core.gui.navigation.DialogHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * Fragment that shows recent app changes.
 * @author Eric Kok
 */
public class ChangelogDialog implements DialogHelper.DialogSpecification {

	private static final long serialVersionUID = -4563410777022941124L;

	@Override
	public int getDialogLayoutId() {
		return R.layout.dialog_changelog;
	}

	@Override
	public int getDialogMenuId() {
		return R.menu.dialog_about;
	}

	@Override
	public boolean onMenuItemSelected(Activity ownerActivity, int selectedItemId) {
		if (selectedItemId == R.id.action_visitwebsite) {
			ownerActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://transdroid.org/about/changelog/")));
			return true;
		}
		return false;
	}

}
