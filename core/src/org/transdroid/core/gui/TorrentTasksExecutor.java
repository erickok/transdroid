package org.transdroid.core.gui;

import java.util.List;

import org.transdroid.daemon.Torrent;

public interface TorrentTasksExecutor {
	void resumeTorrent(Torrent torrent);
	void pauseTorrent(Torrent torrent);
	void startTorrent(Torrent torrent, boolean forced);
	void stopTorrent(Torrent torrent);
	void removeTorrent(Torrent torrent, boolean withData);
	void updateLabel(Torrent torrent, String newLabel);
	void updateTrackers(Torrent torrent, List<String> newTrackers);
	void updateLocation(Torrent torrent, String newLocation);
	void refreshTorrentDetails(Torrent torrent);
	void refreshTorrentFiles(Torrent torrent);
}