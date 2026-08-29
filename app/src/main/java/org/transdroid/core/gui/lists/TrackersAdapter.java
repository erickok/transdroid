/*
 * Copyright 2010-2024 Eric Kok et al.
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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.transdroid.daemon.Tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that shows a list of {@link Tracker} objects with their connection status.
 *
 * @author Eric Kok
 */
public class TrackersAdapter extends BaseAdapter {

    private final Context context;
    private List<Tracker> items;

    public TrackersAdapter(Context context, List<Tracker> items) {
        this.context = context;
        this.items = items == null ? new ArrayList<>() : items;
    }

    /**
     * Replaces all items in this adapter with the given new list of trackers.
     *
     * @param newItems The new list of trackers to display
     */
    public void update(List<Tracker> newItems) {
        this.items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Tracker getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TrackerView trackerView;
        if (convertView == null) {
            trackerView = TrackerView_.build(context);
        } else {
            trackerView = (TrackerView) convertView;
        }
        trackerView.bind(getItem(position));
        return trackerView;
    }

}
