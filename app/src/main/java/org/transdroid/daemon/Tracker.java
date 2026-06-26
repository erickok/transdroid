/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.transdroid.daemon;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a single tracker of a torrent, together with its connection status and (optional)
 * status/error message.
 *
 * @author erickok
 */
public final class Tracker implements Parcelable {

    public static final Parcelable.Creator<Tracker> CREATOR = new Parcelable.Creator<Tracker>() {
        public Tracker createFromParcel(Parcel in) {
            return new Tracker(in);
        }

        public Tracker[] newArray(int size) {
            return new Tracker[size];
        }
    };

    private final String url;
    private final TrackerStatus status;
    private final String message;

    public Tracker(String url, TrackerStatus status, String message) {
        this.url = url;
        this.status = status == null ? TrackerStatus.UNKNOWN : status;
        this.message = message;
    }

    private Tracker(Parcel in) {
        this.url = in.readString();
        this.status = TrackerStatus.fromCode(in.readInt());
        this.message = in.readString();
    }

    public String getUrl() {
        return url;
    }

    public TrackerStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeInt(status.getCode());
        dest.writeString(message);
    }

}
