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
package org.transdroid.core.app.search;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a search result as retrieved by querying the Torrent Search package.
 * @author Eric Kok
 */
public class SearchResult implements Parcelable {

	private final int id;
	private final String name;
	private final String torrentUrl;
	private final String detailsUrl;
	private final String size;
	private final Date addedOn;
	private final String seeders;
	private final String leechers;

	public SearchResult(int id, String name, String torrentUrl, String detailsUrl, String size, long addedOnTime,
			String seeders, String leechers) {
		this.id = id;
		this.name = name;
		this.torrentUrl = torrentUrl;
		this.detailsUrl = detailsUrl;
		this.size = size;
		this.addedOn = (addedOnTime == -1L) ? null : new Date(addedOnTime);
		this.seeders = seeders;
		this.leechers = leechers;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTorrentUrl() {
		return torrentUrl;
	}

	public String getDetailsUrl() {
		return detailsUrl;
	}

	public String getSize() {
		return size;
	}

	public Date getAddedOn() {
		return addedOn;
	}

	public String getSeeders() {
		return seeders;
	}

	public String getLeechers() {
		return leechers;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(id);
		out.writeString(name);
		out.writeString(torrentUrl);
		out.writeString(detailsUrl);
		out.writeString(size);
		out.writeLong(addedOn == null ? -1 : addedOn.getTime());
		out.writeString(seeders);
		out.writeString(leechers);
	}

	public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
		public SearchResult createFromParcel(Parcel in) {
			return new SearchResult(in);
		}

		public SearchResult[] newArray(int size) {
			return new SearchResult[size];
		}
	};

	public SearchResult(Parcel in) {
		id = in.readInt();
		name = in.readString();
		torrentUrl = in.readString();
		detailsUrl = in.readString();
		size = in.readString();
		long addedOnIn = in.readLong();
		addedOn = addedOnIn == -1 ? null : new Date(addedOnIn);
		seeders = in.readString();
		leechers = in.readString();
	}

}
