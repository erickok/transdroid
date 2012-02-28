package org.transdroid.gui.util;

import java.util.List;

import org.transdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class ActivityUtil {

	private static final String LOG_NAME = "Activity util";

	/**
     * Get current version number
     * @return A string with the application version number
     */
    public static String getVersionNumber(Context context) {
            String version = "?";
            try {
                    PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    version = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_NAME, "Package name not found to retrieve version number", e);
            }
            return version;
    }

	/**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * @param context The application's environment.
     * @param intent The Intent to check for availability.
     * @return True if an Intent with the specified action can be sent and responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * Builds a (reusable) dialog that asks to install some application from the Android market
     * @param messageResourceID The message to show to the user
     * @param marketUri The application's URI on the Android Market
     * @return The dialog to show
     */
	public static Dialog buildInstallDialog(final Activity activity, int messageResourceID, final Uri marketUri) {
		return buildInstallDialog(activity, messageResourceID, marketUri, false, activity.getString(R.string.oifm_install));
	}
    
    /**
     * Builds a (reusable) dialog that asks to install some application from the Android market
     * @param messageResourceID The message to show to the user
     * @param marketUri The application's URI on the Android Market
     * @param buttonText The text to show on the positive (install) button
     * @param alternativeNegativeButtonHandler The click handler for the negative dialog button
     * @return The dialog to show
     */
	public static Dialog buildInstallDialog(final Activity activity, int messageResourceID, final Uri marketUri, 
			final boolean closeAfterInstallFailure, CharSequence buttonText) {
		AlertDialog.Builder fbuilder = new AlertDialog.Builder(activity);
		fbuilder.setMessage(messageResourceID);
		fbuilder.setCancelable(true);
		fbuilder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent install = new Intent(Intent.ACTION_VIEW, marketUri);
				if (ActivityUtil.isIntentAvailable(activity, install)) {
					activity.startActivity(install);
				} else {
					Toast.makeText(activity, R.string.oifm_nomarket, Toast.LENGTH_LONG).show();
					if (closeAfterInstallFailure) {
						activity.finish();
					}
				}
				dialog.dismiss();
			}
		});
		fbuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				if (closeAfterInstallFailure) {
					activity.finish();
				}
			}
		});
		return fbuilder.create();
	}
    
}
