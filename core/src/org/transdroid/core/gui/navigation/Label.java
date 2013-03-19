package org.transdroid.core.gui.navigation;

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

	public Label(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean matches(Torrent torrent) {
		return torrent.getLabelName() != null && torrent.getLabelName().equals(name);
	}
	
	private Label(Parcel in) {
		this.name = in.readString();
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
	}
	
}
