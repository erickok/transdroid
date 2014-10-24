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

import org.transdroid.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Spinner that holds actions that can be performed on list selections. The spinner itself has some title, which can for
 * example be used to show the number of selected items.
 * @author Eric Kok
 */
public class SelectionModificationSpinner extends Spinner {

	private SelectionDropDownAdapter selectionAdapter;
	private OnModificationActionSelectedListener onModificationActionSelected = null;

	/**
	 * Instantiates a spinner that contains some fixed actions for a user to modify selections.
	 * @param context The interface context where the spinner will be shown in
	 */
	public SelectionModificationSpinner(Context context) {
		super(context);
		selectionAdapter = new SelectionDropDownAdapter(context);
		setAdapter(selectionAdapter);
	}

	/**
	 * Updates the fixed title text shown in the spinner, regardless of spinner item action selection.
	 * @param title The new static string to show, such as the number of selected items
	 */
	public void updateTitle(String title) {
		selectionAdapter.titleView.setText(title);
		invalidate();
	}

	/**
	 * Sets the listener for action selection events.
	 * @param onModificationActionSelected The listener that handles performing of the actions as selected in this
	 *            spinner by the user
	 */
	public void setOnModificationActionSelectedListener(OnModificationActionSelectedListener onModificationActionSelected) {
		this.onModificationActionSelected = onModificationActionSelected;
	}

	@Override
	public void setSelection(int position) {
		if (position == 0) {
			onModificationActionSelected.selectAll();
		} else if (position == 1) {
			onModificationActionSelected.selectFinished();
		} else if (position == 2) {
			onModificationActionSelected.invertSelection();
		}
		super.setSelection(position);
	}
	
	/**
	 * Local adapter that holds the actions which can be performed and a title text view that always shows instead of a
	 * list item as in a normal spinner.
	 */
	private class SelectionDropDownAdapter extends ArrayAdapter<String> {

		protected TextView titleView = null;

		public SelectionDropDownAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_1, new String[] {
					context.getString(R.string.navigation_selectall),
					context.getString(R.string.navigation_selectfinished),
					context.getString(R.string.navigation_invertselection) });
			titleView = new TextView(getContext());
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// This returns the singleton text view showing the title with the number of selected items
			return titleView;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			// This returns the actions to show in the spinner list
			return super.getView(position, convertView, parent);
		}

	}

	/**
	 * Interface to implement if an interface want to respond to selection modification actions.
	 */
	public interface OnModificationActionSelectedListener {
		public void selectAll();
		public void selectFinished();
		public void invertSelection();
	}

}
