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
 * Represents a single peer connected to a torrent on a server daemon.
 *
 * @author erickok
 */
public final class Peer implements Parcelable {

    public static final Parcelable.Creator<Peer> CREATOR = new Parcelable.Creator<Peer>() {
        public Peer createFromParcel(Parcel in) {
            return new Peer(in);
        }

        public Peer[] newArray(int size) {
            return new Peer[size];
        }
    };

    private final String address;
    private final String client;
    private final int downSpeed;
    private final int upSpeed;
    private final float progress;
    private final Boolean isEncrypted;
    private String countryCode;

    public Peer(String address, String client, int downSpeed, int upSpeed, float progress, Boolean isEncrypted,
                String countryCode) {
        this.address = address;
        this.client = client;
        this.downSpeed = downSpeed;
        this.upSpeed = upSpeed;
        this.progress = progress;
        this.isEncrypted = isEncrypted;
        this.countryCode = countryCode;
    }

    private Peer(Parcel in) {
        this.address = in.readString();
        this.client = in.readString();
        this.downSpeed = in.readInt();
        this.upSpeed = in.readInt();
        this.progress = in.readFloat();
        // Nullable Boolean stored as int sentinel: -1 unknown, 0 false, 1 true
        int enc = in.readInt();
        this.isEncrypted = (enc == -1) ? null : (enc == 1);
        this.countryCode = in.readString();
    }

    public String getAddress() {
        return address;
    }

    public String getClient() {
        return client;
    }

    public int getDownSpeed() {
        return downSpeed;
    }

    public int getUpSpeed() {
        return upSpeed;
    }

    /**
     * The peer's download progress, in range [0,1].
     *
     * @return The fraction completed, or a negative number if unknown
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Whether the connection to this peer is encrypted, or null if the client does not report it.
     */
    public Boolean isEncrypted() {
        return isEncrypted;
    }

    /**
     * The 2-letter ISO country code for this peer's IP, or null if unknown.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Overrides the country code, e.g. from a locally resolved GeoIP database.
     *
     * @param countryCode The 2-letter ISO country code to set
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(client);
        dest.writeInt(downSpeed);
        dest.writeInt(upSpeed);
        dest.writeFloat(progress);
        dest.writeInt(isEncrypted == null ? -1 : (isEncrypted ? 1 : 0));
        dest.writeString(countryCode);
    }

}
