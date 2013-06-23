/*
 * Taken from the 'Learning Android' project,;
 * released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android
 */
package org.ifies.android.sax;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Comparable<Item>, Parcelable {

	public void setId(int id) {
            this._id = id;
    }
   
    public int getId() {
            return _id;
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
   
    private int _id;
    private String title;
    private String link;
    private String description;
    private Date pubDate;
    private String enclosureUrl;
    private String enclosureType;
    private long enclosureLength;

    /**
     * Returns 'the' item link, which preferably is the enclosure url, but otherwise the link (or null if that is empty too)
     * @return A single link url to be used
     */
    public String getTheLink() {
        if (this.getEnclosureUrl() != null) {
        	return this.getEnclosureUrl();
        } else {
        	return this.getLink();
        }
    }
   
    /**
     * CompareTo is used to compare (and sort) item based on their publication dates
     */
	@Override
	public int compareTo(Item another) {
		if (another == null || this.pubDate == null || another.getPubdate() == null) {
			return 0;
		}
		return this.pubDate.compareTo(another.getPubdate());
	}

	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(_id);
		out.writeString(title);
		out.writeString(link);
		out.writeString(description);
		out.writeLong(pubDate == null? -1: pubDate.getTime());
		out.writeString(enclosureUrl);
		out.writeString(enclosureType);
		out.writeLong(enclosureLength);
	}
	public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
		public Item createFromParcel(Parcel in) {
			return new Item(in);
		}
		public Item[] newArray(int size) {
			return new Item[size];
		}
	};
	private Item(Parcel in) {
		_id = in.readInt();
		title = in.readString();
		link = in.readString();
		description = in.readString();
		long pubDateIn = in.readLong();
		pubDate = pubDateIn == -1? null: new Date(pubDateIn);
		enclosureUrl = in.readString();
		enclosureType = in.readString();
		enclosureLength = in.readLong();
	}

}