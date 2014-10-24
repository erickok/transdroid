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
 * Quick and dirty file size formatter.
 * @author erickok
 */
public class FileSizeConverter {

	private static final String DECIMAL_FORMATTER = "%.1f";

	/**
	 * A quantity in which to express a file size.
	 * @author erickok
	 */
	public enum SizeUnit {
		B, KB, MB, GB
	}

	private static int INC_SIZE = 1024;

	/**
	 * Returns a file size (in bytes) in a different unit, as a formatted string
	 * @param from The file size in bytes
	 * @param to The unit to convert to
	 * @return A formatted string with number (rounded to one decimal) and unit, e.g. 1177.4MB
	 */
	public static String getSize(long from, SizeUnit to) {
		String out;
		switch (to) {
		case B:
			out = String.valueOf(from);
			break;
		case KB:
			out = String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE);
			break;
		case MB:
			out = String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE / INC_SIZE);
			break;
		default:
			out = String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE / INC_SIZE / INC_SIZE);
			break;
		}

		return (out + " " + to.toString());
	}

	/**
	 * Returns a file size as nice readable string, with unit, e.g. 1234567890 (bytes) returns 1,15GB
	 * @param from The file size in bytes
	 * @return A formatted string with number (rounded to one decimal), with unit text
	 */
	public static String getSize(long from) {
		return getSize(from, true);
	}

	// Returns a file size in bytes in a nice readable formatted string
	/**
	 * Returns a file size as nice readable string, e.g. 1234567890 (bytes) returns 1,15 or 1,15GB
	 * @param from The file size in bytes
	 * @param withUnit Whether to also append the appropriate unit (B, KB, MB, GB) as text
	 * @return A formatted string with number (rounded to one decimal) and optionally unit
	 */
	public static String getSize(long from, boolean withUnit) {
		if (from < INC_SIZE) {
			return String.valueOf(from) + (withUnit ? SizeUnit.B.toString() : "");
		} else if (from < (INC_SIZE * INC_SIZE)) {
			return String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE)
					+ (withUnit ? SizeUnit.KB.toString() : "");
		} else if (from < (INC_SIZE * INC_SIZE * INC_SIZE)) {
			return String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE / INC_SIZE)
					+ (withUnit ? SizeUnit.MB.toString() : "");
		} else {
			return String.format(DECIMAL_FORMATTER, ((double) from) / INC_SIZE / INC_SIZE / INC_SIZE)
					+ (withUnit ? SizeUnit.GB.toString() : "");
		}
	}

	/**
	 * Returns the unit to display some file size (as returned by getSize(long)) in, e.g. 1234567890 (bytes) returns GB
	 * as it is 1.2GB big
	 * @param from The file size in bytes
	 * @return The unit, i.e. B, KB, MB or GB
	 */
	public static SizeUnit getSizeUnit(long from) {
		if (from < INC_SIZE) {
			return SizeUnit.B;
		} else if (from < (INC_SIZE * INC_SIZE)) {
			return SizeUnit.KB;
		} else if (from < (INC_SIZE * INC_SIZE * INC_SIZE)) {
			return SizeUnit.MB;
		} else {
			return SizeUnit.GB;
		}
	}

}
