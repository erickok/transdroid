package org.transdroid.core.gui.navigation;

import java.util.Arrays;
import java.util.List;

import org.transdroid.core.R;
import org.transdroid.core.gui.lists.SimpleListItem;

import android.content.Context;

/**
 * Enumeration of all status types, which filter the list of shown torrents based on transfer activity.
 * @author Eric Kok
 */
public enum StatusType {

	ShowAll {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(context.getString(R.string.navigation_status_showall));
		}
	},
	OnlyDownloading {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(context.getString(R.string.navigation_status_onlydown));
		}
	},
	OnlyUploading {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(context.getString(R.string.navigation_status_onlyup));
		}
	},
	OnlyActive {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(context.getString(R.string.navigation_status_onlyactive));
		}
	},
	OnlyInactive {
		StatusTypeFilter getFilterItem(Context context) {
			return new StatusTypeFilter(context.getString(R.string.navigation_status_onlyinactive));
		}
	};

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

	public static class StatusTypeFilter implements SimpleListItem {

		private final String name;

		StatusTypeFilter(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

	}

}
