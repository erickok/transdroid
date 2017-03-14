package org.transdroid.connect.model;

import java.util.Calendar;
import java.util.Date;

public final class Torrent {

	public static final long UNKNOWN = -1L;

	private final long id;
	private final String hash;
	private final String name;
	private final TorrentStatus statusCode;
	private final String locationDir;

	private final int rateDownload;
	private final int rateUpload;
	private final int seedersConnected;
	private final int seedersKnown;
	private final int leechersConnected;
	private final int leechersKnown;
	private final long eta;

	private final long downloadedEver;
	private final long uploadedEver;
	private final long totalSize;
	private final float partDone;
	private final float available;
	private final String label;

	private final Date dateAdded;
	private final Date dateDone;
	private final String error;

	public Torrent(long id,
				   String hash,
				   String name,
				   TorrentStatus statusCode,
				   String locationDir,
				   int rateDownload,
				   int rateUpload,
				   int seedersConnected,
				   int seedersKnown,
				   int leechersConnected,
				   int leechersKnown,
				   long eta,
				   long downloadedEver,
				   long uploadedEver,
				   long totalSize,
				   float partDone,
				   float available,
				   String label,
				   Date dateAdded,
				   Date realDateDone,
				   String error) {

		this.id = id;
		this.hash = hash;
		this.name = name;
		this.statusCode = statusCode;
		this.locationDir = locationDir;

		this.rateDownload = rateDownload;
		this.rateUpload = rateUpload;
		this.seedersConnected = seedersConnected;
		this.seedersKnown = seedersKnown;
		this.leechersConnected = leechersConnected;
		this.leechersKnown = leechersKnown;
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
			if (this.partDone == 1) {
				// Finished but no finished date: set so move to bottom of list
				Calendar cal = Calendar.getInstance();
				cal.clear();
				cal.set(1900, Calendar.DECEMBER, 31);
				this.dateDone = cal.getTime();
			} else if (eta == -1 || eta == -2) {
				// Unknown eta: move to the top of the list
				this.dateDone = new Date(Long.MAX_VALUE);
			} else {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, (int) eta);
				this.dateDone = cal.getTime();
			}
		}
		this.error = error;
	}

	public long id() {
		return id;
	}

	public String hash() {
		return hash;
	}

	public String name() {
		return name;
	}

	public TorrentStatus statusCode() {
		return statusCode;
	}

	public String locationDir() {
		return locationDir;
	}

	public int rateDownload() {
		return rateDownload;
	}

	public int rateUpload() {
		return rateUpload;
	}

	public int seedersConnected() {
		return seedersConnected;
	}

	public int seedersKnown() {
		return seedersKnown;
	}

	public int leechersConnected() {
		return leechersConnected;
	}

	public int leechersKnown() {
		return leechersKnown;
	}

	public long eta() {
		return eta;
	}

	public long downloadedEver() {
		return downloadedEver;
	}

	public long uploadedEver() {
		return uploadedEver;
	}

	public long totalSize() {
		return totalSize;
	}

	public float partDone() {
		return partDone;
	}

	public float available() {
		return available;
	}

	public String label() {
		return label;
	}

	public Date dateAdded() {
		return dateAdded;
	}

	public Date dateDone() {
		return dateDone;
	}

	public String error() {
		return error;
	}

	/**
	 * Returns the unique torrent-specific id, which is the torrent's hash or (if not available) the local index number
	 * @return The torrent's (session-transient) unique id
	 */
	public String uniqueId() {
		if (this.hash == null) {
			return Long.toString(this.id);
		} else {
			return this.hash;
		}
	}

	/**
	 * Gives the upload/download seed ratio.
	 * @return The ratio in range [0,r]
	 */
	public double ratio() {
		return ((double) uploadedEver) / ((double) downloadedEver);
	}

	/**
	 * Gives the percentage of the download that is completed
	 * @return The downloaded percentage in range [0,1]
	 */
	public float downloadedPercentage() {
		return partDone;
	}

	/**
	 * Returns whether this torrents is actively downloading or not.
	 * @param dormantAsInactive If true, dormant (0KB/s, so no data transfer) torrents are not considered actively downloading
	 * @return True if this torrent is to be treated as being in a downloading state, that is, it is trying to finish a download
	 */
	public boolean isDownloading(boolean dormantAsInactive) {
		return statusCode == TorrentStatus.DOWNLOADING && (!dormantAsInactive || rateDownload > 0);
	}

	/**
	 * Returns whether this torrents is actively seeding or not.
	 * @param dormantAsInactive If true, dormant (0KB/s, so no data transfer) torrents are not considered actively seeding
	 * @return True if this torrent is to be treated as being in a seeding state, that is, it is sending data to leechers
	 */
	public boolean isSeeding(boolean dormantAsInactive) {
		return statusCode == TorrentStatus.SEEDING && (!dormantAsInactive || rateUpload > 0);
	}

	/**
	 * Indicates if the torrent can be paused at this moment
	 * @return If it can be paused
	 */
	public boolean canPause() {
		// Can pause when it is downloading or seeding
		return statusCode == TorrentStatus.DOWNLOADING || statusCode == TorrentStatus.SEEDING;
	}

	/**
	 * Indicates whether the torrent can be resumed
	 * @return If it can be resumed
	 */
	public boolean canResume() {
		// Can resume when it is paused
		return statusCode == TorrentStatus.PAUSED;
	}

	/**
	 * Indicates if the torrent can be started at this moment
	 * @return If it can be started
	 */
	public boolean canStart() {
		// Can start when it is queued
		return statusCode == TorrentStatus.QUEUED;
	}

	/**
	 * Indicates whether the torrent can be stopped
	 * @return If it can be stopped
	 */
	public boolean canStop() {
		// Can stop when it is downloading or seeding or paused
		return statusCode == TorrentStatus.DOWNLOADING || statusCode == TorrentStatus.SEEDING
				|| statusCode == TorrentStatus.PAUSED;
	}

	public Torrent mimicResume() {
		return mimicStatus(downloadedPercentage() >= 1 ? TorrentStatus.SEEDING : TorrentStatus.DOWNLOADING);
	}

	public Torrent mimicPause() {
		return mimicStatus(TorrentStatus.PAUSED);
	}

	public Torrent mimicStart() {
		return mimicStatus(downloadedPercentage() >= 1 ? TorrentStatus.SEEDING : TorrentStatus.DOWNLOADING);
	}

	public Torrent mimicStop() {
		return mimicStatus(TorrentStatus.QUEUED);
	}

	public Torrent mimicNewLabel(String newLabel) {
		return new Torrent(id, hash, name, statusCode, locationDir, rateDownload, rateUpload, seedersConnected, seedersKnown, leechersConnected,
				leechersKnown, eta, downloadedEver, uploadedEver, totalSize, partDone, available, newLabel, dateAdded, dateDone, error);
	}

	public Torrent mimicChecking() {
		return mimicStatus(TorrentStatus.CHECKING);
	}

	public Torrent mimicNewLocation(String newLocation) {
		return new Torrent(id, hash, name, statusCode, newLocation, rateDownload, rateUpload, seedersConnected, seedersKnown, leechersConnected,
				leechersKnown, eta, downloadedEver, uploadedEver, totalSize, partDone, available, label, dateAdded, dateDone, error);
	}

	@Override
	public String toString() {
		// (HASH_OR_ID) NAME
		return "(" + uniqueId() + ") " + name;
	}

	private Torrent mimicStatus(TorrentStatus newStatus) {
		return new Torrent(id, hash, name, newStatus, locationDir, rateDownload, rateUpload, seedersConnected, seedersKnown, leechersConnected,
				leechersKnown, eta, downloadedEver, uploadedEver, totalSize, partDone, available, label, dateAdded, dateDone, error);
	}

}
