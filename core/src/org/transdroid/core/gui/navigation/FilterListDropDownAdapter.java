package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;

import android.view.View;
import android.view.ViewGroup;

/**
 * List adapter that holds filter items, that is, servers, view types and labels and is displayed as content to a 
 * Spinner instead of a ListView.
 * @author Eric Kok
 */
@EBean
public class FilterListDropDownAdapter extends FilterListAdapter {

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// This returns the item shown in the spinner
		NavigationSelectionView filterItemView;
		if (convertView == null || !(convertView instanceof NavigationSelectionView)) {
			filterItemView = NavigationSelectionView_.build(context).setNavigationFilterManager(navigationFilterManager);
		} else {
			filterItemView = (NavigationSelectionView) convertView;
		}
		filterItemView.bind();
		return filterItemView;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// This returns the item to show in the drop down list
		return super.getView(position, convertView, parent);
	}
	
}
