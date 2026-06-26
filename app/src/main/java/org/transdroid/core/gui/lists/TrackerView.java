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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.daemon.Tracker;

/**
 * View that represents a single {@link Tracker}, showing its URL and (optional) status message, with
 * a left-edge bar coloured by the tracker's connection status.
 *
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_tracker)
public class TrackerView extends StatusBarLayout {

    @ViewById
    protected TextView urlText, messageText;

    public TrackerView(Context context) {
        super(context, null);
    }

    public void bind(Tracker tracker) {

        urlText.setText(tracker.getUrl());

        if (TextUtils.isEmpty(tracker.getMessage())) {
            messageText.setVisibility(View.GONE);
        } else {
            messageText.setText(tracker.getMessage());
            messageText.setVisibility(View.VISIBLE);
        }

        switch (tracker.getStatus()) {
            case WORKING:
                setBarColor(getResources().getColor(R.color.green));
                break;
            case ERROR:
                setBarColor(getResources().getColor(R.color.red));
                break;
            case DISABLED:
            case UNKNOWN:
            default:
                setBarColor(getResources().getColor(R.color.grey));
                break;
        }

    }

}
