/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui;

import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;

import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;

import java.util.List;

/**
 * Interface to be implemented by any activity that wants containing fragments to be able to load data and execute commands against a torrent server.
 * @author Eric Kok
 */
public interface TorrentTasksExecutor {
	void resumeTorrent(Torrent torrent);

	void pauseTorrent(Torrent torrent);

	void startTorrent(Torrent torrent, boolean forced);

	void stopTorrent(Torrent torrent);

	void removeTorrent(Torrent torrent, boolean withData);

	void forceRecheckTorrent(Torrent torrent);

	void updateLabel(Torrent torrent, String newLabel);

	void updateTrackers(Torrent torrent, List<String> newTrackers);

	void updateLocation(Torrent torrent, String newLocation);

	void refreshTorrentDetails(Torrent torrent);

	void refreshTorrentFiles(Torrent torrent);

	void updatePriority(Torrent torrent, List<TorrentFile> files, Priority priority);
}
