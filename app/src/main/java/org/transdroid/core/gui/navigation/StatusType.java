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

import java.util.Arrays;
import java.util.List;

import org.transdroid.R;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.daemon.Torrent;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Enumeration of all status types, which filter the list of shown torrents based on transfer activity.
 * @author Eric Kok
 */
public enum StatusType {

	ShowAll {
		public StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.ShowAll, context.getString(R.string.navigation_status_showall));
		}
	},
	OnlyDownloading {
		public StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyDownloading, context.getString(R.string.navigation_status_onlydown));
		}
	},
	OnlyUploading {
		public StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyUploading, context.getString(R.string.navigation_status_onlyup));
		}
	},
	OnlyActive {
		public StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyActive, context.getString(R.string.navigation_status_onlyactive));
		}
	},
	OnlyInactive {
		public StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyInactive, context.getString(R.string.navigation_status_onlyinactive));
		}
	};

	/**
	 * Returns the status type to show all torrents, represented as filter item to show in the navigation list.
	 * @param context The Android UI context, to access translations
	 * @return The show ShowAll status type filter item
	 */
	public static StatusTypeFilter getShowAllType(Context context) {
		return ShowAll.getFilterItem(context);
	}
	
	/**
	 * Returns a list with all status types, represented as filter item that can be shown in the GUI.
	 * @param context The Android UI context, to access translations
	 * @return A list of filter items for all available status types
	 */
	public static List<StatusTypeFilter> getAllStatusTypes(Context context) {
		return Arrays.asList(ShowAll.getFilterItem(context), OnlyDownloading.getFilterItem(context),
				OnlyUploading.getFilterItem(context), OnlyActive.getFilterItem(context),
				OnlyInactive.getFilterItem(context));
	}

	/**
	 * Every status type can return a filter item that represents it in the navigation
	 * @param context The Android UI context, to access translations
	 * @return A filter item object to show in the GUI
	 */
	public abstract StatusTypeFilter getFilterItem(Context context);

	public static class StatusTypeFilter implements SimpleListItem, NavigationFilter {

		private final StatusType statusType;
		private final String name;

		StatusTypeFilter(StatusType statusType, String name) {
			this.statusType = statusType;
			this.name = name;
		}

		public StatusType getStatusType() {
			return statusType;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getCode() {
			// Uses the class name and status type enum to provide a unique navigation filter code
			return StatusTypeFilter.class.getSimpleName() + "_" + statusType.name();
		}

		/**
		 * Returns true if the torrent status matches this (selected) status type, false otherwise
		 * @param torrent The torrent to match against this status type
		 * @param dormantAsInactive If true, dormant (0KB/s, so no data transfer) torrents are never actively
		 *            downloading or seeding
		 */
		@Override
		public boolean matches(Torrent torrent, boolean dormantAsInactive) {
			switch (statusType) {
			case OnlyDownloading:
				return torrent.isDownloading(dormantAsInactive);
			case OnlyUploading:
				return torrent.isSeeding(dormantAsInactive);
			case OnlyActive:
				return torrent.isDownloading(dormantAsInactive)
						|| torrent.isSeeding(dormantAsInactive);
			case OnlyInactive:
				return !torrent.isDownloading(dormantAsInactive) && !torrent.isSeeding(dormantAsInactive);
			default:
				return true;
			}
		}
		
		private StatusTypeFilter(Parcel in) {
			this.statusType = StatusType.valueOf(in.readString());
			this.name = in.readString();
		}

	    public static final Parcelable.Creator<StatusTypeFilter> CREATOR = new Parcelable.Creator<StatusTypeFilter>() {
	    	public StatusTypeFilter createFromParcel(Parcel in) {
	    		return new StatusTypeFilter(in);
	    	}

			public StatusTypeFilter[] newArray(int size) {
			    return new StatusTypeFilter[size];
			}
	    };

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(statusType.name());
			dest.writeString(name);
		}
		
	}

}
