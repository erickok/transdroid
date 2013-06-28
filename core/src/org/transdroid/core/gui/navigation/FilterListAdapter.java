package org.transdroid.core.gui.navigation;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.lists.MergeAdapter;
import org.transdroid.core.gui.lists.ViewHolderAdapter;
import org.transdroid.core.gui.navigation.StatusType.StatusTypeFilter;

import android.content.Context;
import android.view.View;

/**
 * List adapter that holds filter items, that is, servers, view types and labels. A header item is inserted where
 * appropriate.
 * @author Eric Kok
 */
@EBean
public class FilterListAdapter extends MergeAdapter {

	@RootContext
	protected Context context;
	private FilterListItemAdapter serverItems = null;
	private FilterListItemAdapter statusTypeItems = null;
	private FilterListItemAdapter labelItems = null;
	protected ViewHolderAdapter statusTypeSeparator;
	protected ViewHolderAdapter labelSeperator;
	protected ViewHolderAdapter serverSeparator;

	/**
	 * Update the list of available servers
	 * @param servers The new list of available servers
	 */
	public void updateServers(List<ServerSetting> servers) {
		if (this.serverItems == null && servers != null) {
			serverSeparator = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
					context.getString(R.string.navigation_servers)));
			serverSeparator.setViewVisibility(servers.isEmpty() ? View.GONE : View.VISIBLE);
			addAdapter(serverSeparator);
			this.serverItems = new FilterListItemAdapter(context, servers);
			addAdapter(serverItems);
		} else if (this.serverItems != null && servers != null) {
			serverSeparator.setViewVisibility(servers.isEmpty() ? View.GONE : View.VISIBLE);
			this.serverItems.update(servers);
		} else {
			serverSeparator.setViewVisibility(View.GONE);
			this.serverItems = null;
		}
	}

	/**
	 * Update the list of available status types
	 * @param statusTypes The new list of available status types
	 */
	public void updateStatusTypes(List<StatusTypeFilter> statusTypes) {
		if (this.statusTypeItems == null && statusTypes != null) {
			statusTypeSeparator = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
					context.getString(R.string.navigation_status)));
			statusTypeSeparator.setViewVisibility(statusTypes.isEmpty() ? View.GONE : View.VISIBLE);
			addAdapter(statusTypeSeparator);
			this.statusTypeItems = new FilterListItemAdapter(context, statusTypes);
			addAdapter(statusTypeItems);
		} else if (this.statusTypeItems != null && statusTypes != null) {
			statusTypeSeparator.setViewVisibility(statusTypes.isEmpty() ? View.GONE : View.VISIBLE);
			this.statusTypeItems.update(statusTypes);
		} else {
			statusTypeSeparator.setViewVisibility(View.GONE);
			this.statusTypeItems = null;
		}
	}

	/**
	 * Update the list of available labels
	 * @param labels The new list of available labels
	 */
	public void updateLabels(List<Label> labels) {
		if (this.labelItems == null && labels != null) {
			labelSeperator = new ViewHolderAdapter(FilterSeparatorView_.build(context).setText(
					context.getString(R.string.navigation_labels)));
			labelSeperator.setViewVisibility(labels.isEmpty() ? View.GONE : View.VISIBLE);
			addAdapter(labelSeperator);
			this.labelItems = new FilterListItemAdapter(context, labels);
			addAdapter(labelItems);
		} else if (this.labelItems != null && labels != null) {
			labelSeperator.setViewVisibility(labels.isEmpty() ? View.GONE : View.VISIBLE);
			this.labelItems.update(labels);
		} else {
			labelSeperator.setViewVisibility(View.GONE);
			this.labelItems = null;
		}
	}

}
