package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Helper for activities to make navigation-related decisions, such as when a device can display a larger, tablet style
 * layout or how to display errors.
 * @author Eric Kok
 */
@SuppressLint("ResourceAsColor")
@EBean
public class NavigationHelper {

	@RootContext
	protected Context context;

	/**
	 * Use with {@link Crouton#showText(android.app.Activity, int, Style)} (and variants) to display error messages.
	 */
	public static Style CROUTON_ERROR_STYLE = new Style.Builder().setBackgroundColor(R.color.crouton_error)
			.setTextSize(13).setDuration(2500).build();

	/**
	 * Use with {@link Crouton#showText(android.app.Activity, int, Style)} (and variants) to display info messages.
	 */
	public static Style CROUTON_INFO_STYLE = new Style.Builder().setBackgroundColor(R.color.crouton_info)
			.setTextSize(13).setDuration(1500).build();

	/**
	 * Whether any search-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public String getAppNameAndVersion() {
		String appName = context.getString(R.string.app_name);
		try {
			PackageInfo m = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return appName + " " + m.versionName + " (" + m.versionCode + ")";
		} catch (NameNotFoundException e) {
			return appName;
		}
	}

	/**
	 * Returns whether the device is considered small (i.e. a phone) rather than large (i.e. a tablet). Can, for
	 * example, be used to determine if a dialog should be shown full screen. Currently is true if the device's smallest
	 * dimension is 500 dip.
	 * @return True if the app runs on a small device, false otherwise
	 */
	public boolean isSmallScreen() {
		return context.getResources().getBoolean(R.bool.show_dialog_fullscreen);
	}

	/**
	 * Whether any search-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableSearchUi() {
		return !context.getPackageName().equals("org.transdroid.lite");
	}

	/**
	 * Whether any RSS-related UI components should be shown in the interface. At the moment returns false only if we
	 * run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableRssUi() {
		return !context.getPackageName().equals("org.transdroid.lite");
	}

}
