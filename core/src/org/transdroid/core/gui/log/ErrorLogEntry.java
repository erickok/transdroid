/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.log;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents an error log entry to be registered in the database.
 * @author Eric Kok
 */
@DatabaseTable(tableName = "ErrorLogEntry")
public class ErrorLogEntry implements Parcelable {

	public static final String ID = "logId";
	public static final String DATEANDTIME = "dateAndTime";

	@DatabaseField(id = true, columnName = ID)
	private Integer logId;
	@DatabaseField(columnName = DATEANDTIME)
	private Date dateAndTime;
	@DatabaseField
	private Integer priority;
	@DatabaseField
	private String tag;
	@DatabaseField
	private String message;

	public ErrorLogEntry() {
	}

	public ErrorLogEntry(Integer priority, String tag, String message) {
		this.dateAndTime = new Date();
		this.priority = priority;
		this.tag = tag;
		this.message = message;
	}

	public Integer getLogId() {
		return logId;
	}

	public Date getDateAndTime() {
		return dateAndTime;
	}

	public Integer getPriority() {
		return priority;
	}

	public String getTag() {
		return tag;
	}

	public String getMessage() {
		return message;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(logId);
		out.writeLong(dateAndTime.getTime());
		out.writeInt(priority);
		out.writeString(tag);
		out.writeString(message);
	}

	public static final Parcelable.Creator<ErrorLogEntry> CREATOR = new Parcelable.Creator<ErrorLogEntry>() {
		public ErrorLogEntry createFromParcel(Parcel in) {
			return new ErrorLogEntry(in);
		}

		public ErrorLogEntry[] newArray(int size) {
			return new ErrorLogEntry[size];
		}
	};

	private ErrorLogEntry(Parcel in) {
		logId = in.readInt();
		dateAndTime = new Date(in.readLong());
		priority = in.readInt();
		tag = in.readString();
		message = in.readString();
	}

}
