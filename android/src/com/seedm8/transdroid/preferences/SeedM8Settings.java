package com.seedm8.transdroid.preferences;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.util.HttpHelper;


public class SeedM8Settings {

	private static final String DEFAULT_NAME = "SeedM8";

	final private String name;
	final private String server;
	final private String username;
	final private int delugePort;
	final private String delugePassword;
	final private int transmissionPort;
	final private String transmissionPassword;
	final private String rtorrentPassword;
	final private String sftpPassword;
	final private boolean alarmOnFinishedDownload;
	final private boolean alarmOnNewTorrent;
	final private String idString;
	
	public SeedM8Settings(String name, String server, String username, int delugePort, 
			String delugePassword, int transmissionPort, String transmissionPassword, 
			String rtorrentPassword, String sftpPassword, boolean alarmOnFinishedDownload, boolean alarmOnNewTorrent, 
			String idString) {
		this.name = name;
		this.server = server;
		this.username = username;
		this.delugePort = delugePort;
		this.delugePassword = delugePassword;
		this.transmissionPort = transmissionPort;
		this.transmissionPassword = transmissionPassword;
		this.rtorrentPassword = rtorrentPassword;
		this.sftpPassword = sftpPassword;
		this.alarmOnFinishedDownload = alarmOnFinishedDownload;
		this.alarmOnNewTorrent = alarmOnNewTorrent;
		this.idString = idString;
	}

	public String getName() {
		return (name == null || name.equals("")? DEFAULT_NAME: name);
	}
	public String getServer() {
		return server;
	}
	public String getUsername() {
		return username;
	}
	public String getDelugePassword() {
		return delugePassword;
	}
	public int getDelugePort() {
		return delugePort;
	}
	public String getTransmissionPassword() {
		return transmissionPassword;
	}
	public int getTransmissionPort() {
		return transmissionPort;
	}
	public String getRtorrentPassword() {
		return rtorrentPassword;
	}
	public String getSftpPassword() {
		return sftpPassword;
	}
	public boolean shouldAlarmOnFinishedDownload() {
		return alarmOnFinishedDownload;
	}
	public boolean shouldAlarmOnNewTorrent() {
		return alarmOnNewTorrent;
	}
	public String getIdString() {
		return idString;
	}

	/**
	 * Builds a text that can be used by a human reader to identify this daemon settings
	 * @return A concatenation of username, address, port and folder, where applicable
	 */
	public String getHumanReadableIdentifier() {
		return this.getUsername() + "@" + getServer();
	}
	
	@Override
	public String toString() {
		return getHumanReadableIdentifier();
	}

	public List<DaemonSettings> createDaemonSettings(int startID) {
		List<DaemonSettings> daemons = new ArrayList<DaemonSettings>();
		// Deluge
		if (getDelugePassword() != null && !getDelugePassword().equals("")) {
			daemons.add(
				new DaemonSettings(
					getName() + " Deluge", 
					Daemon.Deluge, 
					getUsername() + "." + getServer(), 
					getDelugePort(), 
					false, 
					false, 
					null, 
					null, 
					true, 
					getUsername(), 
					getDelugePassword(), 
					getDelugePassword(),  
					OS.Linux, 
					null,
					"sftp://" + getServer() + "/home/" + getUsername() + "/private/deluge/data/",
					getSftpPassword(), 
					HttpHelper.DEFAULT_CONNECTION_TIMEOUT,
					shouldAlarmOnFinishedDownload(), 
					shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		// Transmission
		if (getTransmissionPassword() != null && !getTransmissionPassword().equals("")) {
			daemons.add(
				new DaemonSettings(
					getName() + " Transmission", 
					Daemon.Transmission, 
					getServer(), 
					getTransmissionPort(), 
					false, 
					false, 
					null, 
					null, 
					true, 
					getUsername(), 
					getTransmissionPassword(), 
					null, 
					OS.Linux, 
					null,
					"sftp://" + getServer() + "/home/" + getUsername() + "/private/transmission/data/",
					getSftpPassword(), 
					HttpHelper.DEFAULT_CONNECTION_TIMEOUT,
					shouldAlarmOnFinishedDownload(), 
					shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		// rTorrent
		if (getRtorrentPassword() != null && !getRtorrentPassword().equals("")) {
			daemons.add(
				new DaemonSettings(
					getName() + " rTorrent", 
					Daemon.rTorrent, 
					getUsername() + "." + getServer(), 
					80, 
					false, 
					false, 
					null, 
					"/" + getUsername() + "/RPC", 
					true, 
					"rutorrent", 
					getRtorrentPassword(), 
					null, 
					OS.Linux, 
					null,
					"sftp://" + getUsername() + "@" + getServer() + "/home/" + getUsername() + "/private/rtorrent/data/",
					getSftpPassword(), 
					HttpHelper.DEFAULT_CONNECTION_TIMEOUT,
					shouldAlarmOnFinishedDownload(), 
					shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		return daemons;
	}

}
