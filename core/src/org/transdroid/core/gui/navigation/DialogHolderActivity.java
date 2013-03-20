package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.transdroid.core.gui.log.Log;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;

@EActivity(resName = "activity_dialogholder")
public class DialogHolderActivity extends SherlockFragmentActivity {

	@Extra
	protected String dialogType;
	
	/**
	 * Use this method to show some dialog; it will show the dialog as full screen fragment on smaller devices. Use the 
	 * DialogFragment's class here only; a new instance will be created by this holder activity.
	 */
	public static void showDialog(Context context, Class<? extends DialogFragment> dialogType) {
		DialogHolderActivity_.intent(context).start();
	}

	@AfterViews
	public void init() {
		try {
			// Instantiate an instance of the requested dialog
			DialogFragment dialog = (DialogFragment) Class.forName(dialogType).newInstance();
			// Determine if the dialog should be shown as full size screen
			boolean isSmall = NavigationHelper_.getInstance_(this).isSmallScreen();
			if (!isSmall) {
				// Show as normal dialog
				dialog.show(this.getSupportFragmentManager(), "about_dialog");
			} else {
				// Small device, so show the fragment full screen
				FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
				// Note: the fragment is not added to the back stack, as this activity already is
				ft.add(android.R.id.content, dialog).commit();
			}
		} catch (InstantiationException e) {
			Log.e(this, "Tried to show a dialog of type " + dialogType + ", but that cannot be instantiated.");
		} catch (IllegalAccessException e) {
			Log.e(this, "Tried to show a dialog of type " + dialogType + ", but it is not accessible.");
		} catch (ClassNotFoundException e) {
			Log.e(this, "Tried to show a dialog of type " + dialogType + ", but that class doesn't exist.");
		}
	}

}
