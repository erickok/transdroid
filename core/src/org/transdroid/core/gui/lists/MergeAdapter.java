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
package org.transdroid.core.gui.lists;

import java.util.ArrayList;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

/**
 * An adapter that can contain many other adapters and shows them in sequence. Taken from
 * http://stackoverflow.com/questions/7964259/android-attaching-multiple-adapters-to-one-adapter and based on the Apache
 * 2-licensed CommonsWare MergeAdapter.
 * @author Eric Kok
 * @author Alex Amiryan
 * @author Mark Murphy
 */
public class MergeAdapter extends BaseAdapter implements SectionIndexer {

	protected ArrayList<ListAdapter> pieces = new ArrayList<ListAdapter>();
	protected String noItemsText;

	/**
	 * Stock constructor, simply chaining to the superclass.
	 */
	public MergeAdapter() {
		super();
	}

	/**
	 * Adds a new adapter to the roster of things to appear in the aggregate list.
	 * @param adapter Source for row views for this section
	 */
	public void addAdapter(ListAdapter adapter) {
		pieces.add(adapter);
		adapter.registerDataSetObserver(new CascadeDataSetObserver());
	}

	/**
	 * Get the data item associated with the specified position in the data set.
	 * @param position Position of the item whose data we want
	 */
	public Object getItem(int position) {
		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				return (piece.getItem(position));
			}

			position -= size;
		}

		return (null);
	}

	public void setNoItemsText(String text) {
		noItemsText = text;
	}

	/**
	 * Get the adapter associated with the specified position in the data set.
	 * @param position Position of the item whose adapter we want
	 */
	public ListAdapter getAdapter(int position) {
		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				return (piece);
			}

			position -= size;
		}

		return (null);
	}

	/**
	 * How many items are in the data set represented by this {@link Adapter}.
	 */
	public int getCount() {
		int total = 0;

		for (ListAdapter piece : pieces) {
			total += piece.getCount();
		}

		if (total == 0 && noItemsText != null) {
			total = 1;
		}

		return (total);
	}

	/**
	 * Returns the number of types of {@link View}s that will be created by {@link #getView(int, View, ViewGroup)}.
	 */
	@Override
	public int getViewTypeCount() {
		int total = 0;

		for (ListAdapter piece : pieces) {
			total += piece.getViewTypeCount();
		}

		return (Math.max(total, 1)); // needed for setListAdapter() before
										// content add'
	}

	/**
	 * Get the type of {@link View} that will be created by {@link #getView(int, View, ViewGroup)} for the specified item.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public int getItemViewType(int position) {
		int typeOffset = 0;
		int result = -1;

		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				result = typeOffset + piece.getItemViewType(position);
				break;
			}

			position -= size;
			typeOffset += piece.getViewTypeCount();
		}

		return (result);
	}

	/**
	 * Are all items in this {@link ListAdapter} enabled? If yes it means all items are selectable and clickable.
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return (false);
	}

	/**
	 * Returns true if the item at the specified position is not a separator.
	 * @param position Position of the item whose data we want
	 */
	@Override
	public boolean isEnabled(int position) {
		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				return (piece.isEnabled(position));
			}

			position -= size;
		}

		return (false);
	}

	/**
	 * Get a {@link View} that displays the data at the specified position in the data set.
	 * @param position Position of the item whose data we want
	 * @param convertView View to recycle, if not null
	 * @param parent ViewGroup containing the returned View
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {

				return (piece.getView(position, convertView, parent));
			}

			position -= size;
		}

		if (noItemsText != null) {
			TextView text = new TextView(parent.getContext());
			text.setText(noItemsText);
			return text;
		}

		return (null);
	}

	/**
	 * Get the row id associated with the specified position in the list.
	 * @param position Position of the item whose data we want
	 */
	public long getItemId(int position) {
		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				return (piece.getItemId(position));
			}

			position -= size;
		}

		return (-1);
	}

	public final int getPositionForSection(int section) {
		int position = 0;

		for (ListAdapter piece : pieces) {
			if (piece instanceof SectionIndexer) {
				Object[] sections = ((SectionIndexer) piece).getSections();
				int numSections = 0;

				if (sections != null) {
					numSections = sections.length;
				}

				if (section < numSections) {
					return (position + ((SectionIndexer) piece).getPositionForSection(section));
				} else if (sections != null) {
					section -= numSections;
				}
			}

			position += piece.getCount();
		}

		return (0);
	}

	public final int getSectionForPosition(int position) {
		int section = 0;

		for (ListAdapter piece : pieces) {
			int size = piece.getCount();

			if (position < size) {
				if (piece instanceof SectionIndexer) {
					return (section + ((SectionIndexer) piece).getSectionForPosition(position));
				}

				return (0);
			} else {
				if (piece instanceof SectionIndexer) {
					Object[] sections = ((SectionIndexer) piece).getSections();

					if (sections != null) {
						section += sections.length;
					}
				}
			}

			position -= size;
		}

		return (0);
	}

	public final Object[] getSections() {
		ArrayList<Object> sections = new ArrayList<Object>();

		for (ListAdapter piece : pieces) {
			if (piece instanceof SectionIndexer) {
				Object[] curSections = ((SectionIndexer) piece).getSections();

				if (curSections != null) {
					for (Object section : curSections) {
						sections.add(section);
					}
				}
			}
		}

		if (sections.size() == 0) {
			return (null);
		}

		return (sections.toArray(new Object[0]));
	}

	private class CascadeDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			notifyDataSetInvalidated();
		}
	}

}
