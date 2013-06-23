/*
 * Taken from the 'Learning Android' project,;
 * released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android
 */
package org.ifies.android.sax;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class Channel implements Parcelable {
       
        public Channel() {
                setCategories(new ArrayList<String>());
                setItems(new ArrayList<Item>());
        }
       
        public void setId(int id) {
                m_Id = id;
        }
        public int getId() {
                return m_Id;
        }

        public void setTitle(String title) {
                m_Title = title;
        }

        public String getTitle() {
                return m_Title;
        }

        public void setLink(String link) {
                m_Link = link;
        }

        public String getLink() {
                return m_Link;
        }

        public void setDescription(String description) {
                m_Description = description;
        }

        public String getDescription() {
                return m_Description;
        }

        public void setPubDate(Date date) {
                m_PubDate = date;
        }

        public Date getPubDate() {
                return m_PubDate;
        }

        public void setLastBuildDate(long lastBuildDate) {
                m_LastBuildDate = lastBuildDate;
        }

        public long getLastBuildDate() {
                return m_LastBuildDate;
        }

        public void setCategories(List<String> categories) {
                m_Categories = categories;
        }
       
        public void addCategory(String category) {
                m_Categories.add(category);
        }

        public List<String> getCategories() {
                return m_Categories;
        }

        public void setItems(List<Item> items) {
                m_Items = items;
        }
       
        public void addItem(Item item) {
                m_Items.add(item);
        }

        public List<Item> getItems() {
                return m_Items;
        }

        public void setImage(String image) {
                m_Image = image;
        }

        public String getImage() {
                return m_Image;
        }

        private int m_Id;
        private String m_Title;
        private String m_Link;
        private String m_Description;
        private Date m_PubDate;
        private long m_LastBuildDate;
        private List<String> m_Categories;
        private List<Item> m_Items;
        private String m_Image;
        
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeInt(m_Id);
			out.writeString(m_Title);
			out.writeString(m_Link);
			out.writeString(m_Description);
			out.writeLong(m_PubDate == null? -1: m_PubDate.getTime());
			out.writeLong(m_LastBuildDate);
			out.writeStringList(m_Categories);
			out.writeTypedList(m_Items);
			out.writeString(m_Image);
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
			m_Id = in.readInt();
			m_Title = in.readString();
			m_Link = in.readString();
			m_Description = in.readString();
			long pubDate = in.readLong();
			m_PubDate = pubDate == -1? null: new Date(pubDate);
			m_LastBuildDate = in.readLong();
			m_Categories = new ArrayList<String>();
			in.readTypedList(m_Items, Item.CREATOR);
			in.readStringList(m_Categories);
			m_Image = in.readString();
		}

}