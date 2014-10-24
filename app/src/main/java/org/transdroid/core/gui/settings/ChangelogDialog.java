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
package org.transdroid.core.gui.settings;

import org.transdroid.R;
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
