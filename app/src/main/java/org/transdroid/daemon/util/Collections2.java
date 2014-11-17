package org.transdroid.daemon.util;

/**
 * Helpers on Collections
 */
public class Collections2 {

	/**
	 * Create a String from an iterable with a separator. Exemple: mkString({1,2,3,4}, ":" => "1:2:3:4"
	 */
	public static <T> String joinString(Iterable<T> iterable, String separator) {
		boolean first = true;
		String result = "";
		for (T anIterable : iterable) {
			result += (first ? "" : separator) + anIterable.toString();
			first = false;
		}
		return result;
	}

}
