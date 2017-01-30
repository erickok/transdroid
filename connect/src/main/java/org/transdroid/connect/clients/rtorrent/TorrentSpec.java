package org.transdroid.connect.clients.rtorrent;

public final class TorrentSpec {

	public String hash;
	public String name;
	public long state;
	public long downloadRate;
	public long uploadRate;
	public long peersConnected;
	public long peersNotConnected;
	public long bytesDone;
	public long bytesUploaded;
	public long bytesTotal;
	public long bytesleft;
	public long timeCreated;
	public long isComplete;
	public long isActive;
	public long isHashChecking;
	public String basePath;
	public String baseFilename;
	public String errorMessage;
	public String timeAdded;
	public String timeFinished;
	public String label;
	public long seedersConnected;
	public long leechersConnected;

}
