/*
 * Taken from the 'Learning Android' project, released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android and modified heavily for Transdroid
 */
package org.transdroid.core.rssparser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Channel implements Parcelable {

	private int id;
	private String title;
	private String link;
	private String description;
	private Date pubDate;
	private long lastBuildDate;
	private List<String> categories;
	private List<Item> items;
	private String image;

	public Channel() {
		this.categories = new ArrayList<>();
		this.items = new ArrayList<>();
	}

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
		return title;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getLink() {
		return link;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setPubDate(Date date) {
		this.pubDate = date;
	}

	public Date getPubDate() {
		return pubDate;
	}

	public void setLastBuildDate(long lastBuildDate) {
		this.lastBuildDate = lastBuildDate;
	}

	public long getLastBuildDate() {
		return lastBuildDate;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public void addCategory(String category) {
		this.categories.add(category);
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public void addItem(Item item) {
		this.items.add(item);
	}

	public List<Item> getItems() {
		return items;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getImage() {
		return image;
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
		out.writeLong(lastBuildDate);
		out.writeTypedList(items);
		out.writeStringList(categories);
		out.writeString(image);
	}

	public static final Parcelable.Creator<Channel> CREATOR = new Parcelable.Creator<Channel>() {
		public Channel createFromParcel(Parcel in) {
			return new Channel(in);
		}

		public Channel[] newArray(int size) {
			return new Channel[size];
		}
	};

	private Channel(Parcel in) {
		this();
		id = in.readInt();
		title = in.readString();
		link = in.readString();
		description = in.readString();
		long pubDateIn = in.readLong();
		pubDate = pubDateIn == -1 ? null : new Date(pubDateIn);
		lastBuildDate = in.readLong();
		categories = new ArrayList<>();
		in.readTypedList(items, Item.CREATOR);
		in.readStringList(categories);
		image = in.readString();
	}

}