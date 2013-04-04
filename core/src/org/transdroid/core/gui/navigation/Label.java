package org.transdroid.core.gui.navigation;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.daemon.Torrent;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents some label that is active or available on the server.
 * @author Eric Kok
 */
public class Label implements SimpleListItem, NavigationFilter {

	private final String name;
	private final int count;

	public Label(org.transdroid.daemon.Label daemonLabel) {
		this.name = daemonLabel.getName();
		this.count = daemonLabel.getCount();
	}

	@Override
	public String getName() {
		return this.name;
	}

	public int getCount() {
		return count;
	}
	
	@Override
	public boolean matches(Torrent torrent) {
		return torrent.getLabelName() != null && torrent.getLabelName().equals(name);
	}
	
	public static List<Label> convertToNavigationLabels(List<org.transdroid.daemon.Label> daemonLabels) {
		if (daemonLabels == null)
			return null;
		List<Label> localLabels = new ArrayList<Label>();
		for (org.transdroid.daemon.Label label : daemonLabels) {
			localLabels.add(new Label(label));
		}
		return localLabels;
	}
	
	private Label(Parcel in) {
		this.name = in.readString();
		this.count = in.readInt();
	}

    public static final Parcelable.Creator<Label> CREATOR = new Parcelable.Creator<Label>() {
    	public Label createFromParcel(Parcel in) {
    		return new Label(in);
    	}

		public Label[] newArray(int size) {
		    return new Label[size];
		}
    };

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeInt(count);
	}
	
}
