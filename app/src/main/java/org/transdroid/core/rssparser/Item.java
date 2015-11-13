/*
 * Taken from the 'Learning Android' project, released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android and modified heavily for Transdroid
 */
package org.transdroid.core.rssparser;

import java.util.Date;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {

	private int id;
	private String title;
	private String link;
	private String description;
	private Date pubDate;
	private String enclosureUrl;
	private String enclosureType;
	private long enclosureLength;

	/**
	 * isNew is not parsed from the RSS feed but may be set using {@link #setIsNew(boolean)} manually
	 */
	private boolean isNew;

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return this.title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getLink() {
		return this.link;
	}

	public void setPubdate(Date pubdate) {
		this.pubDate = pubdate;
	}

	public Date getPubdate() {
		return this.pubDate;
	}

	public void setEnclosureUrl(String enclosureUrl) {
		this.enclosureUrl = enclosureUrl;
	}

	public void setEnclosureLength(long enclosureLength) {
		this.enclosureLength = enclosureLength;
	}

	public void setEnclosureType(String enclosureType) {
		this.enclosureType = enclosureType;
	}

	public String getEnclosureUrl() {
		return this.enclosureUrl;
	}

	public String getEnclosureType() {
		return this.enclosureType;
	}

	public long getEnclosureLength() {
		return this.enclosureLength;
	}

	public void setIsNew(boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isNew() {
		return isNew;
	}

	/**
	 * Returns 'the' item link as string, which preferably is the enclosure URL, but otherwise the link (or null if that
	 * is empty too).
	 * @return A single link URL string to be used
	 */
	public String getTheLink() {
		if (this.getEnclosureUrl() != null) {
			return this.getEnclosureUrl();
		} else {
			return this.getLink();
		}
	}

	/**
	 * Returns 'the' item link as URI, which preferably is the enclosure URL, but otherwise the link (or null if that is
	 * empty too).
	 * @return A single link URI to be used
	 */
	public Uri getTheLinkUri() {
		return Uri.parse(getTheLink());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(id);
		out.writeString(title);
		out.writeString(link);
		out.writeString(description);
		out.writeLong(pubDate == null ? -1 : pubDate.getTime());
		out.writeString(enclosureUrl);
		out.writeString(enclosureType);
		out.writeLong(enclosureLength);
		out.writeInt(isNew ? 1 : 0);
	}

	public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
		public Item createFromParcel(Parcel in) {
			return new Item(in);
		}

		public Item[] newArray(int size) {
			return new Item[size];
		}
	};

	public Item() {
	}

	private Item(Parcel in) {
		id = in.readInt();
		title = in.readString();
		link = in.readString();
		description = in.readString();
		long pubDateIn = in.readLong();
		pubDate = pubDateIn == -1 ? null : new Date(pubDateIn);
		enclosureUrl = in.readString();
		enclosureType = in.readString();
		enclosureLength = in.readLong();
		isNew = in.readInt() == 1;
	}

}