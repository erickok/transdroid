package org.transdroid.core.gui.remoterss.data;

import android.os.Parcelable;

import org.transdroid.core.gui.lists.SimpleListItem;

import java.util.Date;

/**
 * Created by twig on 1/06/2016.
 */
public abstract class RemoteRssItem implements Parcelable, SimpleListItem {
    protected String title;
    protected String link;
    protected String sourceName; // Name of RSS feed channel
    protected long timestamp;

    @Override
    public String getName() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Date getTimestamp() {
        return new Date(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
