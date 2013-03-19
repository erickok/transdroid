package org.transdroid.core.gui.navigation;

import java.util.Arrays;
import java.util.List;

import org.transdroid.core.R;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Enumeration of all status types, which filter the list of shown torrents based on transfer activity.
 * @author Eric Kok
 */
public enum StatusType {

	ShowAll {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.ShowAll, context.getString(R.string.navigation_status_showall));
		}
	},
	OnlyDownloading {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyDownloading, context.getString(R.string.navigation_status_onlydown));
		}
	},
	OnlyUploading {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyUploading, context.getString(R.string.navigation_status_onlyup));
		}
	},
	OnlyActive {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(StatusType.OnlyActive, context.getString(R.string.navigation_status_onlyactive));
		}
	},
	OnlyInactive {
		StatusTypeFilter getFilterItem(Context context) {
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
	abstract StatusTypeFilter getFilterItem(Context context);

	public static class StatusTypeFilter implements SimpleListItem, NavigationFilter {

		private final StatusType statusType;
		private final String name;

		StatusTypeFilter(StatusType statusType, String name) {
			this.statusType = statusType;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean matches(Torrent torrent) {
			switch (statusType) {
			case OnlyDownloading:
				return torrent.getStatusCode() == TorrentStatus.Downloading;
			case OnlyUploading:
				return torrent.getStatusCode() == TorrentStatus.Seeding;
			case OnlyActive:
				return torrent.getStatusCode() == TorrentStatus.Downloading
						|| torrent.getStatusCode() == TorrentStatus.Seeding;
			case OnlyInactive:
				return torrent.getStatusCode() == TorrentStatus.Checking
						|| torrent.getStatusCode() == TorrentStatus.Error
						|| torrent.getStatusCode() == TorrentStatus.Paused
						|| torrent.getStatusCode() == TorrentStatus.Queued
						|| torrent.getStatusCode() == TorrentStatus.Unknown
						|| torrent.getStatusCode() == TorrentStatus.Waiting;
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
