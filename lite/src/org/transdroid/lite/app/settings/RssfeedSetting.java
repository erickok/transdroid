package org.transdroid.lite.app.settings;

import org.transdroid.lite.gui.navigation.FilterItem;

import android.net.Uri;
import android.text.TextUtils;

/**
 * Represents a user-specified RSS feed.
 * @author Eric Kok
 */
public class RssfeedSetting implements FilterItem {

	private static final String DEFAULT_NAME = "Default";
	
	private final int order;
	private final String name;
	private final String url;
	private final boolean requiresAuth;
	private String lastNew;

	public RssfeedSetting(int order, String name, String baseUrl, boolean needsAuth) {
		this.order = order;
		this.name = name;
		this.url = baseUrl;
		this.requiresAuth = needsAuth;
		this.lastNew = null;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		if (!TextUtils.isEmpty(name))
			return name;
		if (!TextUtils.isEmpty(url))
			return Uri.parse(url).getHost();
		return DEFAULT_NAME;
	}

	public String getUrl() {
		return url;
	}

	public boolean requiresExternalAuthentication() {
		return requiresAuth;
	}

	/**
	 * Returns the URL of the item that was the newest last time we checked this feed
	 * @return The last new item's URL as URL-encoded string
	 */
	public String getLastNew() {
		return this.lastNew;
	}

	/**
	 * Record the URL of what is now the last item we retrieved
	 * @param lastNew The URL of the last new item as URL-encoded string
	 */
	public void setLastNew(String lastNew) {
		this.lastNew = lastNew;
	}
	
}
