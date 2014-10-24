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
package org.transdroid.daemon.task;

import java.util.ArrayList;

import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;

import android.os.Bundle;

public class SetFilePriorityTask extends DaemonTask {
	protected SetFilePriorityTask(IDaemonAdapter adapter, Torrent targetTorrent, Bundle data) {
		super(adapter, DaemonMethod.SetFilePriorities, targetTorrent, data);
	}
	public static SetFilePriorityTask create(IDaemonAdapter adapter, Torrent targetTorrent, Priority newPriority, ArrayList<TorrentFile> forFiles) {
		Bundle data = new Bundle();
		data.putInt("NEW_PRIORITY", newPriority.getCode());
		data.putParcelableArrayList("FOR_FILES", forFiles);
		return new SetFilePriorityTask(adapter, targetTorrent, data);
	}
	public static SetFilePriorityTask create(IDaemonAdapter adapter, Torrent targetTorrent, Priority newPriority, TorrentFile forFile) {
		ArrayList<TorrentFile> forFiles = new ArrayList<TorrentFile>();
		forFiles.add(forFile);
		return create(adapter, targetTorrent, newPriority, forFiles);
	}
	public Priority getNewPriority() {
		return Priority.getPriority(extras.getInt("NEW_PRIORITY"));
	}
	public ArrayList<TorrentFile> getForFiles() {
		return extras.getParcelableArrayList("FOR_FILES");
	}
}