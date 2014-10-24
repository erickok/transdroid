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

public enum DaemonMethod {
	Retrieve (0),
	AddByUrl (1),
	AddByMagnetUrl (2),
	AddByFile (3),
	Remove (4),
	Pause (5),
	PauseAll (6),
	Resume (7),
	ResumeAll (8),
	Stop (9),
	StopAll (10),
	Start (11),
	StartAll (12),
	GetFileList (13), 
	SetFilePriorities (14),
	SetTransferRates (15),
	SetLabel(16), 
	SetDownloadLocation (17),
	GetTorrentDetails (18), 
	SetTrackers (19), 
	SetAlternativeMode (20),
	GetStats (21),
	ForceRecheck (22);

	private int code;
    private static final Map<Integer,DaemonMethod> lookup  = new HashMap<Integer,DaemonMethod>();

	static {
	    for(DaemonMethod s : EnumSet.allOf(DaemonMethod.class))
	         lookup.put(s.getCode(), s);
	}

	DaemonMethod(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static DaemonMethod getStatus(int code) {
		return lookup.get(code);
	}

}
