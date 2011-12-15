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

/**
 * Represents a file or torrent priority.
 * 
 * @author erickok
 *
 */
public enum Priority {
	
	Off (0),
	Low (1),
	Normal (2),
	High (3);

	private int code;
    private static final Map<Integer,Priority> lookup  = new HashMap<Integer,Priority>();

	static {
	    for(Priority s : EnumSet.allOf(Priority.class))
	         lookup.put(s.getCode(), s);
	}

	Priority(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	public static Priority getPriority(int code) {
		return lookup.get(code);
	}

	public int comparePriorityTo(Priority another) {
		return new Integer(this.getCode()).compareTo(new Integer(another.getCode()));
	}
	
}
