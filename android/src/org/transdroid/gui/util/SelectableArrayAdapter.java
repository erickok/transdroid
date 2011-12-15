package org.transdroid.gui.util;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class SelectableArrayAdapter<T> extends ArrayAdapter<T> {

	private List<T> selected;
	private OnSelectedChangedListener listener;

	public SelectableArrayAdapter(Context context, List<T> objects, OnSelectedChangedListener listener) {
		this(context, objects);
		this.listener = listener;
	}

	public SelectableArrayAdapter(Context context, List<T> objects) {
		super(context, objects);
		this.selected = new ArrayList<T>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, convertView, parent, selected.contains(getItem(position)));
	}

	public abstract View getView(int position, View convertView, ViewGroup paret, boolean selected);

	/**
	 * Is called by views to indicate an item was either selected or deselected
	 * @param item The item for which the selected state has changed
	 * @param isChecked If the item is now checked/selected
	 */
	public void itemChecked(T item, boolean isChecked) {
		if (!isChecked && selected.contains(item)) {
			selected.remove(item);
		}
		if (isChecked && !selected.contains(item)) {
			selected.add(item);
		}
		if (listener != null) {
			listener.onSelectedResultsChanged();
		}
	}
	
	/**
	 * Whether an item is currently selected
	 * @param item The item, which should be present in the underlying list
	 * @return True if the item is currently selected, false otherwise
	 */
	public boolean isItemChecked(T item) {
		return selected.contains(item);
	}

	@Override
    public void replace(List<T> objects) {
		// Reset the 'selected' list as well
		this.selected.clear();
		super.replace(objects);
		if (listener != null) {
			listener.onSelectedResultsChanged();
		}
    }
	
	/**
	 * Returns the list of all checked/selected items
	 * @return The list of selected items
	 */
	public List<T> getSelected() {
		return this.selected;
	}
	
	/**
	 * Clears the entire item selection, leaving it empty
	 */
	public void clearSelection() {
		this.selected.clear();
		if (listener != null) {
			listener.onSelectedResultsChanged();
		}
	}

	public void invertSelection() {
		for (T item : getAllItems()) {
			if (selected.contains(item)) {
				selected.remove(item);
			} else {
				selected.add(item);
			}
		}
		if (listener != null) {
			listener.onSelectedResultsChanged();
		}
	}

	/**
	 * Listener to changes in the state of selected items in an ArrayAdapter
	 */
	public interface OnSelectedChangedListener {
		public void onSelectedResultsChanged();
	}
	
}
