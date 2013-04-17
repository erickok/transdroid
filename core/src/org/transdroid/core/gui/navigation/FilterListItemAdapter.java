package org.transdroid.core.gui.navigation;

import java.util.List;

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.lists.SimpleListItemView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class FilterListItemAdapter extends BaseAdapter {

	private final Context context;
	private List<? extends SimpleListItem> items;

	public FilterListItemAdapter(Context context, List<? extends SimpleListItem> items) {
		this.context = context;
		this.items = items;
	}

	/**
	 * Allows updating of the full data list underlying this adapter, replacing all items
	 * @param newItems The new list of filter items to display
	 */
	public void update(List<? extends SimpleListItem> newItems) {
		this.items = newItems;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public SimpleListItem getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FilterListItemView filterItemView;
		if (convertView == null || !(convertView instanceof SimpleListItemView)) {
			filterItemView = FilterListItemView_.build(context);
		} else {
			filterItemView = (FilterListItemView) convertView;
		}
		filterItemView.bind(getItem(position));
		return filterItemView;
	}

}