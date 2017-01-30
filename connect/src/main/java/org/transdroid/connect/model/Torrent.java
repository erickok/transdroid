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
				// UNknown eta: move to the top of the list
				this.dateDone = new Date(Long.MAX_VALUE);
			} else {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, (int) eta);
				this.dateDone = cal.getTime();
			}
		}
		this.error = error;
	}

}
