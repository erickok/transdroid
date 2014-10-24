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
package org.transdroid.core.gui.lists;

import org.transdroid.R;
import org.transdroid.daemon.TorrentsSortBy;

import android.content.Context;

/**
 * Represents a way in which a torrents list can be sorted.
 * @author Eric Kok
 */
public class SortByListItem implements SimpleListItem {

	private final TorrentsSortBy sortBy;
	private final String name;

	public SortByListItem(Context context, TorrentsSortBy sortBy) {
		this.sortBy = sortBy;
		switch (sortBy) {
		case DateAdded:
			this.name = context.getString(R.string.action_sort_added);
			break;
		case DateDone:
			this.name = context.getString(R.string.action_sort_done);
			break;
		case Ratio:
			this.name = context.getString(R.string.action_sort_ratio);
			break;
		case Status:
			this.name = context.getString(R.string.action_sort_status);
			break;
		case UploadSpeed:
			this.name = context.getString(R.string.action_sort_upspeed);
			break;
		default:
			this.name = context.getString(R.string.action_sort_alpha);
			break;
		}
	}
	
	/**
	 * Returns the contained represented sort order.
	 * @return The sort by order as its enumeration value
	 */
	public TorrentsSortBy getSortBy() {
		return sortBy;
	}
	
	@Override
	public String getName() {
		return name;
	}

}
