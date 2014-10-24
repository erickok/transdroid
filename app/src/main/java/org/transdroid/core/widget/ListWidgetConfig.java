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
package org.transdroid.core.widget;

import org.transdroid.core.gui.navigation.StatusType;
import org.transdroid.daemon.TorrentsSortBy;

/**
 * Represents a set of settings that define how the user configured a specific app widget.
 * @author Eric Kok
 */
public class ListWidgetConfig {

	private final int serverId;
	private final StatusType statusType;
	private final TorrentsSortBy sortBy;
	private final boolean reserveSort;
	private final boolean showStatusView;
	private final boolean useDarkTheme;

	public ListWidgetConfig(int serverId, StatusType statusType, TorrentsSortBy sortBy, boolean reverseSort,
			boolean showStatusView, boolean useDarkTheme) {
		this.serverId = serverId;
		this.statusType = statusType;
		this.sortBy = sortBy;
		this.reserveSort = reverseSort;
		this.showStatusView = showStatusView;
		this.useDarkTheme = useDarkTheme;
	}

	public int getServerId() {
		return serverId;
	}

	public StatusType getStatusType() {
		return statusType;
	}

	public TorrentsSortBy getSortBy() {
		return sortBy;
	}

	public boolean shouldReserveSort() {
		return reserveSort;
	}

	public boolean shouldShowStatusView() {
		return showStatusView;
	}
	
	public boolean shouldUseDarkTheme() {
		return useDarkTheme;
	}

}
