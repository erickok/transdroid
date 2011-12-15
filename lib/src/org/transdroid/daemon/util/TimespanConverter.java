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
package org.transdroid.daemon.util;

/**
 * Quick and dirty time calculations helper.
 * 
 * @author erickok
 *
 */
public class TimespanConverter {

	private final static int ONE_MINUTE = 60;
	private final static int ONE_HOUR = 60 * 60;
	private final static int ONE_DAY = 60 * 60 * 24;
	
	/**
	 * Returns a nicely formatted string of days, hours, minutes and seconds
	 * @param from The number of input seconds to convert
	 * @return A formatted string with separate days, hours, minutes and seconds
	 */
	public static String getTime(int from, boolean inDays) {
		
		// less then ONE_MINUTE left
		if (from < ONE_MINUTE) {
			return String.valueOf(from) + "s";

		// less than ONE_HOUR left
		} else if (from < ONE_HOUR) {
			return (from / ONE_MINUTE) + "m " + (from % ONE_MINUTE) + "s";

		// less than ONE_DAY left
		} else if (from < ONE_DAY) {
			int whole_hours = (from / ONE_HOUR);
			int whole_minutes = (from - (whole_hours * ONE_HOUR)) / ONE_MINUTE;
			int seconds = (from - (whole_hours * ONE_HOUR) - (whole_minutes * ONE_MINUTE));
			return whole_hours + "h " + whole_minutes + "m " + seconds + "s";
			
		// over ONE_DAY left
		} else {
			int whole_days = (from / ONE_DAY);
			int whole_hours = (from - (whole_days * ONE_DAY)) / ONE_HOUR;
			int whole_dayshours = (from / ONE_HOUR);
			int whole_minutes = (from - (whole_days * ONE_DAY) - (whole_hours * ONE_HOUR)) / ONE_MINUTE;
			int seconds = (from - (whole_days * ONE_DAY) - (whole_hours * ONE_HOUR) - (whole_minutes * ONE_MINUTE));
			if (inDays) {
				return whole_days + "d " + whole_hours + "h " + whole_minutes + "m " + seconds + "s";
			} else {
				return whole_dayshours + "h " + whole_minutes + "m " + seconds + "s";
			}
		}
	}
	
}
