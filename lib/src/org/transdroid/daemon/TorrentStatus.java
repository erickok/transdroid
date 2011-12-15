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
 package org.transdroid.daemon;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum TorrentStatus {
	Waiting (1),
	Checking (2),
	Downloading (4),
	Seeding (8),
	Paused (16),
	Queued (32),
	Error (64),
	Unknown (0);
	
	private int code;
    private static final Map<Integer,TorrentStatus> lookup  = new HashMap<Integer,TorrentStatus>();

	static {
	    for(TorrentStatus s : EnumSet.allOf(TorrentStatus.class))
	         lookup.put(s.getCode(), s);
	}

	TorrentStatus(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static TorrentStatus getStatus(int code) {
		return lookup.get(code);
	}

	public int compareStatusCodeTo(TorrentStatus another) {
		return new Integer(this.getCode()).compareTo(new Integer(another.getCode()));
	}
	
}
