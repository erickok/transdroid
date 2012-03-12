package com.xirvik.transdroid.preferences;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.util.HttpHelper;


public class XirvikSettings {

	private static final String DEFAULT_NAME = "Xirvik";

	private static final int TFB4RT_PORT = 443;
	private static final String TFB4RT_FOLDER = "/tfx";
	private static final int RTORRENT_PORT = 443;
	public static final String RTORRENT_FOLDER = "/RPC2";
	private static final int UTORRENT_PORT = 5010;

	final private String name;
	final private XirvikServerType type;
	final private String server;
	final private String folder;
	final private String username;
	final private String password;
	final private boolean alarmOnFinishedDownload;
	final private boolean alarmOnNewTorrent;
	final private String idString;
	
	public XirvikSettings(String name, XirvikServerType type, String server, String folder, String username, 
			String password, boolean alarmOnFinishedDownload, boolean alarmOnNewTorrent, 
			String idString) {
		this.name = name;
		this.type = type;
		this.server = server;
		this.folder = folder;
		this.username = username;
		this.password = password;
		this.alarmOnFinishedDownload = alarmOnFinishedDownload;
		this.alarmOnNewTorrent = alarmOnNewTorrent;
		this.idString = idString;
	}

	public String getName() {
		return (name == null || name.equals("")? DEFAULT_NAME: name);
	}
	public XirvikServerType getType() {
		return type;
	}
	public String getServer() {
		return server;
	}
	public String getFolder() {
		return folder;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
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
		boolean isDedi = getType() == XirvikServerType.Dedicated;
		if (getType() == XirvikServerType.Shared || isDedi) {
			daemons.add(
					new DaemonSettings(
							getName() + (isDedi? " Torrentflux-b4rt": ""), 
							Daemon.Tfb4rt, getServer(), TFB4RT_PORT, 
							true, true, null, 
							TFB4RT_FOLDER, true, getUsername(), getPassword(), 
							OS.Linux, "/", "ftp://" + getName() + ":" + getServer() + "/",
							getPassword(), HttpHelper.DEFAULT_CONNECTION_TIMEOUT, shouldAlarmOnFinishedDownload(), 
							shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		if (getType() == XirvikServerType.SharedRtorrent || getType() == XirvikServerType.SemiDedicated || isDedi) {
			daemons.add(
					new DaemonSettings(
							getName() + (isDedi? " rTorrent": ""), 
							Daemon.rTorrent, getServer(), RTORRENT_PORT, 
							true, true, null, 
							getFolder(), true, getUsername(), getPassword(), 
							OS.Linux, "/", "ftp://" + getName() + ":" + getServer() + "/",
							getPassword(), HttpHelper.DEFAULT_CONNECTION_TIMEOUT, shouldAlarmOnFinishedDownload(),
							shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		if (isDedi) {
			daemons.add(
					new DaemonSettings(
							getName() + " uTorrent", 
							Daemon.uTorrent, getServer(), UTORRENT_PORT, 
							false, false, null, 
							null, true, getUsername(), getPassword(), 
							OS.Linux, "/", "ftp://" + getName() + ":" + getServer() + "/",
							getPassword(), HttpHelper.DEFAULT_CONNECTION_TIMEOUT, shouldAlarmOnFinishedDownload(),
							shouldAlarmOnNewTorrent(), "" + startID++, true));
		}
		return daemons;
	}

}
