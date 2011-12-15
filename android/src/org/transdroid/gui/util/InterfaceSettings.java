/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.gui.util;

/**
 * A class that contains all the user interface settings.
 * 
 * @author erickok
 *
 */
public class InterfaceSettings {

	private boolean swipeLabels;
	private int refreshTimerInterval;
	private boolean hideRefreshMessage;
	private boolean askBeforeRemove;
	
	public InterfaceSettings(boolean swipeLabels, int refreshTimerInterval, boolean showOnlyDownloading, 
			boolean hideRefreshMessage, boolean askBeforeRemove, boolean enableAds) {
		this.swipeLabels = swipeLabels;
		this.refreshTimerInterval = refreshTimerInterval;
		this.hideRefreshMessage = hideRefreshMessage;
		this.askBeforeRemove = askBeforeRemove;
	}
	
	public int getRefreshTimerInterval() {
		return refreshTimerInterval;
	}
	
	public boolean shouldHideRefreshMessage() {
		return hideRefreshMessage;
	}

	public boolean getAskBeforeRemove() {
		return askBeforeRemove;
	}

	public boolean shouldSwipeLabels() {
		return swipeLabels;
	}

}
