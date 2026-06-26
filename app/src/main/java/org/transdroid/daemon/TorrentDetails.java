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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a torrent's fine details (currently only the trackers).
 *
 * @author erickok
 */
public final class TorrentDetails implements Parcelable {

    public static final Parcelable.Creator<TorrentDetails> CREATOR = new Parcelable.Creator<TorrentDetails>() {
        public TorrentDetails createFromParcel(Parcel in) {
            return new TorrentDetails(in);
        }

        public TorrentDetails[] newArray(int size) {
            return new TorrentDetails[size];
        }
    };
    private final List<String> trackers;
    private final List<String> errors;
    private final List<Integer> pieces;
    private final List<Tracker> trackerDetails;

    public TorrentDetails(List<String> trackers, List<String> errors) {
        this(trackers, errors, new ArrayList<Integer>(), null);
    }

    public TorrentDetails(List<String> trackers, List<String> errors, List<Integer> pieces) {
        this(trackers, errors, pieces, null);
    }

    public TorrentDetails(List<String> trackers, List<String> errors, List<Integer> pieces,
                          List<Tracker> trackerDetails) {
        this.trackers = trackers;
        this.errors = errors;
        this.pieces = pieces == null ? new ArrayList<Integer>() : pieces;
        this.trackerDetails = trackerDetails == null ? new ArrayList<Tracker>() : trackerDetails;
    }

    private TorrentDetails(Parcel in) {
        this.trackers = in.createStringArrayList();
        this.errors = in.createStringArrayList();

        int[] piecesarray = in.createIntArray();
        this.pieces = new ArrayList<>(piecesarray.length);
        for (int i : piecesarray) {
            this.pieces.add(i);
        }

        this.trackerDetails = in.createTypedArrayList(Tracker.CREATOR);
    }

    public List<String> getTrackers() {
        return trackers;
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * Builds a string with one tracker (URL) per line
     *
     * @return A \n-separated string of trackers
     */
    public String getTrackersText() {
        // Build a string with one tracker URL per line
        StringBuilder trackersText = new StringBuilder();
        for (String tracker : trackers) {
            trackersText.append(trackersText.length() == 0 ? "" : "\n").append(tracker);
        }
        return trackersText.toString();
    }

    /**
     * Builds a string with one error per line
     *
     * @return A \n-separated string of errors
     */
    public String getErrorsText() {
        // Build a string with one tracker error per line
        StringBuilder errorsText = new StringBuilder();
        for (String error : errors) {
            errorsText.append(errorsText.length() == 0 ? "" : "\n").append(error);
        }
        return errorsText.toString();
    }

    public List<Integer> getPieces() {
        return this.pieces;
    }

    /**
     * The per-tracker details (URL, connection status and optional message). May be empty if the
     * client/adapter does not provide structured tracker information; callers should fall back to
     * {@link #getTrackers()} in that case.
     *
     * @return The list of trackers with status, never null
     */
    public List<Tracker> getTrackerDetails() {
        return this.trackerDetails;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(trackers);
        dest.writeStringList(errors);

        int[] piecesarray = new int[this.pieces.size()];
        for (int i = 0; i < this.pieces.size(); i++) {
            if (this.pieces.get(i) != null) {
                piecesarray[i] = this.pieces.get(i);
            }
        }
        dest.writeIntArray(piecesarray);
        dest.writeTypedList(trackerDetails);
    }

}
