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
 package org.transdroid.gui;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MainViewType {
	ShowAll (1),
	OnlyDownloading (2),
	OnlyUploading (3),
	OnlyInactive (4),
	OnlyActive (5);

	private int code;
    private static final Map<Integer,MainViewType> lookup  = new HashMap<Integer,MainViewType>();

	static {
	    for(MainViewType s : EnumSet.allOf(MainViewType.class))
	         lookup.put(s.getCode(), s);
	}

	MainViewType(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static MainViewType getMainViewType(int code) {
		return lookup.get(code);
	}

}
