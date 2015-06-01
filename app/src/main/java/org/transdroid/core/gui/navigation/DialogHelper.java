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

import java.io.Serializable;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.transdroid.core.gui.*;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

/**
 * Helper class that show a dialog either as pop-up or as full screen activity. Should be used by calling
 * {@link #showDialog(Context, DialogSpecification)} with in instance of the dialog specification that should be shown,
 * from the calling activity's {@link Activity#onCreateDialog(int)}.
 * @author Eric Kok
 */
@EActivity
public class DialogHelper extends Activity {

	@Extra
	protected DialogSpecification dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(dialog.getDialogLayoutId());
		// TODO getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(dialog.getDialogMenuId(), menu);
        return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Action bar up button clicked; navigate up all the way back to the torrents activity
			TorrentsActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
			return true;
		}
		return dialog.onMenuItemSelected(this, item.getItemId());
	}
	
	/**
	 * Call this from {@link Activity#onCreateDialog(int)}, supplying an instance of the {@link DialogSpecification}
	 * that should be shown to the user.
	 * @param context The activity that calls this method and which will own the constructed dialog
	 * @param dialog An instance of the specification for the dialog that needs to be shown
	 * @return Either an instance of a {@link Dialog} that the activity should further control or null if the dialog
	 *         will instead be opened as a full screen activity
	 */
	public static Dialog showDialog(Context context, DialogSpecification dialog) {
		
		// If the device is large (i.e. a tablet) then return a dialog to show
		if (!NavigationHelper_.getInstance_(context).isSmallScreen())
			return new PopupDialog(context, dialog);
		
		// This is a small device; create a full screen dialog (which is just an activity)
		DialogHelper_.intent(context).dialog(dialog).start();
		return null;
		
	}
	
	/**
	 * A specific dialog that shows some layout (resource) as contents. It has no buttons or other chrome.
	 */
	protected static class PopupDialog extends Dialog {
		public PopupDialog(Context context, DialogSpecification dialog) {
			super(context);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(dialog.getDialogLayoutId());
		}
	}
	
	/**
	 * Specification for some dialog that can be show to the user, consisting of a custom layout and possibly an action 
	 * bar menu. Warning: the action bar, and thus the menu options, is only shown when the dialog is presented as full 
	 * screen activity. Use only for unimportant actions.
	 */
	public interface DialogSpecification extends Serializable {
		int getDialogLayoutId();
		int getDialogMenuId();
		boolean onMenuItemSelected(Activity ownerActivity, int selectedItemId);
	}
	
}
