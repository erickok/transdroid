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
 package org.transdroid.daemon.task;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A daemon task represents some action that needs to be performed
 * on the server daemon. It has no capabilities on itself; these are 
 * marshaled to the daemon adapter. Therefore all needed info (the
 * parameters) needs to be added to the extras bundle.
 * 
 * To help create these tasks and there data, each possible daemon 
 * method is created using a task-specific separate class with a 
 * create() method.
 * 
 * This class is Parcelable so it can be persisted in between an 
 * Activity breakdown and recreation.
 * 
 * @author erickok
 *
 */
public class DaemonTask implements Parcelable {

	protected IDaemonAdapter adapter;
	protected final DaemonMethod method;
	protected final Torrent targetTorrent;
	protected final Bundle extras;

	private DaemonTask(Parcel in) {
		this.method = DaemonMethod.getStatus(in.readInt());
		this.targetTorrent = in.readParcelable(Torrent.class.getClassLoader());
		this.extras = in.readBundle();
	}
	
	protected DaemonTask(IDaemonAdapter adapter, DaemonMethod method, Torrent targetTorrent, Bundle extras) {
		this.adapter = adapter;
		this.method = method;
		this.targetTorrent = targetTorrent;
		if (extras == null) {
			this.extras = new Bundle();
		} else {
			this.extras = extras;
		}
	}
	
	/**
	 * Execute the task on the appropriate daemon adapter
	 */
	public DaemonTaskResult execute() {
		return adapter.executeTask(this);
	}

	public DaemonMethod getMethod() {
		return method;
	}
	
	public Daemon getAdapterType() {
		return this.adapter.getType();
	}

	public Torrent getTargetTorrent() {
		return targetTorrent;
	}

	public Bundle getExtras() {
		return extras;
	}

    public static final Parcelable.Creator<DaemonTask> CREATOR = new Parcelable.Creator<DaemonTask>() {
    	public DaemonTask createFromParcel(Parcel in) {
    		return new DaemonTask(in);
    	}

		public DaemonTask[] newArray(int size) {
		    return new DaemonTask[size];
		}
    };

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(method.getCode());
		dest.writeParcelable(targetTorrent, 0);
		dest.writeBundle(extras);
	}
	
	/**
	 * Returns a readable description of this task in the form 'MethodName on AdapterName with TorrentName and AllExtras'
	 */
	public String toString() {
		return method.toString() + (adapter == null? "": " on " + adapter.getType()) + (targetTorrent != null || extras != null? " with ": "") + (targetTorrent == null? "": targetTorrent.toString() + (targetTorrent != null && extras != null? " and ": "")) + (extras == null? "": extras.toString());
	}

}
