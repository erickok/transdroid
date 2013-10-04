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

import java.util.Calendar;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a torrent on a server daemon.
 * 
 * @author erickok
 *
 */
public final class Torrent implements Parcelable, Comparable<Torrent> {

	final private long id;
	final private String hash;
	final private String name;
	private TorrentStatus statusCode;
	private String locationDir;
	
	final private int rateDownload;
	final private int rateUpload;
	final private int peersGettingFromUs;
	final private int peersSendingToUs;
	final private int peersConnected;
	final private int peersKnown;
	final private int eta;
	
	final private long downloadedEver;
	final private long uploadedEver;
	final private long totalSize;
	final private float partDone;
	final private float available;
	private String label;

	final private Date dateAdded;
	final private Date dateDone;
	final private String error;
	final private Daemon daemon;
	
	//public long getID() { return id; }
	//public String getHash() { return hash; }
	public String getName() { return name; }
	public TorrentStatus getStatusCode() { return statusCode; }
	public String getLocationDir() { return locationDir; }
	
	public int getRateDownload() { return rateDownload; }
	public int getRateUpload() { return rateUpload; }
	public int getPeersGettingFromUs() { return peersGettingFromUs; }
	public int getPeersSendingToUs() { return peersSendingToUs; }
	public int getPeersConnected() { return peersConnected; }
	public int getPeersKnown() { return peersKnown; }
	public int getEta() { return eta; }
	
	public long getDownloadedEver() { return downloadedEver; }
	public long getUploadedEver() { return uploadedEver; }
	public long getTotalSize() { return totalSize; }
	public float getPartDone() { return partDone; }
	public float getAvailability() { return available; }
	public String getLabelName() { return label; }

	public Date getDateAdded() { return dateAdded; }
	public Date getDateDone() { return dateDone; }
	public String getError() { return error; }
	public Daemon getDaemon() { return daemon; }
	
	private Torrent(Parcel in) {
		this.id = in.readLong();
		this.hash = in.readString();
		this.name = in.readString();
		this.statusCode = TorrentStatus.getStatus(in.readInt());
		this.locationDir = in.readString();
		
		this.rateDownload = in.readInt();
		this.rateUpload = in.readInt();
		this.peersGettingFromUs = in.readInt();
		this.peersSendingToUs = in.readInt();
		this.peersConnected = in.readInt();
		this.peersKnown = in.readInt();
		this.eta = in.readInt();
		
		this.downloadedEver = in.readLong();
		this.uploadedEver = in.readLong();
		this.totalSize = in.readLong();
		this.partDone = in.readFloat();
		this.available = in.readFloat();
		this.label = in.readString();

		long lDateAdded = in.readLong();
		this.dateAdded = (lDateAdded == -1)? null: new Date(lDateAdded);
		long lDateDone = in.readLong();
		this.dateDone = (lDateDone == -1)? null: new Date(lDateDone);
		this.error = in.readString();
		this.daemon = Daemon.valueOf(in.readString());
	}
	
