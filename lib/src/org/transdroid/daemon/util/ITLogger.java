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
 * Interface that should be implemented for any logging 
 * information to get from the daemons. Applications using
 * this library should attach an instance using 
 * <code>TLog.setLogger(ITLogger)</code>
 * 
 * @author erickok
 */
public interface ITLogger {

	/**
	 * Send a DEBUG log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The debug message to log
	 */
	public abstract void d(String self, String msg);

	/**
	 * Send an ERROR log message.
	 * @param self Unique source tag, identifying the part of Transdroid it happens in
	 * @param msg The error message to log
	 */
	public abstract void e(String self, String msg);
	
}
