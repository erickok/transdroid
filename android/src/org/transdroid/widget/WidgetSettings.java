package org.transdroid.widget;

import org.transdroid.daemon.DaemonSettings;

public class WidgetSettings {
	
	final private int id;
	final private DaemonSettings daemonSettings;
	final private int refreshInterval;
	final private int layoutResourceId;

	public WidgetSettings(int id, DaemonSettings daemonSettings, int refreshInterval, int layoutResourceId) {
		this.id = id;
		this.daemonSettings = daemonSettings;
		this.refreshInterval = refreshInterval;
		this.layoutResourceId = layoutResourceId;
	}
	
	/**
	 * Returns the unique widget ID
	 * @return The widget ID as assigned by the AppWidgetManager
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Returns the daemon settings for this widget
	 * @return The user-chosen daemon settings object
	 */
	public DaemonSettings getDaemonSettings() {
		return daemonSettings;
	}
	
	/**
	 * Returns the refresh interval
	 * @return The refresh interval in seconds
	 */
	public int getRefreshInterval() {
		return refreshInterval;
	}
	
	/**
	 * Returns the R.layout resource ID
	 * @return The resource ID fo the XML layout
	 */
	public int getLayoutResourceId() {
		return layoutResourceId;
	}
	
}