	public Torrent(long id, String hash, String name, TorrentStatus statusCode, String locationDir, int rateDownload, int rateUpload, 
			int peersGettingFromUs, int peersSendingToUs, int peersConnected, int peersKnown, int eta, 
			long downloadedEver, long uploadedEver, long totalSize, float partDone, float available, String label, 
			Date dateAdded, Date realDateDone, String error, Daemon daemon) {
		this.id = id;
		this.hash = hash;
		this.name = name;
		this.statusCode = statusCode;
		this.locationDir = locationDir;
		
		this.rateDownload = rateDownload;
		this.rateUpload = rateUpload;
		this.peersGettingFromUs = peersGettingFromUs;
		this.peersSendingToUs = peersSendingToUs;
		this.peersConnected = peersConnected;
		this.peersKnown = peersKnown;
		this.eta = eta;
		
		this.downloadedEver = downloadedEver;
		this.uploadedEver = uploadedEver;
		this.totalSize = totalSize;
		this.partDone = partDone;
		this.available = available;
		this.label = label;
		
		this.dateAdded = dateAdded;
		if (realDateDone != null) {
			this.dateDone = realDateDone;
		} else {
			if( this.partDone == 1){ //finished but no finished date set so move to bottom of list
				Calendar cal = Calendar.getInstance();
				cal.clear();
				cal.set(1900, 12, 31);
				this.dateDone = cal.getTime();
			} else if (eta == -1 || eta == -2) {
				this.dateDone = new Date(Long.MAX_VALUE);
			} else {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, eta);
				this.dateDone = cal.getTime();
			}
		}
		this.error = error;
		this.daemon = daemon;
	}
	
	/**
	 * Returns the torrent-specific ID, which is the torrent's hash or (if not available) the long number
	 * @return The torrent's (session-transient) unique ID
	 */
	public String getUniqueID() {
		if (this.hash == null) {
			return "" + this.id;
		} else {
			return this.hash;
		}
	}

	/**
	 * Gives the upload/download seed ratio. 
	 * @return The ratio in range [0,r]
	 */
	public double getRatio() {
		return ((double)uploadedEver) / ((double)downloadedEver);
	}

	/**
	 * Gives the percentage of the download that is completed
	 * @return The downloaded percentage in range [0,1]
	 */
	public float getDownloadedPercentage() {
		return partDone;
	}

	/**
	 * Indicates if the torrent can be paused at this moment
	 * @return If it can be paused
	 */
	public boolean canPause() {
		// Can pause when it is downloading or seeding
		return statusCode == TorrentStatus.Downloading || statusCode == TorrentStatus.Seeding;
	}

	/**
	 * Indicates whether the torrent can be resumed
	 * @return If it can be resumed
	 */
	public boolean canResume() {
		// Can resume when it is paused
		return statusCode == TorrentStatus.Paused;
	}

	/**
	 * Indicates if the torrent can be started at this moment
	 * @return If it can be started
	 */
	public boolean canStart() {
		// Can start when it is queued
		return statusCode == TorrentStatus.Queued;
	}

	/**
	 * Indicates whether the torrent can be stopped
	 * @return If it can be stopped
	 */
	public boolean canStop() {
		// Can stop when it is downloading or seeding or paused
		return statusCode == TorrentStatus.Downloading || statusCode == TorrentStatus.Seeding || statusCode == TorrentStatus.Paused;
	}

	public void mimicResume() {
		if (getDownloadedPercentage() >= 1) {
			statusCode = TorrentStatus.Seeding;
		} else {
			statusCode = TorrentStatus.Downloading;
		}
	}

	public void mimicPause() {
		statusCode = TorrentStatus.Paused;
	}

	public void mimicStart() {
		if (getDownloadedPercentage() >= 1) {
			statusCode = TorrentStatus.Seeding;
		} else {
			statusCode = TorrentStatus.Downloading;
		}
	}

	public void mimicStop() {
		statusCode = TorrentStatus.Queued;
	}

	public void mimicNewLabel(String newLabel) {
		label = newLabel;
	}

	public void mimicNewLocation(String newLocation) {
		locationDir = newLocation;
	}
	
	@Override
	public String toString() {
		// (HASH_OR_ID) NAME
		return "(" + ((hash != null)? hash: String.valueOf(id)) + ") " + name;
	}
	
	@Override
	public int compareTo(Torrent another) {
		// Compare torrent objects on their name (used for sorting only!)
		return name.compareTo(another.getName());
	}
	
    public static final Parcelable.Creator<Torrent> CREATOR = new Parcelable.Creator<Torrent>() {
    	public Torrent createFromParcel(Parcel in) {
    		return new Torrent(in);
    	}

		public Torrent[] newArray(int size) {
		    return new Torrent[size];
		}
    };

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(hash);
		dest.writeString(name);
		dest.writeInt(statusCode.getCode());
		dest.writeString(locationDir);

		dest.writeInt(rateDownload);
		dest.writeInt(rateUpload);
		dest.writeInt(peersGettingFromUs);
		dest.writeInt(peersSendingToUs);
		dest.writeInt(peersConnected);
		dest.writeInt(peersKnown);
		dest.writeInt(eta);
		
		dest.writeLong(downloadedEver);
		dest.writeLong(uploadedEver);
		dest.writeLong(totalSize);
		dest.writeFloat(partDone);
		dest.writeFloat(available);
		dest.writeString(label);

		dest.writeLong((dateAdded == null)? -1: dateAdded.getTime());
		dest.writeLong((dateDone == null)? -1: dateDone.getTime());
		dest.writeString(error);
		dest.writeString(daemon.name());
	}
	
}
