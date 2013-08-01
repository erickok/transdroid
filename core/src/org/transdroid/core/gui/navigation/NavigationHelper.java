package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;

import com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

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
	private Boolean inDebugMode;
	private static ImageLoader imageCache;

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
	 * Returns (and initialises, if needed) an image cache that uses memory and (1MB) local storage.
	 * @return An image cache that loads web images synchronously and transparently
	 */
	public ImageLoader getImageCache() {
		if (imageCache == null) {
			// @formatter:off
			imageCache = ImageLoader.getInstance();
			Builder imageCacheBuilder = new Builder(context)
				.defaultDisplayImageOptions(
						new DisplayImageOptions.Builder()
						.cacheInMemory()
						.cacheOnDisc()
						.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
						.showImageForEmptyUri(R.drawable.ic_launcher)
						.build())
				.memoryCache(
						new UsingFreqLimitedMemoryCache(1024 * 1024))
				.discCache(
						new FileCountLimitedDiscCache(context.getCacheDir(), new Md5FileNameGenerator(), 25));
			imageCache.init(imageCacheBuilder.build());
			// @formatter:on
		}
		return imageCache;
	}

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
	 * Returns whether the application is running in debug mode, as opposed to release mode. Use to show/hide features
	 * in the ui based on the build mode.
	 * @return True if the app is compiled in/running as debug mode, false otherwise
	 */
	public boolean inDebugMode() {
		try {
			if (inDebugMode == null) {
				PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				inDebugMode = (pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
			}
			return inDebugMode;
		} catch (NameNotFoundException e) {
			return false;
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

	/**
	 * Converts a string into a {@link Spannable} that displays the string in the Roboto Condensed font
	 * @param string A plain text {@link String}
	 * @return A {@link Spannable} that can be applied to supporting views (such as the action bar title) so that the
	 *         input string will be displayed using the Roboto Condensed font (if the OS has this)
	 */
	public static SpannableString buildCondensedFontString(String string) {
		if (string == null)
			return null;
		SpannableString s = new SpannableString(string);
		s.setSpan(new TypefaceSpan("sans-serif-condensed"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return s;
	}

}
