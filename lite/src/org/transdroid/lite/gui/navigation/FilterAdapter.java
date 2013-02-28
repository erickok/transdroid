package org.transdroid.lite.gui.navigation;

import java.util.List;

import org.transdroid.lite.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.commonsware.cwac.merge.MergeAdapter;

/**
 * List adapter that holds filter items, that is, servers, view types and labels. A header item is intersted where
 * appropriate.
 * @author Eric Kok
 */
public class FilterAdapter extends MergeAdapter {

	private Context context;
	private FilterItemAdapter serverItems = null;
	private FilterItemAdapter statusTypeItems = null;
	private FilterItemAdapter labelItems = null;

	public FilterAdapter(Context context) {
		this.context = context;
	}

	/**
	 * Update the list of available servers.
	 * @param servers The new list of available servers
	 */
	public void updateServers(List<FilterItem> servers) {
		if (this.serverItems == null && servers != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_servers)), false);
			this.serverItems = new FilterItemAdapter(context, servers);
			addAdapter(serverItems);
		} else if (this.serverItems != null && servers != null) {
			this.serverItems.update(servers);
		} else {
			this.serverItems = null;
		}
	}

	/**
	 * Update the list of available status types.
	 * @param statusTypes The new list of available status types
	 */
	public void updateStatusTypes(List<FilterItem> statusTypes) {
		if (this.statusTypeItems == null && statusTypes != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_status)), false);
			this.statusTypeItems = new FilterItemAdapter(context, statusTypes);
			addAdapter(statusTypeItems);
		} else if (this.statusTypeItems != null && statusTypes != null) {
			this.statusTypeItems.update(statusTypes);
		} else {
			this.statusTypeItems = null;
		}
	}

	/**
	 * Update the list of available labels.
	 * @param labels The new list of available labels
	 */
	public void updateLabels(List<FilterItem> labels) {
		if (this.labelItems == null && labels != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_labels)), false);
			this.labelItems = new FilterItemAdapter(context, labels);
			addAdapter(labelItems);
		} else if (this.serverItems != null && labels != null) {
			this.labelItems.update(labels);
		} else {
			this.labelItems = null;
		}
	}

	protected class FilterItemAdapter extends BaseAdapter {

		private final Context context;
		private List<FilterItem> items;

		public FilterItemAdapter(Context context, List<FilterItem> items) {
			this.context = context;
			this.items = items;
		}

		/**
		 * Allows updating of the full data list underlying this adapter, replacing all items
		 * @param newItems The new list of filter items to display
		 */
		public void update(List<FilterItem> newItems) {
			this.items = newItems;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public FilterItem getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FilterItemView filterItemView;
			if (convertView == null) {
				filterItemView = FilterItemView_.build(context);
			} else {
				filterItemView = (FilterItemView) convertView;
			}
			filterItemView.bind(getItem(position));
			return filterItemView;
		}

	}
}
