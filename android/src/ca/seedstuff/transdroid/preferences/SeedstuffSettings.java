package ca.seedstuff.transdroid.preferences;

import java.util.ArrayList;
import java.util.List;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.OS;
import org.transdroid.daemon.util.HttpHelper;

public class SeedstuffSettings {

	private static final String DEFAULT_NAME = "Seedstuff";

	private static final int RTORRENT_PORT = 443;
	private static final String RTORRENT_FOLDER_PART = "/user/";
	private static final int FTP_PORT = 32001;

	final private String name;
	final private String server;
	final private String username;
	final private String password;
	final private boolean alarmOnFinishedDownload;
	final private boolean alarmOnNewTorrent;
	final private String idString;

	public SeedstuffSettings(String name, String server, String username, String password, boolean alarmOnFinishedDownload,
			boolean alarmOnNewTorrent, String idString) {
		this.name = name;
		this.server = server;
		this.username = username;
		this.password = password;
		this.alarmOnFinishedDownload = alarmOnFinishedDownload;
		this.alarmOnNewTorrent = alarmOnNewTorrent;
		this.idString = idString;
	}

	public String getName() {
		return (name == null || name.equals("") ? DEFAULT_NAME : name);
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
		return getServer();
	}

	public String getServer() {
		return server;
	}

	@Override
	public String toString() {
		return getHumanReadableIdentifier();
	}

	public List<DaemonSettings> createDaemonSettings(int startID) {
		List<DaemonSettings> daemons = new ArrayList<DaemonSettings>();
		// rTorrent
		daemons.add(new DaemonSettings(getName(), Daemon.rTorrent, getServer(), RTORRENT_PORT, true, true, null,
				RTORRENT_FOLDER_PART + getUsername(), true, getUsername(), getPassword(), null, OS.Linux,
				"/rtorrent/downloads/", "ftp://" + getName() + "@" + getServer() + FTP_PORT + "/rtorrents/downloads/",
				getPassword(), HttpHelper.DEFAULT_CONNECTION_TIMEOUT, shouldAlarmOnFinishedDownload(),
				shouldAlarmOnNewTorrent(), "" + startID++, true));
		return daemons;
	}

}
