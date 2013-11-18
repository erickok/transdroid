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

import java.util.HashMap;
import java.util.Map;

import org.transdroid.daemon.util.FileSizeConverter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a single file contained in a torrent.
 * 
 * @author erickok
 *
 */
public final class TorrentFile implements Parcelable, Comparable<TorrentFile>, Finishable {

	private final String key;
	private final String name;
	private final String relativePath;
	private final String fullPath;
	private final long totalSize;
	private final long downloaded;
	private Priority priority;

	public TorrentFile(String key, String name, String relativePath, String fullPath, long size, long done, Priority priority) {
		this.key = key;
		this.name = name;
		this.relativePath = relativePath;
		this.fullPath = fullPath;
		this.totalSize = size;
		this.downloaded = done;
		this.priority = priority;
	}

	private TorrentFile(Parcel in) {
		this.key = in.readString();
		this.name = in.readString();
		this.relativePath = in.readString();
		this.fullPath = in.readString();
		this.totalSize = in.readLong();
		this.downloaded = in.readLong();
		this.priority = Priority.getPriority(in.readInt());
	}
	
	public String getKey() {
		return this.key;
	}
	public String getName() {
		return this.name;
	}
	public String getRelativePath() {
		return this.relativePath;
	}
	public String getFullPath() {
		return this.fullPath;
	}
	public long getTotalSize() {
		return this.totalSize;
	}
	public long getDownloaded() {
		return this.downloaded;
	}

	public Priority getPriority() {
		return priority;
	}

	public void mimicPriority(Priority newPriority) {
		priority = newPriority;
	}

	public float getPartDone() {
		return (float)downloaded / (float)totalSize;
	}

	/**
	 * Returns a text showing the percentage that is already downloaded of this file
	 * @return A string indicating the progress, e.g. '85%'
	 */
	public String getProgressText() {
		return String.format("%.1f", getPartDone() * 100) + "%";
	}

	/**
	 * Returns a text showing the downloaded and total sizes of this file
	 * @return A string with the sizes, e.g. '125.3 of 251.2 MB'
	 */
	public String getDownloadedAndTotalSizeText() {
		return FileSizeConverter.getSize(getDownloaded()) + " / " + FileSizeConverter.getSize(getTotalSize());
	}

	/**
	 * Returns if the download for this file is complete
	 * @return True if the downloaded size equals the total size, i.e. if it is completed
	 */
	public boolean isComplete() {
		return getDownloaded() == getTotalSize();
	}

	/**
	 * Returns the full path of this file as it should be located on the remote server
	 * @return The full path, as String
	 */
	public String getFullPathUri() {
		return "file://" + getFullPath();
	}

    /**
     * Try to infer the mime type of this file
     * @return The mime type of this file, or null if it could not be inferred
     */
	public String getMimeType() {
		// TODO: Test if this still works
		
		if (getFullPath() != null && getFullPath().contains(".")) {
			final String ext = getFullPath().substring(getFullPath().lastIndexOf('.') + 1);
			if (mimeTypes.containsKey(ext)) {
				// One of the known extensions: return logical mime type
				return mimeTypes.get(ext);
			}
		}
		// Unknown/none/unregistered extension: return null
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isStarted() {
		return getPartDone() > 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isFinished() {
		return getPartDone() >= 1;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(TorrentFile another) {
		// Compare file objects on their name (used for sorting only!)
		return name.compareTo(another.getName());
	}

	private static final Map<String, String> mimeTypes = fillMimeTypes();
    private static Map<String, String> fillMimeTypes() {
    	// Full mime type support list is in http://code.google.com/p/android-vlc-remote/source/browse/trunk/AndroidManifest.xml
    	// We use a selection of the most popular/obvious ones
    	HashMap<String, String> types = new HashMap<String, String>();
    	// Application
    	types.put("m4a", "application/x-extension-m4a");
    	types.put("flac", "application/x-flac");
    	types.put("mkv", "application/x-matroska");
    	types.put("ogg", "application/x-ogg");
    	// Audio
    	types.put("m3u", "audio/mpegurl");
    	types.put("mp3", "audio/mpeg");
    	types.put("mpa", "audio/mpeg");
    	types.put("mpc", "audio/x-musepack");
    	types.put("wav", "audio/x-wav");
    	types.put("wma", "audio/x-ms-wma");
    	// Video
    	types.put("3gp", "video/3gpp");
    	types.put("avi", "video/x-avi");
    	types.put("flv", "video/x-flv");
    	types.put("mov", "video/quicktime");
    	types.put("mp4", "video/mp4");
    	types.put("mpg", "video/mpeg");
    	types.put("mpeg", "video/mpeg");
    	types.put("vob", "video/mpeg");
    	types.put("wmv", "video/x-ms-wmv");
    	return types;
    }
    
    public static final Parcelable.Creator<TorrentFile> CREATOR = new Parcelable.Creator<TorrentFile>() {
    	public TorrentFile createFromParcel(Parcel in) {
    		return new TorrentFile(in);
    	}

		public TorrentFile[] newArray(int size) {
		    return new TorrentFile[size];
		}
    };

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(key);
		dest.writeString(name);
		dest.writeString(relativePath);
		dest.writeString(fullPath);
		dest.writeLong(downloaded);
		dest.writeLong(totalSize);
		dest.writeInt(priority.getCode());
	}

}
