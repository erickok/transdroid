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

import org.transdroid.daemon.DaemonMethod;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;

import android.os.Bundle;

public class StartTask extends DaemonTask {
	protected StartTask(IDaemonAdapter adapter, Torrent targetTorrent, Bundle data) {
		super(adapter, DaemonMethod.Start, targetTorrent, data);
	}
	public static StartTask create(IDaemonAdapter adapter, Torrent targetTorrent, boolean forceStart) {
		Bundle data = new Bundle();
		data.putBoolean("FORCED", forceStart);
		return new StartTask(adapter, targetTorrent, data);
	}
	public boolean isForced() {
		return extras.getBoolean("FORCED");
	}
}
