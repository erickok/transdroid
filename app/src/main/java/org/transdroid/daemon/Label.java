/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.daemon;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a label on a server daemon.
 * 
 * @author Lexa
 *
 */
public final class Label implements Parcelable, Comparable<Label> {

	final private String name;
	final private int count;

	public String getName() { return name; }
	public int getCount() { return count; }
	
	private Label(Parcel in) {
		this.name = in.readString();
		this.count = in.readInt();
	}
	
	public Label(String name, int count) {
		this.name = name;
		this.count = count;
	}
	
	@Override
	public String toString() {
		return name;//+"("+String.valueOf(count)+")";
	}
	
	@Override
	public int compareTo(Label another) {
		// Compare Label objects on their name (used for sorting only!)
		return name.compareTo(another.getName());
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
