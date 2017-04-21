package org.transdroid.core.gui.remoterss.data;

import android.os.Parcelable;

import org.transdroid.core.gui.lists.SimpleListItem;

import java.util.Date;
import java.util.List;

/**
 * @author Twig
 */
public abstract class RemoteRssChannel implements Parcelable, SimpleListItem {
    protected int id;
    protected String name;
    protected String link;
    protected long lastUpdated;
    protected List<RemoteRssItem> items;

    @Override
    public int describeContents() {
        return 0;
    }

    public int getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public Date getLastUpdated() {
        return new Date(lastUpdated);
    }

    public List<RemoteRssItem> getItems() {
        return items;
    }

    @Override
    public String getName() {
        return name;
    }
}
