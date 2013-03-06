package org.transdroid.core.gui.navigation;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;
import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.lists.SimpleListItemAdapter;
import org.transdroid.core.gui.navigation.FilterSeparatorView_;

import android.content.Context;

import com.commonsware.cwac.merge.MergeAdapter;

/**
 * List adapter that holds filter items, that is, servers, view types and labels. A header item is inserted where
 * appropriate.
 * @author Eric Kok
 */
@EBean
public class FilterListAdapter extends MergeAdapter {

	@RootContext
	protected Context context;
	private SimpleListItemAdapter serverItems = null;
	private SimpleListItemAdapter statusTypeItems = null;
	private SimpleListItemAdapter labelItems = null;

	/**
	 * Update the list of available servers
	 * @param servers The new list of available servers
	 */
	public void updateServers(List<? extends SimpleListItem> servers) {
		if (this.serverItems == null && servers != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_servers)), false);
			this.serverItems = new SimpleListItemAdapter(context, servers);
			addAdapter(serverItems);
		} else if (this.serverItems != null && servers != null) {
			this.serverItems.update(servers);
		} else {
			this.serverItems = null;
		}
	}

	/**
	 * Update the list of available status types
	 * @param statusTypes The new list of available status types
	 */
	public void updateStatusTypes(List<? extends SimpleListItem> statusTypes) {
		if (this.statusTypeItems == null && statusTypes != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_status)), false);
			this.statusTypeItems = new SimpleListItemAdapter(context, statusTypes);
			addAdapter(statusTypeItems);
		} else if (this.statusTypeItems != null && statusTypes != null) {
			this.statusTypeItems.update(statusTypes);
		} else {
			this.statusTypeItems = null;
		}
	}

	/**
	 * Update the list of available labels
	 * @param labels The new list of available labels
	 */
	public void updateLabels(List<? extends SimpleListItem> labels) {
		if (this.labelItems == null && labels != null) {
			addView(FilterSeparatorView_.build(context).setText(context.getString(R.string.navigation_labels)), false);
			this.labelItems = new SimpleListItemAdapter(context, labels);
			addAdapter(labelItems);
		} else if (this.serverItems != null && labels != null) {
			this.labelItems.update(labels);
		} else {
			this.labelItems = null;
		}
	}

}
