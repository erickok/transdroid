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
 * Universal logger; applications using this library should 
 * attach an ITLogger using <code>setLogger(ITLogger)</code>
 * to receive any logging information from the daemons.
 * 
 * @author erickok
 */
public class DLog {

	private static final String LOG_TAG = "Transdroid";
	
	private static ITLogger instance = null;

	public static void setLogger(ITLogger logger) {
		instance = logger;
	}
	
	/**
	 * Send a DEBUG log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The debug message to log
	 */
	public static void d(String self, String msg) {
		if (instance != null) {
			instance.d(LOG_TAG, self + ": " + msg);
		}
	}

	/**
	 * Send an ERROR log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The error message to log
	 */
	public static void e(String self, String msg) {
		if (instance != null) {
			instance.e(LOG_TAG, self + ": " + msg);
		}
	}
	
}
