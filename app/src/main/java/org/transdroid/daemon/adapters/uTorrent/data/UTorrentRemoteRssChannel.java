package org.transdroid.daemon.adapters.uTorrent.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.transdroid.core.gui.remoterss.data.RemoteRssChannel;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;

import java.util.ArrayList;

/**
 * uTorrent implementation of RemoteRssChannel.
 *
 * @author Twig
 */
public class UTorrentRemoteRssChannel extends RemoteRssChannel {
    public static final Parcelable.Creator<UTorrentRemoteRssChannel> CREATOR = new Parcelable.Creator<UTorrentRemoteRssChannel>() {
        public UTorrentRemoteRssChannel createFromParcel(Parcel in) {
            return new UTorrentRemoteRssChannel(in);
        }

        public UTorrentRemoteRssChannel[] newArray(int size) {
            return new UTorrentRemoteRssChannel[size];
        }
    };

    public UTorrentRemoteRssChannel(JSONArray json) throws JSONException {
//        boolean enabled = json.getBoolean(1);
        boolean isCustomAlias = !json.getBoolean(2);

        id = json.getInt(0);
        link = json.getString(6);
        lastUpdated = json.getLong(7);

        if (isCustomAlias) {
            name = link.split("\\|")[0];
            link = link.split("\\|")[1];
        } else {
            name = link;
        }

        items = new ArrayList<>();

        JSONArray filesJson = json.getJSONArray(8);
        RemoteRssItem file;

        for (int i = 0; i < filesJson.length(); i++) {
            file = new UTorrentRemoteRssItem(filesJson.getJSONArray(i));
            file.setSourceName(name);
            items.add(file);
        }
    }

    public UTorrentRemoteRssChannel(Parcel in) {
        id = in.readInt();
        name = in.readString();
        link = in.readString();
        lastUpdated = in.readLong();

        items = new ArrayList<>();
        in.readList(items, UTorrentRemoteRssItem.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(link);
        dest.writeLong(lastUpdated);
        dest.writeList(items);
    }
}
