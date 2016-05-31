package org.transdroid.daemon.Utorrent.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.transdroid.core.gui.lists.SimpleListItem;

public class RemoteRssFile implements Parcelable, SimpleListItem {
    public String name;
    public String title;
    public String link;
    public String feedLabel;
    public long timestamp;
    public int season;
    public int episode;

    public RemoteRssFile(JSONArray json) throws JSONException {
        name = json.getString(0);
        title = json.getString(1);
        link = json.getString(2);
        timestamp = json.getLong(5);
        season = json.getInt(6);
        episode = json.getInt(7);
    }


    public static final Parcelable.Creator<RemoteRssFile> CREATOR = new Parcelable.Creator<RemoteRssFile>() {
   		public RemoteRssFile createFromParcel(Parcel in) {
            return new RemoteRssFile(in);
        }

   		public RemoteRssFile[] newArray(int size) {
   			return new RemoteRssFile[size];
   		}
   	};

    public RemoteRssFile(Parcel in) {
        name = in.readString();
        title = in.readString();
        link = in.readString();
        feedLabel = in.readString();
        timestamp = in.readLong();
        season = in.readInt();
        episode = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(link);
        dest.writeString(feedLabel);
        dest.writeLong(timestamp);
        dest.writeInt(season);
        dest.writeInt(episode);
    }

    @Override
    public String getName() {
        return title;
    }
}
