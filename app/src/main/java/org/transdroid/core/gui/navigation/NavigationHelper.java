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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration.Builder;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.BuildConfig;
import org.transdroid.R;

import java.io.IOException;
import java.util.List;

/**
 * Helper for activities to make navigation-related decisions, such as when a device can display a larger, tablet style layout or how to display
 * errors.
 * @author Eric Kok
 */
@SuppressLint("ResourceAsColor")
@EBean
public class NavigationHelper {

	private static final int REQUEST_TORRENT_READ_PERMISSION = 0;
	private static final int REQUEST_SETTINGS_READ_PERMISSION = 1;
	private static final int REQUEST_SETTINGS_WRITE_PERMISSION = 2;

	private static ImageLoader imageCache;
	@RootContext
	protected Context context;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public boolean checkTorrentReadPermission(final Activity activity) {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ||
				checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_TORRENT_READ_PERMISSION);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public boolean checkSettingsReadPermission(final Activity activity) {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ||
				checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_SETTINGS_READ_PERMISSION);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public boolean checkSettingsWritePermission(final Activity activity) {
		return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ||
				checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_SETTINGS_WRITE_PERMISSION);
	}

	private boolean checkPermission(final Activity activity, final String permission, final int requestCode) {
		if (hasPermission(permission))
			// Permission already granted
			return true;
		if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
			// Never asked again: show a dialog with an explanation
			activity.runOnUiThread(new Runnable() {
				public void run() {
					new MaterialDialog.Builder(context).content(R.string.permission_readtorrent).positiveText(android.R.string.ok)
						.onPositive(new MaterialDialog.SingleButtonCallback() {
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
								ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
							}
						}).show();
				}
			});
			return false;
		}
		// Permission not granted (and we asked for it already before)
		ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_TORRENT_READ_PERMISSION);
		return false;
	}

	private boolean hasPermission(String requiredPermission) {
		return ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED;
	}

	public Boolean handleTorrentReadPermissionResult(int requestCode, int[] grantResults) {
		if (requestCode == REQUEST_TORRENT_READ_PERMISSION) {
			// Return permission granting result
			return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
		}
		return null;
	}

	public Boolean handleSettingsReadPermissionResult(int requestCode, int[] grantResults) {
		if (requestCode == REQUEST_SETTINGS_READ_PERMISSION) {
			// Return permission granting result
			return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
		}
		return null;
	}

	public Boolean handleSettingsWritePermissionResult(int requestCode, int[] grantResults) {
		if (requestCode == REQUEST_SETTINGS_WRITE_PERMISSION) {
			// Return permission granting result
			return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
		}
		return null;
	}

	/**
	 * Converts a string into a {@link Spannable} that displays the string in the Roboto Condensed font
	 * @param string A plain text {@link String}
	 * @return A {@link Spannable} that can be applied to supporting views (such as the action bar title) so that the input string will be displayed
	 * using the Roboto Condensed font (if the OS has this)
	 */
	public static SpannableString buildCondensedFontString(String string) {
		if (string == null) {
			return null;
		}
		SpannableString s = new SpannableString(string);
		s.setSpan(new TypefaceSpan("sans-serif-condensed"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return s;
	}

	/**
	 * Analyses a torrent http or magnet URI and tries to come up with a reasonable human-readable name.
	 * @param rawTorrentUri The raw http:// or magnet: link to the torrent
	 * @return A best-guess, reasonably long name for the linked torrent
	 */
	public static String extractNameFromUri(Uri rawTorrentUri) {

		if (rawTorrentUri.getScheme() == null) {
			// Probably an incorrect URI; just return the whole thing
			return rawTorrentUri.toString();
		}

		if (rawTorrentUri.getScheme().equals("magnet")) {
			// Magnet links might have a dn (display name) parameter
			String dn = getQueryParameter(rawTorrentUri, "dn");
			if (dn != null && !dn.equals("")) {
				return dn;
			}
			// If not, try to return the hash that is specified as xt (exact topci)
			String xt = getQueryParameter(rawTorrentUri, "xt");
			if (xt != null && !xt.equals("")) {
				return xt;
			}
		}

		if (rawTorrentUri.isHierarchical()) {
			String path = rawTorrentUri.getPath();
			if (path != null) {
				if (path.contains("/")) {
					path = path.substring(path.lastIndexOf("/"));
				}
				return path;
			}
		}

		// No idea what to do with this; return as is
		return rawTorrentUri.toString();
	}

	private static String getQueryParameter(Uri uri, String parameter) {
		int start = uri.toString().indexOf(parameter + "=");
		if (start >= 0) {
			int begin = start + (parameter + "=").length();
			int end = uri.toString().indexOf("&", begin);
			return uri.toString().substring(begin, end >= 0 ? end : uri.toString().length());
		}
		return null;
	}

	/**
	 * Returns (and initialises, if needed) an image cache that uses memory and (1MB) local storage.
	 * @return An image cache that loads web images synchronously and transparently
	 */
	public ImageLoader getImageCache() {
		if (imageCache == null) {
			imageCache = ImageLoader.getInstance();
			try {
				LruDiskCache diskCache = new LruDiskCache(context.getCacheDir(), null, new Md5FileNameGenerator(), 640000, 25);
				// @formatter:off
				Builder imageCacheBuilder = new Builder(context)
						.defaultDisplayImageOptions(
								new DisplayImageOptions.Builder()
										.cacheInMemory(true)
										.cacheOnDisk(true)
										.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
										.showImageForEmptyUri(R.drawable.ic_launcher).build())
						.memoryCache(new UsingFreqLimitedMemoryCache(1024 * 1024))
						.diskCache(diskCache);
				imageCache.init(imageCacheBuilder.build());
			// @formatter:on
			} catch (IOException e) {
				// The cache directory is always available on Android; ignore this exception
			}
		}
		return imageCache;
	}

	public void forceOpenInBrowser(Uri link) {
		Intent intent = new Intent(Intent.ACTION_VIEW).setData(link);
		List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
		for (ResolveInfo resolveInfo : activities) {
			if (activities.size() == 1 || (resolveInfo.isDefault && resolveInfo.activityInfo.packageName.equals(context.getPackageName()))) {
				// There is a default browser; use this
				intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
				return;
			}
		}
		// No default browser found: open chooser
		try {
			context.startActivity(Intent.createChooser(intent, "Open..."));
		} catch (Exception e) {
			// No browser installed; consume and fail silently
		}
	}

	/**
	 * Returns the application name (like Transdroid) and version name (like 1.5.0), appended by the version code (like 180).
	 * @return The app name and version, such as 'Transdroid 1.5.0 (180)'
	 */
	public String getAppNameAndVersion() {
		return context.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + " (" + Integer.toString(BuildConfig.VERSION_CODE) + ")";
	}

	/**
	 * Returns whether the device is considered small (i.e. a phone) rather than large (i.e. a tablet). Can, for example, be used to determine if a
	 * dialog should be shown full screen. Currently is true if the device's smallest dimension is 500 dip.
	 * @return True if the app runs on a small device, false otherwise
	 */
	public boolean isSmallScreen() {
		return context.getResources().getBoolean(R.bool.show_dialog_fullscreen);
	}

	/**
	 * Whether any search-related UI components should be shown in the interface. At the moment returns false only if we run as Transdroid Lite
	 * version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableSearchUi() {
		return context.getResources().getBoolean(R.bool.search_available);
	}

	/**
	 * Whether any RSS-related UI components should be shown in the interface. At the moment returns false only if we run as Transdroid Lite version.
	 * @return True if search is enabled, false otherwise
	 */
	public boolean enableRssUi() {
		return context.getResources().getBoolean(R.bool.rss_available);
	}

	/**
	 * Returns whether any seedbox-related components should be shown in the interface; specifically the option to add server settings via easy
	 * seedbox-specific screens.
	 * @return True if seedbox settings should be shown, false otherwise
	 */
	public boolean enableSeedboxes() {
		return context.getResources().getBoolean(R.bool.seedboxes_available);
	}

	/**
	 * Whether the custom app update checker should be used to check for new app and search module versions.
	 * @return True if it should be checked against transdroid.org if there are app updates (as opposed to using the Play Store for updates, for
	 * example), false otherwise
	 */
	public boolean enableUpdateChecker() {
		return context.getResources().getBoolean(R.bool.updatecheck_available);
	}

}
