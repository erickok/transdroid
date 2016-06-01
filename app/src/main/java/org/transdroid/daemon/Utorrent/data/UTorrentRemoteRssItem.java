package org.transdroid.daemon.Utorrent.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;

import java.util.Calendar;
import java.util.Date;

public class UTorrentRemoteRssItem extends RemoteRssItem {
    public String name;
//    public int season;
//    public int episode;

    public UTorrentRemoteRssItem(JSONArray json) throws JSONException {
        name = json.getString(0);
        title = json.getString(1);
        link = json.getString(2);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(json.getLong(5) * 1000);
        timestamp = calendar.getTime();

//        season = json.getInt(6);
//        episode = json.getInt(7);
    }


    public static final Parcelable.Creator<UTorrentRemoteRssItem> CREATOR = new Parcelable.Creator<UTorrentRemoteRssItem>() {
   		public UTorrentRemoteRssItem createFromParcel(Parcel in) {
            return new UTorrentRemoteRssItem(in);
        }

   		public UTorrentRemoteRssItem[] newArray(int size) {
   			return new UTorrentRemoteRssItem[size];
   		}
   	};

    public UTorrentRemoteRssItem(Parcel in) {
        name = in.readString();
        title = in.readString();
        link = in.readString();
        sourceName = in.readString();
        timestamp = (Date) in.readSerializable();
//        season = in.readInt();
//        episode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(link);
        dest.writeString(sourceName);
        dest.writeSerializable(timestamp);
//        dest.writeInt(season);
//        dest.writeInt(episode);
    }
}
